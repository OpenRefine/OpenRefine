---
id: clustering-in-depth
title: Clustering Methods In-depth
sidebar_label: Clustering methods in-depth
---

*Methods and theory behind the clustering functionality in OpenRefine.*

# Introduction

In OpenRefine, [clustering](https://docs.openrefine.org/manual/cellediting#cluster-and-edit) refers to the operation of "finding groups of
different values that might be alternative representations of the same
thing."

It is worth noting that clustering in OpenRefine works only at the
syntactic level (the character composition of the cell value) and, while
very useful to spot errors, typos, and inconsistencies, it's by no means
enough to perform effective semantically-aware reconciliation. This is
why OpenRefine uses [external semantically-aware reconciliation services](https://docs.openrefine.org/manual/reconciling)
(such as Wikidata) to compensate for the deficiencies of syntax-level
clustering alone.

# Methodologies

To strike a balance between general applicability and
usefulness, OpenRefine ships with a selected number of clustering
methods and algorithms that have proven effective and fast enough to use
in a wide variety of situations.

OpenRefine currently offers 2 broad categories of clustering methods:
1. Token-based (n-gram, key collision, etc.)
2. Character-based, also known as Edit distance (Levenshtein distance, PPM, etc.)

**NOTE:**  Performance differs depending on the strings that you want to cluster in your data which might be short or very long or varying.  String complexity length has a large part to do with the algorithm that might perform faster (but not necessarily better!).  In general, it's usually best to use heavy algorithms for shorter strings that can provide better quality – like Levenshtein distance.  And token-based algorithms for longer strings such as n-grams(q-grams), bag distance, Jaccard similarity, Dice coefficient, etc. (some of which we do not provide currently).

## Key Collision Methods

"Key Collision" methods are based on the idea of creating an alternative
representation of a value (a "key") that contains only the most valuable
or meaningful part of the string and "buckets" (or "bin" as it's
described inside OpenRefine's code) together different strings based on
the fact that their key is the same (hence the name "key collision").

### Fingerprint

The fingerprinting method is the least likely to produce false
positives, which is why it is the default method.

The process that generates the key from a string value is the following
(note that the order of these operations is significant):

- remove leading and trailing whitespace
- change all characters to their lowercase representation
- remove all punctuation and control characters
- normalize extended western characters to their ASCII representation
  (for example "gödel" → "godel")
- split the string into whitespace-separated tokens
- sort the tokens and remove duplicates
- join the tokens back together

If you're curious, the code that performs this is
[here](http://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/clustering/binning/FingerprintKeyer.java).

Several factors play a role in this fingerprint:

- because whitespace is normalized, characters are lowercased, and
  punctuation is removed, those parts don't play a differentiation
  role in the fingerprint. Because these attributes of the string are
  the least significant in terms of meaning differentiation, these
  turn out to be the most varying parts of the strings, and removing
  them has a substantial benefit in emerging clusters.
- because the string parts are sorted, the given order of tokens
  doesn't matter (so "Cruise, Tom" and "Tom Cruise" both end up with a
  fingerprint "cruise tom" and therefore end up in the same cluster)
- normalizing extended western characters plays the role of
  reproducing data entry mistakes performed when entering extended
  characters with an ASCII-only keyboard. Note that this procedure can
  also lead to false positives. For example, "gödel" and "godél" would
  both end up with "godel" as their fingerprint, but they're likely to
  be different names, so this might work less effectively for datasets
  where extended characters play a substantial differentiation role.

### N-Gram Fingerprint

The [n-gram](http://en.wikipedia.org/wiki/N-gram) fingerprint method does the following:
- change all characters to their lowercase representation
- remove all punctuation, whitespace, and control characters
- obtain all the string n-grams
- sort the n-grams and remove duplicates
- join the sorted n-grams back together
- normalize extended western characters to their ASCII representation

So, for example, the 2-gram fingerprint of "Paris" is "arispari" and the
1-gram fingerprint is "aiprs".

[Check the code](http://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/clustering/binning/NGramFingerprintKeyer.java)
if you're curious about the details.

Why is this useful? In practice, using big values for n-grams doesn't
yield any advantage over the previous fingerprint method, but using
2-grams and 1-grams, while yielding many false positives, can find
clusters that the previous method didn't find even with strings that
have small differences, with a very small performance price.

For example "Krzysztof", "Kryzysztof", and "Krzystof" have different
lengths and different regular fingerprints, but share the same 1-gram
fingerprint because they use the same letters.

### Phonetic Fingerprint

Phonetic fingerprinting is a way to transform tokens into the way they are
pronounced. This is useful to spot errors that are due to people
misunderstanding or not knowing the spelling of a word after only
hearing it. The idea is that similar-sounding words will end up
sharing the same key and thus being binned in the same cluster.

For example, "Reuben Gevorkiantz" and "Ruben Gevorkyants" share the same
phonetic fingerprint for English pronunciation but they have different
fingerprints for both the regular and n-gram fingerprinting methods
above, no matter the size of the n-gram.

OpenRefine supports multiple "Phonetic" algorithms including:

#### Metaphone3

Metaphone3 improves phonetic encoding for not just words in the English Language but also non-English words, first names, and last names common in the United States.

The 'approximate' aspect of the encoding for OpenRefine (version 2.1.3) is implemented according to the following rules:

- All vowels are encoded to the same value - 'A'.

- If the parameter *encodeVowels* is set to false, only *initial* vowels will be encoded at all.
- If *encodeVowels* is set to true, 'A' will be encoded at all places in the word that any vowels are normally pronounced.
- 'W', as well as 'Y', are treated as vowels. Although there are differences in their pronunciation in different circumstances that lead to their being classified as vowels under some circumstances and as consonants in others, for the 'fuzziness' component of the Soundex and Metaphone family of algorithms they will be always be treated here as vowels.
- Voiced and un-voiced consonant pairs are mapped to the same encoded value. That is:
  * 'D' and 'T' -> 'T'
  * 'B' and 'P' -> 'P'
  * 'G' and 'K' -> 'K'
  * 'Z' and 'S' -> 'S'
  * 'V' and 'F' -> 'F'

- In addition to the above voiced/unvoiced rules, 'CH' and 'SH' -> 'X', where 'X' represents the "-SH-" and "-CH-" sounds in Metaphone 3 encoding.

- Also, the sound that is spelled as "TH" in English is encoded to '0' (zero).

#### Cologne Phonetics

Cologne Phonetics, also called Kölner Phonetik, is a phonetic algorithm that assigns a sequence of digits (called the **phonetic code**) to words.

Unlike Metaphone3, Cologne Phonetics is optimized for the German language.

The process of encoding words using Cologne phonetics can be broken down to the following steps:
**Step 1:**

The encoding process is as follows:
- The word is preprocessed. That is,
  - conversion to upper case
  - transcription of germanic umlauts
  - removal of non-alphabetical characters

-The letters are then replaced by their phonetic codes according to the following table:

<table>
   <thead>
      <tr>
         <th>Letter</th>
         <th>Context</th>
         <th>Code</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>A, E, I, O, U</td>
         <td> </td>
         <td>0</td>
      </tr>
      <tr>
         <td>H</td>
         <td></td>
         <td>-</td>
      </tr>
      <tr>
         <td>B</td>
         <td></td>
         <td rowspan="2">1</td>
      </tr>
      <tr>
         <td>P</td>
         <td>not before H</td>
      </tr>
      <tr>
         <td>D, T</td>
         <td>not before C, S, Z</td>
         <td>2</td>
      </tr>
      <tr>
         <td>F, V, W</td>
         <td></td>
         <td rowspan="2">3</td>
      </tr>
      <tr>
         <td>P</td>
         <td>before H</td>
      </tr>
      <tr>
         <td>G, K, Q</td>
         <td></td>
         <td rowspan="3">4</td>
      </tr>
      <tr>
         <td rowspan="2">C</td>
         <td>at onset before A, H, K, L, O, Q, R, U, X</td>
      </tr>
      <tr>
         <td>before A, H, K, O, Q, U, X except after S, Z</td>
      </tr>
      <tr>
         <td>X</td>
         <td>not after C, K, Q</td>
         <td>48</td>
      </tr>
      <tr>
         <td>L</td>
         <td></td>
         <td>5</td>
      </tr>
      <tr>
         <td>M, N</td>
         <td></td>
         <td>6</td>
      </tr>
      <tr>
         <td>R</td>
         <td></td>
         <td>7</td>
      </tr>
      <tr>
         <td>S, Z</td>
         <td></td>
         <td rowspan="6">8</td>
      </tr>
      <tr>
         <td rowspan="3">C</td>
         <td>after S, Z</td>
      </tr>
      <tr>
         <td>at onset except before A, H, K, L, O, Q, R, U, X</td>
      </tr>
      <tr>
         <td>not before A, H, K, O, Q, U, X</td>
      </tr>
      <tr>
         <td>D, T</td>
         <td>before C, S, Z</td>
      </tr>
      <tr>
         <td>X</td>
         <td>after C, K, Q</td>
      </tr>
   </tbody>
</table>

**For example**
Following the rules stated above, the phrase "Good morning" in German could be encoded as:

Guten Morgen -> GUTENMORGEN -> 40206607406

**Step 2:**

Any consecutive duplicate digit is removed

4020**66**07406 -> 4020607406

**Step 3:**

Removal of all codes "0" **except** at the beginning.

4**0**2**0**6**0**74**0**6 -> 426746

Hence, by the Cologne phonetic, Guteng Morgen is encoded as 426746

**Note** that two or more identical consecutive digits can occur if they occur after removing the "0" digits.

#### Daitch-Moktoff

The Daitch-Moktoff phonetic algorithm was created by  Randy Daitch and Gary Mokotoff of the Jewish Genealogical Society (New York), hence its name.

It is a refinement of the [Soundex](https://en.wikipedia.org/wiki/Soundex) algorithms designed to allow greater accuracy in matching Slavic and Yiddish surnames with similar pronunciation but differences in spelling.

The rules for converting surnames into D-M codes are as follows:

- Names are coded to six digits, each digit representing a sound listed in the [coding chart](https://www.jewishgen.org/infofiles/soundex.html)
- When a name lacks enough coded sounds for six digits, use zeros to fill to six digits.
- The letters A, E, I, O, U, J, and Y are always coded at the beginning of a name as in Alice.  In any other situation, they are ignored except when two of them form a pair and the pair comes before a vowel, as in Isaiah but not Freud.
- The letter H is coded at the beginning of a name, as in Haber, or preceding a vowel, as in Manheim, otherwise, it is not coded.
- When adjacent sounds can combine to form a larger sound, they are given the code number of the larger sound.  Mintz is not coded MIN-T-Z but MIN-TZ.
- When adjacent letters have the same code number, they are coded as one sound, as in TOPF, which is not coded TO-P-F but TO-PF.  Exceptions to this rule are the letter combinations MN and NM, whose letters are coded separately, as in Kleinman.
- When a surname consists of more than one word, it is coded as if one word, such as "Ben Aron", is treated as "Benaron".
- Several letters and letter combinations pose the problem that they may sound in one of two ways.  The letter and letter combinations CH, CK, C, J, and RS are assigned two possible code numbers.

#### Beider-Morse

The [Beider-Morse Phonetic Matching](https://stevemorse.org/phonetics/bmpm.htm) (BMPM) is a very intelligent algorithm, compared to [Metaphone](https://en.wikipedia.org/wiki/Metaphone), whose purpose is to match names that are phonetically equivalent to the expected name. However,  unlike [soundex](https://en.wikipedia.org/wiki/Soundex) methods, the “sounds-alike” test is based not only on the spelling but on the linguistic properties of various languages.

The steps for comparison are as follows:

**Step 1:** Determines the language from the spelling of the name

The spelling of a name can include some letters or letter combinations that allow the language to be determined. Some examples are:

- "tsch", final "mann" and "witz" are specifically German

- final and initial "cs" and "zs" are necessarily Hungarian

- "cz", "cy", initial "rz" and "wl", final "cki", letters "ś", "ł" and "ż" can be only Polish

**Step 2:** Applies phonetic rules to identify the language and translates the name into phonetic alphabets

**Step 3:** Calculating the Approximate Phonetic Value

**Step 4:** Calculating the Hebrew Phonetic Value

The entire process is described in detail in this [document](https://stevemorse.org/phonetics/bmpm.htm)

## Nearest Neighbor Methods

While key collisions methods are very fast, they tend to be either too
strict or too lax with no way to fine-tune how much difference between
strings we are willing to tolerate.

The
[Nearest Neighbor](https://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm)
methods (also known as kNN), on the other hand, provide a parameter (the
radius or `k`) which represents a distance threshold: any pair of
strings that are closer than a certain value will be binned together.

Unfortunately, given n strings, there are `n(n-1)/2` pairs of strings
(and relative distances) that need to be compared and this turns out to
be too slow even for small datasets (a dataset with 3000 rows require
4.5 million distance calculations\!).

We have tried various methods to speed up this process but the one that
works the best is called 'blocking' and is, in fact, a hybrid between
key collision and kNN. This works by performing a first pass over the
sequence of strings to evaluate and obtain 'blocks' in which all strings
share a substring of a given 'blocking size' (which defaults to 6 chars
in OpenRefine).

Blocking doesn't change the computational complexity of the kNN method
but drastically reduces the number of strings that will be matched
against one another (because strings are matched only inside the block
that contains them). So instead of `n(n-1)/2` we now have `nm(m-1)/2`
but `n` is the number of blocks and `m` is the average size of the
block. In practice, this turns out to be dramatically faster because the
block size is comparable to the number of strings and the blocks are
normally much smaller. For example, for 3000 strings, you can have a
thousand blocks composed of 10 strings each, which requires 45k
distances to calculate instead of 4.5M\!

If you're not in a hurry, OpenRefine lets you select the size of the
blocking substring and you can lower it down to 2 or 1 and make sure
that blocking is not hiding a potential pair from your search...
although in practice, anything lower than 3 normally turns out to be a
waste of time.

All the above is shared between all the kNN methods, the difference of
operation lies in the method used to evaluate the distance between the
two strings.

For kNN distances, we found that blocking with less than 3 or 4 chars explodes the amount of time clustering takes and yields very few new valuable results, but your mileage may vary.

### Levenshtein Distance

The [Levenshtein](http://en.wikipedia.org/wiki/Levenshtein\_distance)
distance (also known as "edit distance") is probably the simplest and
most intuitive distance function between strings and is often still very
effective due to its general applicability. It measures the minimal number of ' edit operations ' that are required to
change one string into the other.

It's worth noting that there are many flavors of edit-based distance
functions (say, the
[Damerau-Levenshtein distance](http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein\_distance),
which considers 'transposition' as a single operation) but in practice,
for clustering purposes, they tend to be equally functional (as long as
the user has control over the distance threshold).

### PPM

This distance is an implementation of
[a seminal paper](http://arxiv.org/abs/cs/0111054) about the use of
the
[Kolmogorov complexity](http://en.wikipedia.org/wiki/Kolmogorov\_complexity)
to estimate 'similarity' between strings and has been widely applied to
the comparison of strings originating from DNA sequencing.

The idea is that because text compressors work by estimating the
information content of a string, if two strings A and B are identical,
compressing A or compressing A+B (concatenating the strings) should
yield very little difference (ideally, a single extra bit to indicate
the presence of the redundant information). On the other hand, if A and
B are very different, compressing A and compressing A+B should yield
dramatic differences in length.

OpenRefine uses a normalized version of the algorithm, where the
distance between A and B is given by

``` 
   d(A,B) = comp(A+B) + comp(B+A) / (comp(A+A) + comp(B+B));
```

where `comp(s)` is the length of bytes of the compressed sequence of the
string `s` and `+` is the append operator. This is used to account for
deviation in the optimality of the given compressors.

While many different compressors can be used, the closer to Kolmogorov
optimality they are (meaning, the better they encode) the more effective
their result.

For this reason, we have used
[Prediction by Partial Matching](http://en.wikipedia.org/wiki/Prediction\_by\_Partial\_Matching)
as the compressor algorithm as it is one of the most effective
compression algorithms for text and works by performing statistical
analysis and predicting what character will come next in a string.

In practice, this method is very lax even for small radius values and
tends to generate many false positives, but because it operates at a
sub-character level it is capable of finding substructures that are not
easily identifiable by distances that work at the character level. So it
should be used as a 'last resort' clustering method; that's why it is
listed last here despite its phenomenal efficacy in other realms.

It is also important to note that in practice similar distances are
more effective on longer strings than on shorter ones; this is mostly an
artifact of the need for the statistical compressors to 'warm up' and
gather enough statistics to start performing well.

# Cluster New Value Suggestions

For each cluster identified, one value is chosen as the initial 'New
Cell Value' to use as the common value for all values in the cluster.
The value chosen is the first value in the Cluster: see the
`ClusteringDialog.prototype.\_updateData` function in
[/main/webapp/modules/core/scripts/dialogs/clustering-dialog.js](https://github.com/OpenRefine/OpenRefine/blob/master/main/webapp/modules/core/scripts/dialogs/clustering-dialog.js).

The first value in the Cluster is determined by two steps:

- a) The order of the items in the Cluster as the Cluster is built
- b) The order of the items in the Cluster after sorting by the count
  of the occurrences of each value

(a) is achieved via a Collections.sort - which is ["guaranteed to be stable: equal elements will not be reordered as a result of the sort."](https://docs.oracle.com/javase/7/docs/api/java/util/Collections.html\#sort(java.util.List,%20java.util.Comparator))<br/>
(b) is achieved by different methods depending on whether you are doing a Nearest Neighbour or Key Collisions (aka Binning) cluster

If you are using Key Collision/Binning then the Cluster is created using a TreeMap which by default ["is sorted according to the natural
ordering of its keys"](https://docs.oracle.com/javase/7/docs/api/java/util/TreeMap.html).<br/>
The key is the string in the cell - so that means it will sort by the natural ordering of the strings in the cluster - which means that it
uses a ['lexicographical' order](http://docs.oracle.com/javase/7/docs/api/java/lang/String.html\#compareTo%28java.lang.String%29) - basically based on the Unicode values in the string

If you are using the Nearest Neighbour sort the Cluster is created in a different way which is (as yet) undocumented. Testing indicates that it may be something like reverse natural ordering.

# Contribute

- We've been focusing mostly on English content or data ported to
  English. We know some of the methods might be biased towards it but
  we're willing to introduce more methods
  once the OpenRefine community gathers more insights into these
  problems;
- OpenRefine's internals support a lot more methods but we have turned
  off many of them because they don't seem to have much practical
  advantage over the ones described here. If you think that
  OpenRefine should use other methods, feel free to suggest them to us
  because we might have overlooked them.

# Suggested Reading

A lot of the code that OpenRefine uses for clustering originates from
research done by the [SIMILE Project](http://simile.mit.edu/) at MIT
which later
[graduated as the Vicino project](http://code.google.com/p/simile-vicino/)
('vicino', pronounced "vitch-ee-no", means 'near' in Italian).

For more information on clustering methods and related research we
suggest you look at the
[bibliography of the Vicino project](http://code.google.com/p/simile-vicino/source/browse/\#svn/trunk/papers).