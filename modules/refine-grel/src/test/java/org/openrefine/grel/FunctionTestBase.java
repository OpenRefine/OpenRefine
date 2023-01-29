
package org.openrefine.grel;

import java.util.Properties;

import org.openrefine.RefineTest;

/**
 * Provides conveniency method to call a function on some arguments.
 * 
 * @author Antonin Delpeuch
 *
 */
public class FunctionTestBase extends RefineTest {

    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    protected static Object invoke(String name, Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (bindings == null) {
            bindings = new Properties();
        }
        if (function == null) {
            throw new IllegalArgumentException("Unknown function " + name);
        }
        if (args == null) {
            return function.call(bindings, new Object[0]);
        } else {
            return function.call(bindings, args);
        }
    }
}
