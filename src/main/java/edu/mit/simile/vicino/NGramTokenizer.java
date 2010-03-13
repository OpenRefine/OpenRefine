package edu.mit.simile.vicino;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.wcohen.ss.api.Token;
import com.wcohen.ss.api.Tokenizer;
import com.wcohen.ss.tokens.BasicToken;
import com.wcohen.ss.tokens.SimpleTokenizer;

/**
 * Wraps another tokenizer, and adds all computes all ngrams of
 * characters from a single token produced by the inner tokenizer.
 */
public class NGramTokenizer implements Tokenizer {

    private int minNGramSize;
    private int maxNGramSize;
    private boolean keepOldTokens;
    private Tokenizer innerTokenizer; 
    
    public static NGramTokenizer DEFAULT_TOKENIZER = new NGramTokenizer(3,5,true,SimpleTokenizer.DEFAULT_TOKENIZER);

    public NGramTokenizer(int minNGramSize,int maxNGramSize,boolean keepOldTokens,Tokenizer innerTokenizer) {
        this.minNGramSize = minNGramSize;
        this.maxNGramSize = maxNGramSize;
        this.keepOldTokens = keepOldTokens;
        this.innerTokenizer = innerTokenizer;
    }

    public Token[] tokenize(String input) {
        Token[] initialTokens = innerTokenizer.tokenize(input);
        List<Token> tokens = new ArrayList<Token>();
        for (int i = 0; i < initialTokens.length; i++) {
            String str = initialTokens[i].getValue();
            if (keepOldTokens) tokens.add( intern(str) );
            for (int lo = 0; lo < str.length(); lo++) {
                for (int len = minNGramSize; len <= maxNGramSize; len++) {
                    if (lo + len < str.length()) {
                        tokens.add(innerTokenizer.intern(str.substring(lo,lo+len))); 
                    }
                }
            }
        }
        return (Token[]) tokens.toArray(new BasicToken[tokens.size()]);
    }
    
    public Token intern(String s) { 
        return innerTokenizer.intern(s); 
    }
    
    public Iterator<Token> tokenIterator() { 
        return innerTokenizer.tokenIterator(); 
    }

    public int maxTokenIndex() { 
        return innerTokenizer.maxTokenIndex(); 
    }
}
