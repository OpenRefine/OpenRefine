package com.metaweb.gridworks.expr;

import java.util.ArrayList;
import java.util.Properties;

public class HasFieldsListImpl extends ArrayList<HasFields> implements HasFieldsList {
    private static final long serialVersionUID = -8635194387420305802L;

    public Object getField(String name, Properties bindings) {
        int c = size();
        if (c > 0 && get(0).fieldAlsoHasFields(name)) {
            HasFieldsListImpl l = new HasFieldsListImpl();
            for (int i = 0; i < size(); i++) {
                l.add(i, (HasFields) this.get(i).getField(name, bindings));
            }
            return l;
        } else {
            Object[] r = new Object[this.size()];
            for (int i = 0; i < r.length; i++) {
                r[i] = this.get(i).getField(name, bindings);
            }
            return r;
        }
    }

    public int length() {
        return size();
    }

    public boolean fieldAlsoHasFields(String name) {
        int c = size();
        return (c > 0 && get(0).fieldAlsoHasFields(name));
    }
}
