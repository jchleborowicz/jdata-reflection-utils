package pl.jdata.utils.reflection;

import com.google.common.collect.Sets;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class ObjectConstructionGenerator {

    private static final Map<Class, Supplier<String>> SIMPLE_VALUE_GENERATORS = new HashMap<>();
    private static final Set<String> IGNORED_PROPERTY_NAMES = Sets.newHashSet("class", "bytes", "declaringClass");

    private static final Random RANDOM = new Random();

    private final Class targetClass;

    private final Map<String, Integer> usedVariableNames = new HashMap<>();

    private static Date generatedDate = new Date();

    private final GeneratorOutputPrinter outputPrinter = new GeneratorOutputPrinter();

    static {
        SIMPLE_VALUE_GENERATORS.put(UUID.class, () -> "UUID.fromString(" + toJavaString(UUID.randomUUID()) + ")");
        SIMPLE_VALUE_GENERATORS.put(String.class, () -> toJavaString(RandomStringUtils.randomAlphabetic(5)));
        SIMPLE_VALUE_GENERATORS.put(Integer.class, () -> Integer.toString(RANDOM.nextInt(100)));
        SIMPLE_VALUE_GENERATORS.put(Integer.TYPE, () -> Integer.toString(RANDOM.nextInt(100)));
        SIMPLE_VALUE_GENERATORS.put(Long.class, () -> RANDOM.nextInt(100) + "L");
        SIMPLE_VALUE_GENERATORS.put(Long.TYPE, () -> RANDOM.nextInt(100) + "L");
        SIMPLE_VALUE_GENERATORS.put(Date.class, () ->
                "isoDateStringToDate(" + toJavaString(DateUtils.dateToIsoDateString(getNextDate())) + ")");
    }

    private ObjectConstructionGenerator(Class targetClass) {
        this.targetClass = targetClass;
    }

    public static void main(String[] args) {
        generate(String.class);
    }

    public static void generate(Class aClass) {
        final ObjectConstructionGenerator generator = new ObjectConstructionGenerator(aClass);

        try {
            final String result = generator.generate();
            System.out.println(result);
        } catch (Exception e) {
            System.out.println(generator.outputPrinter.toString());
            throw e;
        }
    }

    private String generate() {
        printNewVariable(targetClass, null);

        return outputPrinter.toString();
    }

    private static Date getNextDate() {
        updateGeneratedDate();
        return generatedDate;
    }

    private static void updateGeneratedDate() {
        generatedDate = new Date(generatedDate.getTime() + RANDOM.nextLong() % 1_000_000_000L);
    }

    private String getVariableName(Class aClass) {
        return getVariableName(StringUtils.uncapitalize(aClass.getSimpleName()));
    }

    /**
     * @return variable name
     */
    private String printNewVariable(Class<?> variableType, Type[] actualTypeArguments) {
        final String variableName = getVariableName(variableType);

        if (isSimpleValue(variableType)) {
            final String value = generateSimpleValue(variableType);
            outputPrinter.printAssignment(variableType.getSimpleName(), variableName, value);
        } else if (variableType.getName().startsWith(targetClass.getPackage().getName())) {
            String typeDeclaration = variableType.getSimpleName();
            String value = "new " + variableType.getSimpleName();

            if (actualTypeArguments != null) {
                final String typeArgumentsAsString = Stream.of(actualTypeArguments)
                        .map(typeArgument -> ((Class) typeArgument).getSimpleName())
                        .collect(joining(", "));
                typeDeclaration += "<" + typeArgumentsAsString + ">";
                value += "<>";
            }
            value += "()";

            outputPrinter.printAssignment(typeDeclaration, variableName, value);

            generateSetters(variableName, variableType, actualTypeArguments);
        } else {
            throw new RuntimeException("Unsupported field type: " + variableType);
        }

        return variableName;
    }

    private String getVariableName(String variableBaseName) {
        final Integer i = usedVariableNames.get(variableBaseName);

        if (i == null) {
            usedVariableNames.put(variableBaseName, 1);
            return variableBaseName;
        }

        usedVariableNames.put((variableBaseName), i + 1);
        return variableBaseName + i;
    }

    private void generateSetters(String variableName, Class aClass, Type[] enclosingParameterTypeArguments) {
        final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(aClass);

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            generateValuesForProperty(variableName, aClass, propertyDescriptor, enclosingParameterTypeArguments);
        }
    }

    private void generateValuesForProperty(String variableName, Class enclosingClass,
                                           PropertyDescriptor propertyDescriptor,
                                           Type[] enclosingParameterTypeArguments) {
        try {
            final String propertyName = propertyDescriptor.getName();
            if (IGNORED_PROPERTY_NAMES.contains(propertyName)) {
                return;
            }

            final Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
                throw new RuntimeException("No write method for " + propertyDescriptor);
            }

            final String setterName = writeMethod.getName();

            final Class<?> propertyType = propertyDescriptor.getPropertyType();
            if (propertyType == List.class || propertyType == Set.class) {
                final Class<?> elementType = getFirstTypeParameter(propertyDescriptor.getReadMethod());

                final String collectionVariableName = getVariableName(propertyName);

                outputPrinter.printBlankLine();

                if (propertyType == List.class) {
                    outputPrinter.printListDeclaration(elementType.getSimpleName(), collectionVariableName);
                } else {
                    outputPrinter.printSetDeclaration(elementType.getSimpleName(), collectionVariableName);
                }

                final String generatedValue;
                if (isSimpleValue(elementType)) {
                    generatedValue = generateSimpleValue(elementType);
                } else {
                    generatedValue = printNewVariable(elementType, null);
                }

                outputPrinter.printAddToList(collectionVariableName, generatedValue);
                outputPrinter.printMethodCall(variableName, setterName, collectionVariableName)
                        .printBlankLine();
            } else {
                if (isSimpleValue(propertyType)) {
                    final String generatedValue = generateSimpleValue(propertyType);
                    outputPrinter.printMethodCall(variableName, setterName, generatedValue);
                } else {
                    final Type[] propertyActualParameterTypeArguments = getActualTypeArguments(propertyDescriptor);

                    final Method readMethod = propertyDescriptor.getReadMethod();
                    final Type genericReturnType = readMethod.getGenericReturnType();

                    final Class<?> actualPropertyType = getActualPropertyType(enclosingClass,
                            genericReturnType, enclosingParameterTypeArguments);
                    final String generatedVariableName =
                            printNewVariable(actualPropertyType, propertyActualParameterTypeArguments);

                    outputPrinter.printBlankLine()
                            .printMethodCall(variableName, setterName, generatedVariableName)
                            .printBlankLine();
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Error when generating property: " + propertyDescriptor, e);
        }
    }

    private static Class getActualPropertyType(Class<?> enclosingClass, Type genericReturnType,
                                               Type[] actualTypeArguments) {
        if (genericReturnType instanceof TypeVariable) {
            final TypeVariable typeVariable = (TypeVariable) genericReturnType;
            final String typeParameterName = typeVariable.getName();

            int genericArgumentIndex =
                    getGenericArgumentIndexByName(enclosingClass.getTypeParameters(), typeParameterName);

            return (Class) actualTypeArguments[genericArgumentIndex];
        } else if (genericReturnType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) genericReturnType).getRawType();
        }
        return (Class) genericReturnType;
    }

    private static int getGenericArgumentIndexByName(TypeVariable<? extends Class<?>>[] typeParameters,
                                                     String typeParameterName) {
        for (int i = 0; i < typeParameters.length; i++) {
            TypeVariable<? extends Class<?>> typeParameter = typeParameters[i];
            if (typeParameter.getName().equals(typeParameterName)) {
                return i;
            }
        }
        throw new RuntimeException("Cannot find type parameter " + typeParameterName);
    }

    private static Type[] getActualTypeArguments(PropertyDescriptor propertyDescriptor) {
        final Method readMethod = propertyDescriptor.getReadMethod();
        final Type genericReturnType = readMethod.getGenericReturnType();

        if (genericReturnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            return parameterizedType.getActualTypeArguments();
        } else {
            return null;
        }
    }


    private static boolean isSimpleValue(Class<?> type) {
        return SIMPLE_VALUE_GENERATORS.containsKey(type) || type.isEnum();
    }

    private static String generateSimpleValue(Class<?> type) {
        if (type.isEnum()) {
            return type.getSimpleName() + "." + type.getDeclaredFields()[0].getName();
        }
        return SIMPLE_VALUE_GENERATORS.get(type).get();
    }

    private static Class<?> getFirstTypeParameter(Method method) {
        ParameterizedType pType = (ParameterizedType) method.getGenericReturnType();
        return (Class<?>) pType.getActualTypeArguments()[0];
    }

    private static String toJavaString(Object object) {
        return "\"" + StringEscapeUtils.escapeJava(object.toString()) + "\"";
    }

}

