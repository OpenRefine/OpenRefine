"""Misc. import tests

Made for Jython.
"""
import imp
import os
import shutil
import sys
import tempfile
import unittest
import subprocess
from test import test_support
from test_chdir import read, safe_mktemp, COMPILED_SUFFIX

class MislabeledImportTestCase(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.orig_cwd = os.getcwd()
        os.chdir(self.dir)
        self.orig_syspath = sys.path
        sys.path.append('')

    def tearDown(self):
        shutil.rmtree(self.dir)
        os.chdir(self.orig_cwd)
        sys.path = self.orig_syspath

    def test_renamed_bytecode(self):
        source = safe_mktemp(dir=self.dir, suffix='.py')
        fp = open(source, 'w')
        fp.write("test = 'imported'")
        fp.close()

        module = os.path.basename(source)[:-3]
        compiled = module + COMPILED_SUFFIX
        # Compile to bytecode
        module_obj = __import__(module)
        self.assertEquals(module_obj.test, 'imported')
        self.assert_(os.path.exists(compiled))

        # Rename the bytecode
        compiled_moved = safe_mktemp(dir=self.dir, suffix=COMPILED_SUFFIX)
        os.rename(compiled, compiled_moved)

        # Ensure we can still import the renamed bytecode
        moved_module = os.path.basename(compiled_moved)[:-len(COMPILED_SUFFIX)]
        module_obj = __import__(moved_module)
        self.assertEquals(module_obj.__file__, os.path.basename(compiled_moved))
        self.assertEquals(module_obj.test, 'imported')

    def test_dunder_init(self):
        os.mkdir('foo')

        # typical import: foo.__init__$py.class is actually compiled
        # with a class name of foo
        init = os.path.join('foo', '__init__.py')
        fp = open(init, 'w')
        fp.write("bar = 'test'")
        fp.close()
        module_obj = __import__('foo')
        self.assertEquals(module_obj.__file__, init)
        self.assertEquals(module_obj.bar, 'test')

        init_compiled = init[:-3] + COMPILED_SUFFIX
        self.assert_(os.path.exists(init_compiled))
        bytecode = read(init_compiled)

        # trigger an abnormal import of foo.__init__; ask for it by the
        # mismatched __init__ name
        fp = open(os.path.join('foo', 'test.py'), 'w')
        fp.write("import __init__; baz = __init__.bar + 'test'; "
                 "init_file = __init__.__file__")
        fp.close()
        module_obj = __import__('foo.test')
        self.assertEquals(module_obj.test.baz, 'testtest')
        self.assertEqual(module_obj.test.init_file,
                         os.path.join('foo', '__init__' + COMPILED_SUFFIX))

        # Ensure a recompile of __init__$py.class wasn't triggered to
        # satisfy the abnormal import
        self.assertEquals(bytecode, read(init_compiled),
                          'bytecode was recompiled')

        # Ensure load_module can still load it as foo (doesn't
        # recompile)
        module_obj = imp.load_module('foo', *imp.find_module('foo'))
        self.assertEquals(module_obj.bar, 'test')

        # Again ensure we didn't recompile
        self.assertEquals(bytecode, read(init_compiled),
                          'bytecode was recompiled')

    def test_corrupt_bytecode(self):
        f = open("empty$py.class", "w")
        f.close()
        self.assertRaises(ImportError, __import__, "empty")

class OverrideBuiltinsImportTestCase(unittest.TestCase):
    def test_override(self):
        modname = os.path.__name__
        tests = [
            ("import os.path"         , "('os.path', None, -1, 'os')"  ),
            ("import os.path as path2", "('os.path', None, -1, 'os')"  ),
            ("from os.path import *"  ,
             "('os.path', ('*',), -1, '%s')" % modname),
            ("from os.path import join",
                 "('os.path', ('join',), -1, '%s')" % modname),
            ("from os.path import join as join2",
                 "('os.path', ('join',), -1, '%s')" % modname),
            ("from os.path import join as join2, split as split2",
                 "('os.path', ('join', 'split'), -1, '%s')" % modname),
        ]

        import sys
        # Replace __builtin__.__import__ to trace the calls
        import __builtin__
        oldimp = __builtin__.__import__
        try:
            def __import__(name, globs, locs, fromlist, level=-1):
                mod = oldimp(name, globs, locs, fromlist, level)
                globs["result"] = str((name, fromlist, level, mod.__name__))
                raise ImportError

            __builtin__.__import__ = __import__
            failed = 0
            for statement, expected in tests:
                try:
                    c = compile(statement, "<unknown>", "exec")
                    exec c in locals(), globals()
                    raise Exception("ImportError expected.")
                except ImportError:
                    pass
                self.assertEquals(expected, result)
        finally:
            __builtin__.__import__ = oldimp

class ImpTestCase(unittest.TestCase):

    def test_imp_find_module_builtins(self):
        self.assertEqual(imp.find_module('sys'), (None, 'sys', ('', '', 6)))
        self.assertEqual(imp.find_module('__builtin__'),
                         (None, '__builtin__', ('', '', 6)))
        self.assertEqual(imp.find_module('imp'), (None, 'imp', ('', '', 6)))

    def test_getattr_module(self):
        '''Replacing __getattr__ in a class shouldn't lead to calls to __getitem__

        http://bugs.jython.org/issue438108'''
        from test import anygui
        # causes a stack overflow if the bug occurs
        self.assertRaises(Exception, getattr, anygui, 'abc')

    def test_import_star(self):
        self.assertEquals(subprocess.call([sys.executable,
        test_support.findfile("import_star_from_java.py")]), 0)

    def test_selfreferential_classes(self):
        from org.python.tests.inbred import Metis
        from org.python.tests.inbred import Zeus
        self.assertEquals(Metis, Zeus.Athena.__bases__[0])
        self.assertEquals(Zeus, Metis.__bases__[0])

    def test_sys_modules_deletion(self):
        self.assertRaises(ZeroDivisionError, __import__, 'test.module_deleter')

    #XXX: this is probably a good test to push upstream to CPython.
    if hasattr(os, "symlink"):
        def test_symlinks(self):
            # Ensure imports work over symlinks.  Did not work in Jython from
            # 2.1 to 2.5.0, fixed in 2.5.1  See
            # http://bugs.jython.org/issue645615.
            sym = test_support.TESTFN+"1"
            try:
                os.mkdir(test_support.TESTFN)
                init = os.path.join(test_support.TESTFN, "__init__.py")
                fp = open(init, 'w')
                fp.write("test = 'imported'")
                fp.close()
                os.symlink(test_support.TESTFN, sym)
                module = os.path.basename(sym)
                module_obj = __import__(module)
                self.assertEquals(module_obj.test, 'imported')

            finally:
                shutil.rmtree(test_support.TESTFN)
                test_support.unlink(sym)

def test_main():
    test_support.run_unittest(MislabeledImportTestCase,
                              OverrideBuiltinsImportTestCase,
                              ImpTestCase)

if __name__ == '__main__':
    test_main()
