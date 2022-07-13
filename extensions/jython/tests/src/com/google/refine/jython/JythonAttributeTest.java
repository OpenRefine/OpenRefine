
package com.google.refine.jython;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.HasFields;
import com.google.refine.model.Cell;
import com.google.refine.model.Row;

public class JythonAttributeTest {

    class MyFieldObject implements HasFields {

        @Override
        public Object getField(String name, Properties bindings) {
            if ("sunshine".equals(name)) {
                return "hammock";
            }
            return null;
        }

        @Override
        public boolean fieldAlsoHasFields(String name) {
            return true;
        }

    }

    @Test
    public void testWrappedObjectsHaveAttributes() {
        Row row = new Row(2);
        row.setCell(0, new Cell("sunshine", null));
        row.setCell(1, new Cell("hammock", null));

        Properties props = new Properties();

        MyFieldObject obj = new MyFieldObject();
        JythonHasFieldsWrapper wrapper = new JythonHasFieldsWrapper(obj, props);
        Assert.assertEquals(wrapper.__findattr__("sunshine").toString(), "hammock");

        props.put("cell", obj);

        Evaluable eval = new JythonEvaluable("return cell.sunshine");
        String result = (String) eval.evaluate(props).toString();
        Assert.assertEquals(result, "hammock");
    }
}
