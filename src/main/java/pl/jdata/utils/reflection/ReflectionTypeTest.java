package pl.jdata.utils.reflection;


import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class ReflectionTypeTest {

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final Class type = String.class;

        final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(type);

        final PropertyDescriptor propertyDescriptor = Stream.of(propertyDescriptors)
                .filter(descriptor -> descriptor.getName().equals("metrics"))
                .findFirst()
                .get();

        describe(propertyDescriptor);
    }

    private static void describe(PropertyDescriptor propertyDescriptor) {
        final Type[] actualTypeArguments = getActualTypeArguments(propertyDescriptor);

        String typeName = propertyDescriptor.getReadMethod().getReturnType().getSimpleName();

        if (actualTypeArguments != null) {
            final String typeArgumentsAsString = Stream.of(actualTypeArguments)
                    .map(typeArgument -> ((Class) typeArgument).getSimpleName())
                    .collect(joining(", "));
            typeName += "<" + typeArgumentsAsString + ">";
        }

        System.out.println(typeName + " " + propertyDescriptor.getName());

        describeProperty(propertyDescriptor, actualTypeArguments);

    }

    private static Type[] getActualTypeArguments(PropertyDescriptor propertyDescriptor) {
        final Method readMethod = propertyDescriptor.getReadMethod();
        final Type genericReturnType = readMethod.getGenericReturnType();

        final Type[] actualTypeArguments;
        if (genericReturnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            actualTypeArguments = parameterizedType.getActualTypeArguments();
        } else {
            actualTypeArguments = null;
        }
        return actualTypeArguments;
    }

    private static void describeProperty(PropertyDescriptor descriptor, Type[] actualTypeArguments) {
        final Class<?> propertyType = descriptor.getPropertyType();

        final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(propertyType);

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (!propertyDescriptor.getName().equals("class")) {
                final Method readMethod = propertyDescriptor.getReadMethod();

                final Type genericReturnType = readMethod.getGenericReturnType();

                final Class actualTypeArgument =
                        getActualPropertyType(propertyType, genericReturnType, actualTypeArguments);

                System.out.println(
                        " * " + actualTypeArgument.getSimpleName() + " " + propertyDescriptor.getName());
            }
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
        } else {
            return (Class) genericReturnType;
        }
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

}

