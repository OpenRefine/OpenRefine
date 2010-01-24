package org.json;

/*
Copyright (c) 2008 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.util.Iterator;


/**
 * This provides static methods to convert an XML text into a JSONObject,
 * and to covert a JSONObject into an XML text.
 * @author JSON.org
 * @version 2
 */
public class JSONML {
	
    /**
     * Parse XML values and store them in a JSONArray.
     * @param x       The XMLTokener containing the source string.
     * @param ja      The JSONArray that is containing the current tag or null
     *     if we are at the outermost level.
     * @return A JSONArray if the value is the outermost tag, otherwise null.
     * @throws JSONException
     */
    private static JSONArray parse(XMLTokener x, JSONArray ja) throws JSONException {
        char       c;
        int        i;
        String     s;
        Object     t;
        
// Test for and skip past these forms:
//      <!-- ... -->
//      <!   ...   >
//      <![  ... ]]>
//      <?   ...  ?>
// Report errors for these forms:
//      <>
//      <=
//      <<
        
        while (true) {
        	t = x.nextContent();
    		if (t == XML.LT) {
    			t = x.nextToken();
                if (t instanceof Character) {
        		
// <!
        	
			        if (t == XML.BANG) {
			            c = x.next();
			            if (c == '-') {
			                if (x.next() == '-') {
			                    x.skipPast("-->");
			                }
			                x.back();
			            } else if (c == '[') {
			                t = x.nextToken();
			                if (t.equals("CDATA") && x.next() == '[') {
			                    x.nextCDATA();
			                } else {
			                	throw x.syntaxError("Expected 'CDATA['");
			                }
			            } else {
				            i = 1;
				            do {
				                t = x.nextMeta();
				                if (t == null) {
				                    throw x.syntaxError("Missing '>' after '<!'.");
				                } else if (t == XML.LT) {
				                    i += 1;
				                } else if (t == XML.GT) {
				                    i -= 1;
				                }
				            } while (i > 0);
			            }
			        } else if (t == XML.QUEST) {

// <?

			        	x.skipPast("?>");
			        } else if (t == XML.SLASH) {

// Close tag </

			        	t = x.nextToken();
			            if (ja == null) {
			                throw x.syntaxError("Mismatched close tag '" + t + "'");
			            }            
			            if (!t.equals(ja.get(0))) {
			                throw x.syntaxError("Mismatched '" + ja.get(0) + "' and '" + t + "'");
			            }
			            if (x.nextToken() != XML.GT) {
			                throw x.syntaxError("Misshaped close tag");
			            }
			            return null;
			        } else {
			            throw x.syntaxError("Misshaped tag");
			        }

// Open tag <

		        } else {
		            JSONArray newja = new JSONArray();
		            JSONObject attributes = new JSONObject();
		            if (ja != null) {
		            	ja.put(newja);
		            }
		            newja.put(t);
		            t = null;
		            for (;;) {
		                if (t == null) {
		                    t = x.nextToken();
		                }
		                if (t == null) {
		                	throw x.syntaxError("Misshaped tag");
		                }
		                if (!(t instanceof String)) {
		                	break;
		                }

//		              attribute = value

	                    s = (String)t;
	                    t = x.nextToken();
	                    if (t == XML.EQ) {
	                        t = x.nextToken();
	                        if (!(t instanceof String)) {
	                            throw x.syntaxError("Missing value");
	                        }
	                        attributes.accumulate(s, t);
	                        t = null;
	                    } else {
	                    	attributes.accumulate(s, "");
	                    }
		            }
                    if (attributes.length() > 0) {
                    	newja.put(attributes);
                    }

// Empty tag <.../>

	                if (t == XML.SLASH) {
	                    if (x.nextToken() != XML.GT) {
	                        throw x.syntaxError("Misshaped tag");
	                    }
	                    if (ja == null) {
	                    	return newja;
	                    }

// Content, between <...> and </...>

	                } else if (t == XML.GT) {
	                	parse(x, newja);
	                	if (ja == null) {
	                		return newja;
	                	}
	                } else {
	                    throw x.syntaxError("Misshaped tag");
	                }
	            }
		    } else {
		    	if (ja != null) {
		    		ja.put(t);
		    	}
		    }
        }
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONArray using the JsonML transform. Each XML tag is represented as
     * a JSONArray in which the first element is the tag name. If the tag has
     * properties, then the second element will be JSONObject containing the
     * name/value pairs. If the tag contains children, then strings and
     * JSONArrays will represent the child tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     * @param string The source string.
     * @return A JSONArray containing the structured data from the XML string.
     * @throws JSONException
     */
    public static JSONArray toJSONArray(String string) throws JSONException {
    	return toJSONArray(new XMLTokener(string));
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONArray using the JsonML transform. Each XML tag is represented as
     * a JSONArray in which the first element is the tag name. If the tag has
     * properties, then the second element will be JSONObject containing the
     * name/value pairs. If the tag contains children, then strings and
     * JSONArrays will represent the child content and tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     * @param x An XMLTokener.
     * @return A JSONArray containing the structured data from the XML string.
     * @throws JSONException
     */
    public static JSONArray toJSONArray(XMLTokener x) throws JSONException {
    	return parse(x, null);
    }


    /**
     * Reverse the JSONML transformation, making an XML text from a JSONArray.
     * @param ja A JSONArray.
     * @param b A string buffer in which to build the text.
     * @throws JSONException
     */
    private static void stringify(JSONArray ja, StringBuffer b)
            throws JSONException {
        int          i;
        JSONObject   jo;
        String       k;
        Iterator     keys;
        int          len;
        Object		 o;
        Object       v;

// Emit <tagName>

         b.append('<');
         b.append(ja.get(0));
         o = ja.opt(1);
         if (o instanceof JSONObject) {

// Loop thru the attributes.

            jo = (JSONObject)o;
            keys = jo.keys();
            while (keys.hasNext()) {
                k = keys.next().toString();
                v = jo.get(k).toString();
                b.append(' ');
                b.append(k);
                b.append("=\"");
                b.append(XML.escape((String)v));
                b.append('"');
            }
            i = 2;
         } else {
        	 i = 1;
         }
         len = ja.length();
         
         if (i >= len) {
        	 b.append("/>");
         } else {
        	 b.append('>');
	         while (i < len) {
	        	 v = ja.get(i);
	        	 if (v instanceof JSONArray) {
	        		 stringify((JSONArray)v, b);
	        	 } else {
	        		 b.append(XML.escape(v.toString()));
	        	 }
	        	 i += 1;
	         }
	         b.append("</");
	         b.append(ja.get(0));
	         b.append('>');
         }
    }
    
    /**
     * Reverse the JSONML transformation, making an XML text from a JSONArray.
     * @param ja A JSONArray.
     * @return An XML string.
     * @throws JSONException
     */
    public static String toString(JSONArray ja) throws JSONException {
    	StringBuffer b = new StringBuffer();
    	stringify(ja, b);
    	return b.toString();
    }
}