package org.deri.grefine.reconcile.util;

import java.util.List;

import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class StringUtils {

	/**
	 * @param strings
	 * @param separator
	 * @param perStringPrefix
	 * @param prefi
	 * @param suffix
	 * @return joined string of elements in the list. each element will be prefixed by perStringPrefix, separated by separator
	 * and the total result is prefixed by prefix and suffixed by suffix. Example
	 * join(["str1", "str2"], "> || ", "p=<", "FILTER (", ">)") = "FILTER (p=<str1> || p=<str2>)"
	 */
	public static String join(String[] strings, String separator, String perStringPrefix, String prefix, String suffix){
		if(strings==null || strings.length==0 || (strings.length==1 && strings[0].isEmpty())){
			return "";
		}
		StringBuilder builder = new StringBuilder(prefix);
		int sizeMinusOne = strings.length -1;
		for(int i=0; i<sizeMinusOne; i+=1){
			String s = strings[i];
			builder.append(perStringPrefix).append(s).append(separator);
		}
		//the last element without the separator
		builder.append(perStringPrefix);
		builder.append(strings[sizeMinusOne]);
		
		builder.append(suffix);
		
		return builder.toString();
	}
	
	/**
	 * @param strings
	 * @param separator
	 * @param perStringPrefix
	 * @param prefi
	 * @param suffix
	 * @return joined string of elements in the list. each element will be prefixed by perStringPrefix, separated by separator
	 * and the total result is prefixed by prefix and suffixed by suffix. Example
	 * join(["str1", "str2"], "> || ", "p=<", "FILTER (", ">)") = "FILTER (p=<str1> || p=<str2>)"
	 */
	public static String join(List<String> strings, String separator, String perStringPrefix, String prefix, String suffix){
		return StringUtils.join(strings.toArray(new String[]{}), separator, perStringPrefix, prefix, suffix); 
	}
	
	/**
	 * @param str1
	 * @param str2
	 * @return similarity between str1 and str2. current implementation uses Levenshtein 
	 */
	public static double getLevenshteinScore(String str1, String str2){
		return new Levenshtein().getSimilarity(str1.toLowerCase(), str2.toLowerCase());
	}
}
