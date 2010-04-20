import unittest


class TestStrReturnsUnicode(unittest.TestCase):

    def test_join(self):
        self.assertEquals(unicode, type(''.join([u'blah'])))

    def test_replace(self):
        self.assertEquals(unicode, type('hello'.replace('o', u'o')))

    def test_string_formatting_s(self):
        self.assertEquals(unicode, type('%s' % u'x'))
        self.assertEquals(unicode, type('%s %s' % (u'x', 'y')))
        self.assertEquals(unicode, type('%(x)s' % {'x' : u'x'}))

    def test_string_formatting_r(self):
        self.assertEquals(unicode, type('%r' % u'x'))
        self.assertEquals(unicode, type('%r %r' % (u'x', 'y')))
        self.assertEquals(unicode, type('%(x)r' % {'x' : u'x'}))

    def test_string_formatting_c(self):
        self.assertEquals(unicode, type('%c' % u'x'))
        self.assertEquals(unicode, type('%c %c' % (u'x', 'y')))
        self.assertEquals(unicode, type('%(x)c' % {'x' : u'x'}))


class TestStrReturnsStr(unittest.TestCase):

    def test_join(self):
        self.assertEquals(str, type(''.join(['blah'])))

    def test_replace(self):
        self.assertEquals(str, type('hello'.replace('o', 'oo')))

    def test_string_formatting_s(self):
        self.assertEquals(str, type('%s' % 'x'))
        self.assertEquals(str, type('%s %s' % ('x', 'y')))
        self.assertEquals(str, type('%(x)s' % {'x' : 'xxx'}))

    def test_string_formatting_r(self):
        self.assertEquals(str, type('%r' % 'x'))
        self.assertEquals(str, type('%r %r' % ('x', 'y')))
        self.assertEquals(str, type('%(x)r' % {'x' : 'x'}))

    def test_string_formatting_c(self):
        self.assertEquals(str, type('%c' % 'x'))
        self.assertEquals(str, type('%c %c' % ('x', 'y')))
        self.assertEquals(str, type('%(x)c' % {'x' : 'x'}))


class TestUnicodeReturnsUnicode(unittest.TestCase):

    def test_join(self):
        self.assertEquals(unicode, type(u''.join([u'blah'])))
        self.assertEquals(unicode, type(u''.join(['blah'])))

    def test_replace(self):
        self.assertEquals(unicode, type(u'hello'.replace('o', u'o')))
        self.assertEquals(unicode, type(u'hello'.replace(u'o', 'o')))
        self.assertEquals(unicode, type(u'hello'.replace(u'o', u'o')))
        self.assertEquals(unicode, type(u'hello'.replace('o', 'o')))

    def test_string_formatting_s(self):
        self.assertEquals(unicode, type(u'%s' % u'x'))
        self.assertEquals(unicode, type(u'%s' % 'x'))
        self.assertEquals(unicode, type(u'%s %s' % (u'x', 'y')))
        self.assertEquals(unicode, type(u'%s %s' % ('x', 'y')))
        self.assertEquals(unicode, type(u'%(x)s' % {'x' : u'x'}))
        self.assertEquals(unicode, type(u'%(x)s' % {'x' : 'x'}))

    def test_string_formatting_r(self):
        self.assertEquals(unicode, type(u'%r' % u'x'))
        self.assertEquals(unicode, type(u'%r' % 'x'))
        self.assertEquals(unicode, type(u'%r %r' % (u'x', 'y')))
        self.assertEquals(unicode, type(u'%r %r' % ('x', 'y')))
        self.assertEquals(unicode, type(u'%(x)r' % {'x' : u'x'}))
        self.assertEquals(unicode, type(u'%(x)r' % {'x' : 'x'}))

    def test_string_formatting_c(self):
        self.assertEquals(unicode, type(u'%c' % u'x'))
        self.assertEquals(unicode, type(u'%c' % 'x'))
        self.assertEquals(unicode, type(u'%c %c' % (u'x', 'y')))
        self.assertEquals(unicode, type(u'%c %c' % ('x', 'y')))
        self.assertEquals(unicode, type(u'%(x)c' % {'x' : u'x'}))
        self.assertEquals(unicode, type(u'%(x)c' % {'x' : 'x'}))


if __name__ == '__main__':
    unittest.main()
