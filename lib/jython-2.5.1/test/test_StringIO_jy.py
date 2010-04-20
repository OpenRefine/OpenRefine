import unittest
import cStringIO
from test import test_support

class TestUnicodeInput(unittest.TestCase):
    def test_differences_handling_unicode(self):
        # Test for the "feature" described on #1089.
        #
        # Basically, StringIO returns unicode objects if you feed it unicode,
        # but cStringIO don't. This should change in future versions of
        # CPython and Jython.
        self.assertEqual(u'foo', cStringIO.StringIO(u'foo').read())
        self.assertEqual('foo', cStringIO.StringIO(u'foo').read())

class TestWrite(unittest.TestCase):
    def test_write_seek_write(self):
        f = cStringIO.StringIO()
        f.write('hello')
        f.seek(2)
        f.write('hi')
        self.assertEquals(f.getvalue(), 'hehio')

    #XXX: this should get pushed to CPython's test_StringIO
    def test_write_past_end(self):
        f = cStringIO.StringIO()
        f.write("abcdef")
        f.seek(10)
        f.write("uvwxyz")
        self.assertEqual(f.getvalue(), 'abcdef\x00\x00\x00\x00uvwxyz')

def test_main():
    test_support.run_unittest(TestUnicodeInput)
    test_support.run_unittest(TestWrite)

if __name__ == '__main__':
    test_main()
