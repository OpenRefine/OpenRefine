package com.google.refine.beam.operations;

import com.google.api.services.bigquery.model.TableRow;

import java.util.regex.Pattern;

public class ExpressionHandler {

  private static final String GREL = "grel:";

  public static Object evaluateExp(TableRow row, String srcColumn, String expression) {
    if (expression.startsWith(GREL)) {
      expression = expression.substring(GREL.length());
    }
    // TODO use grel expression handling
    final StringBuilder ret = new StringBuilder();
    final String[] splits = expression.split(Pattern.quote("+"));
    for (String split : splits) {
      split = split.trim();
      if (split.equals("value")) {
        final Object fieldVal = row.get(srcColumn);
        split = fieldVal == null ? "" : fieldVal.toString();
      }
      // } else if (split.startsWith("cells")) {
      // Matcher matcher = cellValuePattern.matcher(split);
      // if (matcher.find()) {
      // final Object fieldVal = row.getField(cachedColumnPos.get(matcher.group(1)));
      // split = fieldVal == null ? "" : fieldVal.toString();
      // } else {
      // throw new IllegalArgumentException("Expression not supported yet: " + expression);
      // }
      // } else {
      split = split.replace("\"", "");
      // }
      ret.append(split);
    }
    return ret.toString();
  }
}
