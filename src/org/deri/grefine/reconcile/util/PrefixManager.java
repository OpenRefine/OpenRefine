package org.deri.grefine.reconcile.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class PrefixManager {
	private Map<String, String> prefixMap = new HashMap<String, String>();

	public PrefixManager(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringTokenizer tokenizer;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			tokenizer = new StringTokenizer(strLine, "\t");
			String prefix = tokenizer.nextToken();
			String uri = tokenizer.nextToken();
			if(!prefixMap.containsKey(uri)){
				prefixMap.put(uri,prefix);
			}
		}
	}

	public String getPrefix(String namespace){
		return prefixMap.get(namespace);
	}
}
