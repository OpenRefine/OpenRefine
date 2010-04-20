
# Jython Database Specification API 2.0
#
# $Id: runner.py 6647 2009-08-10 17:23:22Z fwierzbicki $
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

"""
To run the tests, simply invoke this script from the commandline:

 jython runner.py <xml config file> [vendor, ...]

If no vendors are given, then all vendors will be tested.  If a
vendor is given, then only that vendor will be tested.
"""

import unittest, os
import xmllib, __builtin__, re

def __imp__(module, attr=None):
    if attr:
        j = __import__(module, globals(), locals())
        return getattr(j, attr)
    else:
        last = module.split(".")[-1]
        return __import__(module, globals(), locals(), last)

class Factory:
    def __init__(self, classname, method):
        self.classname = classname
        self.method = method
        self.arguments = []
        self.keywords = {}

class Testcase:
    def __init__(self, frm, impt):
        self.frm = frm
        self.impt = impt
        self.ignore = []

class Test:
    def __init__(self, name, os):
        self.name = name
        self.os = os
        self.factory = None
        self.tests = []

class Vendor:
    def __init__(self, name, datahandler=None):
        self.name = name
        self.scroll = None
        self.datahandler = datahandler
        self.tests = []
        self.tables = {}

class ConfigParser(xmllib.XMLParser):
    """
    A simple XML parser for the config file.
    """
    def __init__(self, **kw):
        apply(xmllib.XMLParser.__init__, (self,), kw)
        self.vendors = []
        self.table_stack = []
        self.re_var = re.compile(r"\${(.*?)}")

    def vendor(self):
        assert len(self.vendors) > 0, "no vendors"
        return self.vendors[-1]

    def test(self):
        v = self.vendor()
        assert len(v.tests) > 0, "no tests"
        return v.tests[-1]

    def factory(self):
        c = self.test()
        assert c.factory, "no factory"
        return c.factory

    def testcase(self):
        s = self.test()
        assert len(s.tests) > 0, "no testcases"
        return s.tests[-1]

    def value(self, value):
        def repl(sub):
            from java.lang import System
            return System.getProperty(sub.group(1), sub.group(1))
        value = self.re_var.sub(repl, value)
        return value

    def start_vendor(self, attrs):
        if attrs.has_key('datahandler'):
            v = Vendor(attrs['name'], attrs['datahandler'])
        else:
            v = Vendor(attrs['name'])
        if attrs.has_key('scroll'):
            v.scroll = attrs['scroll']
        self.vendors.append(v)

    def start_test(self, attrs):
        v = self.vendor()
        c = Test(attrs['name'], attrs['os'])
        v.tests.append(c)

    def start_factory(self, attrs):
        c = self.test()
        f = Factory(attrs['class'], attrs['method'])
        c.factory = f

    def start_argument(self, attrs):
        f = self.factory()
        if attrs.has_key('type'):
            f.arguments.append((attrs['name'], getattr(__builtin__, attrs['type'])(self.value(attrs['value']))))
        else:
            f.arguments.append((attrs['name'], self.value(attrs['value'])))

    def start_keyword(self, attrs):
        f = self.factory()
        if attrs.has_key('type'):
            f.keywords[attrs['name']] = getattr(__builtin__, attrs['type'])(self.value(attrs['value']))
        else:
            f.keywords[attrs['name']] = self.value(attrs['value'])

    def start_ignore(self, attrs):
        t = self.testcase()
        t.ignore.append(attrs['name'])

    def start_testcase(self, attrs):
        c = self.test()
        c.tests.append(Testcase(attrs['from'], attrs['import']))

    def start_table(self, attrs):
        self.table_stack.append((attrs['ref'], attrs['name']))

    def end_table(self):
        del self.table_stack[-1]

    def handle_data(self, data):
        if len(self.table_stack):
            ref, tabname = self.table_stack[-1]
            self.vendor().tables[ref] = (tabname, data.strip())

class SQLTestCase(unittest.TestCase):
    """
    Base testing class.  It contains the list of table and factory information
    to run any tests.
    """
    def __init__(self, name, vendor, factory):
        unittest.TestCase.__init__(self, name)
        self.vendor = vendor
        self.factory = factory
        if self.vendor.datahandler:
            self.datahandler = __imp__(self.vendor.datahandler)

    def table(self, name):
        return self.vendor.tables[name]

    def has_table(self, name):
        return self.vendor.tables.has_key(name)

def make_suite(vendor, testcase, factory, mask=None):
    clz = __imp__(testcase.frm, testcase.impt)
    caseNames = filter(lambda x, i=testcase.ignore: x not in i, unittest.getTestCaseNames(clz, "test"))
    if mask is not None:
        caseNames = filter(lambda x, mask=mask: x == mask, caseNames)
    tests = [clz(caseName, vendor, factory) for caseName in caseNames]
    return unittest.TestSuite(tests)

def test(vendors, include=None, mask=None):
    for vendor in vendors:
        if not include or vendor.name in include:
            print
            print "testing [%s]" % (vendor.name)
            for test in vendor.tests:
                if not test.os or test.os == os.name:
                    for testcase in test.tests:
                        suite = make_suite(vendor, testcase, test.factory, mask)
                        unittest.TextTestRunner().run(suite)
        else:
            print
            print "skipping [%s]" % (vendor.name)

if __name__ == '__main__':
    import sys, getopt

    try:
        opts, args = getopt.getopt(sys.argv[1:], "t:", [])
    except getopt.error, msg:
        print "%s -t [testmask] <vendor>[,<vendor>]"
        sys.exit(0)

    mask = None
    for a in opts:
        opt, arg = a
        if opt == '-t':
            mask = arg

    configParser = ConfigParser()
    fp = open(args[0], "r")
    configParser.feed(fp.read())
    fp.close()
    test(configParser.vendors, args[1:], mask=mask)
    sys.exit(0)
