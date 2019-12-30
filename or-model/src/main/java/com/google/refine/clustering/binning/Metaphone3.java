/*

Copyright 2010, Lawrence Philips
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

/*
 * A request from the author: Please comment and sign any changes you make to 
 * the Metaphone 3 reference implementation.
 * <br>
 * Please do NOT reformat this module to Refine's coding standard,
 * but instead keep the original format so that it can be more easily compared
 * to any modified fork of the original.
 */

/**
 * Metaphone 3<br>
 * VERSION 2.1.3
 * 
 * by Lawrence Philips<br>
 * 
 * Metaphone 3 is designed to return an *approximate* phonetic key (and an alternate
 * approximate phonetic key when appropriate) that should be the same for English
 * words, and most names familiar in the United States, that are pronounced *similarly*.
 * The key value is *not* intended to be an *exact* phonetic, or even phonemic,
 * representation of the word. This is because a certain degree of 'fuzziness' has
 * proven to be useful in compensating for variations in pronunciation, as well as
 * misheard pronunciations. For example, although americans are not usually aware of it,
 * the letter 's' is normally pronounced 'z' at the end of words such as "sounds".<br><br>
 * 
 * The 'approximate' aspect of the encoding is implemented according to the following rules:<br><br>
 * 
 * (1) All vowels are encoded to the same value - 'A'. If the parameter encodeVowels
 * is set to false, only *initial* vowels will be encoded at all. If encodeVowels is set
 * to true, 'A' will be encoded at all places in the word that any vowels are normally
 * pronounced. 'W' as well as 'Y' are treated as vowels. Although there are differences in
 * the pronunciation of 'W' and 'Y' in different circumstances that lead to their being
 * classified as vowels under some circumstances and as consonants in others, for the purposes
 * of the 'fuzziness' component of the Soundex and Metaphone family of algorithms they will
 * be always be treated here as vowels.<br><br>
 *
 * (2) Voiced and un-voiced consonant pairs are mapped to the same encoded value. This
 * means that:<br>
 * 'D' and 'T' -> 'T'<br>
 * 'B' and 'P' -> 'P'<br>
 * 'G' and 'K' -> 'K'<br>
 * 'Z' and 'S' -> 'S'<br>
 * 'V' and 'F' -> 'F'<br><br>
 *
 * - In addition to the above voiced/unvoiced rules, 'CH' and 'SH' -> 'X', where 'X'
 * represents the "-SH-" and "-CH-" sounds in Metaphone 3 encoding.<br><br>
 *
 * - Also, the sound that is spelled as "TH" in English is encoded to '0' (zero symbol). (Although
 * Americans are not usually aware of it, "TH" is pronounced in a voiced (e.g. "that") as
 * well as an unvoiced (e.g. "theater") form, which are naturally mapped to the same encoding.)<br><br>
 * 
 * The encodings in this version of Metaphone 3 are according to pronunciations common in the
 * United States. This means that they will be inaccurate for consonant pronunciations that
 * are different in the United Kingdom, for example "tube" -> "CHOOBE" -> XAP rather than american TAP.<br><br>
 *
 * Metaphone 3 was preceded by by Soundex, patented in 1919, and Metaphone and Double Metaphone,
 * developed by Lawrence Philips. All of these algorithms resulted in a significant number of
 * incorrect encodings. Metaphone3 was tested against a database of about 100 thousand English words,
 * names common in the United States, and non-English words found in publications in the United States,
 * with an emphasis on words that are commonly mispronounced, prepared by the Moby Words website,
 * but with the Moby Words 'phonetic' encodings algorithmically mapped to Double Metaphone encodings.
 * Metaphone3 increases the accuracy of encoding of english words, common names, and non-English
 * words found in american publications from the 89% for Double Metaphone, to over 98%.<br><br>
 *
 * DISCLAIMER:
 * Anthropomorphic Software LLC claims only that Metaphone 3 will return correct encodings,
 * within the 'fuzzy' definition of correct as above, for a very high percentage of correctly
 * spelled English and commonly recognized non-English words. Anthropomorphic Software LLC
 * warns the user that a number of words remain incorrectly encoded, that misspellings may not
 * be encoded 'properly', and that people often have differing ideas about the pronunciation
 * of a word. Therefore, Metaphone 3 is not guaranteed to return correct results every time, and
 * so a desired target word may very well be missed. Creators of commercial products should
 * keep in mind that systems like Metaphone 3 produce a 'best guess' result, and should
 * condition the expectations of end users accordingly.<br><br>
 *
 * METAPHONE3 IS PROVIDED "AS IS" WITHOUT
 * WARRANTY OF ANY KIND. LAWRENCE PHILIPS AND ANTHROPOMORPHIC SOFTWARE LLC
 * MAKE NO WARRANTIES, EXPRESS OR IMPLIED, THAT IT IS FREE OF ERROR,
 * OR ARE CONSISTENT WITH ANY PARTICULAR STANDARD OF MERCHANTABILITY, 
 * OR THAT IT WILL MEET YOUR REQUIREMENTS FOR ANY PARTICULAR APPLICATION.
 * LAWRENCE PHILIPS AND ANTHROPOMORPHIC SOFTWARE LLC DISCLAIM ALL LIABILITY
 * FOR DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES RESULTING FROM USE 
 * OF THIS SOFTWARE. 
 *
 * @author Lawrence Philips
 * 
 * Metaphone 3 is designed to return an <i>approximate</i> phonetic key (and an alternate
 * approximate phonetic key when appropriate) that should be the same for English
 * words, and most names familiar in the United States, that are pronounced "similarly".
 * The key value is <i>not</i> intended to be an exact phonetic, or even phonemic,
 * representation of the word. This is because a certain degree of 'fuzziness' has
 * proven to be useful in compensating for variations in pronunciation, as well as
 * misheard pronunciations. For example, although americans are not usually aware of it,
 * the letter 's' is normally pronounced 'z' at the end of words such as "sounds".<br><br>
 * 
 * The 'approximate' aspect of the encoding is implemented according to the following rules:<br><br>
 * 
 * (1) All vowels are encoded to the same value - 'A'. If the parameter encodeVowels
 * is set to false, only *initial* vowels will be encoded at all. If encodeVowels is set
 * to true, 'A' will be encoded at all places in the word that any vowels are normally
 * pronounced. 'W' as well as 'Y' are treated as vowels. Although there are differences in
 * the pronunciation of 'W' and 'Y' in different circumstances that lead to their being
 * classified as vowels under some circumstances and as consonants in others, for the purposes
 * of the 'fuzziness' component of the Soundex and Metaphone family of algorithms they will
 * be always be treated here as vowels.<br><br>
 *
 * (2) Voiced and un-voiced consonant pairs are mapped to the same encoded value. This
 * means that:<br>
 * 'D' and 'T' -> 'T'<br>
 * 'B' and 'P' -> 'P'<br>
 * 'G' and 'K' -> 'K'<br>
 * 'Z' and 'S' -> 'S'<br>
 * 'V' and 'F' -> 'F'<br><br>
 *
 * - In addition to the above voiced/unvoiced rules, 'CH' and 'SH' -> 'X', where 'X'
 * represents the "-SH-" and "-CH-" sounds in Metaphone 3 encoding.<br><br>
 *
 * - Also, the sound that is spelled as "TH" in English is encoded to '0' (zero symbol). (Although
 * americans are not usually aware of it, "TH" is pronounced in a voiced (e.g. "that") as
 * well as an unvoiced (e.g. "theater") form, which are naturally mapped to the same encoding.)<br><br>
 *
 * In the "Exact" encoding, voiced/unvoiced pairs are <i>not</i> mapped to the same encoding, except
 * for the voiced and unvoiced versions of 'TH', sounds such as 'CH' and 'SH', and for 'S' and 'Z',
 * so that the words whose metaph keys match will in fact be closer in pronunciation that with the
 * more approximate setting. Keep in mind that encoding settings for search strings should always
 * be exactly the same as the encoding settings of the stored metaph keys in your database!
 * Because of the considerably increased accuracy of Metaphone3, it is now possible to use this
 * setting and have a very good chance of getting a correct encoding.
 * <br><br>
 * In the Encode Vowels encoding, all non-initial vowels and diphthongs will be encoded to
 * 'A', and there will only be one such vowel encoding character between any two consonants.
 * It turns out that there are some surprising wrinkles to encoding non-initial vowels in
 * practice, pre-eminently in inversions between spelling and pronunciation such as e.g.
 * "wrinkle" => 'RANKAL', where the last two sounds are inverted when spelled.
 * <br><br>
 * The encodings in this version of Metaphone 3 are according to pronunciations common in the
 * United States. This means that they will be inaccurate for consonant pronunciations that
 * are different in the United Kingdom, for example "tube" -> "CHOOBE" -> XAP rather than american TAP.
 * <br><br>
 * 
 */

package com.google.refine.clustering.binning;

public class Metaphone3 {

    /** Length of word sent in to be encoded, as 
	* measured at beginning of encoding. */
	int  m_length;

    /** Length of encoded key string. */
    int m_metaphLength;

    /** Flag whether or not to encode non-initial vowels. */
    boolean m_encodeVowels;

    /** Flag whether or not to encode consonants as exactly 
	* as possible. */
	boolean m_encodeExact;

	/** Internal copy of word to be encoded, allocated separately
	* from string pointed to in incoming parameter. */
	String m_inWord;
    
	/** Running copy of primary key. */
    StringBuffer m_primary;

    /** Running copy of secondary key. */
    StringBuffer m_secondary;

    /** Index of character in m_inWord currently being
	* encoded. */
    int m_current;

    /** Index of last character in m_inWord. */
	int m_last;

	/** Flag that an AL inversion has already been done. */
	boolean flag_AL_inversion;
	
	/** Default size of key storage allocation */
	int MAX_KEY_ALLOCATION = 32;
	
	/** Default maximum length of encoded key. */
	int DEFAULT_MAX_KEY_LENGTH = 8;
	
	////////////////////////////////////////////////////////////////////////////////
	// Metaphone3 class definition
	////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor, default. This constructor is most convenient when
	 * encoding more than one word at a time. New words to encode can
	 * be set using SetWord(char *).
	 *
	 */
	Metaphone3()
	{
	    m_primary = new StringBuffer();
	    m_secondary = new StringBuffer();

	    m_metaphLength = DEFAULT_MAX_KEY_LENGTH;
	    m_encodeVowels = false;
		m_encodeExact = false;
	}

	/**
	 * Constructor, parameterized. The Metaphone3 object will
	 * be initialized with the incoming string, and can be called
	 * on to encode this string. This constructor is most convenient
	 * when only one word needs to be encoded.
	 * 
	 * @param in pointer to char string of word to be encoded.
	 *
	 */
	Metaphone3(String in)
	{
		this();
		
		SetWord(in);
	}

	/**
	 * Sets word to be encoded.
	 * 
	 * @param in pointer to EXTERNALLY ALLOCATED char string of 
	 * the word to be encoded.
	 *
	 */
	void SetWord(String in)
	{
		m_inWord = in.toUpperCase();;
		m_length = m_inWord.length();
	}

	/**
	 * Sets length allocated for output keys.
	 * If incoming number is greater than maximum allowable 
	 * length returned by GetMaximumKeyLength(), set key length
	 * to maximum key length and return false;  otherwise, set key 
	 * length to parameter value and return true.
	 * 
	 * @param inKeyLength new length of key.
	 * @return true if able to set key length to requested value.
	 *
	*/
	boolean SetKeyLength(int inKeyLength)
	{
	    if(inKeyLength < 1)
		{
			// can't have that -
			// no room for terminating null
			inKeyLength = 1;
		}

		if(inKeyLength > MAX_KEY_ALLOCATION)
	    {
	        m_metaphLength = MAX_KEY_ALLOCATION;
	        return false;
	    }

	     m_metaphLength = inKeyLength;
	     return true;
	}

    /**
     * Adds an encoding character to the encoded key value string - one parameter version.
     * 
     * @param main primary encoding character to be added to encoded key string.
     */
    void MetaphAdd(String in)
    {
    	if(!(in.equals("A") 
    			&& (m_primary.length() > 0) 
    			&& (m_primary.charAt(m_primary.length() - 1) == 'A')))
    	{
    		m_primary.append(in);
    	}
    	
    	if(!(in.equals("A") 
    			&& (m_secondary.length() > 0) 
    			&& (m_secondary.charAt(m_secondary.length() - 1) == 'A')))
    	{
    		m_secondary.append(in);
    	}
    }
    
	/**
	 * Adds an encoding character to the encoded key value string - two parameter version
	 * 
	 * @param main primary encoding character to be added to encoded key string
	 * @param alt alternative encoding character to be added to encoded alternative key string
	 *
	 */
	void MetaphAdd(String main, String alt)
	{
    	if(!(main.equals("A") 
    			&& (m_primary.length() > 0) 
    			&& (m_primary.charAt(m_primary.length() - 1) == 'A')))
    	{
    		m_primary.append(main);
    	}
    	
    	if(!(alt.equals("A") 
    			&& (m_secondary.length() > 0) 
    			&& (m_secondary.charAt(m_secondary.length() - 1) == 'A')))
    	{
			if(!alt.isEmpty())
			{
				m_secondary.append(alt);
			}
    	}
	}

	/**
	 * Adds an encoding character to the encoded key value string - Exact/Approx version
	 * 
	 * @param mainExact primary encoding character to be added to encoded key string if 
	 * m_encodeExact is set
	 *
	 * @param altExact alternative encoding character to be added to encoded alternative 
	 * key string if m_encodeExact is set
	 *
	 * @param main primary encoding character to be added to encoded key string
	 *
	 * @param alt alternative encoding character to be added to encoded alternative key string
	 *
	 */	
	void MetaphAddExactApprox(String mainExact, String altExact, String main, String alt)
	{
		if(m_encodeExact)
		{
			MetaphAdd(mainExact, altExact);
		}
		else
		{
			MetaphAdd(main, alt);
		}
	}

	/**
	 * Adds an encoding character to the encoded key value string - Exact/Approx version
	 * 
	 * @param mainExact primary encoding character to be added to encoded key string if 
	 * m_encodeExact is set
	 *
	 * @param main primary encoding character to be added to encoded key string
	 *
	 */	
	void MetaphAddExactApprox(String mainExact, String main)
	{
		if(m_encodeExact)
		{
			MetaphAdd(mainExact);
		}
		else
		{
			MetaphAdd(main);
		}
	}	 
	/** Retrieves maximum number of characters currently allocated for encoded key. 
	 *
	 * @return short integer representing the length allowed for the key.
	 */
   int GetKeyLength(){return m_metaphLength;}

	/** Retrieves maximum number of characters allowed for encoded key. 
	 *
	 * @return short integer representing the length of allocated storage for the key.
	 */
   int GetMaximumKeyLength(){return (int)MAX_KEY_ALLOCATION;}

	/** Sets flag that causes Metaphone3 to encode non-initial vowels. However, even 
	 * if there are more than one vowel sound in a vowel sequence (i.e. 
	 * vowel diphthong, etc.), only one 'A' will be encoded before the next consonant or the
	 * end of the word.
	 *
	 * @param inEncodeVowels Non-initial vowels encoded if true, not if false. 
	 */
   void SetEncodeVowels(boolean inEncodeVowels){m_encodeVowels = inEncodeVowels;}

	/** Retrieves setting determining whether or not non-initial vowels will be encoded. 
	 *
	 * @return true if the Metaphone3 object has been set to encode non-initial vowels, false if not.
	 */
   boolean GetEncodeVowels(){return m_encodeVowels;}

	/** Sets flag that causes Metaphone3 to encode consonants as exactly as possible.
	 * This does not include 'S' vs. 'Z', since americans will pronounce 'S' at the
	 * at the end of many words as 'Z', nor does it include "CH" vs. "SH". It does cause
	 * a distinction to be made between 'B' and 'P', 'D' and 'T', 'G' and 'K', and 'V'
	 * and 'F'.
	 *
	 * @param inEncodeExact consonants to be encoded "exactly" if true, not if false. 
	 */
   void SetEncodeExact(boolean inEncodeExact){m_encodeExact = inEncodeExact;}

	/** Retrieves setting determining whether or not consonants will be encoded "exactly".
	 *
	 * @return true if the Metaphone3 object has been set to encode "exactly", false if not.
	 */
   boolean GetEncodeExact(){return m_encodeExact;}

	/** Retrieves primary encoded key.
	 *
	 * @return a character pointer to the primary encoded key
	 */
	String GetMetaph()
	{
		String primary = new String(m_primary);
		return primary;
	}

	/** Retrieves alternate encoded key, if any. 
	 *
	 * @return a character pointer to the alternate encoded key
	 */
	String GetAlternateMetaph()
	{
		String secondary = new String(m_secondary);
		return secondary;
	}

	/**
	 * Test for close front vowels
	 *
	 * @return true if close front vowel
	 */
	boolean Front_Vowel(int at)
	{
		if(((CharAt(at) == 'E') || (CharAt(at) == 'I') || (CharAt(at) == 'Y')))
		{
			return true;
		}

		return false;
	}

	/**
	 * Detect names or words that begin with spellings
	 * typical of german or slavic words, for the purpose
	 * of choosing alternate pronunciations correctly
	 *
	 */
	boolean SlavoGermanic()
	{
		if(StringAt(0, 3, "SCH", "")
			|| StringAt(0, 2, "SW", "")
			|| (CharAt(0) == 'J')
			|| (CharAt(0) == 'W'))
		{
			return true;
		}

		return false;
	}	
	/**
	 * Tests if character is a vowel
	 * 
	 * @param inChar character to be tested in string to be encoded
	 * @return true if character is a vowel, false if not
	 *
	 */
	boolean IsVowel(char inChar)
	{
	    if((inChar == 'A') 
			|| (inChar == 'E') 
			|| (inChar == 'I') 
			|| (inChar == 'O') 
			|| (inChar == 'U') 
			|| (inChar == 'Y') 
			|| (inChar == 'À')
			|| (inChar == 'Á')
			|| (inChar == 'Â')
			|| (inChar == 'Ã')
			|| (inChar == 'Ä')
			|| (inChar == 'Å')
			|| (inChar == 'Æ')
			|| (inChar == 'È')
			|| (inChar == 'É')
	 		|| (inChar == 'Ê')
			|| (inChar == 'Ë')
			|| (inChar == 'Ì')
			|| (inChar == 'Í')
			|| (inChar == 'Î')
	 		|| (inChar == 'Ï')
			|| (inChar == 'Ò')
			|| (inChar == 'Ó')
			|| (inChar == 'Ô')
			|| (inChar == 'Õ')
			|| (inChar == 'Ö')
			|| (inChar == '')
			|| (inChar == 'Ø')
			|| (inChar == 'Ù')
			|| (inChar == 'Ú')
			|| (inChar == 'Û')
			|| (inChar == 'Ü')
			|| (inChar == 'Ý')
			|| (inChar == ''))
		{
	        return true;
		}

	    return false;
	}

	/**
	 * Tests if character in the input string is a vowel
	 * 
	 * @param at position of character to be tested in string to be encoded
	 * @return true if character is a vowel, false if not
	 *
	 */
	boolean IsVowel(int at)
	{
	    if((at < 0) || (at >= m_length))
		{
	        return false;
		}

	    char it = CharAt(at);

	    if(IsVowel(it))
		{
	        return true;
		}

	    return false;
	}

	/**
	 * Skips over vowels in a string. Has exceptions for skipping consonants that
	 * will not be encoded.
	 *
	 * @param at position, in string to be encoded, of character to start skipping from
	 *
	 * @return position of next consonant in string to be encoded 
	 */
	int SkipVowels(int at)
	{
	    if(at < 0)
		{
	        return 0;
		}

	    if(at >= m_length)
		{
	        return m_length;
		}

	    char it = CharAt(at);

	    while(IsVowel(it) || (it == 'W'))
	    {
	        if(StringAt(at, 4, "WICZ", "WITZ", "WIAK", "")
				|| StringAt((at - 1), 5, "EWSKI", "EWSKY", "OWSKI", "OWSKY", "")
				|| (StringAt(at, 5, "WICKI", "WACKI", "") && ((at + 4) == m_last)))
	        {
	            break;
	        }

	        at++;
	        if(((CharAt(at - 1) == 'W') && (CharAt(at) == 'H'))
	            && !(StringAt(at, 3, "HOP", "")
	                  || StringAt(at, 4, "HIDE", "HARD", "HEAD", "HAWK", "HERD", "HOOK", "HAND", "HOLE", "")
	                  || StringAt(at, 5, "HEART", "HOUSE", "HOUND", "")
	                  || StringAt(at, 6, "HAMMER", "")))
			{
	            at++;
			}
	        
	        if(at > (m_length - 1))
	        {
	        	break;
	        }
	        it = CharAt(at);
	    }

	    return at;
	}

	/**
	 * Advanced counter m_current so that it indexes the next character to be encoded
	 *
	 * @param ifNotEncodeVowels number of characters to advance if not encoding internal vowels
	 * @param ifEncodeVowels number of characters to advance if encoding internal vowels
	 *
	 */
	void AdvanceCounter(int ifNotEncodeVowels, int ifEncodeVowels)
	{
		if(!m_encodeVowels)
		{
			m_current += ifNotEncodeVowels;
		}
		else
		{
			m_current += ifEncodeVowels;
		}
	}
	
	
    /**
     * Subscript safe .charAt()
     * 
	 * @param at index of character to access
	 * @return null if index out of bounds, .charAt() otherwise
	 */
	char CharAt(int at)
	{
	    // check substring bounds
		if((at < 0)
	    	|| (at > (m_length - 1)))
	    {
	    	return '\0';
	    }

	    return m_inWord.charAt(at);
	}
	
	/**
	 * Tests whether the word is the root or a regular english inflection
	 * of it, e.g. "ache", "achy", "aches", "ached", "aching", "achingly"
	 * This is for cases where we want to match only the root and corresponding
	 * inflected forms, and not completely different words which may have the
	 * same substring in them.
	 */
	boolean RootOrInflections(String inWord, String root)
	{
		int len = root.length();
		String test;

		test = root + "S";
		if((inWord.equals(root))
				|| (inWord.equals(test)))
		{
			return true;
		}

		if(root.charAt(len - 1) != 'E')
		{
			test = root + "ES";
		}
		
		if(inWord.equals(test))
		{
			return true;
		}

		if(root.charAt(len - 1) != 'E')
		{
			test = root + "ED";
		}
		else
		{
			test = root + "D";
		}
		
		if(inWord.equals(test))
		{
			return true;
		}

		if(root.charAt(len - 1) == 'E')
		{
			root = root.substring(0, len - 1);
		}

		test = root + "ING";
		if(inWord.equals(test))
		{
			return true;
		}

		test = root + "INGLY";
		if(inWord.equals(test))
		{
			return true;
		}

		test = root + "Y";
		if(inWord.equals(test))
		{
			return true;
		}

		return false;
	}
	
    /**
	 * Determines if one of the substrings sent in is the same as
	 * what is at the specified position in the string being encoded.
	 * 
	 * @param start
	 * @param length
	 * @param compareStrings
	 * @return
	 */
	boolean StringAt(int start, int length, String... compareStrings)
	{
	    // check substring bounds
		if((start < 0)
	    	|| (start > (m_length - 1))
	    	|| ((start + length - 1) > (m_length - 1)))
	    {
	    	return false;
	    }

	    String target = m_inWord.substring(start, (start + length));
	      
		for(String strFragment : compareStrings)
		{
			if(target.equals(strFragment))
			{
				return true;
			}			
		}
	    return false;
	}
	
	/**
	 * Encodes input string to one or two key values according to Metaphone 3 rules.
	 *
	 */
	void Encode()
	{
	    flag_AL_inversion = false;

	    m_current = 0;

		m_primary.setLength(0);
	    m_secondary.setLength(0);

	    if(m_length < 1)
		{
	        return;
		}

	    //zero based index
		m_last = m_length - 1;

	    ///////////main loop//////////////////////////
		while(!(m_primary.length() > m_metaphLength) && !(m_secondary.length() > m_metaphLength))
	    {
 	        if(m_current >= m_length)
			{
	            break;
			}

 	        switch(CharAt(m_current))
	        {	                        
	            case 'B':

					Encode_B();
	                break;
	                        
	            case 'ß':
				case 'Ç':

	                MetaphAdd("S");
	                m_current++;
	                break;

	            case 'C':

					Encode_C();
	                break;

	            case 'D':

					Encode_D();
	                break;

	            case 'F':
	                    
					Encode_F();
	                break;

	            case 'G':

					Encode_G();
	                break;

	            case 'H':

					Encode_H();
	                break;
	                
	            case 'J':

					Encode_J();
	                break;

	            case 'K':

					Encode_K();
	                break;

	            case 'L':

					Encode_L();
	                break;

	            case 'M':

					Encode_M();
	                break;

	            case 'N':

					Encode_N();
	                break;

	            case 'Ñ':
	                
	                MetaphAdd("N");
	                m_current++;
	                break;

	            case 'P':

					Encode_P();
	                break;

	            case 'Q':

					Encode_Q();
	                break;

	            case 'R':

					Encode_R();
					break;

	            case 'S':

					Encode_S();
	                break;

	            case 'T':

					Encode_T();
	                break;

	            case 'Ð': // eth
				case 'Þ': // thorn

	                MetaphAdd("0");
	                m_current++;
	                break;

	           case 'V':

					Encode_V();
	                break;

	            case 'W':

					Encode_W();
	                break;

	            case 'X':

					Encode_X();
	                break;

	            case '':

	                MetaphAdd("X");
	                m_current++;
	                break;

				case '':

	                MetaphAdd("S");
	                m_current++;
	                break;

	            case 'Z':
					
	                Encode_Z();
	                break;

	            default:
	            	
					if(IsVowel(CharAt(m_current)))
					{
						Encode_Vowels();
						break;
					}
	            
	                m_current++;
	                
	        }
	    }

	    //only give back m_metaphLength number of chars in m_metaph
		if(m_primary.length() > m_metaphLength)
	    {
			m_primary.setLength(m_metaphLength);
	    }

		if(m_secondary.length() > m_metaphLength)
	    {
			m_secondary.setLength(m_metaphLength);
	    }

		// it is possible for the two metaphs to be the same 
		// after truncation. lose the second one if so
		if((m_primary.toString()).equals(m_secondary.toString()))
		{
			m_secondary.setLength(0);
		}
	}

	/**
	 * Encodes all initial vowels to A.
	 *
	 * Encodes non-initial vowels to A if m_encodeVowels is true
	 * 
	 * 
	*/
	void Encode_Vowels()
	{
		if(m_current == 0)
		{
			// all init vowels map to 'A' 
			// as of Double Metaphone
			MetaphAdd("A"); 
		}
		else if(m_encodeVowels)
		{
			if(CharAt(m_current) != 'E')
			{
				if(Skip_Silent_UE())
				{
					return;
				}

                if (O_Silent())
                {
                    m_current++;
                    return;
                }

				// encode all vowels and
				// diphthongs to the same value
				MetaphAdd("A"); 
			}
			else
			{
				Encode_E_Pronounced();
			}
		}

		if(!(!IsVowel(m_current - 2) && StringAt((m_current - 1), 4, "LEWA", "LEWO", "LEWI", "")))
		{
			m_current = SkipVowels(m_current);
		}
		else
		{
			m_current++;
		}
	}
	
	/**
	 * Encodes cases where non-initial 'e' is pronounced, taking
	 * care to detect unusual cases from the greek.
	 *
	 * Only executed if non initial vowel encoding is turned on
	 * 
	 * 
	 */
	void Encode_E_Pronounced()
	{
		// special cases with two pronunciations
		// 'agape' 'lame' 'resume'
		if((StringAt(0, 4, "LAME", "SAKE", "PATE", "") && (m_length == 4))
			|| (StringAt(0, 5, "AGAPE", "") && (m_length == 5))
			|| ((m_current == 5) && StringAt(0, 6, "RESUME", "")))
		{
			MetaphAdd("", "A");
			return;
		}
		
		// special case "inge" => 'INGA', 'INJ'
		if(StringAt(0, 4, "INGE", "")
			&& (m_length == 4))
		{
			MetaphAdd("A", "");
			return;
		}
		
		// special cases with two pronunciations
		// special handling due to the difference in
		// the pronunciation of the '-D'
		if((m_current == 5) && StringAt(0, 7, "BLESSED", "LEARNED", ""))
		{
			MetaphAddExactApprox("D", "AD", "T", "AT");
			m_current += 2;
			return;
		}
		
		// encode all vowels and diphthongs to the same value
		if((!E_Silent() 
				&& !flag_AL_inversion 
				&& !Silent_Internal_E())
				|| E_Pronounced_Exceptions())
		{
			MetaphAdd("A"); 
		}
		
		// now that we've visited the vowel in question
		flag_AL_inversion = false;
	}

    /**
     * Tests for cases where non-initial 'o' is not pronounced
     * Only executed if non initial vowel encoding is turned on
     * 
     * @return true if encoded as silent - no addition to m_metaph key
     *
    */
    boolean O_Silent()
    {
        // if "iron" at beginning or end of word and not "irony"
        if ((CharAt(m_current) == 'O') 
            && StringAt((m_current - 2), 4, "IRON", ""))
        {
            if ((StringAt(0, 4, "IRON", "")
                || (StringAt((m_current - 2), 4, "IRON", "")
                    && (m_last == (m_current + 1))))
                && !StringAt((m_current - 2), 6, "IRONIC", ""))
            {
                return true;
            }
        }

        return false;
    }
    
	/**
	 * Tests and encodes cases where non-initial 'e' is never pronounced
	 * Only executed if non initial vowel encoding is turned on
	 * 
	 * @return true if encoded as silent - no addition to m_metaph key
	 *
	*/
	boolean E_Silent()
	{	
		if(E_Pronounced_At_End())
		{
			return false;
		}

		// 'e' silent when last letter, altho
		if((m_current == m_last)
			// also silent if before plural 's'
			// or past tense or participle 'd', e.g.
			// 'grapes' and 'banished' => PNXT
			|| ((StringAt(m_last, 1, "S", "D", "")
				&& (m_current > 1)
				&& ((m_current + 1) == m_last)
					// and not e.g. "nested", "rises", or "pieces" => RASAS
					&& !(StringAt((m_current - 1), 3, "TED", "SES", "CES", "")
						|| StringAt(0, 9, "ANTIPODES", "ANOPHELES", "")
						|| StringAt(0, 8, "MOHAMMED", "MUHAMMED", "MOUHAMED", "")
						|| StringAt(0, 7, "MOHAMED", "")
						|| StringAt(0, 6, "NORRED", "MEDVED", "MERCED", "ALLRED", "KHALED", "RASHED", "MASJED", "")	
						|| StringAt(0, 5, "JARED", "AHMED", "HAMED", "JAVED", "") 
						|| StringAt(0, 4, "ABED", "IMED", ""))))
				// e.g.  'wholeness', 'boneless', 'barely'
				|| (StringAt((m_current + 1), 4, "NESS", "LESS", "") && ((m_current + 4) == m_last))
				|| (StringAt((m_current + 1), 2, "LY", "") && ((m_current + 2) == m_last)
						&& !StringAt(0, 6, "CICELY", "")))
		{
			return true;
		}

		return false;
	}

	/**
	 * Tests for words where an 'E' at the end of the word
	 * is pronounced
	 *
	 * special cases, mostly from the greek, spanish, japanese, 
	 * italian, and french words normally having an acute accent. 
	 * also, pronouns and articles
	 * 
	 * Many Thanks to ali, QuentinCompson, JeffCO, ToonScribe, Xan,
	 * Trafalz, and VictorLaszlo, all of them atriots from the Eschaton, 
	 * for all their fine contributions!
	 * 
	 * @return true if 'E' at end is pronounced
	 * 
	*/
	boolean E_Pronounced_At_End()
	{
		if((m_current == m_last)
			&& (StringAt((m_current - 6), 7, "STROPHE", "")
			// if a vowel is before the 'E', vowel eater will have eaten it. 
			//otherwise, consonant + 'E' will need 'E' pronounced
			|| (m_length == 2)
			|| ((m_length == 3) && !IsVowel(0))
			// these german name endings can be relied on to have the 'e' pronounced
			|| (StringAt((m_last - 2), 3, "BKE", "DKE", "FKE", "KKE", "LKE", 
										 "NKE", "MKE", "PKE", "TKE", "VKE", "ZKE", "")
				&& !StringAt(0, 5, "FINKE", "FUNKE", "")
				&& !StringAt(0, 6, "FRANKE", ""))
			|| StringAt((m_last - 4), 5, "SCHKE", "")
			|| (StringAt(0, 4, "ACME", "NIKE", "CAFE", "RENE", "LUPE", "JOSE", "ESME", "") && (m_length == 4))
			|| (StringAt(0, 5, "LETHE", "CADRE", "TILDE", "SIGNE", "POSSE", "LATTE", "ANIME", "DOLCE", "CROCE", 
								"ADOBE", "OUTRE", "JESSE", "JAIME", "JAFFE", "BENGE", "RUNGE", 
								"CHILE", "DESME", "CONDE", "URIBE", "LIBRE", "ANDRE", "") && (m_length == 5))
			|| (StringAt(0, 6, "HECATE", "PSYCHE", "DAPHNE", "PENSKE", "CLICHE", "RECIPE", 
							   "TAMALE", "SESAME", "SIMILE", "FINALE", "KARATE", "RENATE", "SHANTE",  
							   "OBERLE", "COYOTE", "KRESGE", "STONGE", "STANGE", "SWAYZE", "FUENTE", 
							   "SALOME", "URRIBE", "") && (m_length == 6))
			|| (StringAt(0, 7, "ECHIDNE", "ARIADNE", "MEINEKE", "PORSCHE", "ANEMONE", "EPITOME", 
								"SYNCOPE", "SOUFFLE", "ATTACHE", "MACHETE", "KARAOKE", "BUKKAKE", 
								"VICENTE", "ELLERBE", "VERSACE", "") && (m_length == 7))
			|| (StringAt(0, 8, "PENELOPE", "CALLIOPE", "CHIPOTLE", "ANTIGONE", "KAMIKAZE", "EURIDICE", 
							   "YOSEMITE", "FERRANTE", "") && (m_length == 8))
			|| (StringAt(0, 9, "HYPERBOLE", "GUACAMOLE", "XANTHIPPE", "") && (m_length == 9))
			|| (StringAt(0, 10, "SYNECDOCHE", "") && (m_length == 10))))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Detect internal silent 'E's e.g. "roseman",
	 * "firestone"
	 * 
	 */
	boolean Silent_Internal_E()
	{
		// 'olesen' but not 'olen'	RAKE BLAKE 
		if((StringAt(0, 3, "OLE", "") 
				&& E_Silent_Suffix(3) && !E_Pronouncing_Suffix(3))
		   || (StringAt(0, 4, "BARE", "FIRE", "FORE", "GATE", "HAGE", "HAVE",
				             "HAZE", "HOLE", "CAPE", "HUSE", "LACE", "LINE", 
				             "LIVE", "LOVE", "MORE", "MOSE", "MORE", "NICE", 
				             "RAKE", "ROBE", "ROSE", "SISE", "SIZE", "WARE", 
				             "WAKE", "WISE", "WINE", "") 
				&& E_Silent_Suffix(4) && !E_Pronouncing_Suffix(4))
		   || (StringAt(0, 5, "BLAKE", "BRAKE", "BRINE", "CARLE", "CLEVE", "DUNNE",
				   			 "HEDGE", "HOUSE", "JEFFE", "LUNCE", "STOKE", "STONE", 
				   			 "THORE", "WEDGE", "WHITE", "") 
				 &&  E_Silent_Suffix(5) && !E_Pronouncing_Suffix(5))
		   || (StringAt(0, 6, "BRIDGE", "CHEESE", "") 
				 && E_Silent_Suffix(6) && !E_Pronouncing_Suffix(6))
		   || StringAt((m_current - 5), 7, "CHARLES", ""))
		{
			return true;
		}
		
		return false;
	}

	/**
	 * Detect conditions required
	 * for the 'E' not to be pronounced
	 * 
	 */
	boolean E_Silent_Suffix(int at)
	{
		if((m_current == (at - 1)) 
				&& (m_length > (at + 1)) 
				&& (IsVowel((at + 1)) 
				|| (StringAt(at, 2, "ST", "SL", "") 
					&& (m_length > (at + 2)))))
		{
			return true;	
		}
		
		return false;
	}
	
	/**
	 * Detect endings that will
	 * cause the 'e' to be pronounced
	 * 
	 */
	boolean E_Pronouncing_Suffix(int at)
	{		
		// e.g. 'bridgewood' - the other vowels will get eaten
		// up so we need to put one in here
		if((m_length == (at + 4)) && StringAt(at, 4, "WOOD", ""))
		{
			return true;
		}
		
		// same as above
		if((m_length == (at + 5)) && StringAt(at, 5, "WATER", "WORTH", ""))
		{
			return true;
		}
				
		// e.g. 'bridgette'
		if((m_length == (at + 3)) && StringAt(at, 3, "TTE", "LIA", "NOW", "ROS", "RAS", ""))
		{
			return true;
		}
		
		// e.g. 'olena'
		if((m_length == (at + 2)) && StringAt(at, 2, "TA", "TT", "NA", "NO", "NE",
													 "RS", "RE", "LA", "AU", "RO", "RA", ""))
		{
			return true;
		}
		
		// e.g. 'bridget'
		if((m_length == (at + 1)) && StringAt(at, 1, "T", "R", ""))
		{
			return true;
		}
		
		return false;
	}

	/**
	 * Exceptions where 'E' is pronounced where it
	 * usually wouldn't be, and also some cases
	 * where 'LE' transposition rules don't apply
	 * and the vowel needs to be encoded here
	 *
	 * @return true if 'E' pronounced 
	 *  
	 */
	boolean E_Pronounced_Exceptions()
	{
		// greek names e.g. "herakles" or hispanic names e.g. "robles", where 'e' is pronounced, other exceptions
		if((((m_current + 1) == m_last) 
				&& (StringAt((m_current - 3), 5, "OCLES", "ACLES", "AKLES", "")
					|| StringAt(0, 4, "INES", "")
					|| StringAt(0, 5, "LOPES", "ESTES", "GOMES", "NUNES", "ALVES", "ICKES", 
							          "INNES", "PERES", "WAGES", "NEVES", "BENES", "DONES", "")
					|| StringAt(0, 6, "CORTES", "CHAVES", "VALDES", "ROBLES", "TORRES", "FLORES", "BORGES", 
									  "NIEVES", "MONTES", "SOARES", "VALLES", "GEDDES", "ANDRES", "VIAJES", 
									  "CALLES", "FONTES", "HERMES", "ACEVES", "BATRES", "MATHES", "")
					|| StringAt(0, 7, "DELORES", "MORALES", "DOLORES", "ANGELES", "ROSALES", "MIRELES", "LINARES", 
							          "PERALES", "PAREDES", "BRIONES", "SANCHES", "CAZARES", "REVELES", "ESTEVES", 
									  "ALVARES", "MATTHES", "SOLARES", "CASARES", "CACERES", "STURGES", "RAMIRES", 
									  "FUNCHES", "BENITES", "FUENTES", "PUENTES", "TABARES", "HENTGES", "VALORES", "")
					|| StringAt(0, 8, "GONZALES", "MERCEDES", "FAGUNDES", "JOHANNES", "GONSALES", "BERMUDES", 
									  "CESPEDES", "BETANCES", "TERRONES", "DIOGENES", "CORRALES", "CABRALES", 
									  "MARTINES", "GRAJALES", "")
					|| StringAt(0, 9, "CERVANTES", "FERNANDES", "GONCALVES", "BENEVIDES", "CIFUENTES", "SIFUENTES",
							  		  "SERVANTES", "HERNANDES", "BENAVIDES", "")
					|| StringAt(0, 10, "ARCHIMEDES", "CARRIZALES", "MAGALLANES", "")))
			|| StringAt(m_current - 2, 4, "FRED", "DGES", "DRED", "GNES", "")
			|| StringAt((m_current - 5), 7, "PROBLEM", "RESPLEN", "")
			|| StringAt((m_current - 4), 6, "REPLEN", "")
			|| StringAt((m_current - 3), 4, "SPLE", ""))
		{
			return true;
		}
		
		return false;
	}

	/**
	 * Encodes "-UE".
	 * 
	 * @return true if encoding handled in this routine, false if not
	 */
	boolean Skip_Silent_UE()
	{
		// always silent except for cases listed below
		if((StringAt((m_current - 1), 3, "QUE", "GUE", "") 
			&& !StringAt(0, 8, "BARBEQUE", "PALENQUE", "APPLIQUE", "")
			// '-que' cases usually french but missing the acute accent
			&& !StringAt(0, 6, "RISQUE", "")
			&& !StringAt((m_current - 3), 5, "ARGUE", "SEGUE", "")
			&& !StringAt(0, 7, "PIROGUE", "ENRIQUE", "")
			&& !StringAt(0, 10, "COMMUNIQUE", ""))
			&& (m_current > 1) 
				&& (((m_current + 1) == m_last) 
					|| StringAt(0, 7, "JACQUES", "")))
		{
			m_current = SkipVowels(m_current);
			return true;
		}

		return false;
	}

	/**
	 * Encodes 'B'
	 * 
	 *
	 */
	void Encode_B()
	{
		if(Encode_Silent_B())
		{
			return;
		}

		// "-mb", e.g", "dumb", already skipped over under
		// 'M', altho it should really be handled here...
		MetaphAddExactApprox("B", "P");

		if((CharAt(m_current + 1) == 'B')
			|| ((CharAt(m_current + 1) == 'P')
			&& ((m_current + 1 < m_last) && (CharAt(m_current + 2) != 'H'))))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}

	/**
	 * Encodes silent 'B' for cases not covered under "-mb-"
	 * 
	 * 
	 * @return true if encoding handled in this routine, false if not
	 *
	*/
	boolean Encode_Silent_B()
	{
		//'debt', 'doubt', 'subtle'
		if(StringAt((m_current - 2), 4, "DEBT", "") 
			|| StringAt((m_current - 2), 5, "SUBTL", "") 
			|| StringAt((m_current - 2), 6, "SUBTIL", "") 
			|| StringAt((m_current - 3), 5, "DOUBT", ""))
		{
			MetaphAdd("T");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes 'C'
	 * 
	 */
	void Encode_C()
	{

		if(Encode_Silent_C_At_Beginning()
			|| Encode_CA_To_S()
			|| Encode_CO_To_S()
			|| Encode_CH()
			|| Encode_CCIA()
			|| Encode_CC()
			|| Encode_CK_CG_CQ()
			|| Encode_C_Front_Vowel()
			|| Encode_Silent_C()
			|| Encode_CZ()
			|| Encode_CS())
		{
			return;
		}

		//else
		if(!StringAt((m_current - 1), 1, "C", "K", "G", "Q", ""))
		{
			MetaphAdd("K");
		}

		//name sent in 'mac caffrey', 'mac gregor
		if(StringAt((m_current + 1), 2, " C", " Q", " G", ""))
		{
			m_current += 2;
		}
		else
		{
			if(StringAt((m_current + 1), 1, "C", "K", "Q", "") 
				&& !StringAt((m_current + 1), 2, "CE", "CI", ""))
			{
				m_current += 2;
				// account for combinations such as Ro-ckc-liffe
				if(StringAt((m_current), 1, "C", "K", "Q", "") 
					&& !StringAt((m_current + 1), 2, "CE", "CI", ""))
				{
					m_current++;
				}
			}
			else
			{
				m_current++;
			}
		}
	}

	/**
	 * Encodes cases where 'C' is silent at beginning of word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_C_At_Beginning()
	{
	    //skip these when at start of word
	    if((m_current == 0)
			&& StringAt(m_current, 2, "CT", "CN", ""))
		{
	        m_current += 1;
			return true;
		}

		return false;
	}


	/**
	 * Encodes exceptions where "-CA-" should encode to S
	 * instead of K including cases where the cedilla has not been used
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CA_To_S()
	{
		// Special case: 'caesar'. 
		// Also, where cedilla not used, as in "linguica" => LNKS
		if(((m_current == 0) && StringAt(m_current, 4, "CAES", "CAEC", "CAEM", ""))
			|| StringAt(0, 8, "FRANCAIS", "FRANCAIX", "LINGUICA", "")
			|| StringAt(0, 6, "FACADE", "")
			|| StringAt(0, 9, "GONCALVES", "PROVENCAL", ""))
		{
			MetaphAdd("S");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encodes exceptions where "-CO-" encodes to S instead of K
	 * including cases where the cedilla has not been used
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CO_To_S()
	{
		// e.g. 'coelecanth' => SLKN0
		if((StringAt(m_current, 4, "COEL", "") 
				&& (IsVowel(m_current + 4) || ((m_current + 3) == m_last)))
			|| StringAt(m_current, 5, "COENA", "COENO", "")
			|| StringAt(0, 8, "FRANCOIS", "MELANCON", "")
			|| StringAt(0, 6, "GARCON", ""))
		{
			MetaphAdd("S");
			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-CH-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CH()
	{
		if(StringAt(m_current, 2, "CH", ""))
		{       
			if(Encode_CHAE()
				|| Encode_CH_To_H()
				|| Encode_Silent_CH()
				|| Encode_ARCH()
				// Encode_CH_To_X() should be
				// called before the germanic
				// and greek encoding functions
				|| Encode_CH_To_X()
				|| Encode_English_CH_To_K()
				|| Encode_Germanic_CH_To_K()
				|| Encode_Greek_CH_Initial()
				|| Encode_Greek_CH_Non_Initial())
			{
				return true;
			}

			if(m_current > 0)
			{
				if(StringAt(0, 2, "MC", "") 
						&& (m_current == 1))
				{
					//e.g., "McHugh"
					MetaphAdd("K");
				}
				else
				{
					MetaphAdd("X", "K");
				}
			}
			else
			{
				MetaphAdd("X");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-CHAE-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CHAE()
	{
		// e.g. 'michael'
		if(((m_current > 0) && StringAt((m_current + 2), 2, "AE", "")))
		{
			if(StringAt(0, 7, "RACHAEL", ""))
			{
				MetaphAdd("X");
			}
			else if(!StringAt((m_current - 1), 1, "C", "K", "G", "Q", ""))
			{
				MetaphAdd("K");
			}

			AdvanceCounter(4, 2);
			return true;
		}

		return false;
	}

	/**
	 * Encdoes transliterations from the hebrew where the
	 * sound 'kh' is represented as "-CH-". The normal pronounciation
	 * of this in english is either 'h' or 'kh', and alternate
	 * spellings most often use "-H-"
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CH_To_H()
	{
		// hebrew => 'H', e.g. 'channukah', 'chabad'
		if(((m_current == 0) 
			&& (StringAt((m_current + 2), 3, "AIM", "ETH", "ELM", "")
			|| StringAt((m_current + 2), 4, "ASID", "AZAN", "")
			|| StringAt((m_current + 2), 5, "UPPAH", "UTZPA", "ALLAH", "ALUTZ", "AMETZ", "")
			|| StringAt((m_current + 2), 6, "ESHVAN", "ADARIM", "ANUKAH", "")
			|| StringAt((m_current + 2), 7, "ALLLOTH", "ANNUKAH", "AROSETH", "")))
			// and an irish name with the same encoding
			|| StringAt((m_current - 3), 7, "CLACHAN", ""))
		{
			MetaphAdd("H");
			AdvanceCounter(3, 2);
			return true;
		}

		return false;
	}

	/**
	 * Encodes cases where "-CH-" is not pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_CH()
	{
		// '-ch-' not pronounced
		if(StringAt((m_current - 2), 7, "FUCHSIA", "")
			|| StringAt((m_current - 2), 5, "YACHT", "")
			|| StringAt(0, 8, "STRACHAN", "")
			|| StringAt(0, 8, "CRICHTON", "")
			|| (StringAt((m_current - 3), 6, "DRACHM", ""))
				&& !StringAt((m_current - 3), 7, "DRACHMA", ""))
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-CH-" to X
	 * English language patterns
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CH_To_X()
	{
		// e.g. 'approach', 'beach'
		if((StringAt((m_current - 2), 4, "OACH", "EACH", "EECH", "OUCH", "OOCH", "MUCH", "SUCH", "")
				&& !StringAt((m_current - 3), 5, "JOACH", ""))
			// e.g. 'dacha', 'macho'
			|| (((m_current + 2) == m_last ) && StringAt((m_current - 1), 4, "ACHA", "ACHO", ""))
			|| (StringAt(m_current, 4, "CHOT", "CHOD", "CHAT", "") && ((m_current + 3) == m_last))
			|| ((StringAt((m_current - 1), 4, "OCHE", "") && ((m_current + 2) == m_last))
					&& !StringAt((m_current - 2), 5, "DOCHE", ""))		
			|| StringAt((m_current - 4), 6, "ATTACH", "DETACH", "KOVACH", "")
			|| StringAt((m_current - 5), 7, "SPINACH", "")
			|| StringAt(0, 6, "MACHAU", "")
			|| StringAt((m_current - 4), 8, "PARACHUT", "")
			|| StringAt((m_current - 5), 8, "MASSACHU", "")
			|| (StringAt((m_current - 3), 5, "THACH", "") && !StringAt((m_current - 1), 4, "ACHE", ""))
			|| StringAt((m_current - 2), 6, "VACHON", "") )
		{
			MetaphAdd("X");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-CH-" to K in contexts of
	 * initial "A" or "E" follwed by "CH"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_English_CH_To_K()
	{
		//'ache', 'echo', alternate spelling of 'michael'
		if(((m_current == 1) && RootOrInflections(m_inWord, "ACHE"))
			|| (((m_current > 3) && RootOrInflections(m_inWord.substring(m_current - 1), "ACHE"))
				&& (StringAt(0, 3, "EAR", "")
					|| StringAt(0, 4, "HEAD", "BACK", "")
					|| StringAt(0, 5, "HEART", "BELLY", "TOOTH", "")))
			|| StringAt((m_current - 1), 4, "ECHO", "")
			|| StringAt((m_current - 2), 7, "MICHEAL", "")
			|| StringAt((m_current - 4), 7, "JERICHO", "")
			|| StringAt((m_current - 5), 7, "LEPRECH", ""))
		{
			MetaphAdd("K", "X");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-CH-" to K in mostly germanic context
	 * of internal "-ACH-", with exceptions
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Germanic_CH_To_K()
	{
		// various germanic
		// "<consonant><vowel>CH-"implies a german word where 'ch' => K
		if(((m_current > 1)
			&& !IsVowel(m_current - 2) 
			&& StringAt((m_current - 1), 3, "ACH", "")
			&& !StringAt((m_current - 2), 7, "MACHADO", "MACHUCA", "LACHANC", "LACHAPE", "KACHATU", "")
			&& !StringAt((m_current - 3), 7, "KHACHAT", "")
			&& ((CharAt(m_current + 2) != 'I') 
				&& ((CharAt(m_current + 2) != 'E')
				|| StringAt((m_current - 2), 6, "BACHER", "MACHER", "MACHEN", "LACHER", "")) )
						// e.g. 'brecht', 'fuchs'
				|| (StringAt((m_current + 2), 1, "T", "S", "") 
						&& !(StringAt(0, 11, "WHICHSOEVER", "") || StringAt(0, 9, "LUNCHTIME", "") ))
						// e.g. 'andromache'
				|| StringAt(0, 4, "SCHR", "")
				|| ((m_current > 2) && StringAt((m_current - 2), 5, "MACHE", ""))
				|| ((m_current == 2) && StringAt((m_current - 2), 4, "ZACH", ""))
				|| StringAt((m_current - 4), 6, "SCHACH", "")
				|| StringAt((m_current - 1), 5, "ACHEN", "")
				|| StringAt((m_current - 3), 5, "SPICH", "ZURCH", "BUECH", "")
				|| (StringAt((m_current - 3), 5, "KIRCH", "JOACH", "BLECH", "MALCH", "")
						// "kirch" and "blech" both get 'X'
						&& !(StringAt((m_current - 3), 8, "KIRCHNER", "") || ((m_current + 1) == m_last)))
				|| (((m_current + 1) == m_last) && StringAt((m_current - 2), 4, "NICH", "LICH", "BACH", ""))
				|| (((m_current + 1) == m_last) 
						&& StringAt((m_current - 3), 5, "URICH", "BRICH", "ERICH", "DRICH", "NRICH", "")
						&& !StringAt((m_current - 5), 7, "ALDRICH", "") 
						&& !StringAt((m_current - 6), 8, "GOODRICH", "")
						&& !StringAt((m_current - 7), 9, "GINGERICH", "")))
				|| (((m_current + 1) == m_last) && StringAt((m_current - 4), 6, "ULRICH", "LFRICH", "LLRICH", 
																				"EMRICH", "ZURICH", "EYRICH", ""))
			// e.g., 'wachtler', 'wechsler', but not 'tichner'
			|| ((StringAt((m_current - 1), 1, "A", "O", "U", "E", "") || (m_current == 0)) 
						&& StringAt((m_current + 2), 1, "L", "R", "N", "M", "B", "H", "F", "V", "W", " ", "")))
		{       
			// "CHR/L-" e.g. 'chris' do not get
			// alt pronunciation of 'X'
			if(StringAt((m_current + 2), 1, "R", "L", "")
				|| SlavoGermanic())
			{
				MetaphAdd("K");
			}
			else
			{
				MetaphAdd("K", "X");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-ARCH-". Some occurances are from greek roots and therefore encode
	 * to 'K', others are from english words and therefore encode to 'X'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ARCH()
	{
		if(StringAt((m_current - 2), 4, "ARCH", ""))
		{
			// "-ARCH-" has many combining forms where "-CH-" => K because of its
			// derivation from the greek
			if(((IsVowel(m_current + 2) && StringAt((m_current - 2), 5, "ARCHA", "ARCHI", "ARCHO", "ARCHU", "ARCHY", ""))
				|| StringAt((m_current - 2), 6, "ARCHEA", "ARCHEG", "ARCHEO", "ARCHET", "ARCHEL", "ARCHES", "ARCHEP", 
												"ARCHEM", "ARCHEN", "")
				|| (StringAt((m_current - 2), 4, "ARCH", "") && (((m_current + 1) == m_last)))
				|| StringAt(0, 7, "MENARCH", ""))
				&& (!RootOrInflections(m_inWord, "ARCH")
					&& !StringAt((m_current - 4), 6, "SEARCH", "POARCH", "")
					&& !StringAt(0, 9, "ARCHENEMY", "ARCHIBALD", "ARCHULETA", "ARCHAMBAU", "")
					&& !StringAt(0, 6, "ARCHER", "ARCHIE", "") 
					&& !((((StringAt((m_current - 3), 5, "LARCH", "MARCH", "PARCH", "")
							|| StringAt((m_current - 4), 6, "STARCH", ""))
							&& !(StringAt(0, 6, "EPARCH", "")
									|| StringAt(0, 7, "NOMARCH", "")
									|| StringAt(0, 8, "EXILARCH", "HIPPARCH", "MARCHESE", "")
									|| StringAt(0, 9, "ARISTARCH", "")
									|| StringAt(0, 9, "MARCHETTI", "")) )
							|| RootOrInflections(m_inWord, "STARCH"))
							&& (!StringAt((m_current - 2), 5, "ARCHU", "ARCHY", "")
									|| StringAt(0, 7, "STARCHY", ""))))) 
			{
				MetaphAdd("K", "X");
			}
			else
			{
				MetaphAdd("X");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-CH-" to K when from greek roots
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Greek_CH_Initial()
	{
		// greek roots e.g. 'chemistry', 'chorus', ch at beginning of root
		if((StringAt(m_current, 6, "CHAMOM", "CHARAC", "CHARIS", "CHARTO", "CHARTU", "CHARYB", "CHRIST", "CHEMIC", "CHILIA", "") 
			|| (StringAt(m_current, 5, "CHEMI", "CHEMO", "CHEMU", "CHEMY", "CHOND", "CHONA", "CHONI", "CHOIR", "CHASM", 
									   "CHARO", "CHROM", "CHROI", "CHAMA", "CHALC", "CHALD", "CHAET","CHIRO", "CHILO", "CHELA", "CHOUS", 
									   "CHEIL", "CHEIR", "CHEIM", "CHITI", "CHEOP", "")
				&& !(StringAt(m_current, 6, "CHEMIN", "") || StringAt((m_current - 2), 8, "ANCHONDO", "")))
			|| (StringAt(m_current, 5, "CHISM", "CHELI", "")
			// exclude spanish "machismo"
				&& !(StringAt(0, 8, "MACHISMO", "")
				// exclude some french words
					|| StringAt(0, 10, "REVANCHISM", "")
					|| StringAt(0, 9, "RICHELIEU", "")
					|| (StringAt(0, 5, "CHISM", "") && (m_length == 5))
					|| StringAt(0, 6, "MICHEL", "")))
			// include e.g. "chorus", "chyme", "chaos"
			|| (StringAt(m_current, 4, "CHOR", "CHOL", "CHYM", "CHYL", "CHLO", "CHOS", "CHUS", "CHOE", "")
					&& !StringAt(0, 6, "CHOLLO", "CHOLLA", "CHORIZ", ""))
			// "chaos" => K but not "chao"
			|| (StringAt(m_current, 4, "CHAO", "") && ((m_current + 3) != m_last))
			// e.g. "abranchiate"
			|| (StringAt(m_current, 4, "CHIA", "")  && !(StringAt(0, 10, "APPALACHIA", "") || StringAt(0, 7, "CHIAPAS", "")))
			// e.g. "chimera"
			|| StringAt(m_current, 7, "CHIMERA", "CHIMAER", "CHIMERI", "") 
			// e.g. "chameleon"
			|| ((m_current == 0) && StringAt(m_current, 5, "CHAME", "CHELO", "CHITO", "") )
			// e.g. "spirochete"
			|| ((((m_current + 4) == m_last) || ((m_current + 5) == m_last)) && StringAt((m_current - 1), 6, "OCHETE", "")))
			// more exceptions where "-CH-" => X e.g. "chortle", "crocheter"
				&& !((StringAt(0, 5, "CHORE",  "CHOLO", "CHOLA", "") && (m_length == 5))
					|| StringAt(m_current, 5, "CHORT", "CHOSE", "")
					|| StringAt((m_current - 3), 7, "CROCHET", "")
					|| StringAt(0, 7, "CHEMISE", "CHARISE", "CHARISS", "CHAROLE", "")) )
		{
			// "CHR/L-" e.g. 'christ', 'chlorine' do not get
			// alt pronunciation of 'X'
			if(StringAt((m_current + 2), 1, "R", "L", ""))
			{
				MetaphAdd("K");
			}
			else
			{
				MetaphAdd("K", "X");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode a variety of greek and some german roots where "-CH-" => K
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Greek_CH_Non_Initial()
	{
		//greek & other roots e.g. 'tachometer', 'orchid', ch in middle or end of root
		if(StringAt((m_current - 2), 6, "ORCHID", "NICHOL", "MECHAN", "LICHEN", "MACHIC", "PACHEL", "RACHIF", "RACHID", 
										"RACHIS", "RACHIC", "MICHAL", "")
			|| StringAt((m_current - 3), 5, "MELCH", "GLOCH", "TRACH", "TROCH", "BRACH", "SYNCH", "PSYCH", 
											"STICH", "PULCH", "EPOCH", "")
			|| (StringAt((m_current - 3), 5, "TRICH", "") && !StringAt((m_current - 5), 7, "OSTRICH", ""))
			|| (StringAt((m_current - 2), 4, "TYCH", "TOCH", "BUCH", "MOCH", "CICH", "DICH", "NUCH", "EICH", "LOCH", 
											 "DOCH", "ZECH", "WYCH", "")
				&& !(StringAt((m_current - 4), 9, "INDOCHINA", "") || StringAt((m_current - 2), 6, "BUCHON", "")))
			|| StringAt((m_current - 2), 5, "LYCHN", "TACHO", "ORCHO", "ORCHI", "LICHO", "")
			|| (StringAt((m_current - 1), 5, "OCHER", "ECHIN", "ECHID", "") && ((m_current == 1) || (m_current == 2)))
			|| StringAt((m_current - 4), 6, "BRONCH", "STOICH", "STRYCH", "TELECH", "PLANCH", "CATECH", "MANICH", "MALACH", 
											"BIANCH", "DIDACH", "")
			|| (StringAt((m_current - 1), 4, "ICHA", "ICHN","") && (m_current == 1))
			|| StringAt((m_current - 2), 8, "ORCHESTR", "")
			|| StringAt((m_current - 4), 8, "BRANCHIO", "BRANCHIF", "")
			|| (StringAt((m_current - 1), 5, "ACHAB", "ACHAD", "ACHAN", "ACHAZ", "")
				&& !StringAt((m_current - 2), 7, "MACHADO", "LACHANC", ""))
			|| StringAt((m_current - 1), 6, "ACHISH", "ACHILL", "ACHAIA", "ACHENE", "")
			|| StringAt((m_current - 1), 7, "ACHAIAN", "ACHATES", "ACHIRAL", "ACHERON", "")
			|| StringAt((m_current - 1), 8, "ACHILLEA", "ACHIMAAS", "ACHILARY", "ACHELOUS", "ACHENIAL", "ACHERNAR", "")
			|| StringAt((m_current - 1), 9, "ACHALASIA", "ACHILLEAN", "ACHIMENES", "")
			|| StringAt((m_current - 1), 10, "ACHIMELECH", "ACHITOPHEL", "")
			// e.g. 'inchoate'
			|| (((m_current - 2) == 0) && (StringAt((m_current - 2), 6, "INCHOA", "")
			// e.g. 'ischemia'
			|| StringAt(0, 4, "ISCH", "")) )
			// e.g. 'ablimelech', 'antioch', 'pentateuch'
			|| (((m_current + 1) == m_last) && StringAt((m_current - 1), 1, "A", "O", "U", "E", "") 
				&& !(StringAt(0, 7, "DEBAUCH", "")
						|| StringAt((m_current - 2), 4, "MUCH", "SUCH", "KOCH", "")
						|| StringAt((m_current - 5), 7, "OODRICH", "ALDRICH", ""))))
		{
			MetaphAdd("K", "X");
			m_current += 2;
			return true;
		}

		return false;
	}
	
	/**
	 * Encodes reliably italian "-CCIA-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CCIA()
	{
		//e.g., 'focaccia'
		if(StringAt((m_current + 1), 3, "CIA", ""))
		{
			MetaphAdd("X", "S");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-CC-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CC()
	{
		//double 'C', but not if e.g. 'McClellan'
		if(StringAt(m_current, 2, "CC", "") && !((m_current == 1) && (CharAt(0) == 'M')))
		{
			// exception
			if (StringAt((m_current - 3), 7, "FLACCID", ""))
			{
				MetaphAdd("S");
				AdvanceCounter(3, 2);
				return true;
			}

			//'bacci', 'bertucci', other italian
			if((((m_current + 2) == m_last) && StringAt((m_current + 2), 1, "I", ""))
				|| StringAt((m_current + 2), 2, "IO", "")
				|| (((m_current + 4) == m_last) && StringAt((m_current + 2), 3, "INO", "INI", "")))
			{
				MetaphAdd("X");
				AdvanceCounter(3, 2);
				return true;
			}

			//'accident', 'accede' 'succeed'
			if(StringAt((m_current + 2), 1, "I", "E", "Y", "")
				//except 'bellocchio','bacchus', 'soccer' get K
				&& !((CharAt(m_current + 2) == 'H') 
					|| StringAt((m_current - 2), 6, "SOCCER", "")))
			{
				MetaphAdd("KS");
				AdvanceCounter(3, 2);
				return true;

			}
			else
			{
				//Pierce's rule
				MetaphAdd("K");
				m_current += 2;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode cases where the consonant following "C" is redundant
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CK_CG_CQ()
	{
		if(StringAt(m_current, 2, "CK", "CG", "CQ", ""))
		{
			// eastern european spelling e.g. 'gorecki' == 'goresky'
			if(StringAt(m_current, 3, "CKI", "CKY", "") 
				&& ((m_current + 2) == m_last)
				&& (m_length > 6))
			{
				MetaphAdd("K", "SK");
			}
			else
			{
				MetaphAdd("K");
			}
			m_current += 2;

			if(StringAt(m_current, 1, "K", "G", "Q", ""))
			{
				m_current++;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "C" preceeds a front vowel such as "E", "I", or "Y".
	 * These cases most likely => S or X
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_C_Front_Vowel()
	{
		if(StringAt(m_current, 2, "CI", "CE", "CY", ""))
		{
			if(Encode_British_Silent_CE()
				|| Encode_CE()
				|| Encode_CI()
				|| Encode_Latinate_Suffixes())
			{
				AdvanceCounter(2, 1);
				return true;
			}

			MetaphAdd("S");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_British_Silent_CE()
	{
		// english place names like e.g.'gloucester' pronounced glo-ster
		if((StringAt((m_current + 1), 5, "ESTER", "") && ((m_current + 5) == m_last))
			|| StringAt((m_current + 1), 10, "ESTERSHIRE", ""))
		{
			return true;
		}

		return false;
	}

	/**
	 *  
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CE()
	{
		// 'ocean', 'commercial', 'provincial', 'cello', 'fettucini', 'medici'
		if((StringAt((m_current + 1), 3, "EAN", "") && IsVowel(m_current - 1))
			// e.g. 'rosacea'
			|| (StringAt((m_current - 1), 4, "ACEA", "") 
				&& ((m_current + 2) == m_last)
				&& !StringAt(0, 7, "PANACEA", ""))
			// e.g. 'botticelli', 'concerto'
			|| StringAt((m_current + 1), 4, "ELLI", "ERTO", "EORL", "") 
			// some italian names familiar to americans
			|| (StringAt((m_current - 3), 5, "CROCE", "") && ((m_current + 1) == m_last)) 
			|| StringAt((m_current - 3), 5, "DOLCE", "") 
			// e.g. 'cello'
			|| (StringAt((m_current + 1), 4, "ELLO", "") 
				&& ((m_current + 4) == m_last)))
		{
			MetaphAdd("X", "S");
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CI()
	{
		// with consonant before C
		// e.g. 'fettucini', but exception for the americanized pronunciation of 'mancini'
		if(((StringAt((m_current + 1), 3, "INI", "") && !StringAt(0, 7, "MANCINI", "")) && ((m_current + 3) == m_last))
			// e.g. 'medici'
			|| (StringAt((m_current - 1), 3, "ICI", "") && ((m_current + 1) == m_last))
			// e.g. "commercial', 'provincial', 'cistercian'
			|| StringAt((m_current - 1), 5, "RCIAL", "NCIAL", "RCIAN", "UCIUS", "")
			// special cases
			|| StringAt((m_current - 3), 6, "MARCIA", "")
			|| StringAt((m_current - 2), 7, "ANCIENT", ""))
		{
			MetaphAdd("X", "S");
			return true;
		}

		// with vowel before C (or at beginning?)
		if(((StringAt(m_current, 3, "CIO", "CIE", "CIA", "")
			&& IsVowel(m_current - 1))
			// e.g. "ciao"
			|| StringAt((m_current + 1), 3, "IAO", ""))
			&& !StringAt((m_current - 4), 8, "COERCION", ""))
		{			
			if((StringAt(m_current, 4, "CIAN", "CIAL", "CIAO", "CIES", "CIOL", "CION", "")
				// exception - "glacier" => 'X' but "spacier" = > 'S'
				|| StringAt((m_current - 3), 7, "GLACIER", "")
				|| StringAt(m_current, 5, "CIENT", "CIENC", "CIOUS", "CIATE", "CIATI", "CIATO", "CIABL", "CIARY", "")
				|| (((m_current + 2) == m_last) && StringAt(m_current, 3, "CIA", "CIO", ""))
				|| (((m_current + 3) == m_last) && StringAt(m_current, 3, "CIAS", "CIOS", "")))
				// exceptions
				&& !(StringAt((m_current - 4), 11, "ASSOCIATION", "")
					|| StringAt(0, 4, "OCIE", "")
					// exceptions mostly because these names are usually from 
					// the spanish rather than the italian in america
					|| StringAt((m_current - 2), 5, "LUCIO", "")
					|| StringAt((m_current - 2), 6, "MACIAS", "")
					|| StringAt((m_current - 3), 6, "GRACIE", "GRACIA", "")  
					|| StringAt((m_current - 2), 7, "LUCIANO", "") 
					|| StringAt((m_current - 3), 8, "MARCIANO", "")
					|| StringAt((m_current - 4), 7, "PALACIO", "") 
					|| StringAt((m_current - 4), 9, "FELICIANO", "") 
					|| StringAt((m_current - 5), 8, "MAURICIO", "") 
					|| StringAt((m_current - 7), 11, "ENCARNACION", "")
					|| StringAt((m_current - 4), 8, "POLICIES", "")
					|| StringAt((m_current - 2), 8, "HACIENDA", "")
					|| StringAt((m_current - 6), 9, "ANDALUCIA", "")
					|| StringAt((m_current - 2), 5, "SOCIO", "SOCIE", "")))
			{
				MetaphAdd("X", "S");
			}
			else
			{
				MetaphAdd("S", "X");
			}
			
			return true;
		}
		
		// exception
		if(StringAt((m_current - 4), 8, "COERCION", ""))
		{
			MetaphAdd("J");
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Latinate_Suffixes()
	{
		if(StringAt((m_current + 1), 4, "EOUS", "IOUS", ""))
		{
			MetaphAdd("X", "S");
			return true;
		}

		return false;
	}

	/**
	 * Encodes some exceptions where "C" is silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_C()
	{
		if(StringAt((m_current + 1), 1, "T", "S", ""))
		{
			if (StringAt(0, 11, "CONNECTICUT", "") 
				|| StringAt(0, 6, "INDICT", "TUCSON", ""))
			{       
				m_current++;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encodes slavic spellings or transliterations
	 * written as "-CZ-"
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CZ()
	{
		if(StringAt((m_current + 1), 1, "Z", "")
			&& !StringAt((m_current - 1), 6, "ECZEMA", ""))
		{
			if(StringAt(m_current, 4, "CZAR", ""))
			{
				MetaphAdd("S");
			}
			// otherwise most likely a czech word...
			else
			{
				MetaphAdd("X");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * "-CS" special cases
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_CS()
	{
		// give an 'etymological' 2nd
		// encoding for "kovacs" so
		// that it matches "kovach"
		if(StringAt(0, 6, "KOVACS", ""))
		{
			MetaphAdd("KS", "X");
			m_current += 2;
			return true;
		}
	
		if(StringAt((m_current - 1), 3, "ACS", "")
			&& ((m_current + 1) == m_last)
			&& !StringAt((m_current - 4), 6, "ISAACS", ""))
		{
			MetaphAdd("X");
			m_current += 2;
			return true;
		}
		
		return false;
	}

	/**
	 * Encode "-D-"
	 * 
	 */
	void Encode_D()
	{
		if(Encode_DG()
			|| Encode_DJ()
			|| Encode_DT_DD()
			|| Encode_D_To_J()
			|| Encode_DOUS()
			|| Encode_Silent_D())
		{
			return;
		}

		if(m_encodeExact)
		{
			// "final de-voicing" in this case
			// e.g. 'missed' == 'mist'
			if((m_current == m_last)
				&& StringAt((m_current - 3), 4, "SSED", ""))
			{
				MetaphAdd("T");
			}
			else
			{
				MetaphAdd("D");
			}
		}
		else
		{
			MetaphAdd("T");
		}
		m_current++;
	}

	/**
	 * Encode "-DG-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_DG()
	{
		if(StringAt(m_current, 2, "DG", ""))
		{
			// excludes exceptions e.g. 'edgar', 
			// or cases where 'g' is first letter of combining form 
			// e.g. 'handgun', 'waldglas'
			if(StringAt((m_current + 2), 1, "A", "O", "")
				// e.g. "midgut"
				|| StringAt((m_current + 1), 3, "GUN", "GUT", "")
				// e.g. "handgrip"
				|| StringAt((m_current + 1), 4, "GEAR", "GLAS", "GRIP", "GREN", "GILL", "GRAF", "")
				// e.g. "mudgard"
				|| StringAt((m_current + 1), 5, "GUARD", "GUILT", "GRAVE", "GRASS", "")
				// e.g. "woodgrouse"
				|| StringAt((m_current + 1), 6, "GROUSE", ""))
			{
				MetaphAddExactApprox("DG", "TK");
			}
			else
			{
				//e.g. "edge", "abridgment"
				MetaphAdd("J");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-DJ-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_DJ()
	{
		// e.g. "adjacent"
		if(StringAt(m_current, 2, "DJ", ""))
		{
			MetaphAdd("J");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-DD-" and "-DT-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_DT_DD()
	{
		// eat redundant 'T' or 'D'
		if(StringAt(m_current, 2, "DT", "DD", ""))
		{
			if(StringAt(m_current, 3, "DTH",  ""))
			{
				MetaphAddExactApprox("D0", "T0");
				m_current += 3;
			}
			else
			{
				if(m_encodeExact)
				{
					// devoice it
					if(StringAt(m_current, 2, "DT", ""))
					{
						MetaphAdd("T");
					}
					else
					{
						MetaphAdd("D");
					}						
				}
				else
				{
					MetaphAdd("T");
				}
				m_current += 2;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "-DU-" "-DI-", and "-DI-" => J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_D_To_J()
	{
		// e.g. "module", "adulate"
		if((StringAt(m_current, 3, "DUL", "") 
				&& (IsVowel(m_current - 1) && IsVowel(m_current + 3)))
			// e.g. "soldier", "grandeur", "procedure"
			|| (((m_current + 3) == m_last) 
				&& StringAt((m_current - 1) , 5, "LDIER", "NDEUR", "EDURE", "RDURE", ""))
			|| StringAt((m_current - 3), 7, "CORDIAL", "")
			// e.g.  "pendulum", "education"
			|| StringAt((m_current - 1), 5, "NDULA", "NDULU", "EDUCA", "")
			// e.g. "individual", "individual", "residuum"
			|| StringAt((m_current - 1), 4, "ADUA", "IDUA", "IDUU", ""))
		{
			MetaphAddExactApprox("J", "D", "J", "T");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode latinate suffix "-DOUS" where 'D' is pronounced as J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_DOUS()
	{
		// e.g. "assiduous", "arduous"
		if(StringAt((m_current + 1), 4, "UOUS", ""))
		{
			MetaphAddExactApprox("J", "D", "J", "T");
			AdvanceCounter(4, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode silent "-D-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 *	 
	 */
	boolean Encode_Silent_D()
	{
		// silent 'D' e.g. 'wednesday', 'handsome'
		if(StringAt((m_current - 2), 9, "WEDNESDAY", "")
			|| StringAt((m_current - 3), 7, "HANDKER", "HANDSOM", "WINDSOR", "")
			// french silent D at end in words or names familiar to americans
			|| StringAt((m_current - 5), 6, "PERNOD", "ARTAUD", "RENAUD", "")
			|| StringAt((m_current - 6), 7, "RIMBAUD", "MICHAUD", "BICHAUD", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-F-"
	 * 
	 */
	void Encode_F()
	{
		// Encode cases where "-FT-" => "T" is usually silent
		// e.g. 'often', 'soften'
		// This should really be covered under "T"!
		if(StringAt((m_current - 1), 5, "OFTEN", ""))
		{
			MetaphAdd("F", "FT");
			m_current += 2;
			return;
		}

		// eat redundant 'F'
		if(CharAt(m_current + 1) == 'F')
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}

		MetaphAdd("F");

	}

	/**
	 * Encode "-G-"
	 * 
	 */
	void Encode_G()
	{
		if(Encode_Silent_G_At_Beginning()
			|| Encode_GG()
			|| Encode_GK()
			|| Encode_GH()
			|| Encode_Silent_G()
			|| Encode_GN()
			|| Encode_GL()
			|| Encode_Initial_G_Front_Vowel()
			|| Encode_NGER()
			|| Encode_GER()
			|| Encode_GEL()
			|| Encode_Non_Initial_G_Front_Vowel()
			|| Encode_GA_To_J())
		{
			return;
		}

		if(!StringAt((m_current - 1), 1, "C", "K", "G", "Q", ""))
		{
			MetaphAddExactApprox("G", "K");
		}

		m_current++;
	}

	/**
	 * Encode cases where 'G' is silent at beginning of word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_G_At_Beginning()
	{    
		//skip these when at start of word
	    if((m_current == 0)
			&& StringAt(m_current, 2, "GN", ""))
		{
	        m_current += 1;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-GG-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GG()
	{
		if(CharAt(m_current + 1) == 'G')
		{
			// italian e.g, 'loggia', 'caraveggio', also 'suggest' and 'exaggerate'
			if(StringAt((m_current - 1), 5, "AGGIA", "OGGIA", "AGGIO", "EGGIO", "EGGIA", "IGGIO", "")
				// 'ruggiero' but not 'snuggies'
				|| (StringAt((m_current - 1), 5, "UGGIE", "") && !(((m_current + 3) == m_last) || ((m_current + 4) == m_last)))
				|| (((m_current + 2) == m_last) && StringAt((m_current - 1), 4, "AGGI", "OGGI", ""))
				|| StringAt((m_current - 2), 6, "SUGGES", "XAGGER", "REGGIE", ""))
			{
				// expection where "-GG-" => KJ
				if (StringAt((m_current - 2), 7, "SUGGEST", ""))
				{
					MetaphAddExactApprox("G", "K");
				}

				MetaphAdd("J");
				AdvanceCounter(3, 2);
			}
			else
			{
				MetaphAddExactApprox("G", "K");
				m_current += 2;
			}
			return true;
		}
		
		return false;
	}

	/**
	 * Encode "-GK-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GK()
	{
		// 'gingko'
		if(CharAt(m_current + 1) == 'K')
		{
			MetaphAdd("K");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-GH-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH()
	{
		if(CharAt(m_current + 1) == 'H')
		{
			if(Encode_GH_After_Consonant()
				|| Encode_Initial_GH()
				|| Encode_GH_To_J()
				|| Encode_GH_To_H()
				|| Encode_UGHT()
				|| Encode_GH_H_Part_Of_Other_Word()
				|| Encode_Silent_GH()
				|| Encode_GH_To_F())
			{
				return true;
			}

			MetaphAddExactApprox("G", "K");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_After_Consonant()
	{
		// e.g. 'burgher', 'bingham'
		if((m_current > 0) 
			&& !IsVowel(m_current - 1)
			// not e.g. 'greenhalgh'
			&& !(StringAt((m_current - 3), 5, "HALGH", "") 
					&& ((m_current + 1) == m_last)))
		{
			MetaphAddExactApprox("G", "K");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_GH()
	{
		if(m_current < 3)
		{
			// e.g. "ghislane", "ghiradelli"
			if(m_current == 0)
			{ 
				if(CharAt(m_current + 2) == 'I')
				{
					MetaphAdd("J");
				}
				else
				{
					MetaphAddExactApprox("G", "K");
				}
				m_current += 2;
				return true;
			}
		}

		return false;
	}


	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_To_J()
	{
		// e.g., 'greenhalgh', 'dunkenhalgh', english names
		if(StringAt((m_current - 2), 4, "ALGH", "") && ((m_current + 1) == m_last))
		{
			MetaphAdd("J", "");
			m_current += 2;
			return true;
		}

		return false;
	}
	
	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_To_H()
	{
		// special cases
		// e.g., 'donoghue', 'donaghy'
		if((StringAt((m_current - 4), 4, "DONO", "DONA", "") && IsVowel(m_current + 2))
			|| StringAt((m_current - 5), 9, "CALLAGHAN", ""))
		{
			MetaphAdd("H");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_UGHT()
	{
		//e.g. "ought", "aught", "daughter", "slaughter"    
		if(StringAt((m_current - 1), 4, "UGHT", ""))
		{
			if ((StringAt((m_current - 3), 5, "LAUGH", "")
				&& !(StringAt((m_current - 4), 7, "SLAUGHT", "")
					|| StringAt((m_current - 3), 7, "LAUGHTO", "")))
					|| StringAt((m_current - 4), 6, "DRAUGH", ""))
			{
				MetaphAdd("FT");
			}
			else
			{
				MetaphAdd("T");
			}
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_H_Part_Of_Other_Word()
	{
		// if the 'H' is the beginning of another word or syllable
		if (StringAt((m_current + 1), 4, "HOUS", "HEAD", "HOLE", "HORN", "HARN", ""))
		{
			MetaphAddExactApprox("G", "K");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_GH()
	{
		//Parker's rule (with some further refinements) - e.g., 'hugh'
		if(((((m_current > 1) && StringAt((m_current - 2), 1, "B", "H", "D", "G", "L", "") )
			//e.g., 'bough'
			|| ((m_current > 2) 
				&& StringAt((m_current - 3), 1, "B", "H", "D", "K", "W", "N", "P", "V", "")
				&& !StringAt(0, 6, "ENOUGH", ""))
			//e.g., 'broughton'
			|| ((m_current > 3) && StringAt((m_current - 4), 1, "B", "H", "") )
			//'plough', 'slaugh'
			|| ((m_current > 3) && StringAt((m_current - 4), 2, "PL", "SL", "") )  
			|| ((m_current > 0) 
					// 'sigh', 'light'
					&& ((CharAt(m_current - 1) == 'I')
						|| StringAt(0, 4, "PUGH", "")
						// e.g. 'MCDONAGH', 'MURTAGH', 'CREAGH'
						|| (StringAt((m_current - 1), 3, "AGH", "") 
								&& ((m_current + 1) == m_last))
						|| StringAt((m_current - 4), 6, "GERAGH", "DRAUGH", "")
						|| (StringAt((m_current - 3), 5, "GAUGH", "GEOGH", "MAUGH", "")
								&& !StringAt(0, 9, "MCGAUGHEY", ""))
						// exceptions to 'tough', 'rough', 'lough'
						|| (StringAt((m_current - 2), 4, "OUGH", "") 
								&& (m_current > 3) 
								&& !StringAt((m_current - 4), 6, "CCOUGH", "ENOUGH", "TROUGH", "CLOUGH", "")))))
			// suffixes starting w/ vowel where "-GH-" is usually silent
			&& (StringAt((m_current - 3), 5, "VAUGH", "FEIGH", "LEIGH", "")
				|| StringAt((m_current - 2), 4, "HIGH", "TIGH", "")
				|| ((m_current + 1) == m_last)
				|| (StringAt((m_current + 2), 2, "IE", "EY", "ES", "ER", "ED", "TY", "") 
					&& ((m_current + 3) == m_last)
					&& !StringAt((m_current - 5), 9, "GALLAGHER", ""))
				|| (StringAt((m_current + 2), 1, "Y", "") && ((m_current + 2) == m_last))
				|| (StringAt((m_current + 2), 3, "ING", "OUT", "") && ((m_current + 4) == m_last))
				|| (StringAt((m_current + 2), 4, "ERTY", "") && ((m_current + 5) == m_last))
				|| (!IsVowel(m_current + 2) 
						|| StringAt((m_current - 3), 5, "GAUGH", "GEOGH", "MAUGH", "")
						|| StringAt((m_current - 4), 8, "BROUGHAM", ""))))
			// exceptions where '-g-' pronounced
			&& !(StringAt(0, 6, "BALOGH", "SABAGH", "")	
				|| StringAt((m_current - 2), 7, "BAGHDAD", "")						
				|| StringAt((m_current - 3), 5, "WHIGH", "")
				|| StringAt((m_current - 5), 7, "SABBAGH", "AKHLAGH", "")))			
		{
			// silent - do nothing
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_Special_Cases()
	{
		boolean handled = false;

		// special case: 'hiccough' == 'hiccup'
		if(StringAt((m_current - 6), 8, "HICCOUGH", ""))
		{
			MetaphAdd("P");
			handled = true;
		}
		// special case: 'lough' alt spelling for scots 'loch'
		else if(StringAt(0, 5, "LOUGH", ""))
		{
			MetaphAdd("K");
			handled = true;
		}
		// hungarian
		else if(StringAt(0, 6, "BALOGH", ""))
		{
			MetaphAddExactApprox("G", "", "K", "");
			handled = true;
		}
		// "maclaughlin"
		else if(StringAt((m_current - 3), 8, "LAUGHLIN", "COUGHLAN", "LOUGHLIN", ""))
		{
			MetaphAdd("K", "F");
			handled = true;
		}
		else if(StringAt((m_current - 3), 5, "GOUGH", "")
				|| StringAt((m_current - 7), 9, "COLCLOUGH", ""))
		{
			MetaphAdd("", "F");
			handled = true;
		}
		
		if(handled)
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GH_To_F()
	{
		// the cases covered here would fall under
		// the GH_To_F rule below otherwise
		if(Encode_GH_Special_Cases())
		{
			return true;
		}
		else
		{
			//e.g., 'laugh', 'cough', 'rough', 'tough'
			if((m_current > 2) 
				&& (CharAt(m_current - 1) == 'U')
				&& IsVowel(m_current - 2)
				&& StringAt((m_current - 3), 1, "C", "G", "L", "R", "T", "N", "S", "")
				&& !StringAt((m_current - 4), 8, "BREUGHEL", "FLAUGHER", ""))
			{
				MetaphAdd("F");
				m_current += 2;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode some contexts where "g" is silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_G()
	{
		// e.g. "phlegm", "apothegm", "voigt"
		if((((m_current + 1) == m_last) 
			&& (StringAt((m_current - 1), 3, "EGM", "IGM", "AGM", "")
				|| StringAt(m_current, 2, "GT", "")))
			|| (StringAt(0, 5, "HUGES", "") && (m_length == 5)))
		{
			m_current++;
			return true;
		}
		
		// vietnamese names e.g. "Nguyen" but not "Ng"
		if(StringAt(0, 2, "NG", "") && (m_current != m_last))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * ENcode "-GN-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GN()
	{
		if(CharAt(m_current + 1) == 'N')
		{
			// 'align' 'sign', 'resign' but not 'resignation'
			// also 'impugn', 'impugnable', but not 'repugnant'
			if(((m_current > 1) 
				&& ((StringAt((m_current - 1), 1, "I", "U", "E", "") 
					|| StringAt((m_current - 3), 9, "LORGNETTE", "")
					|| StringAt((m_current - 2), 9, "LAGNIAPPE", "")
					|| StringAt((m_current - 2), 6, "COGNAC", "")
					|| StringAt((m_current - 3), 7, "CHAGNON", "")
					|| StringAt((m_current - 5), 9, "COMPAGNIE", "")
					|| StringAt((m_current - 4), 6, "BOLOGN", ""))
				// Exceptions: following are cases where 'G' is pronounced
				// in "assign" 'g' is silent, but not in "assignation"
				&& !(StringAt((m_current + 2), 5, "ATION", "")
					|| StringAt((m_current + 2), 4, "ATOR", "")
					|| StringAt((m_current + 2), 3, "ATE", "ITY", "")
				// exception to exceptions, not pronounced:
				|| (StringAt((m_current + 2), 2, "AN", "AC", "IA", "UM", "") 
					&& !(StringAt((m_current - 3), 8, "POIGNANT", "")
						|| StringAt((m_current - 2), 6, "COGNAC", "")))
				|| StringAt(0, 7, "SPIGNER", "STEGNER", "")
				|| (StringAt(0, 5, "SIGNE", "") && (m_length == 5))
				|| StringAt((m_current - 2), 5, "LIGNI", "LIGNO", "REGNA", "DIGNI", "WEGNE", 
												"TIGNE", "RIGNE", "REGNE", "TIGNO", "")
				|| StringAt((m_current - 2), 6, "SIGNAL", "SIGNIF", "SIGNAT", "")
				|| StringAt((m_current - 1), 5, "IGNIT", ""))
				&& !StringAt((m_current - 2), 6, "SIGNET", "LIGNEO", "") ))
				//not e.g. 'cagney', 'magna'
				|| (((m_current + 2) == m_last) 
						&& StringAt(m_current, 3, "GNE", "GNA", "")
						&& !StringAt((m_current - 2), 5, "SIGNA", "MAGNA", "SIGNE", "")))
			{
				MetaphAddExactApprox("N", "GN", "N", "KN");
			}
			else
			{
				MetaphAddExactApprox("GN", "KN");
			}
			m_current += 2;
			return true;
		}
		return false;
	}

	/**
	 * Encode "-GL-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GL()
	{
		//'tagliaro', 'puglia' BUT add K in alternative 
		// since americans sometimes do this
		if(StringAt((m_current + 1), 3, "LIA", "LIO", "LIE", "") 
			&& IsVowel(m_current - 1))
		{
			MetaphAddExactApprox("L", "GL", "L", "KL");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Initial_G_Soft()
	{
		if(((StringAt((m_current + 1), 2, "EL", "EM", "EN", "EO", "ER", "ES", "IA", "IN", "IO", "IP", "IU", "YM", "YN", "YP", "YR", "EE", "")
				|| StringAt((m_current + 1), 3, "IRA", "IRO", ""))
			// except for smaller set of cases where => K, e.g. "gerber"
			&& !(StringAt((m_current + 1), 3, "ELD", "ELT", "ERT", "INZ", "ERH", "ITE", "ERD", "ERL", "ERN", 
											  "INT", "EES", "EEK", "ELB", "EER", "")
					|| StringAt((m_current + 1), 4, "ERSH", "ERST", "INSB", "INGR", "EROW", "ERKE", "EREN", "")
					|| StringAt((m_current + 1), 5, "ELLER", "ERDIE", "ERBER", "ESUND", "ESNER", "INGKO", "INKGO", 
													"IPPER", "ESELL", "IPSON", "EEZER", "ERSON", "ELMAN", "")
					|| StringAt((m_current + 1), 6, "ESTALT", "ESTAPO", "INGHAM", "ERRITY", "ERRISH", "ESSNER", "ENGLER", "")
					|| StringAt((m_current + 1), 7, "YNAECOL", "YNECOLO", "ENTHNER", "ERAGHTY", "")
					|| StringAt((m_current + 1), 8, "INGERICH", "EOGHEGAN", "")))
			||(IsVowel(m_current + 1)
				&& (StringAt((m_current + 1), 3, "EE ", "EEW", "")
						|| (StringAt((m_current + 1), 3, "IGI", "IRA", "IBE", "AOL", "IDE", "IGL", "") 
														&& !StringAt((m_current + 1), 5, "IDEON", "") )
					|| StringAt((m_current + 1), 4, "ILES", "INGI", "ISEL", "")
					|| (StringAt((m_current + 1), 5, "INGER", "") && !StringAt((m_current + 1), 8, "INGERICH", "")) 
					|| StringAt((m_current + 1), 5, "IBBER", "IBBET", "IBLET", "IBRAN", "IGOLO", "IRARD", "IGANT", "")
					|| StringAt((m_current + 1), 6, "IRAFFE", "EEWHIZ","")
					|| StringAt((m_current + 1), 7, "ILLETTE", "IBRALTA", ""))))
		{
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where 'G' is at start of word followed
	 * by a "front" vowel e.g. 'E', 'I', 'Y'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_G_Front_Vowel()
	{
		// 'g' followed by vowel at beginning
		if((m_current == 0) && Front_Vowel(m_current + 1))
		{
			// special case "gila" as in "gila monster"
			if(StringAt((m_current + 1), 3, "ILA", "")
				&& (m_length == 4))
			{
				MetaphAdd("H");
			}
			else if(Initial_G_Soft())
			{
				MetaphAddExactApprox("J", "G", "J", "K");
			}
			else
			{
				// only code alternate 'J' if front vowel
				if((m_inWord.charAt(m_current + 1) == 'E') || (m_inWord.charAt(m_current + 1) == 'I'))
				{
					MetaphAddExactApprox("G", "J", "K", "J");
				}
				else
				{
					MetaphAddExactApprox("G", "K");
				}
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-NGER-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_NGER()
	{
		if((m_current > 1)
			&& StringAt((m_current - 1), 4, "NGER", ""))
		{
			// default 'G' => J  such as 'ranger', 'stranger', 'manger', 'messenger', 'orangery', 'granger'
			// 'boulanger', 'challenger', 'danger', 'changer', 'harbinger', 'lounger', 'ginger', 'passenger'
			// except for these the following
			if(!(RootOrInflections(m_inWord, "ANGER")
				|| RootOrInflections(m_inWord, "LINGER")
				|| RootOrInflections(m_inWord, "MALINGER")
				|| RootOrInflections(m_inWord, "FINGER")
				|| (StringAt((m_current - 3), 4, "HUNG", "FING", "BUNG", "WING", "RING", "DING", "ZENG", 
												 "ZING", "JUNG", "LONG", "PING", "CONG", "MONG", "BANG", 
												 "GANG", "HANG", "LANG", "SANG", "SING", "WANG", "ZANG", "")
					// exceptions to above where 'G' => J	
					&& !(StringAt((m_current - 6), 7, "BOULANG", "SLESING", "KISSING", "DERRING", "")
							|| StringAt((m_current - 8), 9, "SCHLESING", "")
							|| StringAt((m_current - 5), 6, "SALING", "BELANG", "")
							|| StringAt((m_current - 6), 7, "BARRING", "")
							|| StringAt((m_current - 6), 9, "PHALANGER", "")
							|| StringAt((m_current - 4), 5, "CHANG", "")))
				|| StringAt((m_current - 4), 5, "STING", "YOUNG", "")
				|| StringAt((m_current - 5), 6, "STRONG", "")
				|| StringAt(0, 3, "UNG", "ENG", "ING", "")
				|| StringAt(m_current, 6, "GERICH", "")
				|| StringAt(0, 6, "SENGER", "")
				|| StringAt((m_current - 3), 6, "WENGER", "MUNGER", "SONGER", "KINGER", "")
				|| StringAt((m_current - 4), 7, "FLINGER", "SLINGER", "STANGER", "STENGER", "KLINGER", "CLINGER", "")
				|| StringAt((m_current - 5), 8, "SPRINGER", "SPRENGER", "")
				|| StringAt((m_current - 3), 7, "LINGERF", "")
				|| StringAt((m_current - 2), 7, "ANGERLY", "ANGERBO", "INGERSO", "") ))
			{
				MetaphAddExactApprox("J", "G", "J", "K");
			}
			else
			{
				MetaphAddExactApprox("G", "J", "K", "J");
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}
	
	/**
	 * Encode "-GER-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GER()
	{
		if((m_current > 0)
			&& StringAt((m_current + 1), 2, "ER", ""))
		{
			// Exceptions to 'GE' where 'G' => K
			// e.g. "JAGER", "TIGER", "LIGER", "LAGER", "LUGER", "AUGER", "EAGER", "HAGER", "SAGER"
			if((((m_current == 2) && IsVowel(m_current - 1) && !IsVowel(m_current - 2) 
					&& !(StringAt((m_current - 2), 5, "PAGER", "WAGER", "NIGER", "ROGER", "LEGER", "CAGER", ""))
				|| StringAt((m_current - 2), 5, "AUGER", "EAGER", "INGER", "YAGER", "")) 
				|| StringAt((m_current - 3), 6, "SEEGER", "JAEGER", "GEIGER", "KRUGER", "SAUGER", "BURGER", 
												"MEAGER", "MARGER", "RIEGER", "YAEGER", "STEGER", "PRAGER", "SWIGER", 
												"YERGER", "TORGER", "FERGER", "HILGER", "ZEIGER", "YARGER", 
												"COWGER", "CREGER", "KROGER", "KREGER", "GRAGER", "STIGER", "BERGER", "")
				// 'berger' but not 'bergerac'
				|| (StringAt((m_current - 3), 6, "BERGER", "") && ((m_current + 2) == m_last))
				|| StringAt((m_current - 4), 7, "KREIGER", "KRUEGER", "METZGER", "KRIEGER", "KROEGER", "STEIGER", 
												"DRAEGER", "BUERGER", "BOERGER", "FIBIGER", "")
				// e.g. 'harshbarger', 'winebarger'
				|| (StringAt((m_current - 3), 6, "BARGER", "") && (m_current > 4))
				// e.g. 'weisgerber'
				|| (StringAt(m_current, 6, "GERBER", "") && (m_current > 0))
				|| StringAt((m_current - 5), 8, "SCHWAGER",	"LYBARGER",	"SPRENGER", "GALLAGER", "WILLIGER", "")
				|| StringAt(0, 4, "HARGER", "")
				|| (StringAt(0, 4, "AGER", "EGER", "") && (m_length == 4))
				|| StringAt((m_current - 1), 6, "YGERNE", "") 
				|| StringAt((m_current - 6), 9, "SCHWEIGER", "")) 
				&& !(StringAt((m_current - 5), 10, "BELLIGEREN", "")
						|| StringAt(0, 7, "MARGERY", "")
						|| StringAt((m_current - 3), 8, "BERGERAC", "")))
			{				
				if(SlavoGermanic())
				{
					MetaphAddExactApprox("G", "K");
				}
				else
				{
					MetaphAddExactApprox("G", "J", "K", "J");											
				}
			}
			else
			{
				MetaphAddExactApprox("J", "G", "J", "K");
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * ENcode "-GEL-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GEL()
	{
		// more likely to be "-GEL-" => JL
		if(StringAt((m_current + 1), 2, "EL", "")
			&& (m_current > 0))
		{
			// except for
			// "BAGEL", "HEGEL", "HUGEL", "KUGEL", "NAGEL", "VOGEL", "FOGEL", "PAGEL"
			if(((m_length == 5) 
					&& IsVowel(m_current - 1) 
					&& !IsVowel(m_current - 2)
					&& !StringAt((m_current - 2), 5, "NIGEL", "RIGEL", ""))
				// or the following as combining forms
				|| StringAt((m_current - 2), 5, "ENGEL", "HEGEL", "NAGEL", "VOGEL", "")
				|| StringAt((m_current - 3), 6, "MANGEL", "WEIGEL", "FLUGEL", "RANGEL", "HAUGEN", "RIEGEL", "VOEGEL", "")
				|| StringAt((m_current - 4), 7, "SPEIGEL", "STEIGEL", "WRANGEL", "SPIEGEL", "")
				|| StringAt((m_current - 4), 8, "DANEGELD", ""))
			{
				if(SlavoGermanic())
				{
					MetaphAddExactApprox("G", "K");
				}
				else
				{
					MetaphAddExactApprox("G", "J", "K", "J");											
				}
			}
			else
			{
				MetaphAddExactApprox("J", "G", "J", "K");
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-G-" followed by a vowel when non-initial leter.
	 * Default for this is a 'J' sound, so check exceptions where
	 * it is pronounced 'G'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Non_Initial_G_Front_Vowel()
	{
		// -gy-, gi-, ge-
		if(StringAt((m_current + 1), 1, "E", "I", "Y", ""))
		{
			// '-ge' at end
			// almost always 'j 'sound
			if(StringAt(m_current, 2, "GE", "") && (m_current == (m_last - 1)))
			{
				if(Hard_GE_At_End())
				{
					if(SlavoGermanic())
					{
						MetaphAddExactApprox("G", "K");
					}
					else
					{
						MetaphAddExactApprox("G", "J", "K", "J");											
					}
				}
				else
				{
					MetaphAdd("J");
				}
			}
			else
			{
				if(Internal_Hard_G())
				{
					// don't encode KG or KK if e.g. "mcgill"
					if(!((m_current == 2) && StringAt(0, 2, "MC", "")) 
						   || ((m_current == 3) && StringAt(0, 3, "MAC", "")))
					{
						if(SlavoGermanic())
						{
							MetaphAddExactApprox("G", "K");
						}
						else
						{
							MetaphAddExactApprox("G", "J", "K", "J");											
						}
					}
				}
				else
				{
					MetaphAddExactApprox("J", "G", "J", "K");
				}
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}
	
	/*
	 * Detect german names and other words that have
	 * a 'hard' 'g' in the context of "-ge" at end
	 * 
	 * @return true if encoding handled in this routine, false if not
	 */
	boolean Hard_GE_At_End()
	{
		if(StringAt(0, 6, "RENEGE", "STONGE", "STANGE", "PRANGE", "KRESGE", "") 
			|| StringAt(0, 5, "BYRGE", "BIRGE", "BERGE", "HAUGE", "")
			|| StringAt(0, 4, "HAGE", "")					
			|| StringAt(0, 5, "LANGE", "SYNGE", "BENGE", "RUNGE", "HELGE", "")
			|| StringAt(0, 4, "INGE", "LAGE", ""))
		{
			return true;
		}
			
		return false;
	}

	/**
	 * Exceptions to default encoding to 'J':
	 * encode "-G-" to 'G' in "-g<frontvowel>-" words
	 * where we are not at "-GE" at the end of the word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Internal_Hard_G()
	{
		// if not "-GE" at end
		if(!(((m_current + 1) == m_last) && (CharAt(m_current + 1) == 'E') ) 
				&& (Internal_Hard_NG()
					|| Internal_Hard_GEN_GIN_GET_GIT()
					|| Internal_Hard_G_Open_Syllable()
					|| Internal_Hard_G_Other()))
		{
			return true;
		}
			
		return false;
	}
	
	/**
	 * Detect words where "-ge-" or "-gi-" get a 'hard' 'g'
	 * even though this is usually a 'soft' 'g' context
	 *  
	 * @return true if 'hard' 'g' detected
	 * 
	 */
	boolean Internal_Hard_G_Other()
	{
		if((StringAt(m_current, 4, "GETH", "GEAR", "GEIS", "GIRL", "GIVI", "GIVE", "GIFT", 
								   "GIRD", "GIRT", "GILV", "GILD", "GELD", "")
					&& !StringAt((m_current - 3), 6, "GINGIV", "") )
				// "gish" but not "largish"
				|| (StringAt((m_current + 1), 3, "ISH", "") && (m_current > 0) && !StringAt(0, 4, "LARG", ""))
				|| (StringAt((m_current - 2), 5, "MAGED", "MEGID", "") && !((m_current + 2) == m_last))
				|| StringAt(m_current, 3, "GEZ", "") 
				|| StringAt(0, 4, "WEGE", "HAGE", "") 
				|| (StringAt((m_current - 2), 6, "ONGEST", "UNGEST", "") 
					&& ((m_current + 3) == m_last)
					&& !StringAt((m_current - 3), 7, "CONGEST", "")) 
				|| StringAt(0, 5, "VOEGE", "BERGE", "HELGE", "")
				|| (StringAt(0, 4, "ENGE", "BOGY", "") && (m_length == 4))
				|| StringAt(m_current, 6, "GIBBON", "") 
				|| StringAt(0, 10, "CORREGIDOR", "") 
				|| StringAt(0, 8, "INGEBORG", "") 
				|| (StringAt(m_current, 4, "GILL", "") 
						&& (((m_current + 3) == m_last) || ((m_current + 4) == m_last))
						&& !StringAt(0, 8, "STURGILL", "")))
		{
			return true;
		}

		return false;
	}

	/**
	 * Detect words where "-gy-", "-gie-", "-gee-", 
	 * or "-gio-" get a 'hard' 'g' even though this is 
	 * usually a 'soft' 'g' context
	 *  
	 * @return true if 'hard' 'g' detected
	 * 
	 */
	boolean Internal_Hard_G_Open_Syllable()
	{
		if(StringAt((m_current + 1), 3, "EYE", "")
			|| StringAt((m_current - 2), 4, "FOGY", "POGY", "YOGI", "") 
			|| StringAt((m_current - 2), 5, "MAGEE", "MCGEE", "HAGIO", "") 
			|| StringAt((m_current - 1), 4, "RGEY", "OGEY", "") 
			|| StringAt((m_current - 3), 5, "HOAGY", "STOGY", "PORGY", "") 
			|| StringAt((m_current - 5), 8, "CARNEGIE", "") 
			|| (StringAt((m_current - 1), 4, "OGEY", "OGIE", "") && ((m_current + 2) == m_last)))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Detect a number of contexts, mostly german names, that
	 * take a 'hard' 'g'.
	 * 
	 * @return true if 'hard' 'g' detected, false if not
	 *  
	 */
	boolean Internal_Hard_GEN_GIN_GET_GIT()
	{
		if((StringAt((m_current - 3), 6, "FORGET", "TARGET", "MARGIT", "MARGET", "TURGEN", 
										 "BERGEN", "MORGEN", "JORGEN", "HAUGEN", "JERGEN", 
										 "JURGEN", "LINGEN", "BORGEN", "LANGEN", "KLAGEN", "STIGER", "BERGER", "") 
					&& !StringAt(m_current, 7, "GENETIC", "GENESIS", "")
					&& !StringAt((m_current - 4), 8, "PLANGENT", ""))
			|| (StringAt((m_current - 3), 6, "BERGIN", "FEAGIN", "DURGIN", "") && ((m_current + 2) == m_last))
			|| (StringAt((m_current - 2), 5, "ENGEN", "") && !StringAt((m_current + 3), 3, "DER", "ETI", "ESI", ""))
			|| StringAt((m_current - 4), 7, "JUERGEN", "")
			|| StringAt(0, 5, "NAGIN", "MAGIN", "HAGIN", "")
			|| (StringAt(0, 5, "ENGIN", "DEGEN", "LAGEN", "MAGEN", "NAGIN", "") && (m_length == 5))
			|| (StringAt((m_current - 2), 5, "BEGET", "BEGIN", "HAGEN", "FAGIN", 
										 "BOGEN", "WIGIN", "NTGEN", "EIGEN", 
										 "WEGEN", "WAGEN", "")
				&& !StringAt((m_current - 5), 8, "OSPHAGEN", "")))
		{
			return true;
		}

		return false;
	}
	/**
	 * Detect a number of contexts of '-ng-' that will
	 * take a 'hard' 'g' despite being followed by a
	 * front vowel.
	 * 
	 * @return true if 'hard' 'g' detected, false if not
	 * 
	 */
	boolean Internal_Hard_NG()
	{
		if((StringAt((m_current - 3), 4, "DANG", "FANG", "SING", "") 
			// exception to exception
					&& !StringAt((m_current - 5), 8, "DISINGEN", "") )
			|| StringAt(0, 5, "INGEB", "ENGEB", "")
			|| (StringAt((m_current - 3), 4, "RING", "WING", "HANG", "LONG", "")
					&& !(StringAt((m_current - 4), 5, "CRING", "FRING", "ORANG", "TWING", "CHANG", "PHANG", "")
						|| StringAt((m_current - 5), 6, "SYRING", "") 
						|| StringAt((m_current - 3), 7, "RINGENC", "RINGENT", "LONGITU", "LONGEVI", "") 
						// e.g. 'longino', 'mastrangelo'
						|| (StringAt(m_current, 4, "GELO", "GINO", "") && ((m_current + 3) == m_last))))
			|| (StringAt((m_current - 1), 3, "NGY", "")
			// exceptions to exception
					&& !(StringAt((m_current - 3), 5, "RANGY", "MANGY", "MINGY", "")
						|| StringAt((m_current - 4), 6, "SPONGY", "STINGY", ""))))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Encode special case where "-GA-" => J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_GA_To_J()
	{
		// 'margary', 'margarine'
		if((StringAt((m_current - 3), 7, "MARGARY", "MARGARI", "")
			// but not in spanish forms such as "margatita"
			&& !StringAt((m_current - 3), 8, "MARGARIT", ""))
			|| StringAt(0, 4, "GAOL", "")
			|| StringAt((m_current - 2), 5, "ALGAE", ""))
		{
			MetaphAddExactApprox("J", "G", "J", "K");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode 'H'
	 * 
	 * 
	 */
	void Encode_H()
	{
		if(Encode_Initial_Silent_H()
			|| Encode_Initial_HS()
			|| Encode_Initial_HU_HW()
			|| Encode_Non_Initial_Silent_H())
		{
			return;
		}

		//only keep if first & before vowel or btw. 2 vowels
		if(!Encode_H_Pronounced())
		{
			//also takes care of 'HH'
			m_current++;
		}
	}

	/**
	 * Encode cases where initial 'H' is not pronounced (in American)
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_Silent_H()
	{
		//'hour', 'herb', 'heir', 'honor'
		if(StringAt((m_current + 1), 3, "OUR", "ERB", "EIR", "")
			|| StringAt((m_current + 1), 4, "ONOR", "")
			|| StringAt((m_current + 1), 5, "ONOUR", "ONEST", ""))
		{
			// british pronounce H in this word
			// americans give it 'H' for the name,
			// no 'H' for the plant
			if((m_current == 0) && StringAt(m_current, 4, "HERB", ""))
			{
				if(m_encodeVowels)
				{
					MetaphAdd("HA", "A");
				}
				else
				{
					MetaphAdd("H", "A");
				}
			}
			else if((m_current == 0) || m_encodeVowels)
			{
				MetaphAdd("A");
			}

			m_current++;
			// don't encode vowels twice
			m_current = SkipVowels(m_current);
			return true;
		}

		return false;
	}

	/**
	 * Encode "HS-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_HS()
	{
		// old chinese pinyin transliteration
		// e.g., 'HSIAO'
		if ((m_current == 0) && StringAt(0, 2, "HS", ""))
		{
			MetaphAdd("X");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "HU-" is pronounced as part of a vowel dipthong
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_HU_HW()
	{
		// spanish spellings and chinese pinyin transliteration
		if (StringAt(0, 3, "HUA", "HUE", "HWA", ""))
		{
			if(!StringAt(m_current, 4, "HUEY", ""))
			{
				MetaphAdd("A");

				if(!m_encodeVowels)
				{
					m_current += 3;
				}
				else
				{
					m_current++;
					// don't encode vowels twice
					while(IsVowel(m_current) || (CharAt(m_current) == 'W'))
					{
						m_current++;
					}
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode cases where 'H' is silent between vowels
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Non_Initial_Silent_H()
	{
		//exceptions - 'h' not pronounced
		// "PROHIB" BUT NOT "PROHIBIT"
		if(StringAt((m_current - 2), 5, "NIHIL", "VEHEM", "LOHEN", "NEHEM", 
										"MAHON", "MAHAN", "COHEN", "GAHAN", "")
			|| StringAt((m_current - 3), 6, "GRAHAM", "PROHIB", "FRAHER", 
											"TOOHEY", "TOUHEY", "")
			|| StringAt((m_current - 3), 5, "TOUHY", "")
			|| StringAt(0, 9, "CHIHUAHUA", ""))
		{
			if(!m_encodeVowels)
			{
				m_current += 2;
			}
			else
			{
				m_current++;
				// don't encode vowels twice
				m_current = SkipVowels(m_current);
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where 'H' is pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_H_Pronounced()
	{
		if((((m_current == 0) 
				|| IsVowel(m_current - 1) 
				|| ((m_current > 0) 
					&& (CharAt(m_current - 1) == 'W'))) 
			&& IsVowel(m_current + 1))
			// e.g. 'alWahhab'
			|| ((CharAt(m_current + 1) == 'H') && IsVowel(m_current + 2))) 
		{
			MetaphAdd("H");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode 'J'
	 * 
	 */
	void Encode_J()
	{
		if(Encode_Spanish_J()
			|| Encode_Spanish_OJ_UJ())
		{
			return;
		}

		Encode_Other_J();
	}

	/**
	 * Encode cases where initial or medial "j" is in a spanish word or name
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Spanish_J()
	{
		//obvious spanish, e.g. "jose", "san jacinto"
		if((StringAt((m_current + 1), 3, "UAN", "ACI", "ALI", "EFE", "ICA", "IME", "OAQ", "UAR", "")
				&& !StringAt(m_current, 8, "JIMERSON", "JIMERSEN", ""))
			|| (StringAt((m_current + 1), 3, "OSE", "") && ((m_current + 3) == m_last))
			|| StringAt((m_current + 1), 4, "EREZ", "UNTA", "AIME", "AVIE", "AVIA", "")
			|| StringAt((m_current + 1), 6, "IMINEZ", "ARAMIL", "")
			|| (((m_current + 2) == m_last) && StringAt((m_current - 2), 5, "MEJIA", ""))
			|| StringAt((m_current - 2), 5, "TEJED", "TEJAD", "LUJAN", "FAJAR", "BEJAR", "BOJOR", "CAJIG", 
											"DEJAS", "DUJAR", "DUJAN", "MIJAR", "MEJOR", "NAJAR", 
											"NOJOS", "RAJED", "RIJAL", "REJON", "TEJAN", "UIJAN", "")
			|| StringAt((m_current - 3), 8, "ALEJANDR", "GUAJARDO", "TRUJILLO", "")
			|| (StringAt((m_current - 2), 5, "RAJAS", "") && (m_current > 2))
			|| (StringAt((m_current - 2), 5, "MEJIA", "") && !StringAt((m_current - 2), 6, "MEJIAN", ""))
			|| StringAt((m_current - 1), 5, "OJEDA", "")
			|| StringAt((m_current - 3), 5, "LEIJA", "MINJA", "")
			|| StringAt((m_current - 3), 6, "VIAJES", "GRAJAL", "")
			|| StringAt(m_current, 8, "JAUREGUI", "")
			|| StringAt((m_current - 4), 8, "HINOJOSA", "")
			|| StringAt(0, 4, "SAN ", "") 
			|| (((m_current + 1) == m_last)
			&& (CharAt(m_current + 1) == 'O')
			// exceptions
			&& !(StringAt(0, 4, "TOJO", "") 
					|| StringAt(0, 5, "BANJO", "") 
					|| StringAt(0, 6, "MARYJO", ""))))
		{
			// americans pronounce "juan" as 'wan'
			// and "marijuana" and "tijuana" also
			// do not get the 'H' as in spanish, so
			// just treat it like a vowel in these cases
			if(!(StringAt(m_current, 4, "JUAN", "") || StringAt(m_current, 4, "JOAQ", "")))
			{
				MetaphAdd("H");
			}
			else
			{
				if(m_current == 0)
				{
					MetaphAdd("A");
				}
			}
			AdvanceCounter(2, 1);
			return true;
		}
		
		// Jorge gets 2nd HARHA. also JULIO, JESUS
		if(StringAt((m_current + 1), 4, "ORGE", "ULIO", "ESUS", "")
			&& !StringAt(0, 6, "JORGEN", ""))
		{
			// get both consonants for "jorge"
			if(((m_current + 4) == m_last) && StringAt((m_current + 1), 4, "ORGE", ""))
			{
				if(m_encodeVowels)
				{
					MetaphAdd("JARJ", "HARHA");				
				}
				else
				{
					MetaphAdd("JRJ", "HRH");
				}				
				AdvanceCounter(5, 5);
				return true;				
			}
			
			MetaphAdd("J", "H");			
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where 'J' is clearly in a german word or name
	 * that americans pronounce in the german fashion
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_German_J()
	{
		if(StringAt((m_current + 1), 2, "AH", "")
			|| (StringAt((m_current + 1), 5, "OHANN", "") && ((m_current + 5) == m_last))
			|| (StringAt((m_current + 1), 3, "UNG", "") && !StringAt((m_current + 1), 4, "UNGL", ""))
			|| StringAt((m_current + 1), 3, "UGO", ""))
		{
			MetaphAdd("A");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-JOJ-" and "-JUJ-" as spanish words
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Spanish_OJ_UJ()
	{
		if(StringAt((m_current + 1), 5, "OJOBA", "UJUY ", ""))
		{
			if(m_encodeVowels)
			{
				MetaphAdd("HAH");
			}
			else
			{
				MetaphAdd("HH");
			}

			AdvanceCounter(4, 3);
			return true;
		}

		return false;
	}

	/**
	 * Encode 'J' => J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_J_To_J()
	{
		if(IsVowel(m_current + 1))
		{
			if((m_current == 0) 
				&& Names_Beginning_With_J_That_Get_Alt_Y())
			{
				// 'Y' is a vowel so encode
				// is as 'A'
				if(m_encodeVowels)
				{
					MetaphAdd("JA", "A");
				}
				else
				{
					MetaphAdd("J", "A");
				}
			}
			else
			{
				if(m_encodeVowels)
				{
					MetaphAdd("JA");
				}
				else
				{
					MetaphAdd("J");
				}
			}

			m_current++;
			m_current = SkipVowels(m_current);
			return false;
		}
		else
		{
			MetaphAdd("J");
			m_current++;
			return true;
		}

//		return false;
	}

	/**
	 * Encode 'J' toward end in spanish words
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Spanish_J_2()
	{
		// spanish forms e.g. "brujo", "badajoz"
		if((((m_current - 2) == 0) 
			&& StringAt((m_current - 2), 4, "BOJA", "BAJA", "BEJA", "BOJO", "MOJA", "MOJI", "MEJI", ""))
			|| (((m_current - 3) == 0)
			&& StringAt((m_current - 3), 5, "FRIJO", "BRUJO", "BRUJA", "GRAJE", "GRIJA", "LEIJA", "QUIJA", ""))
			|| (((m_current + 3) == m_last) 
			&& StringAt((m_current - 1), 5, "AJARA", ""))
			|| (((m_current + 2) == m_last) 
			&& StringAt((m_current - 1), 4, "AJOS", "EJOS", "OJAS", "OJOS", "UJON", "AJOZ", "AJAL", "UJAR", "EJON", "EJAN", ""))
			|| (((m_current + 1) == m_last) 
			&& (StringAt((m_current - 1), 3, "OJA", "EJA", "") && !StringAt(0, 4, "DEJA", ""))))
		{
			MetaphAdd("H");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode 'J' as vowel in some exception cases
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_J_As_Vowel()
	{
		if(StringAt(m_current, 5, "JEWSK", ""))
		{
			MetaphAdd("J", "");
			return true;
		}
		
		// e.g. "stijl", "sejm" - dutch, scandanavian, and eastern european spellings
		if((StringAt((m_current + 1), 1, "L", "T", "K", "S", "N", "M", "")
				// except words from hindi and arabic
				&& !StringAt((m_current + 2), 1, "A", ""))
			|| StringAt(0, 9, "HALLELUJA", "LJUBLJANA", "")
			|| StringAt(0, 4, "LJUB", "BJOR", "")
			|| StringAt(0, 5, "HAJEK", "")
			|| StringAt(0, 3, "WOJ", "")
			// e.g. 'fjord'
			|| StringAt(0, 2, "FJ", "")
			// e.g. 'rekjavik', 'blagojevic'
			|| StringAt(m_current, 5, "JAVIK", "JEVIC", "")
			|| (((m_current + 1) == m_last) && StringAt(0, 5, "SONJA", "TANJA", "TONJA", "")))

		{
			return true;
		}
		return false;
	}

	/**
	 * Call routines to encode 'J', in proper order
	 * 
	 */
	void Encode_Other_J()
	{
		if(m_current == 0)
		{
			if(Encode_German_J())
			{
				return;
			}
			else
			{
				if(Encode_J_To_J())
				{
					return;
				}
			}
		}
		else
		{
			if(Encode_Spanish_J_2())
			{
				return;
			}
			else if(!Encode_J_As_Vowel())
			{
				MetaphAdd("J");
			}
			
			//it could happen! e.g. "hajj"
			// eat redundant 'J'
			if(CharAt(m_current + 1) == 'J')
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}
		}
	}

	/**
	 * Encode 'K'
	 * 
	 * 
	 */
	void Encode_K()
	{
		if(!Encode_Silent_K())
		{
			MetaphAdd("K");

			// eat redundant 'K's and 'Q's
			if((CharAt(m_current + 1) == 'K')
				|| (CharAt(m_current + 1) == 'Q'))
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}
		}
	}

	/**
	 * Encode cases where 'K' is not pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_K()
	{
	    //skip this except for special cases
	    if((m_current == 0)
			&& StringAt(m_current, 2, "KN", ""))
	    {
	        if(!(StringAt((m_current + 2), 5, "ESSET", "IEVEL", "") || StringAt((m_current + 2), 3, "ISH", "") ))
	        {
	            m_current += 1;
				return true;
	        }
	    }

		// e.g. "know", "knit", "knob"	
		if((StringAt((m_current + 1), 3, "NOW", "NIT", "NOT", "NOB", "")
				// exception, "slipknot" => SLPNT but "banknote" => PNKNT
				&& !StringAt(0, 8, "BANKNOTE", ""))
			|| StringAt((m_current + 1), 4, "NOCK", "NUCK", "NIFE", "NACK", "")
			|| StringAt((m_current + 1), 5, "NIGHT", ""))
		{
			// N already encoded before
			// e.g. "penknife"
			if ((m_current > 0) && CharAt(m_current - 1) == 'N')
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}

			return true;
		}
		
		return false;
	}

	/**
	 * Encode 'L'
	 *
	 * Includes special vowel transposition 
	 * encoding, where 'LE' => AL
	 * 
	 */
	void Encode_L()
	{
		// logic below needs to know this
		// after 'm_current' variable changed 
		int save_current = m_current;

		Interpolate_Vowel_When_Cons_L_At_End();
		
		if(Encode_LELY_To_L()
			|| Encode_COLONEL()
			|| Encode_French_AULT()
			|| Encode_French_EUIL()
			|| Encode_French_OULX()
			|| Encode_Silent_L_In_LM()
			|| Encode_Silent_L_In_LK_LV()
			|| Encode_Silent_L_In_OULD())
		{
			return;
		}

		if(Encode_LL_As_Vowel_Cases())
		{
			return;
		}

		Encode_LE_Cases(save_current);
	}

	/**
	 * Cases where an L follows D, G, or T at the
	 * end have a schwa pronounced before the L
	 * 
	 */
	void Interpolate_Vowel_When_Cons_L_At_End()
	{
		if(m_encodeVowels == true)
		{
			// e.g. "ertl", "vogl"
			if((m_current == m_last) 
				&& StringAt((m_current - 1), 1, "D", "G", "T", ""))
			{
				MetaphAdd("A");
			}
		}
	}
	
	/**
	 * Catch cases where 'L' spelled twice but pronounced
	 * once, e.g., 'DOCILELY' => TSL
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_LELY_To_L()
	{
		// e.g. "agilely", "docilely"
		if(StringAt((m_current - 1), 5, "ILELY", "")
			&& ((m_current + 3) == m_last))
		{
			MetaphAdd("L");
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode special case "colonel" => KRNL. Can somebody tell
	 * me how this pronounciation came to be?
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_COLONEL()
	{
		if(StringAt((m_current - 2), 7, "COLONEL", ""))
		{
			MetaphAdd("R");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-AULT-", found in a french names
	 * 
	 * @return true if encoding handled in this routine, false if not
	 *  
	 */
	boolean Encode_French_AULT()
	{
		// e.g. "renault" and "foucault", well known to americans, but not "fault"
		if((m_current > 3)
			&& (StringAt((m_current - 3), 5, "RAULT", "NAULT", "BAULT", "SAULT", "GAULT", "CAULT", "")
				|| StringAt((m_current - 4), 6, "REAULT", "RIAULT", "NEAULT", "BEAULT", ""))
			&& !(RootOrInflections(m_inWord, "ASSAULT") 
				|| StringAt((m_current - 8), 10, "SOMERSAULT","")
				|| StringAt((m_current - 9), 11, "SUMMERSAULT", "")))
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-EUIL-", always found in a french word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_French_EUIL()
	{
		// e.g. "auteuil"
		if(StringAt((m_current - 3), 4, "EUIL", "") && (m_current == m_last))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-OULX", always found in a french word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_French_OULX()
	{
		// e.g. "proulx"
		if(StringAt((m_current - 2), 4, "OULX", "") && ((m_current + 1) == m_last))
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encodes contexts where 'L' is not pronounced in "-LM-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_L_In_LM()
	{
		if(StringAt(m_current, 2, "LM", "LN", ""))
		{
			// e.g. "lincoln", "holmes", "psalm", "salmon"
			if((StringAt((m_current - 2), 4, "COLN", "CALM", "BALM", "MALM", "PALM", "")
				|| (StringAt((m_current - 1), 3, "OLM", "") && ((m_current + 1) == m_last))
				|| StringAt((m_current - 3), 5, "PSALM", "QUALM", "")
				|| StringAt((m_current - 2), 6,  "SALMON", "HOLMES", "")
				|| StringAt((m_current - 1), 6,  "ALMOND", "")
				|| ((m_current == 1) && StringAt((m_current - 1), 4, "ALMS", "") ))
				&& (!StringAt((m_current + 2), 1, "A", "") 
					&& !StringAt((m_current - 2), 5, "BALMO", "")
					&& !StringAt((m_current - 2), 6, "PALMER", "PALMOR", "BALMER", "")
					&& !StringAt((m_current - 3), 5, "THALM", "")))
			{
				m_current++;
				return true;
			}
			else
			{
				MetaphAdd("L");
				m_current++;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encodes contexts where '-L-' is silent in 'LK', 'LV'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_L_In_LK_LV()
	{
		if((StringAt((m_current - 2), 4, "WALK", "YOLK", "FOLK", "HALF", "TALK", "CALF", "BALK", "CALK", "")
			|| (StringAt((m_current - 2), 4, "POLK", "") 
				&& !StringAt((m_current - 2), 5, "POLKA", "WALKO", ""))
			|| (StringAt((m_current - 2), 4, "HALV", "") 
				&& !StringAt((m_current - 2), 5, "HALVA", "HALVO", ""))
			|| (StringAt((m_current - 3), 5, "CAULK", "CHALK", "BAULK", "FAULK", "")
				&& !StringAt((m_current - 4), 6, "SCHALK", ""))
			|| (StringAt((m_current - 2), 5, "SALVE", "CALVE", "")
			|| StringAt((m_current - 2), 6, "SOLDER", ""))
			// exceptions to above cases where 'L' is usually pronounced
			&& !StringAt((m_current - 2), 6, "SALVER", "CALVER", ""))
			&& !StringAt((m_current - 5), 9, "GONSALVES", "GONCALVES", "")
			&& !StringAt((m_current - 2), 6, "BALKAN", "TALKAL", "")
			&& !StringAt((m_current - 3), 5, "PAULK", "CHALF", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode 'L' in contexts of "-OULD-" where it is silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_L_In_OULD()
	{
		//'would', 'could'
		if(StringAt((m_current - 3), 5, "WOULD", "COULD", "") 
			|| (StringAt((m_current - 4), 6, "SHOULD", "") 
				&& !StringAt((m_current - 4), 8, "SHOULDER", "")))
		{
			MetaphAddExactApprox("D", "T");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-ILLA-" and "-ILLE-" in spanish and french
	 * contexts were americans know to pronounce it as a 'Y'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_LL_As_Vowel_Special_Cases()
	{
		if(StringAt((m_current - 5), 8, "TORTILLA", "") 
			|| StringAt((m_current - 8), 11, "RATATOUILLE", "")
			// e.g. 'guillermo', "veillard"
			|| (StringAt(0, 5, "GUILL", "VEILL", "GAILL", "")
				// 'guillotine' usually has '-ll-' pronounced as 'L' in english 
				&& !(StringAt((m_current - 3), 7, "GUILLOT", "GUILLOR", "GUILLEN", "")
					|| (StringAt(0, 5, "GUILL", "") && (m_length == 5))))
			// e.g. "brouillard", "gremillion"
			|| StringAt(0, 7, "BROUILL", "GREMILL", "ROBILL", "")
			// e.g. 'mireille'
			|| (StringAt((m_current - 2), 5, "EILLE", "") 
					&& ((m_current + 2) == m_last)
					// exception "reveille" usually pronounced as 're-vil-lee'
					&& !StringAt((m_current - 5), 8, "REVEILLE", "")))
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode other spanish cases where "-LL-" is pronounced as 'Y'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_LL_As_Vowel()
	{
		//spanish e.g. "cabrillo", "gallegos" but also "gorilla", "ballerina" -
		// give both pronounciations since an american might pronounce "cabrillo"
		// in the spanish or the american fashion.
		if((((m_current + 3) == m_length) 
			&& StringAt((m_current - 1), 4, "ILLO", "ILLA", "ALLE", ""))
			|| (((StringAt((m_last - 1), 2, "AS", "OS", "")
					|| StringAt(m_last, 2, "AS", "OS", "")
					|| StringAt(m_last, 1, "A", "O", "")) 
						&& StringAt((m_current - 1), 2, "AL", "IL", ""))
				&& !StringAt((m_current - 1), 4, "ALLA", ""))
			|| StringAt(0, 5, "VILLE", "VILLA", "")
			|| StringAt(0, 8, "GALLARDO", "VALLADAR", "MAGALLAN", "CAVALLAR", "BALLASTE", "")
			|| StringAt(0, 3, "LLA", ""))
		{
			MetaphAdd("L", "");
			m_current += 2;
			return true;
		}
		return false;
	}

	/**
	 * Call routines to encode "-LL-", in proper order
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_LL_As_Vowel_Cases()
	{
		if(CharAt(m_current + 1) == 'L')
		{
			if(Encode_LL_As_Vowel_Special_Cases())
			{
				return true;
			}
			else if(Encode_LL_As_Vowel())
			{
				return true;
			}
			m_current += 2;

		}
		else
		{
			m_current++;
		}

		return false;
	}

	/**
	 * Encode vowel-encoding cases where "-LE-" is pronounced "-EL-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Vowel_LE_Transposition(int save_current)
	{
		// transposition of vowel sound and L occurs in many words,
		// e.g. "bristle", "dazzle", "goggle" => KAKAL
		if(m_encodeVowels 
			&& (save_current > 1) 
			&& !IsVowel(save_current - 1) 
			&& (CharAt(save_current + 1) == 'E')
			&& (CharAt(save_current - 1) != 'L')
			&& (CharAt(save_current - 1) != 'R')
			// lots of exceptions to this:
			&& !IsVowel(save_current + 2)
			&& !StringAt(0, 7, "ECCLESI", "COMPLEC", "COMPLEJ", "ROBLEDO", "")
			&& !StringAt(0, 5, "MCCLE", "MCLEL", "")
			&& !StringAt(0, 6, "EMBLEM", "KADLEC", "")
			&& !(((save_current + 2) == m_last) && StringAt(save_current, 3, "LET", ""))
			&& !StringAt(save_current, 7, "LETTING", "")
			&& !StringAt(save_current, 6, "LETELY", "LETTER", "LETION", "LETIAN", "LETING", "LETORY", "")
			&& !StringAt(save_current, 5, "LETUS", "LETIV", "")
			&& !StringAt(save_current, 4, "LESS", "LESQ", "LECT", "LEDG", "LETE", "LETH", "LETS", "LETT", "")
			&& !StringAt(save_current, 3, "LEG", "LER", "LEX", "")
			// e.g. "complement" !=> KAMPALMENT
			&& !(StringAt(save_current, 6, "LEMENT", "")
				&& !(StringAt((m_current - 5), 6, "BATTLE", "TANGLE", "PUZZLE", "RABBLE", "BABBLE", "")
						|| StringAt((m_current - 4), 5, "TABLE", "")))
			&& !(((save_current + 2) == m_last) && StringAt((save_current - 2), 5, "OCLES", "ACLES", "AKLES", ""))
			&& !StringAt((save_current - 3), 5, "LISLE", "AISLE", "")
			&& !StringAt(0, 4, "ISLE", "")
			&& !StringAt(0, 6, "ROBLES", "")
			&& !StringAt((save_current - 4), 7, "PROBLEM", "RESPLEN", "")
			&& !StringAt((save_current - 3), 6, "REPLEN", "")
			&& !StringAt((save_current - 2), 4, "SPLE", "")
			&& (CharAt(save_current - 1) != 'H')
			&& (CharAt(save_current - 1) != 'W'))
		{
			MetaphAdd("AL");
			flag_AL_inversion = true;

			// eat redundant 'L'
			if(CharAt(save_current + 2) == 'L')
			{
				m_current = save_current + 3;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode special vowel-encoding cases where 'E' is not
	 * silent at the end of a word as is the usual case
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Vowel_Preserve_Vowel_After_L(int save_current)
	{
		// an example of where the vowel would NOT need to be preserved
		// would be, say, "hustled", where there is no vowel pronounced
		// between the 'l' and the 'd'
		if(m_encodeVowels
			&& !IsVowel(save_current - 1) 
			&& (CharAt(save_current + 1) == 'E')
			&& (save_current > 1) 
			&& ((save_current + 1) != m_last) 
			&& !(StringAt((save_current + 1), 2, "ES", "ED", "") 
			&& ((save_current + 2) == m_last))
			&& !StringAt((save_current - 1), 5, "RLEST", "") )
		{
			MetaphAdd("LA");
			m_current = SkipVowels(m_current);
			return true;
		}

		return false;
	}

	/**
	 * Call routines to encode "-LE-", in proper order
	 *
	 * @param save_current index of actual current letter
	 *
	 */
	void Encode_LE_Cases(int save_current)
	{
		if(Encode_Vowel_LE_Transposition(save_current))
		{
			return;
		}
		else
		{
			if(Encode_Vowel_Preserve_Vowel_After_L(save_current))
			{
				return;
			}
			else
			{
				MetaphAdd("L");
			}
		}
	}

	/**
	 * Encode "-M-"
	 * 
	 */
	void Encode_M()
	{
		if(Encode_Silent_M_At_Beginning()
			|| Encode_MR_And_MRS()
			|| Encode_MAC()
			|| Encode_MPT())
		{
			return;
		}

		// Silent 'B' should really be handled
		// under 'B", not here under 'M'!
		Encode_MB();

		MetaphAdd("M");
	}

	/**
	 * Encode cases where 'M' is silent at beginning of word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_M_At_Beginning()
	{    
		//skip these when at start of word
	    if((m_current == 0)
			&& StringAt(m_current, 2, "MN", ""))
		{
	        m_current += 1;
			return true;
		}

		return false;
	}

	/**
	 * Encode special cases "Mr." and "Mrs."
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_MR_And_MRS()
	{
		if((m_current == 0) && StringAt(m_current, 2, "MR", ""))
		{
			// exceptions for "mr." and "mrs."
			if((m_length == 2) && StringAt(m_current, 2, "MR", ""))
			{
				if(m_encodeVowels)
				{
					MetaphAdd("MASTAR");
				}
				else
				{
					MetaphAdd("MSTR");
				}
				m_current += 2;
				return true;
			}
			else if((m_length == 3) && StringAt(m_current, 3, "MRS", "")) 
			{
				if(m_encodeVowels)
				{
					MetaphAdd("MASAS");
				}
				else
				{
					MetaphAdd("MSS");
				}
				m_current += 3;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode "Mac-" and "Mc-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_MAC()
	{
		// should only find irish and 
		// scottish names e.g. 'macintosh'
		if((m_current == 0) 
				&& (StringAt(0, 7, "MACIVER", "MACEWEN", "")
				|| StringAt(0, 8, "MACELROY", "MACILROY", "")
				||  StringAt(0, 9, "MACINTOSH", "")
				|| StringAt(0, 2, "MC", "")	))
		{
			if(m_encodeVowels)
			{
				MetaphAdd("MAK");
			}
			else
			{
				MetaphAdd("MK");
			}
			
			if(StringAt(0, 2, "MC", ""))
			{	
				if(StringAt((m_current + 2), 1, "K", "G", "Q", "")
					// watch out for e.g. "McGeorge"
					&& !StringAt((m_current + 2), 4, "GEOR", ""))
				{
					m_current += 3;				
				}
				else
				{
					m_current += 2;
				}
			}
			else
			{	
				m_current += 3;
			}
				
			return true;
		}

		return false;
	}

	/**
	 * Encode silent 'M' in context of "-MPT-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_MPT()
	{
		if(StringAt((m_current - 2), 8, "COMPTROL", "")
			|| StringAt((m_current - 4), 7, "ACCOMPT", ""))

		{
			MetaphAdd("N");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Test if 'B' is silent in these contexts
	 * 
	 * @return true if 'B' is silent in this context
	 * 
	 */
	boolean Test_Silent_MB_1()
	{
		// e.g. "LAMB", "COMB", "LIMB", "DUMB", "BOMB"
		// Handle combining roots first
		if (((m_current == 3) 
				&& StringAt((m_current - 3), 5, "THUMB", ""))
			|| ((m_current == 2)
				&& StringAt((m_current - 2), 4, "DUMB", "BOMB", "DAMN", "LAMB", "NUMB", "TOMB", "") ))
		{
			return true;
		}

		return false;
	}

	/**
	 * Test if 'B' is pronounced in this context
	 * 
	 * @return true if 'B' is pronounced in this context
	 * 
	 */
	boolean Test_Pronounced_MB()
	{
		if (StringAt((m_current - 2), 6, "NUMBER", "")
			|| (StringAt((m_current + 2), 1, "A", "") 
				&& !StringAt((m_current - 2), 7, "DUMBASS", ""))
			|| StringAt((m_current + 2), 1, "O", "") 
			|| StringAt((m_current - 2), 6, "LAMBEN", "LAMBER", "LAMBET", "TOMBIG", "LAMBRE", ""))
		{
			return true;
		}

		return false;
	}

	/**
	 * Test whether "-B-" is silent in these contexts
	 * 
	 * @return true if 'B' is silent in this context
	 * 
	 */
	boolean Test_Silent_MB_2()
	{
		// 'M' is the current letter
		if ((CharAt(m_current + 1) == 'B') && (m_current > 1)
			&& (((m_current + 1) == m_last)
			// other situations where "-MB-" is at end of root
			// but not at end of word. The tests are for standard
			// noun suffixes.
			// e.g. "climbing" => KLMNK
			|| StringAt((m_current + 2), 3, "ING", "ABL", "")
			|| StringAt((m_current + 2), 4, "LIKE", "")
			|| ((CharAt(m_current + 2) == 'S') && ((m_current + 2) == m_last))
			|| StringAt((m_current - 5), 7, "BUNCOMB", "")
			// e.g. "bomber", 
			|| (StringAt((m_current + 2), 2, "ED", "ER", "") 
			&& ((m_current + 3) == m_last) 
			&& (StringAt(0, 5, "CLIMB", "PLUMB", "")
			// e.g. "beachcomber"
			|| !StringAt((m_current - 1), 5, "IMBER", "AMBER", "EMBER", "UMBER", ""))
			// exceptions
			&& !StringAt((m_current - 2), 6, "CUMBER", "SOMBER", "") ) ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Test if 'B' is pronounced in these "-MB-" contexts
	 * 
	 * @return true if "-B-" is pronounced in these contexts
	 * 
	 */
	boolean Test_Pronounced_MB_2()
	{
		// e.g. "bombastic", "umbrage", "flamboyant"
		if (StringAt((m_current - 1), 5, "OMBAS", "OMBAD", "UMBRA", "")
			|| StringAt((m_current - 3), 4, "FLAM", "") )
		{
			return true;
		}

		return false;
	}

	/**
	 * Tests for contexts where "-N-" is silent when after "-M-"
	 * 
	 * @return true if "-N-" is silent in these contexts
	 * 
	 */
	boolean Test_MN()
	{

		if ((CharAt(m_current + 1) == 'N') 
			&& (((m_current + 1) == m_last)
			// or at the end of a word but followed by suffixes
			|| (StringAt((m_current + 2), 3, "ING", "EST", "") && ((m_current + 4) == m_last))
			|| ((CharAt(m_current + 2) == 'S') && ((m_current + 2) == m_last))
			|| (StringAt((m_current + 2), 2, "LY", "ER", "ED", "") 
				&& ((m_current + 3) == m_last))  
			|| StringAt((m_current - 2), 9, "DAMNEDEST", "")
			|| StringAt((m_current - 5), 9, "GODDAMNIT", "") ))
		{
			return true; 
		}

		return false;
	}

	/**
	 * Call routines to encode "-MB-", in proper order
	 * 
	 */
	void Encode_MB()
	{
		if(Test_Silent_MB_1())
		{
			if(Test_Pronounced_MB())
			{
				m_current++;
			}
			else
			{
				m_current += 2; 
			}
		}
		else if(Test_Silent_MB_2())
		{
			if(Test_Pronounced_MB_2())
			{
				m_current++;
			}
			else
			{
				m_current += 2; 
			}
		}
		else if(Test_MN())
		{
			m_current += 2; 
		}
		else
		{
			// eat redundant 'M'
			if (CharAt(m_current + 1) == 'M')
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}
		}
	}

	/**
	 * Encode "-N-"
	 * 
	 */
	void Encode_N()
	{
		if(Encode_NCE())
		{
			return;
		}

		// eat redundant 'N'
		if(CharAt(m_current + 1) == 'N')
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}

		if (!StringAt((m_current - 3), 8, "MONSIEUR", "")
			// e.g. "aloneness", 
			&& !StringAt((m_current - 3), 6, "NENESS", ""))
		{
			MetaphAdd("N");
		}
	}

	/**
	 * Encode "-NCE-" and "-NSE-"
	 * "entrance" is pronounced exactly the same as "entrants"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_NCE()
	{
		//'acceptance', 'accountancy'
		if(StringAt((m_current + 1), 1, "C", "S", "")
			&& StringAt((m_current + 2), 1, "E", "Y", "I", "")
			&& (((m_current + 2) == m_last)
				|| (((m_current + 3) == m_last)) 
					&& (CharAt(m_current + 3) == 'S')))
		{
			MetaphAdd("NTS");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-P-"
	 * 
	 */
	void Encode_P()
	{
		if(Encode_Silent_P_At_Beginning()
		   || Encode_PT()
		   || Encode_PH()
		   || Encode_PPH()
		   || Encode_RPS()
		   || Encode_COUP()
		   || Encode_PNEUM()
		   || Encode_PSYCH()
		   || Encode_PSALM())
		{
			return;
		}

		Encode_PB();

		MetaphAdd("P");
	}

	/**
	 * Encode cases where "-P-" is silent at the start of a word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_P_At_Beginning()
	{
	    //skip these when at start of word
	    if((m_current == 0)
			&& StringAt(m_current, 2, "PN", "PF", "PS", "PT", ""))
		{
	        m_current += 1;
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "-P-" is silent before "-T-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PT()
	{
		// 'pterodactyl', 'receipt', 'asymptote'
		if((CharAt(m_current + 1) == 'T'))
		{
			if (((m_current == 0) && StringAt(m_current, 5, "PTERO", ""))
				|| StringAt((m_current - 5), 7, "RECEIPT", "")
				|| StringAt((m_current - 4), 8, "ASYMPTOT", ""))
			{
				MetaphAdd("T");
				m_current += 2;
				return true;
			}
		}
		return false;
	}

	/**
	 * Encode "-PH-", usually as F, with exceptions for
	 * cases where it is silent, or where the 'P' and 'T'
	 * are pronounced seperately because they belong to 
	 * two different words in a combining form
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PH()
	{
		if(CharAt(m_current + 1) == 'H')
		{
			// 'PH' silent in these contexts
			if (StringAt(m_current, 9, "PHTHALEIN", "")
				|| ((m_current == 0) && StringAt(m_current, 4, "PHTH", ""))
				|| StringAt((m_current - 3), 10, "APOPHTHEGM", ""))
			{
				MetaphAdd("0");
				m_current += 4;
			}
			// combining forms
			//'sheepherd', 'upheaval', 'cupholder'
			else if((m_current > 0)
				&& (StringAt((m_current + 2), 3, "EAD", "OLE", "ELD", "ILL", "OLD", "EAP", "ERD", 
												 "ARD", "ANG", "ORN", "EAV", "ART", "")
					|| StringAt((m_current + 2), 4, "OUSE", "")
					|| (StringAt((m_current + 2), 2, "AM", "") && !StringAt((m_current -1), 5, "LPHAM", ""))
					|| StringAt((m_current + 2), 5, "AMMER", "AZARD", "UGGER", "")
					|| StringAt((m_current + 2), 6, "OLSTER", ""))
						&& !StringAt((m_current - 3), 5, "LYMPH", "NYMPH", ""))
			{
				MetaphAdd("P");
				AdvanceCounter(3, 2);
			}
			else
			{
				MetaphAdd("F");
				m_current += 2;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode "-PPH-". I don't know why the greek poet's
	 * name is transliterated this way...
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PPH()
	{
		// 'sappho'
		if((CharAt(m_current + 1) == 'P') 
				&& ((m_current + 2) < m_length) && (CharAt(m_current + 2) == 'H'))
		{
			MetaphAdd("F");
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-CORPS-" where "-PS-" not pronounced
	 * since the cognate is here from the french
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_RPS()
	{
		//'-corps-', 'corpsman'
		if(StringAt((m_current - 3), 5, "CORPS", "") 
			&& !StringAt((m_current - 3), 6, "CORPSE", ""))
		{
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-COUP-" where "-P-" is not pronounced
	 * since the word is from the french
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_COUP()
	{
		//'coup'
		if((m_current == m_last) 
			&& StringAt((m_current - 3), 4, "COUP", "")
			&& !StringAt((m_current - 5), 6, "RECOUP", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode 'P' in non-initial contexts of "-PNEUM-" 
	 * where is also silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PNEUM()
	{
		//'-pneum-'
		if(StringAt((m_current + 1), 4, "NEUM", ""))
		{
			MetaphAdd("N");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode special case "-PSYCH-" where two encodings need to be
	 * accounted for in one syllable, one for the 'PS' and one for
	 * the 'CH'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PSYCH()
	{
		//'-psych-'
		if(StringAt((m_current + 1), 4, "SYCH", ""))
		{
			if(m_encodeVowels)
			{
				MetaphAdd("SAK");
			}
			else
			{
				MetaphAdd("SK");
			}

			m_current += 5;
			return true;
		}

		return false;
	}

	/**
	 * Encode 'P' in context of "-PSALM-", where it has
	 * become silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_PSALM()
	{
		//'-psalm-'
		if(StringAt((m_current + 1), 4, "SALM", ""))
		{
			// go ahead and encode entire word
			if(m_encodeVowels)
			{
				MetaphAdd("SAM");
			}
			else
			{
				MetaphAdd("SM");
			}

			m_current += 5;
			return true;
		}

		return false;
	}

	/**
	 * Eat redundant 'B' or 'P'
	 *
	 */
	void Encode_PB()
	{
		// e.g. "campbell", "raspberry"
		// eat redundant 'P' or 'B'
		if(StringAt((m_current + 1), 1, "P", "B", ""))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}

	/**
	 * Encode "-Q-"
	 * 
	 */
	void Encode_Q()
	{
		// current pinyin
		if(StringAt(m_current, 3, "QIN", ""))
		{
			MetaphAdd("X");
			m_current++;
			return;
		}

		// eat redundant 'Q'
		if(CharAt(m_current + 1) == 'Q')
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}

		MetaphAdd("K");
	}

	/**
	 * Encode "-R-"
	 * 
	 */
	void Encode_R()
	{
		if(Encode_RZ())
		{
			return;
		}
		
		if(!Test_Silent_R())
		{
			if(!Encode_Vowel_RE_Transposition())
			{
				MetaphAdd("R");
			}
		}
		
		// eat redundant 'R'; also skip 'S' as well as 'R' in "poitiers"
		if((CharAt(m_current + 1) == 'R') || StringAt((m_current - 6), 8, "POITIERS", ""))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}
	
	/**
	 * Encode "-RZ-" according
	 * to american and polish pronunciations
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_RZ()
	{
		if(StringAt((m_current - 2), 4, "GARZ", "KURZ", "MARZ", "MERZ", "HERZ", "PERZ", "WARZ", "")
			|| StringAt(m_current, 5, "RZANO", "RZOLA", "")
			|| StringAt((m_current - 1), 4, "ARZA", "ARZN", ""))
		{
			return false;
		}
		
		// 'yastrzemski' usually has 'z' silent in
		// united states, but should get 'X' in poland
		if(StringAt((m_current - 4), 11, "YASTRZEMSKI", ""))
		{
			MetaphAdd("R", "X");
			m_current += 2;
			return true;
		}
		// 'BRZEZINSKI' gets two pronunciations
		// in the united states, neither of which
		// are authentically polish
		if(StringAt((m_current - 1), 10, "BRZEZINSKI", ""))
		{
			MetaphAdd("RS", "RJ");
			// skip over 2nd 'Z'
			m_current += 4;
			return true;
		}
		// 'z' in 'rz after voiceless consonant gets 'X'
		// in alternate polish style pronunciation
		else if(StringAt((m_current - 1), 3, "TRZ", "PRZ", "KRZ", "")
				|| (StringAt(m_current, 2, "RZ", "")
						&& (IsVowel(m_current - 1) || (m_current == 0))))
		{
			MetaphAdd("RS", "X");
			m_current += 2;
			return true;
		}
		// 'z' in 'rz after voiceled consonant, vowel, or at
		// beginning gets 'J' in alternate polish style pronunciation
		else if(StringAt((m_current - 1), 3, "BRZ", "DRZ", "GRZ", ""))
		{
			MetaphAdd("RS", "J");
			m_current += 2;
			return true;
		}

		return false;
	}
	
	/**
	 * Test whether 'R' is silent in this context
	 *
	 * @return true if 'R' is silent in this context
	 * 
	 */
	boolean Test_Silent_R()
	{
		// test cases where 'R' is silent, either because the 
		// word is from the french or because it is no longer pronounced.
		// e.g. "rogier", "monsieur", "surburban"
		if(((m_current == m_last)
			// reliably french word ending
			&& StringAt((m_current - 2), 3, "IER", "")
			// e.g. "metier"
			&& (StringAt((m_current - 5), 3, "MET", "VIV", "LUC", "")
			// e.g. "cartier", "bustier"
			|| StringAt((m_current - 6), 4, "CART", "DOSS", "FOUR", "OLIV", "BUST", "DAUM", "ATEL", 
											"SONN", "CORM", "MERC", "PELT", "POIR", "BERN", "FORT", "GREN", 
											"SAUC", "GAGN", "GAUT", "GRAN", "FORC", "MESS", "LUSS", "MEUN", 
											"POTH", "HOLL", "CHEN", "")
			// e.g. "croupier"
			|| StringAt((m_current - 7), 5, "CROUP", "TORCH", "CLOUT", "FOURN", "GAUTH", "TROTT", 
											"DEROS", "CHART", "")
			// e.g. "chevalier"
			|| StringAt((m_current - 8), 6, "CHEVAL", "LAVOIS", "PELLET", "SOMMEL", "TREPAN", "LETELL", "COLOMB", "")
			|| StringAt((m_current - 9), 7, "CHARCUT", "")
			|| StringAt((m_current - 10), 8, "CHARPENT", "")))
			|| StringAt((m_current - 2), 7, "SURBURB", "WORSTED", "")
			|| StringAt((m_current - 2), 9, "WORCESTER", "")
			|| StringAt((m_current - 7), 8, "MONSIEUR", "")
			|| StringAt((m_current - 6), 8, "POITIERS", "") )
		{
			return true;
		}

		return false;
	}

	/**
	 * Encode '-re-" as 'AR' in contexts
	 * where this is the correct pronunciation
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Vowel_RE_Transposition()
	{
		// -re inversion is just like
		// -le inversion
		// e.g. "fibre" => FABAR or "centre" => SANTAR
		if((m_encodeVowels)
			&& (CharAt(m_current + 1) == 'E')
			&& (m_length > 3)
			&& !StringAt(0, 5, "OUTRE", "LIBRE", "ANDRE", "")
			&& !(StringAt(0, 4, "FRED", "TRES", "") && (m_length == 4))
			&& !StringAt((m_current - 2), 5, "LDRED", "LFRED", "NDRED", "NFRED", "NDRES", "TRES", "IFRED", "")
			&& !IsVowel(m_current - 1)
			&& (((m_current + 1) == m_last)
				|| (((m_current + 2) == m_last) 
						&& StringAt((m_current + 2), 1, "D", "S", ""))))
		{
			MetaphAdd("AR");
			return true;
		}

		return false;
	}

	/**
	 * Encode "-S-"
	 * 
	 */
	void Encode_S()
	{
		if(Encode_SKJ()
			|| Encode_Special_SW()
			|| Encode_SJ()
			|| Encode_Silent_French_S_Final()
			|| Encode_Silent_French_S_Internal()
			|| Encode_ISL()
			|| Encode_STL()
			|| Encode_Christmas()
			|| Encode_STHM()
			|| Encode_ISTEN()
			|| Encode_Sugar()
			|| Encode_SH()
			|| Encode_SCH()
			|| Encode_SUR()
			|| Encode_SU()
			|| Encode_SSIO()
			|| Encode_SS()
			|| Encode_SIA()
			|| Encode_SIO()
			|| Encode_Anglicisations()
			|| Encode_SC()
			|| Encode_SEA_SUI_SIER()
			|| Encode_SEA())
		{
			return;
		}

		MetaphAdd("S");

		if(StringAt((m_current + 1), 1, "S", "Z", "")
			&& !StringAt((m_current + 1), 2, "SH", ""))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}

	/**
	 * Encode a couple of contexts where scandinavian, slavic
	 * or german names should get an alternate, native 
	 * pronunciation of 'SV' or 'XV'
	 * 
	 * @return true if handled
	 * 
	 */
	boolean Encode_Special_SW()
	{
		if(m_current == 0)
		{
			// 
			if(Names_Beginning_With_SW_That_Get_Alt_SV())
			{
				MetaphAdd("S", "SV");
				m_current += 2;
				return true;
			}
	
			// 
			if(Names_Beginning_With_SW_That_Get_Alt_XV())
			{
				MetaphAdd("S", "XV");
				m_current += 2;
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Encode "-SKJ-" as X ("sh"), since americans pronounce
	 * the name Dag Hammerskjold as "hammer-shold"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SKJ()
	{
		// scandinavian
		if(StringAt(m_current, 4, "SKJO", "SKJU", "")
			&& IsVowel(m_current + 3))
		{
			MetaphAdd("X");
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode initial swedish "SJ-" as X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SJ()
	{
		if(StringAt(0, 2, "SJ", ""))
		{
			MetaphAdd("X");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode final 'S' in words from the french, where they
	 * are not pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_French_S_Final()
	{
		// "louis" is an exception because it gets two pronuncuations
		if(StringAt(0, 5, "LOUIS", "") && (m_current == m_last))
		{
			MetaphAdd("S", "");
			m_current++;
			return true;			
		}
				
		// french words familiar to americans where final s is silent
		if((m_current == m_last) 
			&& (StringAt(0, 4, "YVES", "")
			|| (StringAt(0, 4, "HORS", "") && (m_current == 3))
			|| StringAt((m_current - 4), 5, "CAMUS", "YPRES", "")
			|| StringAt((m_current - 5), 6, "MESNES", "DEBRIS", "BLANCS", "INGRES", "CANNES", "")
			|| StringAt((m_current - 6), 7, "CHABLIS", "APROPOS", "JACQUES", "ELYSEES", "OEUVRES", 
											"GEORGES", "DESPRES", "")
			|| StringAt(0, 8, "ARKANSAS", "FRANCAIS", "CRUDITES", "BRUYERES", "")
			|| StringAt(0, 9, "DESCARTES", "DESCHUTES", "DESCHAMPS", "DESROCHES", "DESCHENES", "")
			|| StringAt(0, 10, "RENDEZVOUS", "")
			|| StringAt(0, 11, "CONTRETEMPS", "DESLAURIERS", ""))
			|| ((m_current == m_last) 
					&& StringAt((m_current - 2), 2, "AI", "OI", "UI", "") 
					&& !StringAt(0, 4, "LOIS", "LUIS", "")))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode non-final 'S' in words from the french where they
	 * are not pronounced.
	 * 
	 * @return true if encoding handled in this routine, false if not
	 *
	 */
	boolean Encode_Silent_French_S_Internal()
	{
		// french words familiar to americans where internal s is silent
		if(StringAt((m_current - 2), 9, "DESCARTES", "")
			|| StringAt((m_current - 2), 7, "DESCHAM", "DESPRES", "DESROCH", "DESROSI", "DESJARD", "DESMARA", 
						"DESCHEN", "DESHOTE", "DESLAUR", "")
			|| StringAt((m_current - 2), 6, "MESNES", "")
			|| StringAt((m_current - 5), 8, "DUQUESNE", "DUCHESNE", "")
			|| StringAt((m_current - 7), 10, "BEAUCHESNE", "")
			|| StringAt((m_current - 3), 7, "FRESNEL", "")
			|| StringAt((m_current - 3), 9, "GROSVENOR", "")
			|| StringAt((m_current - 4), 10, "LOUISVILLE", "")
			|| StringAt((m_current - 7), 10, "ILLINOISAN", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode silent 'S' in context of "-ISL-"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ISL()
	{
		//special cases 'island', 'isle', 'carlisle', 'carlysle'
		if((StringAt((m_current - 2), 4, "LISL", "LYSL", "AISL", "") 
			&& !StringAt((m_current - 3), 7, "PAISLEY", "BAISLEY", "ALISLAM", "ALISLAH", "ALISLAA", ""))
			|| ((m_current == 1) 
				&& ((StringAt((m_current - 1), 4, "ISLE", "") 
					|| StringAt((m_current - 1), 5, "ISLAN", ""))
					&& !StringAt((m_current - 1), 5, "ISLEY", "ISLER", ""))))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-STL-" in contexts where the 'T' is silent. Also
	 * encode "-USCLE-" in contexts where the 'C' is silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_STL()
	{
		//'hustle', 'bustle', 'whistle'
		if((StringAt(m_current, 4, "STLE", "STLI", "") 
				&& !StringAt((m_current + 2), 4, "LESS", "LIKE", "LINE", ""))
			|| StringAt((m_current - 3), 7, "THISTLY", "BRISTLY",  "GRISTLY", "")
			// e.g. "corpuscle"
			|| StringAt((m_current - 1), 5, "USCLE", ""))
		{
			// KRISTEN, KRYSTLE, CRYSTLE, KRISTLE all pronounce the 't'
			// also, exceptions where "-LING" is a nominalizing suffix
			if(StringAt(0, 7, "KRISTEN", "KRYSTLE", "CRYSTLE", "KRISTLE", "")
				|| StringAt(0, 11, "CHRISTENSEN", "CHRISTENSON", "")
				|| StringAt((m_current - 3), 9, "FIRSTLING", "")
				|| StringAt((m_current - 2), 8,  "NESTLING",  "WESTLING", ""))
			{
				MetaphAdd("ST");
				m_current += 2;
			}
			else
			{
				if(m_encodeVowels 
					&& (CharAt(m_current + 3) == 'E') 
					&& (CharAt(m_current + 4) != 'R')
					&& !StringAt((m_current + 3), 4, "ETTE", "ETTA", "")
					&& !StringAt((m_current + 3), 2, "EY", ""))
				{
					MetaphAdd("SAL");
					flag_AL_inversion = true;			
				}
				else
				{
					MetaphAdd("SL");
				}
				m_current += 3;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode "christmas". Americans always pronounce this as "krissmuss"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Christmas()
	{
		//'christmas'
		if(StringAt((m_current - 4), 8, "CHRISTMA", ""))
		{
			MetaphAdd("SM");
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-STHM-" in contexts where the 'TH'
	 * is silent.
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_STHM()
	{
		//'asthma', 'isthmus'
		if(StringAt(m_current, 4, "STHM", ""))
		{
			MetaphAdd("SM");
			m_current += 4;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-ISTEN-" and "-STNT-" in contexts
	 * where the 'T' is silent
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ISTEN()
	{
		// 't' is silent in verb, pronounced in name
		if(StringAt(0, 8, "CHRISTEN", ""))
		{
			// the word itself
			if(RootOrInflections(m_inWord, "CHRISTEN")
				|| StringAt(0, 11, "CHRISTENDOM", ""))
			{
				MetaphAdd("S", "ST");
			}
			else
			{
				// e.g. 'christenson', 'christene'				
				MetaphAdd("ST");				
			}
			m_current += 2;
			return true;
		}
		
		//e.g. 'glisten', 'listen'
		if(StringAt((m_current - 2), 6, "LISTEN", "RISTEN", "HASTEN", "FASTEN", "MUSTNT", "")
			|| StringAt((m_current - 3), 7,  "MOISTEN", ""))
		{
			MetaphAdd("S");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode special case "sugar"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Sugar()
	{
		//special case 'sugar-'
		if(StringAt(m_current, 5, "SUGAR", ""))
		{
			MetaphAdd("X");
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-SH-" as X ("sh"), except in cases
	 * where the 'S' and 'H' belong to different combining
	 * roots and are therefore pronounced seperately
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SH()
	{
		if(StringAt(m_current, 2, "SH", ""))
		{
			// exception
			if(StringAt((m_current - 2), 8, "CASHMERE", ""))
			{
				MetaphAdd("J");				
				m_current += 2;
				return true;
			}
			
			//combining forms, e.g. 'clotheshorse', 'woodshole'
			if((m_current > 0)
				// e.g. "mishap"
				&& ((StringAt((m_current + 1), 3, "HAP", "") && ((m_current + 3) == m_last))
				// e.g. "hartsheim", "clothshorse"
				|| StringAt((m_current + 1), 4, "HEIM", "HOEK", "HOLM", "HOLZ", "HOOD", "HEAD", "HEID", 
											    "HAAR", "HORS", "HOLE", "HUND", "HELM", "HAWK", "HILL", "")
				// e.g. "dishonor"
				|| StringAt((m_current + 1), 5, "HEART", "HATCH", "HOUSE", "HOUND", "HONOR", "")
				// e.g. "mishear"
				|| (StringAt((m_current + 2), 3, "EAR", "") && ((m_current + 4) == m_last))
				// e.g. "hartshorn"
				|| (StringAt((m_current + 2), 3, "ORN", "") && !StringAt((m_current - 2), 7, "UNSHORN", ""))
				// e.g. "newshour" but not "bashour", "manshour"
				|| (StringAt((m_current + 1), 4, "HOUR", "") 
					&& !(StringAt(0, 7, "BASHOUR", "") || StringAt(0, 8, "MANSHOUR", "") || StringAt(0, 6, "ASHOUR", "") ))
				// e.g. "dishonest", "grasshopper"
				|| StringAt((m_current + 2), 5, "ARMON", "ONEST", "ALLOW", "OLDER", "OPPER", "EIMER", "ANDLE", "ONOUR", "")
				// e.g. "dishabille", "transhumance"
				|| StringAt((m_current + 2), 6, "ABILLE", "UMANCE", "ABITUA", "")))
			{
				if (!StringAt((m_current - 1), 1, "S", ""))
					MetaphAdd("S");
			}
			else
			{
				MetaphAdd("X");
			}

			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-SCH-" in cases where the 'S' is pronounced
	 * seperately from the "CH", in words from the dutch, italian,
	 * and greek where it can be pronounced SK, and german words
	 * where it is pronounced X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SCH()
	{
		// these words were combining forms many centuries ago
		if(StringAt((m_current + 1), 2, "CH", ""))
		{
			if((m_current > 0)
				// e.g. "mischief", "escheat"
				&& (StringAt((m_current + 3), 3, "IEF", "EAT", "")
				// e.g. "mischance"
				|| StringAt((m_current + 3), 4, "ANCE", "ARGE", "")
				// e.g. "eschew"
				|| StringAt(0, 6, "ESCHEW", "")))
			{
				MetaphAdd("S");
				m_current++;
				return true;
			}

			//Schlesinger's rule
			//dutch, danish, italian, greek origin, e.g. "school", "schooner", "schiavone", "schiz-"
			if((StringAt((m_current + 3), 2, "OO", "ER", "EN", "UY", "ED", "EM", "IA", "IZ", "IS", "OL", "")
					&& !StringAt(m_current, 6, "SCHOLT", "SCHISL", "SCHERR", ""))
				|| StringAt((m_current + 3), 3, "ISZ", "")
				|| (StringAt((m_current - 1), 6, "ESCHAT", "ASCHIN", "ASCHAL", "ISCHAE", "ISCHIA", "")
						&& !StringAt((m_current - 2), 8, "FASCHING", ""))
				|| (StringAt((m_current - 1), 5, "ESCHI", "")  && ((m_current + 3) == m_last))
				|| (CharAt(m_current + 3) == 'Y'))
			{
				// e.g. "schermerhorn", "schenker", "schistose"
				if(StringAt((m_current + 3), 2, "ER", "EN", "IS", "")
					&& (((m_current + 4) == m_last) 
						|| StringAt((m_current + 3), 3, "ENK", "ENB", "IST", "")))
				{
					MetaphAdd("X", "SK");
				}
				else
				{
					MetaphAdd("SK");
				}
				m_current += 3;
				return true;
			}
			else
			{	
				MetaphAdd("X");
				m_current += 3;
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode "-SUR<E,A,Y>-" to J, unless it is at the beginning,
	 * or preceeded by 'N', 'K', or "NO"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SUR()
	{
		// 'erasure', 'usury'
		if(StringAt((m_current + 1), 3, "URE", "URA", "URY", ""))
		{
			//'sure', 'ensure'
			if ((m_current == 0)
				|| StringAt((m_current - 1), 1, "N", "K", "")
				|| StringAt((m_current - 2), 2, "NO", ""))
			{
				MetaphAdd("X");
			}
			else
			{
				MetaphAdd("J");
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-SU<O,A>-" to X ("sh") unless it is preceeded by
	 * an 'R', in which case it is encoded to S, or it is
	 * preceeded by a vowel, in which case it is encoded to J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SU()
	{
		//'sensuous', 'consensual'
		if(StringAt((m_current + 1), 2, "UO", "UA", "") && (m_current != 0))
		{
			// exceptions e.g. "persuade"
			if(StringAt((m_current - 1), 4, "RSUA", ""))
			{
				MetaphAdd("S");
			}
			// exceptions e.g. "casual"
			else if(IsVowel(m_current - 1))
			{
				MetaphAdd("J", "S");
			}
			else
			{
				MetaphAdd("X", "S");
			}

			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-SSIO-" in contexts where it is pronounced
	 * either J or X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SSIO()
	{
		if(StringAt((m_current + 1), 4, "SION", ""))
		{
			//"abcission"
			if (StringAt((m_current - 2), 2, "CI", ""))
			{
				MetaphAdd("J");
			}
			//'mission'
			else
			{
				if (IsVowel(m_current - 1))
				{
					MetaphAdd("X");
				}
			}

			AdvanceCounter(4, 2);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-SS-" in contexts where it is pronounced X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SS()
	{
		// e.g. "russian", "pressure"
		if(StringAt((m_current - 1), 5, "USSIA", "ESSUR", "ISSUR", "ISSUE", "")
			// e.g. "hessian", "assurance"
			|| StringAt((m_current - 1), 6, "ESSIAN", "ASSURE", "ASSURA", "ISSUAB", "ISSUAN", "ASSIUS", ""))
		{
			MetaphAdd("X");
			AdvanceCounter(3, 2);
			return true;
		}

		return false;
	}

	/**
	 * Encodes "-SIA-" in contexts where it is pronounced
	 * as X ("sh"), J, or S
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SIA()
	{
		// e.g. "controversial", also "fuchsia", "ch" is silent
		if(StringAt((m_current - 2), 5, "CHSIA", "")
			|| StringAt((m_current - 1), 5, "RSIAL", ""))
		{
			MetaphAdd("X");
			AdvanceCounter(3, 1);
			return true;
		}
		
		// names generally get 'X' where terms, e.g. "aphasia" get 'J'
		if((StringAt(0, 6, "ALESIA", "ALYSIA", "ALISIA", "STASIA", "")
				&& (m_current == 3)
				&& !StringAt(0, 9, "ANASTASIA", ""))
			|| StringAt((m_current - 5), 9, "DIONYSIAN", "")
			|| StringAt((m_current - 5), 8, "THERESIA", ""))
		{
			MetaphAdd("X", "S");
			AdvanceCounter(3, 1);
			return true;
		}

		if((StringAt(m_current, 3, "SIA", "") && ((m_current + 2) == m_last)) 
			|| (StringAt(m_current, 4, "SIAN", "") && ((m_current + 3) == m_last))
			|| StringAt((m_current - 5), 9, "AMBROSIAL", ""))
		{
			if ((IsVowel(m_current - 1) || StringAt((m_current - 1), 1, "R", ""))
				// exclude compounds based on names, or french or greek words
				&& !(StringAt(0, 5, "JAMES", "NICOS", "PEGAS", "PEPYS", "")
				|| StringAt(0, 6, "HOBBES", "HOLMES", "JAQUES", "KEYNES", "")
				|| StringAt(0, 7, "MALTHUS", "HOMOOUS", "")
				|| StringAt(0, 8, "MAGLEMOS", "HOMOIOUS", "")
				|| StringAt(0, 9, "LEVALLOIS", "TARDENOIS", "") 
				|| StringAt((m_current - 4), 5, "ALGES", "") ))
			{
				MetaphAdd("J");
			}
			else
			{
				MetaphAdd("S");
			}
			
			AdvanceCounter(2, 1);
			return true;
		}
		return false;
	}

	/**
	 * Encodes "-SIO-" in contexts where it is pronounced
	 * as J or X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SIO()
	{
		// special case, irish name
		if(StringAt(0, 7, "SIOBHAN", ""))
		{
			MetaphAdd("X");
			AdvanceCounter(3, 1);
			return true;
		}
		
		if(StringAt((m_current + 1), 3, "ION", ""))
		{
			// e.g. "vision", "version"
			if (IsVowel(m_current - 1) || StringAt((m_current - 2), 2, "ER", "UR", ""))
			{
				MetaphAdd("J");
			}
			else // e.g. "declension"
			{
				MetaphAdd("X");
			}

			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "-S-" might well be from a german name
	 * and add encoding of german pronounciation in alternate m_metaph
	 * so that it can be found in a genealogical search
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Anglicisations()
	{
		//german & anglicisations, e.g. 'smith' match 'schmidt', 'snider' match 'schneider'
		//also, -sz- in slavic language altho in hungarian it is pronounced 's'
		if(((m_current == 0) 
			&& StringAt((m_current + 1), 1, "M", "N", "L", ""))
			|| StringAt((m_current + 1), 1, "Z", ""))
		{
			MetaphAdd("S", "X");

			// eat redundant 'Z'
			if(StringAt((m_current + 1), 1, "Z", ""))
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}

			return true;
		}

		return false;
	}

	/**
	 * Encode "-SC<vowel>-" in contexts where it is silent,
	 * or pronounced as X ("sh"), S, or SK
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SC()
	{
		if(StringAt(m_current, 2, "SC", ""))
		{
			// exception 'viscount'
			if(StringAt((m_current - 2), 8, "VISCOUNT", ""))
			{
				m_current += 1;
				return true;
			}

			// encode "-SC<front vowel>-"
			if(StringAt((m_current + 2), 1, "I", "E", "Y", ""))
			{
				// e.g. "conscious"
				if(StringAt((m_current + 2), 4, "IOUS", "") 
					// e.g. "prosciutto"
					|| StringAt((m_current + 2), 3, "IUT", "")
					|| StringAt((m_current - 4), 9, "OMNISCIEN", "")
					// e.g. "conscious"
					|| StringAt((m_current - 3), 8, "CONSCIEN", "CRESCEND", "CONSCION", "")
					|| StringAt((m_current - 2), 6, "FASCIS", ""))
				{
					MetaphAdd("X");
				}
				else if(StringAt(m_current, 7, "SCEPTIC", "SCEPSIS", "")
							|| StringAt(m_current, 5, "SCIVV", "SCIRO", "")
							// commonly pronounced this way in u.s.
							|| StringAt(m_current, 6, "SCIPIO", "")
							|| StringAt((m_current - 2), 10, "PISCITELLI", ""))
				{
					MetaphAdd("SK");
				}
				else
				{
					MetaphAdd("S");
				}
				m_current += 2;
				return true;
			}

			MetaphAdd("SK");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-S<EA,UI,IER>-" in contexts where it is pronounced
	 * as J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SEA_SUI_SIER()
	{
		// "nausea" by itself has => NJ as a more likely encoding. Other forms
		// using "nause-" (see Encode_SEA()) have X or S as more familiar pronounciations
		if((StringAt((m_current - 3), 6, "NAUSEA", "") && ((m_current + 2) == m_last))
			// e.g. "casuistry", "frasier", "hoosier"
			|| StringAt((m_current - 2), 5, "CASUI", "")
			|| (StringAt((m_current - 1), 5, "OSIER", "ASIER", "")
					&& !(StringAt(0, 6, "EASIER","") 
						|| StringAt(0, 5, "OSIER","") 
						|| StringAt((m_current - 2), 6, "ROSIER", "MOSIER", ""))))
		{
			MetaphAdd("J", "X");
			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "-SE-" is pronounced as X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_SEA()
	{
		if((StringAt(0, 4, "SEAN", "") && ((m_current + 3) == m_last))
			|| (StringAt((m_current - 3), 6, "NAUSEO", "")
			&& !StringAt((m_current - 3), 7, "NAUSEAT", "")))
		{
			MetaphAdd("X");
			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-T-"
	 * 
	 */
	void Encode_T()
	{
		if(Encode_T_Initial()
			|| Encode_TCH()
			|| Encode_Silent_French_T()
			|| Encode_TUN_TUL_TUA_TUO()
			|| Encode_TUE_TEU_TEOU_TUL_TIE()
			|| Encode_TUR_TIU_Suffixes()
			|| Encode_TI()
			|| Encode_TIENT()
			|| Encode_TSCH()
			|| Encode_TZSCH()
			|| Encode_TH_Pronounced_Separately()
			|| Encode_TTH()
			|| Encode_TH())
		{
			return;
		}

		// eat redundant 'T' or 'D'
		if(StringAt((m_current + 1), 1, "T", "D", ""))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}

		MetaphAdd("T");
	}

	/**
	 * Encode some exceptions for initial 'T'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_T_Initial()
	{
		if(m_current == 0)
		{
			// americans usually pronounce "tzar" as "zar"
			if (StringAt((m_current + 1), 3, "SAR", "ZAR", ""))
			{
				m_current++;
				return true;
			}

			// old 'École française d'Extrême-Orient' chinese pinyin where 'ts-' => 'X'
			if (((m_length == 3) && StringAt((m_current + 1), 2, "SO", "SA", "SU", ""))
				||	((m_length == 4) && StringAt((m_current + 1), 3, "SAO", "SAI", ""))
				||	((m_length == 5) && StringAt((m_current + 1), 4, "SING", "SANG", "")))
			{
				MetaphAdd("X");
				AdvanceCounter(3, 2);
				return true;
			}
			
			// "TS<vowel>-" at start can be pronounced both with and without 'T'
			if (StringAt((m_current + 1), 1, "S", "") && IsVowel(m_current + 2))
			{
				MetaphAdd("TS", "S");
				AdvanceCounter(3, 2);
				return true;
			}
			
			// e.g. "Tjaarda"
			if (StringAt((m_current + 1), 1, "J", ""))
			{
				MetaphAdd("X");
				AdvanceCounter(3, 2);
				return true;
			}

			// cases where initial "TH-" is pronounced as T and not 0 ("th")
			if ((StringAt((m_current + 1), 2, "HU", "") && (m_length == 3))
				|| StringAt((m_current + 1), 3, "HAI", "HUY", "HAO", "")
				|| StringAt((m_current + 1), 4, "HYME", "HYMY", "HANH", "")
				|| StringAt((m_current + 1), 5, "HERES", ""))
			{
				MetaphAdd("T");
				AdvanceCounter(3, 2);
				return true;
			}
		}

		return false;
	}

	/**
	 * Encode "-TCH-", reliably X ("sh", or in this case, "ch")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TCH()
	{
		if(StringAt((m_current + 1), 2, "CH", ""))
		{
			MetaphAdd("X");
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode the many cases where americans are aware that a certain word is
	 * french and know to not pronounce the 'T'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * TOUCHET CHABOT BENOIT
	 */
	boolean Encode_Silent_French_T()
	{
		// french silent T familiar to americans
		if(((m_current == m_last) && StringAt((m_current - 4), 5, "MONET", "GENET", "CHAUT", ""))
			|| StringAt((m_current - 2), 9, "POTPOURRI", "")
			|| StringAt((m_current - 3), 9, "BOATSWAIN", "")
			|| StringAt((m_current - 3), 8, "MORTGAGE", "")
			|| (StringAt((m_current - 4), 5, "BERET", "BIDET", "FILET", "DEBUT", "DEPOT", "PINOT", "TAROT", "")
			|| StringAt((m_current - 5), 6, "BALLET", "BUFFET", "CACHET", "CHALET", "ESPRIT", "RAGOUT", "GOULET",
											"CHABOT", "BENOIT", "")
			|| StringAt((m_current - 6), 7, "GOURMET", "BOUQUET", "CROCHET", "CROQUET", "PARFAIT", "PINCHOT", 
											"CABARET", "PARQUET", "RAPPORT", "TOUCHET", "COURBET", "DIDEROT", "")
			|| StringAt((m_current - 7), 8, "ENTREPOT", "CABERNET", "DUBONNET", "MASSENET", "MUSCADET", "RICOCHET", "ESCARGOT", "")
			|| StringAt((m_current - 8), 9, "SOBRIQUET", "CABRIOLET", "CASSOULET", "OUBRIQUET", "CAMEMBERT", ""))
			&& !StringAt((m_current + 1), 2, "AN", "RY", "IC", "OM", "IN", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TU<N,L,A,O>-" in cases where it is pronounced
	 * X ("sh", or in this case, "ch")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TUN_TUL_TUA_TUO()
	{
		// e.g. "fortune", "fortunate"               
		if(StringAt((m_current - 3), 6, "FORTUN", "")
			// e.g. "capitulate"
			|| (StringAt(m_current, 3, "TUL", "")
				&& (IsVowel(m_current - 1) && IsVowel(m_current + 3)))
			// e.g. "obituary", "barbituate"
			||  StringAt((m_current - 2), 5, "BITUA", "BITUE", "")
			// e.g. "actual"
			|| ((m_current > 1) && StringAt(m_current, 3, "TUA", "TUO", "")))
		{
			MetaphAdd("X", "T");
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-T<vowel>-" forms where 'T' is pronounced as X 
	 * ("sh", or in this case "ch")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TUE_TEU_TEOU_TUL_TIE()
	{
		// 'constituent', 'pasteur'
		if(StringAt((m_current + 1), 4, "UENT", "")
			|| StringAt((m_current - 4), 9, "RIGHTEOUS",  "")
			|| StringAt((m_current - 3), 7, "STATUTE",  "")
			|| StringAt((m_current - 3), 7, "AMATEUR",  "")
			// e.g. "blastula", "pasteur"
			|| (StringAt((m_current - 1), 5, "NTULE", "NTULA", "STULE", "STULA", "STEUR", ""))
			// e.g. "statue"
			|| (((m_current + 2) == m_last) && StringAt(m_current, 3, "TUE", ""))
			// e.g. "constituency"
			|| StringAt(m_current, 5, "TUENC", "")
			// e.g. "statutory"
			|| StringAt((m_current - 3), 8, "STATUTOR", "")
			// e.g. "patience"
			|| (((m_current + 5) == m_last) && StringAt(m_current, 6, "TIENCE", "")))
		{
			MetaphAdd("X", "T");
			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TU-" forms in suffixes where it is usually
	 * pronounced as X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TUR_TIU_Suffixes()
	{
		// 'adventure', 'musculature'
		if((m_current > 0) && StringAt((m_current + 1), 3, "URE", "URA", "URI", "URY", "URO", "IUS", ""))
		{
			// exceptions e.g. 'tessitura', mostly from romance languages
			if ((StringAt((m_current + 1), 3, "URA", "URO", "") 
				//&& !StringAt((m_current + 1), 4, "URIA", "") 
				&& ((m_current + 3) == m_last))
				&& !StringAt((m_current - 3), 7, "VENTURA", "")
				// e.g. "kachaturian", "hematuria"
				|| StringAt((m_current + 1), 4, "URIA", ""))
			{
				MetaphAdd("T");
			}
			else
			{
				MetaphAdd("X", "T");
			}

			AdvanceCounter(2, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TI<O,A,U>-" as X ("sh"), except
	 * in cases where it is part of a combining form,
	 * or as J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TI()
	{
		// '-tio-', '-tia-', '-tiu-'
		// except combining forms where T already pronounced e.g 'rooseveltian'
		if((StringAt((m_current + 1), 2, "IO", "") && !StringAt((m_current - 1), 5, "ETIOL", ""))
			|| StringAt((m_current + 1), 3, "IAL", "")
			|| StringAt((m_current - 1), 5, "RTIUM", "ATIUM", "")
			|| ((StringAt((m_current + 1), 3, "IAN", "") && (m_current > 0))
					&& !(StringAt((m_current - 4), 8, "FAUSTIAN", "")
			|| StringAt((m_current - 5), 9, "PROUSTIAN", "")
			|| StringAt((m_current - 2), 7, "TATIANA", "")
			||(StringAt((m_current - 3), 7, "KANTIAN", "GENTIAN", "")
			|| StringAt((m_current - 8), 12, "ROOSEVELTIAN", "")))
			|| (((m_current + 2) == m_last) 
					&& StringAt(m_current, 3, "TIA", "")
			// exceptions to above rules where the pronounciation is usually X
			&& !(StringAt((m_current - 3), 6, "HESTIA", "MASTIA", "")
				|| StringAt((m_current - 2), 5, "OSTIA", "")
				|| StringAt(0, 3, "TIA", "")
				|| StringAt((m_current - 5), 8, "IZVESTIA", "")))
			|| StringAt((m_current + 1), 4, "IATE", "IATI", "IABL", "IATO", "IARY", "")
			|| StringAt((m_current - 5), 9, "CHRISTIAN", "")))
		{
			if(((m_current == 2) && StringAt(0, 4, "ANTI", ""))
				|| StringAt(0, 5, "PATIO", "PITIA", "DUTIA", ""))
			{
				MetaphAdd("T");
			}
			else if(StringAt((m_current - 4), 8, "EQUATION", ""))
			{
				MetaphAdd("J");
			}
			else
			{
				if(StringAt(m_current, 4, "TION", ""))
				{
					MetaphAdd("X");
				}
				else if(StringAt(0, 5, "KATIA", "LATIA", ""))
				{
					MetaphAdd("T", "X");
				}
				else
				{
					MetaphAdd("X", "T");
				}
			}

			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TIENT-" where "TI" is pronounced X ("sh")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TIENT()
	{
		// e.g. 'patient'
		if(StringAt((m_current + 1), 4, "IENT", ""))
		{
			MetaphAdd("X", "T");
			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TSCH-" as X ("ch")
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TSCH()
	{
		//'deutsch'
		if(StringAt(m_current, 4, "TSCH", "")
			// combining forms in german where the 'T' is pronounced seperately
			&& !StringAt((m_current - 3), 4, "WELT", "KLAT", "FEST", ""))
		{
			// pronounced the same as "ch" in "chit" => X
			MetaphAdd("X");
			m_current += 4;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TZSCH-" as X ("ch")
	 * 
	 * "Neitzsche is peachy"
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TZSCH()
	{
		//'neitzsche'
		if(StringAt(m_current, 5, "TZSCH", ""))
		{
			MetaphAdd("X");
			m_current += 5;
			return true;
		}

		return false;
	}

	/**
	 * Encodes cases where the 'H' in "-TH-" is the beginning of
	 * another word in a combining form, special cases where it is
	 * usually pronounced as 'T', and a special case where it has
	 * become pronounced as X ("sh", in this case "ch")
	 *
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TH_Pronounced_Separately()
	{
		//'adulthood', 'bithead', 'apartheid'
		if(((m_current > 0) 
				&& StringAt((m_current + 1), 4, "HOOD", "HEAD", "HEID", "HAND", "HILL", "HOLD", 
												"HAWK", "HEAP", "HERD", "HOLE", "HOOK", "HUNT",   
												"HUMO", "HAUS", "HOFF", "HARD", "")
				&& !StringAt((m_current - 3), 5, "SOUTH", "NORTH", ""))
			|| StringAt((m_current + 1), 5, "HOUSE", "HEART", "HASTE", "HYPNO", "HEQUE", "")
			// watch out for greek root "-thallic"
			|| (StringAt((m_current + 1), 4, "HALL", "") 
				&& ((m_current + 4) == m_last)
				&& !StringAt((m_current - 3), 5, "SOUTH", "NORTH", "")) 
			|| (StringAt((m_current + 1), 3, "HAM", "") 
					&& ((m_current + 3) == m_last) 
					&& !(StringAt(0, 6, "GOTHAM", "WITHAM", "LATHAM", "")
						 || StringAt(0, 7, "BENTHAM", "WALTHAM", "WORTHAM", "")
						 || StringAt(0, 8, "GRANTHAM", "")))
			|| (StringAt((m_current + 1), 5, "HATCH", "")
			&& !((m_current == 0) || StringAt((m_current - 2), 8, "UNTHATCH", "")))
			|| StringAt((m_current - 3), 7, "WARTHOG", "")
			// and some special cases where "-TH-" is usually pronounced 'T'
			|| StringAt((m_current - 2), 6, "ESTHER", "")
			|| StringAt((m_current - 3), 6, "GOETHE", "")
			|| StringAt((m_current - 2), 8, "NATHALIE", ""))
		{
			// special case
			if (StringAt((m_current - 3), 7, "POSTHUM", ""))
			{
				MetaphAdd("X");
			}
			else
			{
				MetaphAdd("T");
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode the "-TTH-" in "matthew", eating the redundant 'T'
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TTH()
	{
		// 'matthew' vs. 'outthink'
		if(StringAt(m_current, 3, "TTH", ""))
		{
			if (StringAt((m_current - 2), 5, "MATTH", ""))
			{
				MetaphAdd("0");
			}
			else
			{
				MetaphAdd("T0");
			}
			m_current += 3;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-TH-". 0 (zero) is used in Metaphone to encode this sound
	 * when it is pronounced as a dipthong, either voiced or unvoiced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_TH()
	{
		if(StringAt(m_current, 2, "TH", "") )
		{
			//'-clothes-'
			if(StringAt((m_current - 3), 7, "CLOTHES", ""))
			{
				// vowel already encoded so skip right to S
				m_current += 3;
				return true;
			}

			//special case "thomas", "thames", "beethoven" or germanic words
			if(StringAt((m_current + 2), 4, "OMAS", "OMPS", "OMPK", "OMSO", "OMSE", 
											"AMES", "OVEN", "OFEN", "ILDA", "ILDE", "") 
				|| (StringAt(0, 4, "THOM", "")  && (m_length == 4))
				|| (StringAt(0, 5, "THOMS", "")  && (m_length == 5))
				|| StringAt(0, 4, "VAN ", "VON ", "") 
				|| StringAt(0, 3, "SCH", ""))
			{
				MetaphAdd("T");

			}
			else
			{
				// give an 'etymological' 2nd
				// encoding for "smith"
				if(StringAt(0, 2, "SM", ""))
				{
					MetaphAdd("0", "T");
				}
				else
				{
					MetaphAdd("0");
				}
			}

			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-V-"
	 * 
	 */
	void Encode_V()
	{
		// eat redundant 'V'
		if(CharAt(m_current + 1) == 'V')
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}

		MetaphAddExactApprox("V", "F");
	}

	/**
	 * Encode "-W-"
	 * 
	 */
	void Encode_W()
	{
		if(Encode_Silent_W_At_Beginning()
			|| Encode_WITZ_WICZ()
			|| Encode_WR()
			|| Encode_Initial_W_Vowel()
			|| Encode_WH()
			|| Encode_Eastern_European_W())
		{
			return;
		}

		// e.g. 'zimbabwe'
		if(m_encodeVowels
			&& StringAt(m_current, 2, "WE", "")
			&& ((m_current + 1) == m_last))
		{
			MetaphAdd("A");
		}
		
		//else skip it
		m_current++;

	}

	/**
	 * Encode cases where 'W' is silent at beginning of word
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Silent_W_At_Beginning()
	{    
		//skip these when at start of word
	    if((m_current == 0)
			&& StringAt(m_current, 2, "WR", ""))
		{
	        m_current += 1;
			return true;
		}

		return false;
	}

	/**
	 * Encode polish patronymic suffix, mapping
	 * alternate spellings to the same encoding,
	 * and including easern european pronounciation
	 * to the american so that both forms can
	 * be found in a genealogy search
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_WITZ_WICZ()
	{
		//polish e.g. 'filipowicz'
		if(((m_current + 3) == m_last) && StringAt(m_current, 4, "WICZ", "WITZ", ""))
		{
			if(m_encodeVowels)
			{
				if((m_primary.length() > 0)
					&& m_primary.charAt(m_primary.length() - 1) == 'A')
				{
					MetaphAdd("TS", "FAX");
				}
				else
				{
					MetaphAdd("ATS", "FAX");					
				}
			}
			else
			{
				MetaphAdd("TS", "FX");
			}
			m_current += 4;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-WR-" as R ('W' always effectively silent)
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_WR()
	{
		//can also be in middle of word
		if(StringAt(m_current, 2, "WR", ""))
		{
			MetaphAdd("R");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "W-", adding central and eastern european
	 * pronounciations so that both forms can be found
	 * in a genealogy search
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_W_Vowel()
	{
		if((m_current == 0) && IsVowel(m_current + 1))
		{
			//Witter should match Vitter
			if(Germanic_Or_Slavic_Name_Beginning_With_W())
			{
				if(m_encodeVowels)
				{
					MetaphAddExactApprox("A", "VA", "A", "FA");
				}
				else
				{
					MetaphAddExactApprox("A", "V", "A", "F");
				}
			}
			else
			{
				MetaphAdd("A");
			}

			m_current++;
			// don't encode vowels twice
			m_current = SkipVowels(m_current);
			return true;
		}

		return false;
	}

	/**
	 * Encode "-WH-" either as H, or close enough to 'U' to be
	 * considered a vowel
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_WH()
	{
		if(StringAt(m_current, 2, "WH", ""))
		{
			// cases where it is pronounced as H
			// e.g. 'who', 'whole'
			if((CharAt(m_current + 2) == 'O')
				// exclude cases where it is pronounced like a vowel
				&& !(StringAt((m_current + 2), 4, "OOSH", "")
				|| StringAt((m_current + 2), 3, "OOP", "OMP", "ORL", "ORT", "")
				|| StringAt((m_current + 2), 2, "OA", "OP", "")))
			{
				MetaphAdd("H");
				AdvanceCounter(3, 2);
				return true;
			}
			else
			{
				// combining forms, e.g. 'hollowhearted', 'rawhide'
				if(StringAt((m_current + 2), 3, "IDE", "ARD", "EAD", "AWK", "ERD", 
												"OOK", "AND", "OLE", "OOD", "")
					|| StringAt((m_current + 2), 4, "EART", "OUSE", "OUND", "")
					|| StringAt((m_current + 2), 5, "AMMER", ""))
				{
					MetaphAdd("H");
					m_current += 2;
					return true;
				}
				else if(m_current == 0)
				{
					MetaphAdd("A");
					m_current += 2;
					// don't encode vowels twice
					m_current = SkipVowels(m_current);
					return true;
				}
			}
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-W-" when in eastern european names, adding
	 * the eastern european pronounciation to the american so
	 * that both forms can be found in a genealogy search
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Eastern_European_W()
	{
		//Arnow should match Arnoff
		if(((m_current == m_last) && IsVowel(m_current - 1)) 
			|| StringAt((m_current - 1), 5, "EWSKI", "EWSKY", "OWSKI", "OWSKY", "")
			|| (StringAt(m_current, 5, "WICKI", "WACKI", "") && ((m_current + 4) == m_last))
			|| StringAt(m_current, 4, "WIAK", "") && ((m_current + 3) == m_last)
			|| StringAt(0, 3, "SCH", ""))
		{
			MetaphAddExactApprox("", "V", "", "F");
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-X-"
	 * 
	 */
	void Encode_X()
	{
		if(Encode_Initial_X()
			|| Encode_Greek_X()
			|| Encode_X_Special_Cases()
			|| Encode_X_To_H()
			|| Encode_X_Vowel()
			|| Encode_French_X_Final())
		{
			return;
		}

		// eat redundant 'X' or other redundant cases
		if(StringAt((m_current + 1), 1, "X", "Z", "S", "")
			// e.g. "excite", "exceed"
			|| StringAt((m_current + 1), 2, "CI", "CE", ""))
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}

	/**
	 * Encode initial X where it is usually pronounced as S
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Initial_X()
	{		
		// current chinese pinyin spelling
		if(StringAt(0, 3, "XIA", "XIO", "XIE", "")
			|| StringAt(0, 2, "XU", ""))
		{
			MetaphAdd("X");
			m_current++;
			return true;
		}
		
		// else
		if((m_current == 0))
		{
			MetaphAdd("S");
			m_current++;
			return true;
		}

		return false;
	}
	
	/**
	 * Encode X when from greek roots where it is usually pronounced as S
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_Greek_X()
	{
		// 'xylophone', xylem', 'xanthoma', 'xeno-'
		if(StringAt((m_current + 1), 3, "YLO", "YLE", "ENO", "")
			|| StringAt((m_current + 1), 4, "ANTH", ""))
		{
			MetaphAdd("S");
			m_current++;
			return true;
		}

		return false;
	}
	
	/**
	 * Encode special cases, "LUXUR-", "Texeira"
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_X_Special_Cases()
	{
		// 'luxury'
		if(StringAt((m_current - 2), 5, "LUXUR", ""))
		{
			MetaphAddExactApprox("GJ", "KJ");
			m_current++;
			return true;
		}
		
		// 'texeira' portuguese/galician name
		if(StringAt(0, 7, "TEXEIRA", "")
			|| StringAt(0, 8, "TEIXEIRA", ""))
		{
			MetaphAdd("X");
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode special case where americans know the
	 * proper mexican indian pronounciation of this name
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_X_To_H()
	{
		// TODO: look for other mexican indian words
		// where 'X' is usually pronounced this way
		if(StringAt((m_current - 2), 6, "OAXACA", "")
			|| StringAt((m_current - 3), 7, "QUIXOTE", ""))
		{
			MetaphAdd("H");
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-X-" in vowel contexts where it is usually 
	 * pronounced KX ("ksh")
	 * account also for BBC pronounciation of => KS
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_X_Vowel()
	{
		// e.g. "sexual", "connexion" (british), "noxious"
		if(StringAt((m_current + 1), 3, "UAL", "ION", "IOU", ""))
		{
			MetaphAdd("KX", "KS");
			AdvanceCounter(3, 1);
			return true;
		}

		return false;
	}

	/**
	 * Encode cases of "-X", encoding as silent when part
	 * of a french word where it is not pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_French_X_Final()
	{
		//french e.g. "breaux", "paix"
		if(!((m_current == m_last) 
			&& (StringAt((m_current - 3), 3, "IAU", "EAU", "IEU", "") 
			|| StringAt((m_current - 2), 2, "AI", "AU", "OU", "OI", "EU", ""))) )
		{
			MetaphAdd("KS");
		}

		return false;
	}

	/**
	 * Encode "-Z-"
	 * 
	 */
	void Encode_Z()
	{
		if(Encode_ZZ()
			|| Encode_ZU_ZIER_ZS()
			|| Encode_French_EZ()
			|| Encode_German_Z())
		{
			return;
		}

		if(Encode_ZH())
		{
			return;
		}
		else
		{
			MetaphAdd("S");
		}

		// eat redundant 'Z'
		if(CharAt(m_current + 1) == 'Z')
		{
			m_current += 2;
		}
		else
		{
			m_current++;
		}
	}

	/**
	 * Encode cases of "-ZZ-" where it is obviously part
	 * of an italian word where "-ZZ-" is pronounced as TS
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ZZ()
	{
		// "abruzzi", 'pizza' 
		if((CharAt(m_current + 1) == 'Z') 
			&& ((StringAt((m_current + 2), 1, "I", "O", "A", "") 
			&& ((m_current + 2) == m_last))
			|| StringAt((m_current - 2), 9, "MOZZARELL", "PIZZICATO", "PUZZONLAN", "")))
		{
			MetaphAdd("TS", "S");
			m_current += 2;
			return true;
		}

		return false;
	}

	/**
	 * Encode special cases where "-Z-" is pronounced as J
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ZU_ZIER_ZS()
	{
		if(((m_current == 1) && StringAt((m_current - 1), 4, "AZUR", "")) 
			|| (StringAt(m_current, 4, "ZIER", "") 
					&& !StringAt((m_current - 2), 6, "VIZIER", ""))
			|| StringAt(m_current, 3, "ZSA", ""))
		{
			MetaphAdd("J", "S");
			
			if(StringAt(m_current, 3, "ZSA", ""))
			{
				m_current += 2;
			}
			else
			{
				m_current++;
			}
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where americans recognize "-EZ" as part
	 * of a french word where Z not pronounced
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_French_EZ()
	{
		if(((m_current == 3) && StringAt((m_current - 3), 4, "CHEZ", ""))
			|| StringAt((m_current - 5), 6, "RENDEZ", ""))
		{
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode cases where "-Z-" is in a german word
	 * where Z => TS in german
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_German_Z()
	{
		if(((m_current == 2) && ((m_current + 1) == m_last) && StringAt((m_current - 2), 4, "NAZI", ""))
			|| StringAt((m_current - 2), 6, "NAZIFY", "MOZART", "")
			|| StringAt((m_current - 3), 4, "HOLZ", "HERZ", "MERZ", "FITZ", "")
			|| (StringAt((m_current - 3), 4, "GANZ", "") && !IsVowel(m_current + 1))
			|| StringAt((m_current - 4), 5, "STOLZ", "PRINZ", "")
			|| StringAt((m_current - 4), 7, "VENEZIA", "")
			|| StringAt((m_current - 3), 6, "HERZOG", "")
			// german words beginning with "sch-" but not schlimazel, schmooze
			|| (m_inWord.contains("SCH") && !(StringAt((m_last - 2), 3, "IZE", "OZE", "ZEL", "")))
			|| ((m_current > 0) && StringAt(m_current, 4, "ZEIT", ""))
			|| StringAt((m_current - 3), 4, "WEIZ", ""))
		{
			if((m_current > 0) && m_inWord.charAt(m_current - 1) == 'T')
			{
				MetaphAdd("S");
			}
			else
			{
				MetaphAdd("TS");
			}
			m_current++;
			return true;
		}

		return false;
	}

	/**
	 * Encode "-ZH-" as J 
	 * 
	 * @return true if encoding handled in this routine, false if not
	 * 
	 */
	boolean Encode_ZH()
	{
		//chinese pinyin e.g. 'zhao', also english "phonetic spelling"
		if(CharAt(m_current + 1) == 'H')
		{
			MetaphAdd("J");
			m_current += 2;
			return true;
		}

		return false;
	}
	
	/**
	 * Test for names derived from the swedish,
	 * dutch, or slavic that should get an alternate
	 * pronunciation of 'SV' to match the native
	 * version
	 * 
	 * @return true if swedish, dutch, or slavic derived name
	 */
	boolean Names_Beginning_With_SW_That_Get_Alt_SV()
	{
		if(StringAt(0, 7, "SWANSON", "SWENSON", "SWINSON", "SWENSEN", 
						  "SWOBODA", "")
			|| StringAt(0, 9, "SWIDERSKI", "SWARTHOUT", "")
			|| StringAt(0, 10, "SWEARENGIN", ""))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Test for names derived from the german
	 * that should get an alternate pronunciation
	 * of 'XV' to match the german version spelled
	 * "schw-"
	 * 
	 * @return true if german derived name
	 */
	boolean Names_Beginning_With_SW_That_Get_Alt_XV()
	{
		if(StringAt(0, 5, "SWART", "")
			|| StringAt(0, 6, "SWARTZ", "SWARTS", "SWIGER", "")
			|| StringAt(0, 7, "SWITZER", "SWANGER", "SWIGERT", 
						      "SWIGART", "SWIHART", "")
			|| StringAt(0, 8, "SWEITZER", "SWATZELL", "SWINDLER", "")
			|| StringAt(0, 9, "SWINEHART", "")
			|| StringAt(0, 10, "SWEARINGEN", ""))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Test whether the word in question
	 * is a name of germanic or slavic origin, for
	 * the purpose of determining whether to add an
	 * alternate encoding of 'V' 
	 * 
	 * @return true if germanic or slavic name
	 */
	boolean Germanic_Or_Slavic_Name_Beginning_With_W()
	{
		if(StringAt(0, 3, "WEE", "WIX", "WAX", "")
			|| StringAt(0, 4, "WOLF", "WEIS", "WAHL", "WALZ", "WEIL", "WERT", 
							  "WINE", "WILK", "WALT", "WOLL", "WADA", "WULF", 
							  "WEHR", "WURM", "WYSE", "WENZ", "WIRT", "WOLK", 
							  "WEIN", "WYSS", "WASS", "WANN", "WINT", "WINK", 
							  "WILE", "WIKE", "WIER", "WELK", "WISE", "")
			|| StringAt(0, 5, "WIRTH", "WIESE", "WITTE", "WENTZ", "WOLFF", "WENDT", 
							  "WERTZ", "WILKE", "WALTZ", "WEISE", "WOOLF", "WERTH", 
							  "WEESE", "WURTH", "WINES", "WARGO", "WIMER", "WISER", 
							  "WAGER", "WILLE", "WILDS", "WAGAR", "WERTS", "WITTY", 
							  "WIENS", "WIEBE", "WIRTZ", "WYMER", "WULFF", "WIBLE", 
							  "WINER", "WIEST", "WALKO", "WALLA", "WEBRE", "WEYER", 
							  "WYBLE", "WOMAC", "WILTZ", "WURST", "WOLAK", "WELKE", 
							  "WEDEL", "WEIST", "WYGAN", "WUEST", "WEISZ", "WALCK", 
							  "WEITZ", "WYDRA", "WANDA", "WILMA", "WEBER", "")
			|| StringAt(0, 6, "WETZEL", "WEINER", "WENZEL", "WESTER", "WALLEN", "WENGER", 
							  "WALLIN", "WEILER", "WIMMER", "WEIMER", "WYRICK", "WEGNER", 
							  "WINNER", "WESSEL", "WILKIE", "WEIGEL", "WOJCIK", "WENDEL", 
							  "WITTER", "WIENER", "WEISER", "WEXLER", "WACKER", "WISNER", 
							  "WITMER", "WINKLE", "WELTER", "WIDMER", "WITTEN", "WINDLE", 
							  "WASHER", "WOLTER", "WILKEY", "WIDNER", "WARMAN", "WEYANT", 
							  "WEIBEL", "WANNER", "WILKEN", "WILTSE", "WARNKE", "WALSER", 
							  "WEIKEL", "WESNER", "WITZEL", "WROBEL", "WAGNON", "WINANS", 
							  "WENNER", "WOLKEN", "WILNER", "WYSONG", "WYCOFF", "WUNDER", 
							  "WINKEL", "WIDMAN", "WELSCH", "WEHNER", "WEIGLE", "WETTER", 
							  "WUNSCH", "WHITTY", "WAXMAN", "WILKER", "WILHAM", "WITTIG", 
							  "WITMAN", "WESTRA", "WEHRLE", "WASSER", "WILLER", "WEGMAN", 
							  "WARFEL", "WYNTER", "WERNER", "WAGNER", "WISSER", "")
			|| StringAt(0, 7, "WISEMAN", "WINKLER", "WILHELM", "WELLMAN", "WAMPLER", "WACHTER", 
							  "WALTHER", "WYCKOFF", "WEIDNER", "WOZNIAK", "WEILAND", "WILFONG", 
							  "WIEGAND", "WILCHER", "WIELAND", "WILDMAN", "WALDMAN", "WORTMAN", 
							  "WYSOCKI", "WEIDMAN", "WITTMAN", "WIDENER", "WOLFSON", "WENDELL", 
							  "WEITZEL", "WILLMAN", "WALDRUP", "WALTMAN", "WALCZAK", "WEIGAND", 
							  "WESSELS", "WIDEMAN", "WOLTERS", "WIREMAN", "WILHOIT", "WEGENER", 
							  "WOTRING", "WINGERT", "WIESNER", "WAYMIRE", "WHETZEL", "WENTZEL", 
							  "WINEGAR", "WESTMAN", "WYNKOOP", "WALLICK", "WURSTER", "WINBUSH", 
							  "WILBERT", "WALLACH", "WYNKOOP", "WALLICK", "WURSTER", "WINBUSH", 
							  "WILBERT", "WALLACH", "WEISSER", "WEISNER", "WINDERS", "WILLMON", 
							  "WILLEMS", "WIERSMA", "WACHTEL", "WARNICK", "WEIDLER", "WALTRIP", 
							  "WHETSEL", "WHELESS", "WELCHER", "WALBORN", "WILLSEY", "WEINMAN", 
							  "WAGAMAN", "WOMMACK", "WINGLER", "WINKLES", "WIEDMAN", "WHITNER", 
							  "WOLFRAM", "WARLICK", "WEEDMAN", "WHISMAN", "WINLAND", "WEESNER", 
							  "WARTHEN", "WETZLER", "WENDLER", "WALLNER", "WOLBERT", "WITTMER", 
							  "WISHART", "WILLIAM", "")
			|| StringAt(0, 8, "WESTPHAL", "WICKLUND", "WEISSMAN", "WESTLUND", "WOLFGANG", "WILLHITE", 
							  "WEISBERG", "WALRAVEN", "WOLFGRAM", "WILHOITE", "WECHSLER", "WENDLING", 
							  "WESTBERG", "WENDLAND", "WININGER", "WHISNANT", "WESTRICK", "WESTLING", 
							  "WESTBURY", "WEITZMAN", "WEHMEYER", "WEINMANN", "WISNESKI", "WHELCHEL", 
							  "WEISHAAR", "WAGGENER", "WALDROUP", "WESTHOFF", "WIEDEMAN", "WASINGER", 
							  "WINBORNE", "")
			|| StringAt(0, 9, "WHISENANT", "WEINSTEIN", "WESTERMAN", "WASSERMAN", "WITKOWSKI", "WEINTRAUB", 
						      "WINKELMAN", "WINKFIELD", "WANAMAKER", "WIECZOREK", "WIECHMANN", "WOJTOWICZ", 
						      "WALKOWIAK", "WEINSTOCK", "WILLEFORD", "WARKENTIN", "WEISINGER", "WINKLEMAN",  
						      "WILHEMINA", "")
			|| StringAt(0, 10, "WISNIEWSKI", "WUNDERLICH", "WHISENHUNT", "WEINBERGER", "WROBLEWSKI", 
							   "WAGUESPACK", "WEISGERBER", "WESTERVELT", "WESTERLUND", "WASILEWSKI", 
							   "WILDERMUTH", "WESTENDORF", "WESOLOWSKI", "WEINGARTEN", "WINEBARGER", 
							   "WESTERBERG", "WANNAMAKER", "WEISSINGER", "")
			|| StringAt(0, 11, "WALDSCHMIDT", "WEINGARTNER", "WINEBRENNER", "")
			|| StringAt(0, 12, "WOLFENBARGER", "")
			|| StringAt(0, 13, "WOJCIECHOWSKI", ""))
		{
			return true;
		}
		
		return false;		
	}
	
	/**
	 * Test whether the word in question
	 * is a name starting with 'J' that should
	 * match names starting with a 'Y' sound.
	 * All forms of 'John', 'Jane', etc, get
	 * and alt to match e.g. 'Ian', 'Yana'. Joelle
	 * should match 'Yael', 'Joseph' should match
	 * 'Yusef'. German and slavic last names are
	 * also included.
	 * 
	 * @return true if name starting with 'J' that
	 * should get an alternate encoding as a vowel
	 */
	boolean Names_Beginning_With_J_That_Get_Alt_Y()
	{
		if(StringAt(0, 3, "JAN", "JON", "JAN", "JIN", "JEN", "")
			|| StringAt(0, 4, "JUHL", "JULY", "JOEL", "JOHN", "JOSH", 
							  "JUDE", "JUNE", "JONI", "JULI", "JENA", 
							  "JUNG", "JINA", "JANA", "JENI", "JOEL", 
							  "JANN", "JONA", "JENE", "JULE", "JANI", 
							  "JONG", "JOHN", "JEAN", "JUNG", "JONE", 
							  "JARA", "JUST", "JOST", "JAHN", "JACO", 
							  "JANG", "JUDE", "JONE", "") 
			|| StringAt(0, 5, "JOANN", "JANEY", "JANAE", "JOANA", "JUTTA", 
							  "JULEE", "JANAY", "JANEE", "JETTA", "JOHNA", 
							  "JOANE", "JAYNA", "JANES", "JONAS", "JONIE", 
							  "JUSTA", "JUNIE", "JUNKO", "JENAE", "JULIO", 
							  "JINNY", "JOHNS", "JACOB", "JETER", "JAFFE", 
							  "JESKE", "JANKE", "JAGER", "JANIK", "JANDA", 
							  "JOSHI", "JULES", "JANTZ", "JEANS", "JUDAH", 
							  "JANUS", "JENNY", "JENEE", "JONAH", "JONAS", 
							  "JACOB", "JOSUE", "JOSEF", "JULES", "JULIE", 
							  "JULIA", "JANIE", "JANIS", "JENNA", "JANNA", 
							  "JEANA", "JENNI", "JEANE", "JONNA", "")
			|| StringAt(0, 6, "JORDAN", "JORDON", "JOSEPH", "JOSHUA", "JOSIAH", 
							  "JOSPEH", "JUDSON", "JULIAN", "JULIUS", "JUNIOR", 
							  "JUDITH", "JOESPH", "JOHNIE", "JOANNE", "JEANNE", 
							  "JOANNA", "JOSEFA", "JULIET", "JANNIE", "JANELL", 
							  "JASMIN", "JANINE", "JOHNNY", "JEANIE", "JEANNA", 
							  "JOHNNA", "JOELLE", "JOVITA", "JOSEPH", "JONNIE", 
							  "JANEEN", "JANINA", "JOANIE", "JAZMIN", "JOHNIE", 
							  "JANENE", "JOHNNY", "JONELL", "JENELL", "JANETT", 
							  "JANETH", "JENINE", "JOELLA", "JOEANN", "JULIAN", 
							  "JOHANA", "JENICE", "JANNET", "JANISE", "JULENE", 
							  "JOSHUA", "JANEAN", "JAIMEE", "JOETTE", "JANYCE", 
							  "JENEVA", "JORDAN", "JACOBS", "JENSEN", "JOSEPH", 
							  "JANSEN", "JORDON", "JULIAN", "JAEGER", "JACOBY", 
							  "JENSON", "JARMAN", "JOSLIN", "JESSEN", "JAHNKE", 
							  "JACOBO", "JULIEN", "JOSHUA", "JEPSON", "JULIUS", 
							  "JANSON", "JACOBI", "JUDSON", "JARBOE", "JOHSON", 
							  "JANZEN", "JETTON", "JUNKER", "JONSON", "JAROSZ", 
							  "JENNER", "JAGGER", "JASMIN", "JEPSEN", "JORDEN", 
							  "JANNEY", "JUHASZ", "JERGEN", "JAKOB", "")
			|| StringAt(0, 7, "JOHNSON", "JOHNNIE", "JASMINE", "JEANNIE", "JOHANNA", 
							  "JANELLE", "JANETTE", "JULIANA", "JUSTINA", "JOSETTE", 
							  "JOELLEN", "JENELLE", "JULIETA", "JULIANN", "JULISSA", 
							  "JENETTE", "JANETTA", "JOSELYN", "JONELLE", "JESENIA", 
							  "JANESSA", "JAZMINE", "JEANENE", "JOANNIE", "JADWIGA", 
							  "JOLANDA", "JULIANE", "JANUARY", "JEANICE", "JANELLA", 
							  "JEANETT", "JENNINE", "JOHANNE", "JOHNSIE", "JANIECE", 
							  "JOHNSON", "JENNELL", "JAMISON", "JANSSEN", "JOHNSEN", 
							  "JARDINE", "JAGGERS", "JURGENS", "JOURDAN", "JULIANO", 
							  "JOSEPHS", "JHONSON", "JOZWIAK", "JANICKI", "JELINEK", 
							  "JANSSON", "JOACHIM", "JANELLE", "JACOBUS", "JENNING", 
							  "JANTZEN", "JOHNNIE",  "")
			|| StringAt(0, 8, "JOSEFINA", "JEANNINE", "JULIANNE", "JULIANNA", "JONATHAN", 
							  "JONATHON", "JEANETTE", "JANNETTE", "JEANETTA", "JOHNETTA", 
							  "JENNEFER", "JULIENNE", "JOSPHINE", "JEANELLE", "JOHNETTE", 
							  "JULIEANN", "JOSEFINE", "JULIETTA", "JOHNSTON", "JACOBSON", 
							  "JACOBSEN", "JOHANSEN", "JOHANSON", "JAWORSKI", "JENNETTE", 
							  "JELLISON", "JOHANNES", "JASINSKI", "JUERGENS", "JARNAGIN", 
							  "JEREMIAH", "JEPPESEN", "JARNIGAN", "JANOUSEK", "")
			|| StringAt(0, 9, "JOHNATHAN", "JOHNATHON", "JORGENSEN", "JEANMARIE", "JOSEPHINA", 
						      "JEANNETTE", "JOSEPHINE", "JEANNETTA", "JORGENSON", "JANKOWSKI", 
						      "JOHNSTONE", "JABLONSKI", "JOSEPHSON", "JOHANNSEN", "JURGENSEN", 
						      "JIMMERSON", "JOHANSSON", "")
			|| StringAt(0, 10, "JAKUBOWSKI", ""))
			{
				return true;
			}

			return false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// example code
		
		Metaphone3 m3 = new Metaphone3();

		//m3.SetEncodeVowels(true);
		//m3.SetEncodeExact(true);

		m3.SetWord("iron");

		m3.Encode();

		System.out.println("iron : " + m3.GetMetaph());
		System.out.println("iron : (alt) " + m3.GetAlternateMetaph());	

		m3.SetWord("witz");

		m3.Encode();

		System.out.println("witz : " + m3.GetMetaph());
		System.out.println("witz : (alt) " + m3.GetAlternateMetaph());	
		
		m3.SetWord("");

		m3.Encode();

		System.out.println("BLANK : " + m3.GetMetaph());
		System.out.println("BLANK : (alt) " + m3.GetAlternateMetaph());	

		// these settings default to false
		m3.SetEncodeExact(true);
		m3.SetEncodeVowels(true);

		String test = new String("Guillermo");
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "VILLASENOR";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "GUILLERMINA";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "PADILLA";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "BJORK";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());
		
		test = "belle";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "ERICH";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "CROCE";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "GLOWACKI";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
		
		test = "qing";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());	
				
		test = "tsing";
		m3.SetWord(test);
		m3.Encode();
		System.out.println(test + " : " + m3.GetMetaph());	
		System.out.println(test + " : (alt) " + m3.GetAlternateMetaph());
		
	}
}


