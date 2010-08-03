function init() {
    // Packages.java.lang.System.err.println("Initializing jython extension");
    
    Packages.com.google.gridworks.expr.MetaParser.registerLanguageParser(
        "jython",
        "Jython",
        Packages.com.google.gridworks.jython.JythonEvaluable.createParser(),
        "return value"
    );
}
