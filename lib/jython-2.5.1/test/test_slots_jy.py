"""Slot tests

Made for Jython.
"""
from test import test_support
import unittest

# The strict tests fail on PyPy (but work on CPython and Jython).
# They're questionable
strict = True

class SlottedTestCase(unittest.TestCase):

    def test_slotted(self):
        class Foo(object):
            __slots__ = 'bar'
        self.assert_('__dict__' not in Foo.__dict__)
        foo = Foo()
        self.assert_(not hasattr(foo, '__dict__'))
        foo.bar = 'hello bar'
        self.assertEqual(foo.bar, 'hello bar')
        self.assertRaises(AttributeError, setattr, foo, 'foo', 'hello foo')

        class Baz(object):
            __slots__ = ['python', 'jython']
        self.assert_('__dict__' not in Baz.__dict__)
        baz = Baz()
        self.assert_(not hasattr(baz, '__dict__'))
        baz.python = 'hello python'
        baz.jython = 'hello jython'
        self.assertEqual(baz.python, 'hello python')
        self.assertEqual(baz.jython, 'hello jython')
        self.assertRaises(AttributeError, setattr, baz, 'foo', 'hello')


class SlottedWithDictTestCase(unittest.TestCase):

    def test_subclass(self):
        class Base(object):
            pass
        class Foo(Base):
            __slots__ = 'bar'
        self.assert_('__dict__' not in Foo.__dict__)
        foo = Foo()
        self.assert_(hasattr(foo, '__dict__'))
        foo.bar = 'hello bar'
        foo.foo = 'hello foo'
        self.assertEqual(foo.bar, 'hello bar')
        self.assertEqual(foo.__dict__, {'foo': 'hello foo'})

    def test_subclass_mro(self):
        class Base(object):
            pass
        class Slotted(object):
            __slots__ = 'baz'
        class Foo(Slotted, Base):
            __slots__ = 'bar'
        if strict:
            self.assert_('__dict__' in Foo.__dict__)
            self.assertEqual(Foo.__dict__['__dict__'].__objclass__, Foo)
        foo = Foo()
        self.assert_(hasattr(foo, '__dict__'))
        foo.bar = 'hello bar'
        foo.baz = 'hello baz'
        foo.foo = 'hello foo'
        self.assertEqual(foo.bar, 'hello bar')
        self.assertEqual(foo.baz, 'hello baz')
        self.assertEqual(foo.__dict__, {'foo': 'hello foo'})

        class Bar(Slotted, Base):
            pass
        if strict:
            self.assert_('__dict__' in Bar.__dict__)
            self.assertEqual(Bar.__dict__['__dict__'].__objclass__, Bar)
        bar = Bar()
        self.assert_(hasattr(bar, '__dict__'))
        bar.bar = 'hello bar'
        bar.baz = 'hello baz'
        bar.foo = 'hello foo'
        self.assertEqual(bar.bar, 'hello bar')
        self.assertEqual(bar.baz, 'hello baz')
        self.assertEqual(bar.__dict__, {'foo': 'hello foo', 'bar': 'hello bar'})

    def test_subclass_oldstyle(self):
        class OldBase:
            pass
        class Foo(OldBase, object):
            __slots__ = 'bar'
        if strict:
            self.assert_('__dict__' in Foo.__dict__)
            self.assertEqual(Foo.__dict__['__dict__'].__objclass__, Foo)
        foo = Foo()
        self.assert_(hasattr(foo, '__dict__'))
        foo.bar = 'hello bar'
        foo.foo = 'hello foo'
        self.assertEqual(foo.bar, 'hello bar')
        self.assertEqual(foo.__dict__, {'foo': 'hello foo'})

        class Bar(OldBase, object):
            __slots__ = '__dict__'
        self.assert_('__dict__' in Bar.__dict__)
        self.assertEqual(Bar.__dict__['__dict__'].__objclass__, Bar)
        bar = Bar()
        self.assert_(hasattr(bar, '__dict__'))
        bar.bar = 'hello bar'
        bar.foo = 'hello foo'
        self.assertEqual(bar.bar, 'hello bar')
        self.assertEqual(bar.__dict__, {'foo': 'hello foo', 'bar': 'hello bar'})

    def test_mixin_oldstyle(self):
        class OldBase:
            pass
        class NewBase(object):
            pass
        class Baz(NewBase, OldBase):
            __slots__ = 'baz'
        self.assert_('__dict__' not in Baz.__dict__)
        baz = Baz()
        self.assert_(hasattr(baz, '__dict__'))
        baz.baz = 'hello baz'
        baz.bar = 'hello bar'
        self.assertEqual(baz.baz, 'hello baz')
        self.assertEqual(baz.bar, 'hello bar')
        self.assertEqual(baz.__dict__, {'bar': 'hello bar'})


class SlottedWithWeakrefTestCase(unittest.TestCase):

    def test_subclass_oldstyle(self):
        class OldBase:
            pass
        class Foo(OldBase, object):
            __slots__ = '__dict__'
        self.assert_(hasattr(Foo, '__weakref__'))


def test_main():
    test_support.run_unittest(SlottedTestCase,
                              SlottedWithDictTestCase,
                              SlottedWithWeakrefTestCase)


if __name__ == '__main__':
    test_main()
