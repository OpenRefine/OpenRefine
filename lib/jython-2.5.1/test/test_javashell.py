import unittest
from test import test_support

import re

import os
import javashell
from org.python.core.util import FileUtil

# testCmds is a list of (command, expectedOutput)
# each command is executed twice, once in unitialized environment and
# once with initialized environment

# smaller set of commands for simple test
testCmds = [
    ("echo hello world", "hello world"),
    ]

# turn off output from javashell.__warn
javashell.__warn = lambda *args: None

def dprint( *args ):
    #print args
    pass


# can instead set testCmds = fullTestCmds

# Note that the validation is incomplete for several of these
# - they should validate depending on platform and pre-post, but
# they don't.

# can assign testCmds = fullTestCmds for more extensive tests
key, value = "testKey", "testValue"
fullTestCmds = [
            # no quotes, should output both words
            ("echo hello world", "hello world"),
            # should print PATH (on NT)
            ("echo PATH=%PATH%", "(PATH=.*;.*)|(PATH=%PATH%)"),
            # should print 'testKey=%testKey%' on NT before initialization,
            # should print 'testKey=' on 95 before initialization,
            # and 'testKey=testValue' after
            ("echo %s=%%%s%%" % (key,key),
                    "(%s=)" % (key,)),
            # should print PATH (on Unix)
            ( "echo PATH=$PATH", "PATH=.*" ),
            # should print 'testKey=testValue' on Unix after initialization
            ( "echo %s=$%s" % (key,key),
                    "(%s=$%s)|(%s=)|(%s=%s)" % (key, key, key, key, value ) ),
            # should output quotes on NT but not on Unix
            ( 'echo "hello there"', '"?hello there"?' ),
            # should print 'why' to stdout.
            ( r'''jython -c "import sys;sys.stdout.write( 'why\n' )"''', "why" ),
            # should print 'why' to stderr.
            # doesn't work on NT because of quoting issues.
            # Have to add the print to give some output to stdout...
            # empty string matches everything...
            ( r'''jython -c "import sys;sys.stderr.write('why\n');print " ''',
              '' )
            ]

class JavaShellTest(unittest.TestCase):
    """This test validates the subshell functionality (javashell, os.environ, popen*).
    Does some white box as well as black box testing.
    """

    def _testCmds( self, _shellEnv, testCmds, whichEnv ):
        """test commands (key) and compare output to expected output (value).
        this actually executes all the commands twice, testing the return
        code by calling system(), and testing some of the output by calling
        execute()
        """
        for cmd, pattern in testCmds:
            dprint( "\nExecuting '%s' with %s environment" % (cmd, whichEnv))
            p = javashell.shellexecute(cmd)
            line = FileUtil.wrap(p.getInputStream()).readlines()[0]
            assert re.match( pattern, line ), \
                    "expected match for %s, got %s" % ( pattern, line )
            dprint( "waiting for", cmd, "to complete")
            assert not p.waitFor(), \
                    "%s failed with %s environment" % (cmd, whichEnv)

    def testSystem( self ):
        """test system and environment functionality"""
        org = os.environ
        self._testCmds( javashell._shellEnv, testCmds, "default" )

        # trigger initialization of environment
        os.environ[ key ] = value

        assert org.get( key, None ) == value, \
                "expected stub to have %s set" % key
        assert os.environ.get( key, None ) == value, \
                "expected real os.environment to have %s set" % key

        # if environment is initialized and jython gets ARGS=-i, it thinks
        # it is running in interactive mode, and fails to exit until
        # process.getOutputStream().close()
        try:
            del os.environ[ "ARGS" ]
        except KeyError:
            pass

        # test system using the non-default environment
        self._testCmds( javashell._shellEnv, testCmds, "initialized" )

        assert os.environ.has_key( "PATH" ), \
                "expected environment to have PATH attribute " \
                "(this may not apply to all platforms!)"

    def testPutEnv( self ):
        "Put an environment variable and ensure that spawned processes see the change"
        value = "Value we set"
        os.putenv( "NEWVARIABLE", value )
        newValue = os.popen( "echo $NEWVARIABLE" ).read().strip()
        if newValue == "$NEWVARIABLE":
            newValue = os.popen( "echo %NEWVARIABLE%" ).read().strip()
        if newValue == "%NEWVARIABLE%":
            raise test_support.TestSkipped( "Unable to find a subshell to execute echo" )
        assert newValue == value, (
            "Expected (%s) to equal value we set (%s)" % (
            newValue, value
            ))

    def testFormatUnicodeCommand(self):
        shell = javashell._ShellEnv(cmd=['runner'])
        self.assertEqual(shell._formatCmd('echo hello'), ['runner', 'echo hello'])
        self.assertEqual(shell._formatCmd(u'echo world'), ['runner', u'echo world'])

    def testExecuteUnicodeCommandWithRedirection(self):
        process = javashell.shellexecute(u'nonexcmd 2>&1')
        stdout = process.getOutputStream().toString()
        process.waitFor()
        self.assertNotEqual(stdout, "", "Redirecting 2>&1 failed with unicode cmd")

def test_main():
    test_support.run_unittest(JavaShellTest)

if __name__ == "__main__":
    test_main()
