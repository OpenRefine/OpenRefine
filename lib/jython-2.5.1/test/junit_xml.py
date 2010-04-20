"""Support for writing JUnit XML test results for the regrtest"""
import os
import re
import sys
import time
import traceback
import unittest
from StringIO import StringIO
from xml.sax import saxutils

# Invalid XML characters (control chars)
EVIL_CHARACTERS_RE = re.compile(r"[\000-\010\013\014\016-\037]")

class JUnitXMLTestRunner:

    """A unittest runner that writes results to a JUnit XML file in
    xml_dir
    """

    def __init__(self, xml_dir):
        self.xml_dir = xml_dir

    def run(self, test):
        result = JUnitXMLTestResult(self.xml_dir)
        test(result)
        result.write_xml()
        return result


class JUnitXMLTestResult(unittest.TestResult):

    """JUnit XML test result writer.

    The name of the file written to is determined from the full module
    name of the first test ran
    """

    def __init__(self, xml_dir):
        unittest.TestResult.__init__(self)
        self.xml_dir = xml_dir

        # The module name of the first test ran
        self.module_name = None

        # All TestCases
        self.tests = []

        # Start time
        self.start = None

        self.old_stdout = sys.stdout
        self.old_stderr = sys.stderr
        sys.stdout = self.stdout = Tee(sys.stdout)
        sys.stderr = self.stderr = Tee(sys.stderr)

    def startTest(self, test):
        unittest.TestResult.startTest(self, test)
        self.ensure_module_name(test)
        self.error, self.failure = None, None
        self.start = time.time()

    def stopTest(self, test):
        took = time.time() - self.start
        unittest.TestResult.stopTest(self, test)
        args = [test, took]
        if self.error:
            args.extend(['error', self.error])
        elif self.failure:
            args.extend(['failure', self.failure])
        self.tests.append(TestInfo.from_testcase(*args))

    def addError(self, test, err):
        unittest.TestResult.addError(self, test, err)
        self.error = err

    def addFailure(self, test, err):
        unittest.TestResult.addFailure(self, test, err)
        self.failure = err

    def ensure_module_name(self, test):
        """Set self.module_name from test if not already set"""
        if not self.module_name:
            self.module_name = '.'.join(test.id().split('.')[:-1])

    def write_xml(self):
        if not self.module_name:
            # No tests ran, nothing to write
            return
        took = time.time() - self.start

        stdout = self.stdout.getvalue()
        stderr = self.stderr.getvalue()
        sys.stdout = self.old_stdout
        sys.stderr = self.old_stderr

        ensure_dir(self.xml_dir)
        filename = os.path.join(self.xml_dir, 'TEST-%s.xml' % self.module_name)
        stream = open(filename, 'w')

        write_testsuite_xml(stream, len(self.tests), len(self.errors),
                            len(self.failures), 0, self.module_name, took)

        for info in self.tests:
            info.write_xml(stream)

        write_stdouterr_xml(stream, stdout, stderr)

        stream.write('</testsuite>')
        stream.close()


class TestInfo(object):

    """The JUnit XML <testcase/> model."""

    def __init__(self, name, took, type=None, exc_info=None):
        # The name of the test
        self.name = name

        # How long it took
        self.took = took

        # Type of test: 'error', 'failure' 'skipped', or None for a
        # success
        self.type = type

        if exc_info:
            self.exc_name = exc_name(exc_info)
            self.message = exc_message(exc_info)
            self.traceback = safe_str(''.join(
                    traceback.format_exception(*exc_info)))
        else:
            self.exc_name = self.message = self.traceback = ''

    @classmethod
    def from_testcase(cls, testcase, took, type=None, exc_info=None):
        name = testcase.id().split('.')[-1]
        return cls(name, took, type, exc_info)

    def write_xml(self, stream):
        stream.write('  <testcase name="%s" time="%.3f"' % (self.name,
                                                            self.took))

        if not self.type:
            # test was successful
            stream.write('/>\n')
            return

        stream.write('>\n    <%s type="%s" message=%s><![CDATA[%s]]></%s>\n' %
                     (self.type, self.exc_name, saxutils.quoteattr(self.message),
                      escape_cdata(self.traceback), self.type))
        stream.write('  </testcase>\n')


class Tee(StringIO):

    """Writes data to this StringIO and a separate stream"""

    def __init__(self, stream):
        StringIO.__init__(self)
        self.stream = stream

    def write(self, data):
        StringIO.write(self, data)
        self.stream.write(data)

    def flush(self):
        StringIO.flush(self)
        self.stream.flush()


def write_testsuite_xml(stream, tests, errors, failures, skipped, name, took):
    """Write the XML header (<testsuite/>)"""
    stream.write('<?xml version="1.0" encoding="utf-8"?>\n')
    stream.write('<testsuite tests="%d" errors="%d" failures="%d" ' %
                 (tests, errors, failures))
    stream.write('skipped="%d" name="%s" time="%.3f">\n' % (skipped, name,
                                                            took))

def write_stdouterr_xml(stream, stdout, stderr):
    """Write the stdout/err tags"""
    if stdout:
        stream.write('  <system-out><![CDATA[%s]]></system-out>\n' %
                     escape_cdata(safe_str(stdout)))
    if stderr:
        stream.write('  <system-err><![CDATA[%s]]></system-err>\n' %
                     escape_cdata(safe_str(stderr)))


def write_direct_test(junit_xml_dir, name, took, type=None, exc_info=None,
                      stdout=None, stderr=None):
    """Write XML for a regrtest 'direct' test; a test which was ran on
    import (which we label as __main__.__import__)
    """
    return write_manual_test(junit_xml_dir, '%s.__main__' % name, '__import__',
                             took, type, exc_info, stdout, stderr)


def write_doctest(junit_xml_dir, name, took, type=None, exc_info=None,
                  stdout=None, stderr=None):
    """Write XML for a regrtest doctest, labeled as __main__.__doc__"""
    return write_manual_test(junit_xml_dir, '%s.__main__' % name, '__doc__',
                             took, type, exc_info, stdout, stderr)


def write_manual_test(junit_xml_dir, module_name, test_name, took, type=None,
                      exc_info=None, stdout=None, stderr=None):
    """Manually write XML for one test, outside of unittest"""
    errors = type == 'error' and 1 or 0
    failures = type == 'failure' and 1 or 0
    skipped = type == 'skipped' and 1 or 0

    ensure_dir(junit_xml_dir)
    stream = open(os.path.join(junit_xml_dir, 'TEST-%s.xml' % module_name),
                  'w')

    write_testsuite_xml(stream, 1, errors, failures, skipped, module_name,
                        took)

    info = TestInfo(test_name, took, type, exc_info)
    info.write_xml(stream)

    write_stdouterr_xml(stream, stdout, stderr)

    stream.write('</testsuite>')
    stream.close()


def ensure_dir(dir):
    """Ensure dir exists"""
    if not os.path.exists(dir):
        os.mkdir(dir)


def exc_name(exc_info):
    """Determine the full name of the exception that caused exc_info"""
    exc = exc_info[1]
    name = getattr(exc.__class__, '__module__', '')
    if name:
        name += '.'
    return name + exc.__class__.__name__


def exc_message(exc_info):
    """Safely return a short message passed through safe_str describing
    exc_info, being careful of unicode values.
    """
    exc = exc_info[1]
    if exc is None:
        return safe_str(exc_info[0])
    if isinstance(exc, BaseException) and isinstance(exc.message, unicode):
        return safe_str(exc.message)
    try:
        return safe_str(str(exc))
    except UnicodeEncodeError:
        try:
            val = unicode(exc)
            return safe_str(val)
        except UnicodeDecodeError:
            return '?'


def escape_cdata(cdata):
    """Escape a string for an XML CDATA section"""
    return cdata.replace(']]>', ']]>]]&gt;<![CDATA[')


def safe_str(base):
    """Return a str valid for UTF-8 XML from a basestring"""
    if isinstance(base, unicode):
        return remove_evil(base.encode('utf-8', 'replace'))
    return remove_evil(base.decode('utf-8', 'replace').encode('utf-8',
                                                              'replace'))


def remove_evil(string):
    """Remove control characters from a string"""
    return EVIL_CHARACTERS_RE.sub('?', string)
