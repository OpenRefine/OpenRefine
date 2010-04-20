import unittest
import os
import sys
import shutil
import __builtin__
import py_compile
from test.test_support import run_unittest, TESTFN, is_jython

class TestMtime(unittest.TestCase):

    def test_mtime_compile(self):
        """
        This test exercises the mtime annotation that is now stored in Jython
        compiled files.  CPython already stores an mtime in its pyc files. To
        exercise this functionality, I am writing a py file, compiling it,
        setting the os modified time to a very low value on the compiled file,
        then changing the py file after a small sleep.  On CPython, this would
        still cause a re-compile.  In Jython before this fix it would not.
        See http://bugs.jython.org/issue1024
        """

        import time
        os.mkdir(TESTFN)
        try:
            mod = "mod1"
            source_path = os.path.join(TESTFN, "%s.py" % mod)
            if is_jython:
                compiled_path = os.path.join(TESTFN, "%s$py.class" % mod)
            else:
                compiled_path = os.path.join(TESTFN, "%s.pyc" % mod)
            fp = open(source_path, "w")
            fp.write("def foo(): return 'first'\n")
            fp.close()
            py_compile.compile(source_path)

            #sleep so that the internal mtime is older for the next source write.
            time.sleep(1)

            fp = open(source_path, "w")
            fp.write("def foo(): return 'second'\n")
            fp.close()

            # make sure the source file's mtime is artificially younger than
            # the compiled path's mtime.
            os.utime(source_path, (1,1))

            sys.path.append(TESTFN)
            import mod1
            self.assertEquals(mod1.foo(), 'second')
        finally:
            shutil.rmtree(TESTFN)

def test_main():
    run_unittest(TestMtime)

if __name__ == "__main__":
    test_main()
