package com.metaweb.gridworks.templating;

import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.gel.ast.FieldAccessorExpr;
import com.metaweb.gridworks.gel.ast.VariableExpr;

public class Parser {
	static public Template parse(String s) throws ParsingException {
		List<Fragment> fragments = new ArrayList<Fragment>();
		
		int start = 0, current = 0;
		while (current < s.length() - 1) {
			char c = s.charAt(current);
			if (c == '\\') {
				current += 2;
				continue;
			}
			
			char c2 = s.charAt(current + 1);
			if (c == '$' && c2 == '{') {
				int closeBrace = s.indexOf('}', current + 2);
				if (closeBrace > current + 1) {
					String columnName = s.substring(current + 2, closeBrace);
					
					if (current > start) {
						fragments.add(new StaticFragment(s.substring(start, current)));
					}
					start = current = closeBrace + 1;
					
					fragments.add(
						new DynamicFragment(
							new FieldAccessorExpr(
								new FieldAccessorExpr(
									new VariableExpr("cells"), 
									columnName), 
								"value")));
					
					continue;
				}
			} else if (c == '{' && c2 == '{') {
				int closeBrace = s.indexOf('}', current + 2);
				if (closeBrace > current + 1 && closeBrace < s.length() - 1 && s.charAt(closeBrace + 1) == '}') {
					String expression = s.substring(current + 2, closeBrace);
					
					if (current > start) {
						fragments.add(new StaticFragment(s.substring(start, current)));
					}
					start = current = closeBrace + 2;
					
					fragments.add(
						new DynamicFragment(
							MetaParser.parse(expression)));

					continue;
				}
			}
			
			current++;
		}
		
		if (start < s.length()) {
			fragments.add(new StaticFragment(s.substring(start)));
		}
		
		return new Template(fragments);
	}
}
