package pl.jdata.utils.reflection;

@SuppressWarnings("UnusedReturnValue")
public class GeneratorOutputPrinter {

    private StringBuilder output = new StringBuilder();

    private boolean emptyLinePrinted;

    public GeneratorOutputPrinter printAssignment(String typeName, String variableName, String value) {
        this.emptyLinePrinted = false;
        output.append(String.format("    final %s %s = %s;\n", typeName, variableName, value));
        return this;
    }

    public GeneratorOutputPrinter printListDeclaration(String elementType, String variableName) {
        printAssignment("List<" + elementType + ">", variableName, "new ArrayList<>()");
        return this;
    }

    public GeneratorOutputPrinter printSetDeclaration(String elementType, String variableName) {
        printAssignment("Set<" + elementType + ">", variableName, "new HashSet<>()");
        return this;
    }

    public GeneratorOutputPrinter printAddToList(String listVariable, String value) {
        this.emptyLinePrinted = false;
        output.append(String.format("    %s.add(%s);\n", listVariable, value));
        return this;
    }

    public GeneratorOutputPrinter printMethodCall(String variableName, String methodName, String value) {
        this.emptyLinePrinted = false;
        output.append(String.format("    %s.%s(%s);\n", variableName, methodName, value));
        return this;
    }

    public GeneratorOutputPrinter printBlankLine() {
        if (!emptyLinePrinted) {
            output.append("\n");
        }
        emptyLinePrinted = true;
        return this;
    }

    @Override
    public String toString() {
        return output.toString();
    }
}
