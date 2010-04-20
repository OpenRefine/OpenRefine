"""Misc subprocess tests"""
import unittest
import os
import sys
from test import test_support
from subprocess import PIPE, Popen, _cmdline2list

class EnvironmentInheritanceTest(unittest.TestCase):

    def testDefaultEnvIsInherited(self):
        # Test for issue #1104
        os.environ['foo'] = 'something'
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["foo"])'],
                   stdout=PIPE)
        self.assertEquals('something', p1.stdout.read())


class JythonOptsTest(unittest.TestCase):

    """ Tests for (some parts of) issue #1187: JYTHON_OPTS should not be
    enriched by arguments
    """

    def testNoJythonOpts(self):
        os.environ['JYTHON_OPTS'] = ''
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["JYTHON_OPTS"])'],
                   stdout=PIPE)
        self.assertEquals('', p1.stdout.read())

    def testExistingJythonOpts(self):
        options = '-Qold -Qwarn'
        os.environ['JYTHON_OPTS'] = options
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["JYTHON_OPTS"])'],
                   stdout=PIPE)
        self.assertEquals(options, p1.stdout.read())


class Cmdline2ListTestCase(unittest.TestCase):

    cmdlines = {
        # From "Parsing C Command-Line Arguments"
        # http://msdn.microsoft.com/en-us/library/a1y7w461(VS.80).aspx
        '"a b c" d e': ['a b c', 'd', 'e'],
        r'"ab\"c" "\\" d': ['ab"c', '\\', 'd'],
        r'a\\\b d"e f"g h': [r'a\\\b', 'de fg', 'h'],
        r'a\\\"b c d': [r'a\"b', 'c', 'd'],
        r'a\\\\"b c" d e': [r'a\\b c', 'd', 'e'],

        r'C:\\foo\bar\baz jy thon': [r'C:\\foo\bar\baz', 'jy', 'thon'],
        r'C:\\Program Files\Foo\Bar qu \\ ux':
            [r'C:\\Program', 'Files\Foo\Bar', 'qu', '\\\\', 'ux'],
        r'"C:\\Program Files\Foo\Bar" qu \\ ux':
            [r'C:\\Program Files\Foo\Bar', 'qu', '\\\\', 'ux'],
        r'dir "C:\\Program Files\Foo\\" bar':
            ['dir', 'C:\\\\Program Files\\Foo\\', 'bar'],

        r'echo "\"I hate Windows!\""': ['echo', '"I hate Windows!"'],
        r'print "jython" "': ['print', 'jython', ''],
        r'print \"jython\" \"': ['print', '"jython"', '"'],
        r'print \"jython\" \\"': ['print', '"jython"', '\\']
    }

    def test_cmdline2list(self):
        for cmdline, argv in self.cmdlines.iteritems():
            self.assertEqual(_cmdline2list(cmdline), argv)


def test_main():
    test_support.run_unittest(
        EnvironmentInheritanceTest,
        JythonOptsTest,
        Cmdline2ListTestCase)


if __name__ == '__main__':
    test_main()
