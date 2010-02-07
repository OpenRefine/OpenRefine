package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class ToTitlecase implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] != null) {
			Object o = args[0];
			String s = o instanceof String ? (String) o : o.toString();
			String[] words = s.split("\\s+");
			
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				if (word.length() > 0) {
					if (sb.length() > 0) {
						sb.append(' ');
					}
					sb.append(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
				}
			}
			
			return sb.toString();
		}
		return null;
	}

}
