1. #### Metaphone3

Metaphone3 improves phonetic encoding for not just words in the English Language but also non-English words, first names, and last names common in the United States.

The 'approximate' aspect of the encoding for OpenRefine (version 2.1.3) is implemented according to the following rules:

- All vowels are encoded to the same value - 'A'.

- If the parameter *encodeVowels* is set to false, only *initial* vowels will be encoded at all.
- If *encodeVowels* is set to true, 'A' will be encoded at all places in the word that any vowels are normally pronounced.
- 'W', as well as 'Y', are treated as vowels. Although there are differences in their pronunciation in different circumstances that lead to their being classified as vowels under some circumstances and as consonants in others, for the 'fuzziness' component of the Soundex and Metaphone family of algorithms they will be always be treated here as vowels.
- Voiced and un-voiced consonant pairs are mapped to the same encoded value. That is:<br>
    * 'D' and 'T' -> 'T'<br>
    * 'B' and 'P' -> 'P'<br>
    * 'G' and 'K' -> 'K'<br>
    * 'Z' and 'S' -> 'S'<br>
    * 'V' and 'F' -> 'F'<br>

- In addition to the above voiced/unvoiced rules, 'CH' and 'SH' -> 'X', where 'X' represents the "-SH-" and "-CH-" sounds in Metaphone 3 encoding.

- Also, the sound that is spelled as "TH" in English is encoded to '0' (zero). 
