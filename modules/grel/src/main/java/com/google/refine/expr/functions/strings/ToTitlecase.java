/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.expr.functions.strings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.WordUtils;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class ToTitlecase implements Function {

    final static private char[] delimiters = { ' ', '\t', '\r', '\n', '.' };
    
    // CSL stop words for book-style title casing
    private static final Set<String> CSL_STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "and", "as", "at", "but", "by", "down", "for", "from", "in", 
        "into", "nor", "of", "on", "onto", "or", "over", "so", "the", "till", 
        "to", "up", "via", "with", "yet"
    ));

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            return WordUtils.capitalizeFully(s, delimiters);
        } else if (args.length == 2 && args[0] != null && args[1] != null) {
            Object o1 = args[0];
            String s = o1 instanceof String ? (String) o1 : o1.toString();
            Object o2 = args[1];
            String param2 = o2 instanceof String ? (String) o2 : o2.toString();
            
            // Check if second parameter is a style specification
            if ("book".equalsIgnoreCase(param2) || "csl".equalsIgnoreCase(param2)) {
                return toBookTitlecase(s);
            } else {
                // Treat as delimiter specification (backward compatibility)
                return WordUtils.capitalizeFully(s, param2.toCharArray());
            }
        } else if (args.length == 3 && args[0] != null && args[1] != null && args[2] != null) {
            Object o1 = args[0];
            String s = o1 instanceof String ? (String) o1 : o1.toString();
            Object o2 = args[1];
            String delims = o2 instanceof String ? (String) o2 : o2.toString();
            Object o3 = args[2];
            String style = o3 instanceof String ? (String) o3 : o3.toString();
            
            if ("book".equalsIgnoreCase(style) || "csl".equalsIgnoreCase(style)) {
                return toBookTitlecase(s);
            } else {
                return WordUtils.capitalizeFully(s, delims.toCharArray());
            }
        } else {
            return new EvalError("Function toTitlecase expects 1-3 arguments");
        }
    }
    
    /**
     * Converts string to book-style title case following CSL standards.
     */
    private String toBookTitlecase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // Use regex to find all words and track their positions
        Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
        Matcher matcher = wordPattern.matcher(input);
        
        // First pass: find all words to determine first and last
        String[] words = wordPattern.matcher(input).results()
                .map(matchResult -> matchResult.group())
                .toArray(String[]::new);
        
        if (words.length == 0) {
            return input;
        }
        
        // Second pass: replace each word with the properly cased version
        StringBuffer result = new StringBuffer();
        matcher.reset();
        int wordIndex = 0;
        
        while (matcher.find()) {
            String word = matcher.group();
            boolean isFirst = wordIndex == 0;
            boolean isLast = wordIndex == words.length - 1;
            
            // Check if this word follows punctuation that should trigger capitalization
            int wordStart = matcher.start();
            boolean followsPunctuation = false;
            if (wordStart > 0) {
                String precedingText = input.substring(0, wordStart);
                // Look for colon, semicolon, or other sentence-starting punctuation
                followsPunctuation = precedingText.matches(".*[:.;!?]\\s*$");
            }
            
            String replacement = processWord(word, isFirst, isLast, followsPunctuation);
            matcher.appendReplacement(result, replacement);
            wordIndex++;
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processWord(String word, boolean isFirst, boolean isLast, boolean followsPunctuation) {
        // Determine if this word should be capitalized
        boolean shouldCapitalize = isFirst || isLast || followsPunctuation || !CSL_STOP_WORDS.contains(word.toLowerCase());
        
        if (shouldCapitalize) {
            return capitalize(word);
        } else {
            return word.toLowerCase();
        }
    }
    
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_to_title_case();
    }

    @Override
    public String getParams() {
        return "string s, string delimiters_or_style (optional)";
    }

    @Override
    public String getReturns() {
        return "string";
    }

}
