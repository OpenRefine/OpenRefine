/*
Copyright 2013 Thomas F. Morris & other contributors
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
    * Neither the names of the project or its
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

package com.google.refine.clustering.binning;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;

public class KeyerTests extends RefineTest {

    private static Keyer keyer;

    private static final String[][] testStrings = {
            { "the multi multi word test", "multi test the word" },
            { " école ÉCole ecoLe ", "ecole" },
            { "a b c d", "a b c d" },
            { " d c b a ", "a b c d" },
            { "\tABC \t DEF ", "abc def" }, // test leading and trailing whitespace
            { "bbb\taaa", "aaa bbb" },
//        {"å","aa"}, // Requested by issue #650, but conflicts with diacritic folding
            { "æø", "aeoe" }, // Norwegian replacements from #650
            { "©ß", "css" }, // issue #409 esszet
            { "\u00D0\u00F0\u00DE\u00FEǷƿ", "ddththww" }, // eth, thorn, & wynn for Icelandic / Olde English
            { "ﬀﬁﬂﬃﬅﬆ", "fffiflffistst" }, // ligatures
            // Test legacy replacements
            { "\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u0100\u0101\u0102\u0103\u0104\u0105",
                    "aaaaaaaaaaaaaaaaaa" },
            { "\u00C7\u00E7\u0106\u0107\u0108\u0109\u010A\u010B\u010C\u010D", "cccccccccc" },
            { "\u00D0\u00F0\u010E\u010F\u0110\u0111", "dddddd" },
            { "\u00C8\u00C9\u00CA\u00CB\u00E8\u00E9\u00EA\u00EB\u0112\u0113\u0114\u0115\u0116\u0117\u0118\u0119\u011A\u011B",
                    "eeeeeeeeeeeeeeeeee" },
            { "\u011C\u011D\u011E\u011F\u0120\u0121\u0122\u0123", "gggggggg" },
            { "\u0124\u0125", "hh" },
            { "\u0126\u0127", "hh" },
            { "\u00CC\u00CD\u00CE\u00CF\u00EC\u00ED\u00EE\u00EF\u0128\u0129\u012A\u012B\u012C\u012D\u012E\u012F\u0130",
                    "iiiiiiiiiiiiiiiii" },
            { "\u0131", "i" },
            { "\u0134\u0135", "jj" },
            { "\u0136\u0137", "kk" },
            { "\u0138", "k" },
            { "\u0139\u013A\u013B\u013C\u013D\u013E\u0141\u0142", "llllllll" },
            { "\u013F\u0140", "ll" },
            { "\u00D1\u00F1\u0143\u0144\u0145\u0146\u0147\u0148", "nnnnnnnn" },
            { "\u0149", "n" }, // decomposed to 'n, but then punctuation is stripped
            { "\u014A\u014B", "nn" },
            { "\u00D2\u00D3\u00D4\u00D5\u00D6\u00F2\u00F3\u00F4\u00F5\u00F6\u014C\u014D\u014E\u014F\u0150\u0151", "oooooooooooooooo" },
            { "\u00D8\u00F8", "oeoe" }, // O with stroke we decompose to oe instead of o
            { "\u0154\u0155\u0156\u0157\u0158\u0159", "rrrrrr" },
            { "\u015A\u015B\u015C\u015D\u015E\u015F\u0160\u0161\u017F", "sssssssss" },
            { "\u0162\u0163\u0164\u0165\u0166\u0167", "tttttt" },
            { "\u00D9\u00DA\u00DB\u00DC\u00F9\u00FA\u00FB\u00FC\u0168\u0169\u016A\u016B\u016C\u016D\u016E\u016F\u0170\u0171\u0172\u0173",
                    "uuuuuuuuuuuuuuuuuuuu" },
            { "\u0174\u0175", "ww" },
            { "\u00DD\u00FD\u00FF\u0176\u0177\u0178", "yyyyyy" },
            { "\u0179\u017A\u017B\u017C\u017D\u017E", "zzzzzz" },
            // Various forms of Unicode whitespace characters - NBSP, em space, en space, etc
            { "a\u0009\nb\u000Bc\u000Cd\re\u0085f\u00A0g\u1680h\u2000i\u2001j\u2002k\u2003l\u2004m\u2005n\u2006o\u2007p\u2008q\u2009r\u200As\u2028t\u2029u\u202Fv\u205Fw\u3000z",
                    "a b c d e f g h i j k l m n o p q r s t u v w z" },
            // Latin-1 Supplement
            { // "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ " + // These are all considered control characters
              // "¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿" // punctuation
                    "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ",
                    "aaaaaaaeceeeeiiiidnooooooeuuuuythssaaaaaaaeceeeeiiiidnooooooeuuuuythy" },
            // Latin Extended A
            { "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ",
                    "aaaaaaccccccccddddeeeeeeeeeegggggggghhhhiiiiiiiiiiijijjjkkkllllllllllnnnnnnnnnoooooooeoerrrrrrssssssssttttttuuuuuuuuuuuuwwyyyzzzzzzs" },
            // Latin Extended B
            // TODO: These don't get folded to ASCII equivalents. Not sure if they should be
//        {"ƄƅƉƊƋƌƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝ",
//         "bbddddhiikkllmnnooooopprssssttttuuuuyyzz                      aaiioouuuuuuuuuux"},
//        {"ǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȔȕȖȗȘșȚțȜȝȞȟȠȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿɀɁɂɃɄɅɆɇɈɉɊɋɌɍɎɏ",
//         "aaaaaeaeggggkkqqqqqssjdzdzggpnnaaaeaeooaaaeeeeeiiiioooorrrruuuusstt    zzaaeeooooooooyy"},
            // Latin Extended Additional
            { "ḀḁḂḃḄḅḆḇḈḉḊḋḌḍḎḏḐḑḒḓḔḕḖḗḘḙḚḛḜḝḞḟḠḡḢḣḤḥḦḧḨḩḪḫḬḭḮḯḰḱḲḳḴḵḶḷḸḹḺḻḼḽḾḿṀṁṂṃṄṅṆṇṈṉṊṋṌṍṎṏṐṑṒṓṔṕṖṗ",
                    "aabbbbbbccddddddddddeeeeeeeeeeffgghhhhhhhhhhiiiikkkkkkllllllllmmmmmmnnnnnnnnoooooooopppp" },
            { "ṘṙṚṛṜṝṞṟṠṡṢṣṤṥṦṧṨṩṪṫṬṭṮṯṰṱṲṳṴṵṶṷṸṹṺṻṼṽṾṿẀẁẂẃẄẅẆẇẈẉẊẋẌẍẎẏẐẑẒẓẔẕ",
                    "rrrrrrrrssssssssssttttttttuuuuuuuuuuvvvvwwwwwwwwwwxxxxyyzzzzzz" },
            { "ẖẗẘẙẚẛẜẝẞẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệ",
                    "htwyasssssaaaaaaaaaaaaaaaaaaaaaaaaeeeeeeeeeeeeeeee" },
            // Latin Extended C - TODO: not supported yet
//        {"ⱠⱡⱢⱣⱤⱥⱦⱧⱨⱩⱪⱫⱬⱭⱮⱯⱰⱱⱲⱳⱴⱵⱶⱷⱸⱹⱺⱻⱼⱽⱾⱿ",
//         "lllprathhkkzzamaaavwwwv   e ejvsz"},
            // Latin Extended D
            { "", "" },
            // Latin Extended E
            { "", "" },
            // TODO: Add tests for non-Western languages
            { "", "" },
    };

    private static final String[][] testNGramStrings = {
            { "abcdefg", "abbccddeeffg" },
            { "gfedcba", "bacbdcedfegf" },
            { "a b c d e f g", "abbccddeeffg" },
            { " a,b.c d\te!f?g ", "abbccddeeffg" },
            { "écÉCec", "ceec" },
            // All the whitespace characters below should be skipped
            { "a\u0009\nb\u000Bc\u000Cd\re\u0085f\u00A0g\u1680h\u2000i\u2001j\u2002k\u2003l\u2004m\u2005n\u2006o\u2007p\u2008q\u2009r\u200As\u2028t\u2029u\u202Fv\u205Fw\u3000z",
                    "abbccddeeffgghhiijjkkllmmnnooppqqrrssttuuvvwwz" },
            { "", "" }, // TODO: add more test cases
            { "", "" },
    };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() {
        keyer = new FingerprintKeyer();
    }

    @AfterMethod
    public void TearDown() {
        keyer = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidParams() {
        keyer.key("test", (Object[]) new String[] { "foo" });
    }

    @Test
    public void testFingerprintKeyer() {
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]), ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }

    @Test
    public void testNGramKeyer() {
        keyer = new NGramFingerprintKeyer();
        for (String[] ss : testNGramStrings) {
            Assert.assertEquals(ss.length, 2, "Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]), ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }

}
