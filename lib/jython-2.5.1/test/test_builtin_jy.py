# -*- coding: utf-8 -*-
import test.test_support, unittest
from test.test_support import TESTFN, unlink

import sys, UserDict

from codecs import BOM_UTF8

class BuiltinTest(unittest.TestCase):

    def test_in_sys_modules(self):
        self.assert_("__builtin__" in sys.modules,
            "__builtin__ not found in sys.modules")

    def test_hasattr_swallows_exceptions(self):
        class Foo(object):
            def __getattr__(self, name):
                raise TypeError()
        self.assert_(not hasattr(Foo(), 'bar'))

    def test_dir(self):
        # for http://bugs.jython.org/issue1196
        class Foo(object):
            def __getattribute__(self, name):
                return name
        self.assertEqual(dir(Foo()), [])

class LoopTest(unittest.TestCase):

    def test_break(self):
        while 1:
            i = 0
            while i<10:
                i = i+1
            else:
                break

class DebugTest(unittest.TestCase):

    def test_debug(self):
        "__debug__ exists"
        try:
            foo = __debug__
        except NameError, e:
            self.assert_(False)

class GetSliceTest(unittest.TestCase):

    def test_getslice(self):
        class F:
            def __getitem__(self,*args): return '__getitem__ '+repr(args)
            def __getslice__(self,*args): return '__getslice__ '+repr(args)
        self.failUnless("__getslice__ (1, 1)" in F()[1:1])

class ChrTest(unittest.TestCase):

    def test_debug(self):
        "chr(None) throws TypeError"
        foo = False
        try:
            chr(None)
        except TypeError, e:
            foo = True
        self.assert_(foo)

class ReturnTest(unittest.TestCase):

    def test_finally(self):
        '''return in finally causes java.lang.VerifyError at compile time'''
        def timeit(f):
            t0 = time.clock()
            try:
                f()
            finally:
                t1 = time.clock()
                return t1 - t0

class ReprTest(unittest.TestCase):
    def test_unbound(self):
        "Unbound methods indicated properly in repr"
        class Foo:
            def bar(s):
                pass
        self.failUnless(repr(Foo.bar).startswith('<unbound method'))

class CallableTest(unittest.TestCase):

    def test_callable_oldstyle(self):
        class Foo:
            pass
        self.assert_(callable(Foo))
        self.assert_(not callable(Foo()))
        class Bar:
            def __call__(self):
                return None
        self.assert_(callable(Bar()))
        class Baz:
            def __getattr__(self, name):
                return None
        self.assert_(callable(Baz()))

    def test_callable_newstyle(self):
        class Foo(object):
            pass
        self.assert_(callable(Foo))
        self.assert_(not callable(Foo()))
        class Bar(object):
            def __call__(self):
                return None
        self.assert_(callable(Bar()))
        class Baz(object):
            def __getattr__(self, name):
                return None
        self.assert_(not callable(Baz()))

class ConversionTest(unittest.TestCase):

    class Foo(object):
        def __int__(self):
            return 3
        def __float__(self):
            return 3.14
    foo = Foo()

    def test_range_non_int(self):
        self.assertEqual(range(self.foo), [0, 1, 2])

    def test_xrange_non_int(self):
        self.assertEqual(list(xrange(self.foo)), [0, 1, 2])

    def test_round_non_float(self):
        self.assertEqual(round(self.Foo(), 1), 3.1)

class ExecEvalTest(unittest.TestCase):

    def test_eval_bom(self):
        self.assertEqual(eval(BOM_UTF8 + '"foo"'), 'foo')
        # Actual BOM ignored, so causes a SyntaxError
        self.assertRaises(SyntaxError, eval,
                          BOM_UTF8.decode('iso-8859-1') + '"foo"')

    def test_parse_str_eval(self):
        foo = 'föö'
        for code, expected in (
            ("'%s'" % foo.decode('utf-8'), foo),
            ("# coding: utf-8\n'%s'" % foo, foo),
            ("%s'%s'" % (BOM_UTF8, foo), foo),
            ("'\rfoo\r'", '\rfoo\r')
            ):
            mod = compile(code, 'test.py', 'eval')
            result = eval(mod)
            self.assertEqual(result, expected)
            result = eval(code)
            self.assertEqual(result, expected)

    def test_parse_str_exec(self):
        foo = 'föö'
        for code, expected in (
            ("bar = '%s'" % foo.decode('utf-8'), foo),
            ("# coding: utf-8\nbar = '%s'" % foo, foo),
            ("%sbar = '%s'" % (BOM_UTF8, foo), foo),
            ("bar = '\rfoo\r'", '\rfoo\r')
            ):
            ns = {}
            exec code in ns
            self.assertEqual(ns['bar'], expected)

    def test_general_eval(self):
        # Tests that general mappings can be used for the locals argument

        class M:
            "Test mapping interface versus possible calls from eval()."
            def __getitem__(self, key):
                if key == 'a':
                    return 12
                raise KeyError
            def keys(self):
                return list('xyz')

        m = M()
        g = globals()
        self.assertEqual(eval('a', g, m), 12)
        self.assertRaises(NameError, eval, 'b', g, m)
        self.assertEqual(eval('dir()', g, m), list('xyz'))
        self.assertEqual(eval('globals()', g, m), g)
        self.assertEqual(eval('locals()', g, m), m)
        #XXX: the following assert holds in CPython because globals must be a
        #     real dict.  Should Jython be as strict?
        #self.assertRaises(TypeError, eval, 'a', m)
        class A:
            "Non-mapping"
            pass
        m = A()
        self.assertRaises(TypeError, eval, 'a', g, m)

        # Verify that dict subclasses work as well
        class D(dict):
            def __getitem__(self, key):
                if key == 'a':
                    return 12
                return dict.__getitem__(self, key)
            def keys(self):
                return list('xyz')

        d = D()
        self.assertEqual(eval('a', g, d), 12)
        self.assertRaises(NameError, eval, 'b', g, d)
        self.assertEqual(eval('dir()', g, d), list('xyz'))
        self.assertEqual(eval('globals()', g, d), g)
        self.assertEqual(eval('locals()', g, d), d)

        # Verify locals stores (used by list comps)
        eval('[locals() for i in (2,3)]', g, d)
        eval('[locals() for i in (2,3)]', g, UserDict.UserDict())

        class SpreadSheet:
            "Sample application showing nested, calculated lookups."
            _cells = {}
            def __setitem__(self, key, formula):
                self._cells[key] = formula
            def __getitem__(self, key):
                return eval(self._cells[key], globals(), self)

        ss = SpreadSheet()
        ss['a1'] = '5'
        ss['a2'] = 'a1*6'
        ss['a3'] = 'a2*7'
        self.assertEqual(ss['a3'], 210)

        # Verify that dir() catches a non-list returned by eval
        # SF bug #1004669
        class C:
            def __getitem__(self, item):
                raise KeyError(item)
            def keys(self):
                return 'a'
        self.assertRaises(TypeError, eval, 'dir()', globals(), C())

    # Done outside of the method test_z to get the correct scope
    z = 0
    f = open(TESTFN, 'w')
    f.write('z = z+1\n')
    f.write('z = z*2\n')
    f.close()
    execfile(TESTFN)

    def test_execfile(self):
        globals = {'a': 1, 'b': 2}
        locals = {'b': 200, 'c': 300}

        class M:
            "Test mapping interface versus possible calls from execfile()."
            def __init__(self):
                self.z = 10
            def __getitem__(self, key):
                if key == 'z':
                    return self.z
                raise KeyError
            def __setitem__(self, key, value):
                if key == 'z':
                    self.z = value
                    return
                raise KeyError

        locals = M()
        locals['z'] = 0
        execfile(TESTFN, globals, locals)
        self.assertEqual(locals['z'], 2)

        self.assertRaises(TypeError, execfile)
        self.assertRaises(TypeError, execfile, TESTFN, {}, ())
        unlink(TESTFN)

class ModuleNameTest(unittest.TestCase):
    """Tests that the module when imported has the same __name__"""

    def test_names(self):
        for name in sys.builtin_module_names:
            if name not in ('time', '_random', 'array', '_collections', '_ast'):
                module = __import__(name)
                self.assertEqual(name, module.__name__)




def test_main():
    test.test_support.run_unittest(BuiltinTest,
                                   LoopTest,
                                   DebugTest,
                                   GetSliceTest,
                                   ChrTest,
                                   ReturnTest,
                                   ReprTest,
                                   CallableTest,
                                   ConversionTest,
                                   ExecEvalTest,
                                   ModuleNameTest,
                                   )

if __name__ == "__main__":
    test_main()
