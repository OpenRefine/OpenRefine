package org.openrefine.expr.functions;

import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.openrefine.RefineTest;
import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.Function;

public class FunctionTestBase extends RefineTest {

	protected Properties bindings;
	
    @BeforeMethod
    public void setUpBindings() {
        bindings = new Properties();
    }

    @AfterMethod
    public void tearDownBindings() {
        bindings = null;
    }

	/**
	 * Lookup a control function by name and invoke it with a variable number of args
	 */
	protected Object invoke(String name, Object... args) {
	    // registry uses static initializer, so no need to set it up
	    Function function = ControlFunctionRegistry.getFunction(name);
	    if (function == null) {
	        throw new IllegalArgumentException("Unknown function "+name);
	    }
	    if (args == null) {
	        return function.call(bindings,new Object[0]);
	    } else {
	        return function.call(bindings,args);
	    }
	}

}
