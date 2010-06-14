function init() {
    Packages.java.lang.System.err.println("Initializing jython extension");
    
    Packages.com.metaweb.gridworks.expr.MetaParser.registerLanguageParser(
        "jython",
        "Jython",
        Packages.com.metaweb.gridworks.jython.JythonEvaluable.createParser(),
        "return value"
    );
}
