"""Misc func tests.

Made for Jython.
"""
import types
import unittest
from test import test_support

xyz = 123

def abc():
    return xyz

class FunctionTypeTestCase(unittest.TestCase):

    def test_func(self):
        self.assertEquals(abc(), 123)

    def test_functiontype(self):
        new_abc = types.FunctionType(abc.func_code, {'xyz': 456},
                                     abc.func_name, abc.func_defaults,
                                     abc.func_closure)
        self.assertEquals(new_abc(), 456)

    def test_functiontype_from_globals(self):
        sm = type(globals())()
        sm.update({'xyz': 789})
        sm_abc = types.FunctionType(abc.func_code, sm, abc.func_name,
                                    abc.func_defaults, abc.func_closure)
        self.assertEquals(sm_abc(), 789)


class MethodHashCodeTestCase(unittest.TestCase):

    def test_builtin_method_hashcode(self):
        foo = 'foo'
        self.assert_(foo.title is not foo.title)
        self.assertEqual(hash(foo.title), hash(foo.title))
        self.assertNotEqual(hash(foo.title), hash('bar'.title))

    def test_method_hashcode(self):
        class Foo(object):
            def bar(self):
                pass
        foo = Foo()
        self.assert_(foo.bar is not foo.bar)
        self.assertEqual(hash(foo.bar), hash(foo.bar))
        self.assertNotEqual(hash(foo.bar), hash(Foo().bar))


def test_main():
    test_support.run_unittest(FunctionTypeTestCase,
                              MethodHashCodeTestCase)

if __name__ == '__main__':
    test_main()
