package edu.mit.simile.vicino;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.wcohen.ss.api.Token;
import com.wcohen.ss.api.Tokenizer;

public class NGramTokenizer implements Tokenizer {

    private int ngram_size;

    public NGramTokenizer(int ngram_size) {
        this.ngram_size = ngram_size;
    }

    public Token[] tokenize(String str) {
        str = normalize(str);
        List<Token> tokens = new ArrayList<Token>();
        for (int i = 0; i < str.length(); i++) {
            int index = i + ngram_size;
            if (index <= str.length()) {
                tokens.add(intern(str.substring(i,index))); 
            }
        }
        return (Token[]) tokens.toArray(new BasicToken[tokens.size()]);
    }

    static final Pattern extra = Pattern.compile("\\p{Cntrl}|\\p{Punct}");
    static final Pattern whitespace = Pattern.compile("\\p{Space}+");

    private String normalize(String s) {
        s = s.trim();
        s = extra.matcher(s).replaceAll("");
        s = whitespace.matcher(s).replaceAll(" ");
        s = s.toLowerCase();
        return s.intern();
    }
     
    private int nextId = 0;
    private Map<String, Token> tokMap = new TreeMap<String, Token>();
    
    public Token intern(String s) {
        s = s.toLowerCase().intern();
        Token tok = tokMap.get(s);
        if (tok == null) {
            tok = new BasicToken(++nextId, s);
            tokMap.put(s, tok);
        }
        return tok;
    }
    
    public Iterator<Token> tokenIterator() {
        return tokMap.values().iterator();
    }
    
    public int maxTokenIndex() {
        return nextId;
    }
    
    public class BasicToken implements Token, Comparable<Token> {
        private final int index;
        private final String value;
    
        BasicToken(int index, String value) {
            this.index = index;
            this.value = value;
        }
    
        public String getValue() {
            return value;
        }
    
        public int getIndex() {
            return index;
        }
    
        public int compareTo(Token t) {
            return index - t.getIndex();
        }
    
        public int hashCode() {
            return value.hashCode();
        }
    
        public String toString() {
            return "[token#" + getIndex() + ":" + getValue() + "]";
        }
    }
}
