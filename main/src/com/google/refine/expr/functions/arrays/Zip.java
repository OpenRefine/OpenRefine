package com.google.refine.expr.functions.arrays;

import com.google.refine.grel.Function;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Zip implements Function {

    @Override
    public String[] transformMany(String[] values) {
        return transformMany(new ArrayList<>(Arrays.asList(values)));
    }
    @Override
    public String[] transformMany(List<String> values) {
        if (values.size() < 2) {
            return new String[]{"Zip function requires at least two columns."};
        }

        int minLength = Integer.MAX_VALUE;

        for (String value : values) {
            String[] valueArray = value.split(",");
            if (valueArray.length < minLength) {
                minLength = valueArray.length;
            }
        }

        List<String> zippedValues = new ArrayList<>();

        for (int i = 0; i < minLength; i++) {
            List<String> rowValues = new ArrayList<>();

            for (String value : values) {
                String[] valueArray = value.split(",");
                rowValues.add(valueArray[i]);
            }

            zippedValues.add(StringUtils.join(rowValues, ""));
        }

        return zippedValues.toArray(new String[0]);
    }

    @Override
    public Object call(Properties bindings, Object[] args) {
        return null;
    }

    @Override
    public String getDescription() {
        return "Zips multiple arrays by concatenating their elements.";
    }

    @Override
    public String getParams() {
        return Function.super.getParams();
    }

    @Override
    public String getReturns() {
        return "string";
    }
}