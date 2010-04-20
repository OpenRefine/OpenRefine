import inspect
import os.path
import sys
import unittest
from test import test_support
from regrtest import runtest

def make_fib_function():
    from org.python.core import PyBytecode, PyFunction
    co_argcount = 1
    co_nlocals = 1
    co_stacksize = 4
    co_flags = 67
    co_code = '|\x00\x00d\x01\x00j\x02\x00p\r\x00\x01|\x00\x00d\x02\x00j\x02\x00o\x08\x00\x01d\x02\x00Sn\x1d\x00\x01t\x00\x00|\x00\x00d\x03\x00\x18\x83\x01\x00t\x00\x00|\x00\x00d\x02\x00\x18\x83\x01\x00\x17Sd\x00\x00S'
    co_consts = (None, 0, 1, 2)
    co_names = ('fib',)
    co_varnames = ('x',)
    co_filename = '<fib test code>'
    co_name = 'fib'
    co_firstlineno = 1
    co_lnotab = '\x00\x01\x1a\x01\x08\x02'
    co_freevars = ()
    co_cellvars = ()

    c = PyBytecode(
        co_argcount, co_nlocals, co_stacksize, co_flags,
        co_code,  co_consts, co_names, co_varnames,
        co_filename, co_name, co_firstlineno, co_lnotab, co_freevars, co_cellvars)

    return PyFunction(c, globals())

fib = make_fib_function()

class PyBytecodeTest(unittest.TestCase):

    def test_fib(self):
        expected_fib = [1,1,2,3,5,8,13,21,34,55]
        for i in range(10):
            self.assertEquals(fib(i), expected_fib[i])

class AdhocRegrtest(unittest.TestCase):

    def setUp(self):
        self.old_verbosity = test_support.verbose
        test_support.verbose = 0
        import pycimport
        sys.path.insert(0, os.path.join(os.path.split(inspect.getfile(self.__class__))[0], 'pbcvm'))

    def test_regrtest_pyc(self):
        for test in (
            # change the names a bit so we don't have to worry about module unloading or spawning a separate JVM
            # however, this testing approach too limits the tests that can be run, so we should rewrite to
            # use subprocess asap
            'test_types_pyc',
            'test_exceptions_pyc'):
            ok = runtest(test, generate=False, verbose=False, quiet=True, testdir=None,
                         huntrleaks=False, junit_xml=None)
            self.assertTrue(ok > 0)

    def tearDown(self):
        # typical unsafe ops we have to do in testing...
        test_support.verbose = self.old_verbosity
        sys.path.pop(0)
        sys.meta_path.pop(0)


def test_main():
    test_support.run_unittest(PyBytecodeTest, AdhocRegrtest)

if __name__ == "__main__":
    test_main()
