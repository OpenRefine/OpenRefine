function init() {
    // Packages.java.lang.System.err.println("Initializing jython extension");
    
    Packages.com.google.refine.expr.MetaParser.registerLanguageParser(
        "jython",
        "Jython",
        Packages.com.google.refine.jython.JythonEvaluable.createParser(),
        "return value"
    );
}
