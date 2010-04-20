# -*- coding: utf-8 -*-
"""Misc unicode tests

Made for Jython.
"""
import re
import sys
import unittest
from StringIO import StringIO
from test import test_support

class UnicodeTestCase(unittest.TestCase):

    def test_simplejson_plane_bug(self):
        # a bug exposed by simplejson: unicode __add__ was always
        # forcing the basic plane
        chunker = re.compile(r'(.*?)(["\\\x00-\x1f])', re.VERBOSE | re.MULTILINE | re.DOTALL)
        orig = u'z\U0001d120x'
        quoted1 = u'"z\U0001d120x"'
        quoted2 = '"' + orig + '"'
        # chunker re gives different results depending on the plane
        self.assertEqual(chunker.match(quoted1, 1).groups(), (orig, u'"'))
        self.assertEqual(chunker.match(quoted2, 1).groups(), (orig, u'"'))

    def test_parse_unicode(self):
        foo = u'ą\n'
        self.assertEqual(len(foo), 2, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 10)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 3)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 10)

    def test_parse_raw_unicode(self):
        foo = ur'ą\n'
        self.assertEqual(len(foo), 3, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 92)
        self.assertEqual(ord(foo[2]), 110)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 4)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 92)
        self.assertEqual(ord(bar[3]), 110)

        for baz in ur'Hello\u0020World !', ur'Hello\U00000020World !':
            self.assertEqual(len(baz), 13, repr(baz))
            self.assertEqual(repr(baz), "u'Hello World !'")
            self.assertEqual(ord(baz[5]), 32)

        quux = ur'\U00100000'
        self.assertEqual(repr(quux), "u'\\U00100000'")
        if sys.maxunicode == 0xffff:
            self.assertEqual(len(quux), 2)
            self.assertEqual(ord(quux[0]), 56256)
            self.assertEqual(ord(quux[1]), 56320)
        else:
            self.assertEqual(len(quux), 1)
            self.assertEqual(ord(quux), 1048576)

    def test_raw_unicode_escape(self):
        foo = u'\U00100000'
        self.assertEqual(foo.encode('raw_unicode_escape'), '\\U00100000')
        self.assertEqual(foo.encode('raw_unicode_escape').decode('raw_unicode_escape'),
                         foo)
        for bar in '\\u', '\\u000', '\\U00000':
            self.assertRaises(UnicodeDecodeError, bar.decode, 'raw_unicode_escape')

    def test_encode_decimal(self):
        self.assertEqual(int(u'\u0039\u0032'), 92)
        self.assertEqual(int(u'\u0660'), 0)
        self.assertEqual(int(u' \u001F\u0966\u096F\u0039'), 99)
        self.assertEqual(long(u'\u0663'), 3)
        self.assertEqual(float(u'\u0663.\u0661'), 3.1)
        self.assertEqual(complex(u'\u0663.\u0661'), 3.1+0j)

    def test_unstateful_end_of_data(self):
        # http://bugs.jython.org/issue1368
        for encoding in 'utf-8', 'utf-16', 'utf-16-be', 'utf-16-le':
            self.assertRaises(UnicodeDecodeError, '\xe4'.decode, encoding)

    def test_formatchar(self):
        self.assertEqual('%c' % 255, '\xff')
        self.assertRaises(OverflowError, '%c'.__mod__, 256)

        result = u'%c' % 256
        self.assert_(isinstance(result, unicode))
        self.assertEqual(result, u'\u0100')
        if sys.maxunicode == 0xffff:
            self.assertEqual(u'%c' % sys.maxunicode, u'\uffff')
        else:
            self.assertEqual(u'%c' % sys.maxunicode, u'\U0010ffff')
        self.assertRaises(OverflowError, '%c'.__mod__, sys.maxunicode + 1)

    def test_repr(self):
        self.assert_(isinstance('%r' % u'foo', str))

    def test_concat(self):
        self.assertRaises(UnicodeDecodeError, lambda : u'' + '毛泽东')
        self.assertRaises(UnicodeDecodeError, lambda : '毛泽东' + u'')

    def test_join(self):
        self.assertRaises(UnicodeDecodeError, u''.join, ['foo', '毛泽东'])
        self.assertRaises(UnicodeDecodeError, '毛泽东'.join, [u'foo', u'bar'])

    def test_file_encoding(self):
        '''Ensure file writing doesn't attempt to encode things by default and reading doesn't
        decode things by default.  This was jython's behavior prior to 2.2.1'''
        EURO_SIGN = u"\u20ac"
        try:
            EURO_SIGN.encode()
        except UnicodeEncodeError:
            # This default encoding can't handle the encoding the Euro sign.  Skip the test
            return

        f = open(test_support.TESTFN, "w")
        self.assertRaises(UnicodeEncodeError, f, write, EURO_SIGN,
                "Shouldn't be able to write out a Euro sign without first encoding")
        f.close()

        f = open(test_support.TESTFN, "w")
        f.write(EURO_SIGN.encode('utf-8'))
        f.close()

        f = open(test_support.TESTFN, "r")
        encoded_euro = f.read()
        f.close()
        os.remove(test_support.TESTFN)
        self.assertEquals('\xe2\x82\xac', encoded_euro)
        self.assertEquals(EURO_SIGN, encoded_euro.decode('utf-8'))


class UnicodeFormatTestCase(unittest.TestCase):

    def test_unicode_mapping(self):
        assertTrue = self.assertTrue
        class EnsureUnicode(dict):
            def __missing__(self, key):
                assertTrue(isinstance(key, unicode))
                return key
        u'%(foo)s' % EnsureUnicode()

    def test_non_ascii_unicode_mod_str(self):
        # Regression test for a problem on the formatting logic: when no unicode
        # args were found, Jython stored the resulting buffer on a PyString,
        # decoding it later to make a PyUnicode. That crashed when the left side
        # of % was a unicode containing non-ascii chars
        self.assertEquals(u"\u00e7%s" % "foo", u"\u00e7foo")


class UnicodeStdIOTestCase(unittest.TestCase):

    def setUp(self):
        self.stdout = sys.stdout

    def tearDown(self):
        sys.stdout = self.stdout

    def test_intercepted_stdout(self):
        msg = u'Circle is 360\u00B0'
        sys.stdout = StringIO()
        print msg,
        self.assertEqual(sys.stdout.getvalue(), msg)


def test_main():
    test_support.run_unittest(UnicodeTestCase,
                              UnicodeFormatTestCase,
                              UnicodeStdIOTestCase)


if __name__ == "__main__":
    test_main()
