"""Test path handling on Windows

Made for Jython.
"""
from __future__ import with_statement
import os
import unittest
from test import test_support

class NTAbspathTestCase(unittest.TestCase):

    def setUp(self):
        with open(test_support.TESTFN, 'w') as fp:
            fp.write('foo')

        # Move to the same drive as TESTFN
        drive, self.path = os.path.splitdrive(os.path.abspath(
                test_support.TESTFN))
        self.orig_cwd = os.getcwd()
        os.chdir(os.path.join(drive, os.sep))

    def tearDown(self):
        os.chdir(self.orig_cwd)
        os.remove(test_support.TESTFN)

    def test_abspaths(self):
        # Ensure r'\TESTFN' and '/TESTFN' are handled as absolute
        for path in self.path, self.path.replace('\\', '/'):
            with open(path) as fp:
                self.assertEqual(fp.read(), 'foo')


def test_main():
    if (os._name if test_support.is_jython else os.name) != 'nt':
        raise test_support.TestSkipped('NT specific test')
    test_support.run_unittest(NTAbspathTestCase)


if __name__ == '__main__':
    test_main()
