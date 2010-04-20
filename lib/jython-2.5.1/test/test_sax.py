# -*- coding: iso-8859-1 -*-
# regression test for SAX 2.0
# $Id: test_sax.py,v 1.13 2004/03/20 07:46:04 fdrake Exp $

import urllib
from xml.sax import handler, make_parser, ContentHandler, \
                    SAXException, SAXReaderNotAvailable, SAXParseException
try:
    make_parser()
except SAXReaderNotAvailable:
    # don't try to test this module if we cannot create a parser
    raise ImportError("no XML parsers available")
from xml.sax.saxutils import XMLGenerator, escape, unescape, quoteattr, \
                             XMLFilterBase, Location
from xml.sax.xmlreader import InputSource, AttributesImpl, AttributesNSImpl
from cStringIO import StringIO
from test.test_support import is_jython, verbose, TestFailed, findfile

# ===== Utilities

tests = 0
failures = []

def confirm(outcome, name):
    global tests

    tests = tests + 1
    if outcome:
        if verbose:
            print "Passed", name
    else:
        print "Failed", name
        failures.append(name)

def test_make_parser2():
    try:
        # Creating parsers several times in a row should succeed.
        # Testing this because there have been failures of this kind
        # before.
        from xml.sax import make_parser
        p = make_parser()
        from xml.sax import make_parser
        p = make_parser()
        from xml.sax import make_parser
        p = make_parser()
        from xml.sax import make_parser
        p = make_parser()
        from xml.sax import make_parser
        p = make_parser()
        from xml.sax import make_parser
        p = make_parser()
    except:
        return 0
    else:
        return p


# ===========================================================================
#
#   saxutils tests
#
# ===========================================================================

# ===== escape

def test_escape_basic():
    return escape("Donald Duck & Co") == "Donald Duck &amp; Co"

def test_escape_all():
    return escape("<Donald Duck & Co>") == "&lt;Donald Duck &amp; Co&gt;"

def test_escape_extra():
    return escape("Hei på deg", {"å" : "&aring;"}) == "Hei p&aring; deg"

# ===== unescape

def test_unescape_basic():
    return unescape("Donald Duck &amp; Co") == "Donald Duck & Co"

def test_unescape_all():
    return unescape("&lt;Donald Duck &amp; Co&gt;") == "<Donald Duck & Co>"

def test_unescape_extra():
    return unescape("Hei på deg", {"å" : "&aring;"}) == "Hei p&aring; deg"

def test_unescape_amp_extra():
    return unescape("&amp;foo;", {"&foo;": "splat"}) == "&foo;"

# ===== quoteattr

def test_quoteattr_basic():
    return quoteattr("Donald Duck & Co") == '"Donald Duck &amp; Co"'

def test_single_quoteattr():
    return (quoteattr('Includes "double" quotes')
            == '\'Includes "double" quotes\'')

def test_double_quoteattr():
    return (quoteattr("Includes 'single' quotes")
            == "\"Includes 'single' quotes\"")

def test_single_double_quoteattr():
    return (quoteattr("Includes 'single' and \"double\" quotes")
            == "\"Includes 'single' and &quot;double&quot; quotes\"")

# ===== make_parser

def test_make_parser():
    try:
        # Creating a parser should succeed - it should fall back
        # to the expatreader
        p = make_parser(['xml.parsers.no_such_parser'])
    except:
        return 0
    else:
        return p


# ===== XMLGenerator

start = '<?xml version="1.0" encoding="iso-8859-1"?>\n'

def test_xmlgen_basic():
    result = StringIO()
    gen = XMLGenerator(result)
    gen.startDocument()
    gen.startElement("doc", {})
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc></doc>"

def test_xmlgen_content():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {})
    gen.characters("huhei")
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc>huhei</doc>"

def test_xmlgen_escaped_content():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {})
    gen.characters(unicode("\xa0\\u3042", "unicode-escape"))
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc>\xa0&#12354;</doc>"

def test_xmlgen_escaped_attr():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {"x": unicode("\\u3042", "unicode-escape")})
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + '<doc x="&#12354;"></doc>'

def test_xmlgen_pi():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.processingInstruction("test", "data")
    gen.startElement("doc", {})
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<?test data?><doc></doc>"

def test_xmlgen_content_escape():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {})
    gen.characters("<huhei&")
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc>&lt;huhei&amp;</doc>"

def test_xmlgen_attr_escape():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {"a": '"'})
    gen.startElement("e", {"a": "'"})
    gen.endElement("e")
    gen.startElement("e", {"a": "'\""})
    gen.endElement("e")
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start \
           + "<doc a='\"'><e a=\"'\"></e><e a=\"'&quot;\"></e></doc>"

def test_xmlgen_attr_escape_manydouble():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {"a": '"\'"'})
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc a='\"&apos;\"'></doc>"

def test_xmlgen_attr_escape_manysingle():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {"a": "'\"'"})
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + '<doc a="\'&quot;\'"></doc>'

def test_xmlgen_ignorable():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startElement("doc", {})
    gen.ignorableWhitespace(" ")
    gen.endElement("doc")
    gen.endDocument()

    return result.getvalue() == start + "<doc> </doc>"

ns_uri = "http://www.python.org/xml-ns/saxtest/"

def test_xmlgen_ns():
    result = StringIO()
    gen = XMLGenerator(result)

    gen.startDocument()
    gen.startPrefixMapping("ns1", ns_uri)
    gen.startElementNS((ns_uri, "doc"), "ns1:doc", {})
    # add an unqualified name
    gen.startElementNS((None, "udoc"), None, {})
    gen.endElementNS((None, "udoc"), None)
    gen.endElementNS((ns_uri, "doc"), "ns1:doc")
    gen.endPrefixMapping("ns1")
    gen.endDocument()

    return result.getvalue() == start + \
           ('<ns1:doc xmlns:ns1="%s"><udoc></udoc></ns1:doc>' %
                                         ns_uri)

# ===== XMLFilterBase

def test_filter_basic():
    result = StringIO()
    gen = XMLGenerator(result)
    filter = XMLFilterBase()
    filter.setContentHandler(gen)

    filter.startDocument()
    filter.startElement("doc", {})
    filter.characters("content")
    filter.ignorableWhitespace(" ")
    filter.endElement("doc")
    filter.endDocument()

    return result.getvalue() == start + "<doc>content </doc>"

# ===========================================================================
#
#   expatreader tests
#
# ===========================================================================

# ===== XMLReader support

def test_expat_file():
    parser = make_parser()
    result = StringIO()
    xmlgen = XMLGenerator(result)

    parser.setContentHandler(xmlgen)
    parser.parse(open(findfile("test.xml")))

    return result.getvalue() == xml_test_out

# ===== DTDHandler support

class TestDTDHandler:

    def __init__(self):
        self._notations = []
        self._entities  = []

    def notationDecl(self, name, publicId, systemId):
        self._notations.append((name, publicId, systemId))

    def unparsedEntityDecl(self, name, publicId, systemId, ndata):
        self._entities.append((name, publicId, systemId, ndata))

def test_expat_dtdhandler():
    parser = make_parser()
    handler = TestDTDHandler()
    parser.setDTDHandler(handler)

    parser.parse(StringIO('''<!DOCTYPE doc [
  <!ENTITY img SYSTEM "expat.gif" NDATA GIF>
  <!NOTATION GIF PUBLIC "-//CompuServe//NOTATION Graphics Interchange Format 89a//EN">
]>
<doc></doc>'''))
    if len(handler._entities) != 1 or len(handler._entities[0]) != 4:
        return 0
    name, pubId, sysId, ndata = handler._entities[0]
    if name != 'img' or not pubId is None or not sysId.endswith('expat.gif') or ndata != 'GIF':
        return 0
    return handler._notations == [("GIF", "-//CompuServe//NOTATION Graphics Interchange Format 89a//EN", None)]

# ===== EntityResolver support

class TestEntityResolver:

    def resolveEntity(self, publicId, systemId):
        inpsrc = InputSource()
        inpsrc.setByteStream(StringIO("<entity/>"))
        return inpsrc

def test_expat_entityresolver():
    parser = make_parser()
    parser.setEntityResolver(TestEntityResolver())
    result = StringIO()
    parser.setContentHandler(XMLGenerator(result))

    parser.parse(StringIO('''<!DOCTYPE doc [
  <!ENTITY test SYSTEM "whatever">
]>
<doc>&test;</doc>'''))
    return result.getvalue() == start + "<doc><entity></entity></doc>"

# ===== Attributes support

class AttrGatherer(ContentHandler):

    def startElement(self, name, attrs):
        self._attrs = attrs

    def startElementNS(self, name, qname, attrs):
        self._attrs = attrs

def test_expat_attrs_empty():
    parser = make_parser()
    gather = AttrGatherer()
    parser.setContentHandler(gather)

    parser.parse(StringIO("<doc/>"))

    return verify_empty_attrs(gather._attrs)

def test_expat_attrs_wattr():
    parser = make_parser()
    gather = AttrGatherer()
    parser.setContentHandler(gather)

    parser.parse(StringIO("<doc attr='val'/>"))

    return verify_attrs_wattr(gather._attrs)

def test_expat_nsattrs_empty():
    parser = make_parser()
    parser.setFeature(handler.feature_namespaces, 1)
    gather = AttrGatherer()
    parser.setContentHandler(gather)

    parser.parse(StringIO("<doc/>"))

    return verify_empty_nsattrs(gather._attrs)

def test_expat_nsattrs_wattr():
    parser = make_parser()
    parser.setFeature(handler.feature_namespaces, 1)
    gather = AttrGatherer()
    parser.setContentHandler(gather)

    parser.parse(StringIO("<doc xmlns:ns='%s' ns:attr='val'/>" % ns_uri))

    attrs = gather._attrs

    return attrs.getLength() == 1 and \
           attrs.getNames() == [(ns_uri, "attr")] and \
           attrs.getQNames() == ["ns:attr"] and \
           len(attrs) == 1 and \
           attrs.has_key((ns_uri, "attr")) and \
           attrs.keys() == [(ns_uri, "attr")] and \
           attrs.get((ns_uri, "attr")) == "val" and \
           attrs.get((ns_uri, "attr"), 25) == "val" and \
           attrs.items() == [((ns_uri, "attr"), "val")] and \
           attrs.values() == ["val"] and \
           attrs.getValue((ns_uri, "attr")) == "val" and \
           attrs[(ns_uri, "attr")] == "val"

# ===== InputSource support

xml_test_out = open(findfile("test.xml.out")).read()

def test_expat_inpsource_filename():
    parser = make_parser()
    result = StringIO()
    xmlgen = XMLGenerator(result)

    parser.setContentHandler(xmlgen)
    parser.parse(findfile("test.xml"))

    return result.getvalue() == xml_test_out

def test_expat_inpsource_sysid():
    parser = make_parser()
    result = StringIO()
    xmlgen = XMLGenerator(result)

    parser.setContentHandler(xmlgen)
    parser.parse(InputSource(findfile("test.xml")))

    return result.getvalue() == xml_test_out

def test_expat_inpsource_stream():
    parser = make_parser()
    result = StringIO()
    xmlgen = XMLGenerator(result)

    parser.setContentHandler(xmlgen)
    inpsrc = InputSource()
    inpsrc.setByteStream(open(findfile("test.xml")))
    parser.parse(inpsrc)

    return result.getvalue() == xml_test_out

# ===== Locator support

class LocatorTest(XMLGenerator):
    def __init__(self, out=None, encoding="iso-8859-1"):
        XMLGenerator.__init__(self, out, encoding)
        self.location = None

    def setDocumentLocator(self, locator):
        XMLGenerator.setDocumentLocator(self, locator)
        self.location = Location(self._locator)

def test_expat_locator_noinfo():
    result = StringIO()
    xmlgen = LocatorTest(result)
    parser = make_parser()
    parser.setContentHandler(xmlgen)

    parser.parse(StringIO("<doc></doc>"))

    return xmlgen.location.getSystemId() is None and \
           xmlgen.location.getPublicId() is None and \
           xmlgen.location.getLineNumber() == 1

def test_expat_locator_withinfo():
    result = StringIO()
    xmlgen = LocatorTest(result)
    parser = make_parser()
    parser.setContentHandler(xmlgen)
    testfile = findfile("test.xml")
    parser.parse(testfile)
    if is_jython:
        # In Jython, the system id is a URL with forward slashes, and
        # under Windows findfile returns a path with backslashes, so
        # replace the backslashes with forward
        testfile = testfile.replace('\\', '/')

    # urllib.quote isn't the exact encoder (e.g. ':' isn't escaped)
    expected = urllib.quote(testfile).replace('%3A', ':')
    return xmlgen.location.getSystemId().endswith(expected) and \
           xmlgen.location.getPublicId() is None


# ===========================================================================
#
#   error reporting
#
# ===========================================================================

def test_expat_incomplete():
    parser = make_parser()
    parser.setContentHandler(ContentHandler()) # do nothing
    try:
        parser.parse(StringIO("<foo>"))
    except SAXParseException:
        return 1 # ok, error found
    else:
        return 0

def test_sax_location_str():
    # pass various values from a locator to the SAXParseException to
    # make sure that the __str__() doesn't fall apart when None is
    # passed instead of an integer line and column number
    #
    # use "normal" values for the locator:
    str(Location(DummyLocator(1, 1)))
    # use None for the line number:
    str(Location(DummyLocator(None, 1)))
    # use None for the column number:
    str(Location(DummyLocator(1, None)))
    # use None for both:
    str(Location(DummyLocator(None, None)))
    return 1

def test_sax_parse_exception_str():
    # pass various values from a locator to the SAXParseException to
    # make sure that the __str__() doesn't fall apart when None is
    # passed instead of an integer line and column number
    #
    # use "normal" values for the locator:
    str(SAXParseException("message", None,
                          DummyLocator(1, 1)))
    # use None for the line number:
    str(SAXParseException("message", None,
                          DummyLocator(None, 1)))
    # use None for the column number:
    str(SAXParseException("message", None,
                          DummyLocator(1, None)))
    # use None for both:
    str(SAXParseException("message", None,
                          DummyLocator(None, None)))
    return 1

class DummyLocator:
    def __init__(self, lineno, colno):
        self._lineno = lineno
        self._colno = colno

    def getPublicId(self):
        return "pubid"

    def getSystemId(self):
        return "sysid"

    def getLineNumber(self):
        return self._lineno

    def getColumnNumber(self):
        return self._colno

# ===========================================================================
#
#   xmlreader tests
#
# ===========================================================================

# ===== AttributesImpl

def verify_empty_attrs(attrs):
    try:
        attrs.getValue("attr")
        gvk = 0
    except KeyError:
        gvk = 1

    try:
        attrs.getValueByQName("attr")
        gvqk = 0
    except KeyError:
        gvqk = 1

    try:
        attrs.getNameByQName("attr")
        gnqk = 0
    except KeyError:
        gnqk = 1

    try:
        attrs.getQNameByName("attr")
        gqnk = 0
    except KeyError:
        gqnk = 1

    try:
        attrs["attr"]
        gik = 0
    except KeyError:
        gik = 1

    return attrs.getLength() == 0 and \
           attrs.getNames() == [] and \
           attrs.getQNames() == [] and \
           len(attrs) == 0 and \
           not attrs.has_key("attr") and \
           attrs.keys() == [] and \
           attrs.get("attrs") is None and \
           attrs.get("attrs", 25) == 25 and \
           attrs.items() == [] and \
           attrs.values() == [] and \
           gvk and gvqk and gnqk and gik and gqnk

def verify_attrs_wattr(attrs):
    return attrs.getLength() == 1 and \
           attrs.getNames() == ["attr"] and \
           attrs.getQNames() == ["attr"] and \
           len(attrs) == 1 and \
           attrs.has_key("attr") and \
           attrs.keys() == ["attr"] and \
           attrs.get("attr") == "val" and \
           attrs.get("attr", 25) == "val" and \
           attrs.items() == [("attr", "val")] and \
           attrs.values() == ["val"] and \
           attrs.getValue("attr") == "val" and \
           attrs.getValueByQName("attr") == "val" and \
           attrs.getNameByQName("attr") == "attr" and \
           attrs["attr"] == "val" and \
           attrs.getQNameByName("attr") == "attr"

def test_attrs_empty():
    return verify_empty_attrs(AttributesImpl({}))

def test_attrs_wattr():
    return verify_attrs_wattr(AttributesImpl({"attr" : "val"}))

# ===== AttributesImpl

def verify_empty_nsattrs(attrs):
    try:
        attrs.getValue((ns_uri, "attr"))
        gvk = 0
    except KeyError:
        gvk = 1

    try:
        attrs.getValueByQName("ns:attr")
        gvqk = 0
    except KeyError:
        gvqk = 1

    try:
        attrs.getNameByQName("ns:attr")
        gnqk = 0
    except KeyError:
        gnqk = 1

    try:
        attrs.getQNameByName((ns_uri, "attr"))
        gqnk = 0
    except KeyError:
        gqnk = 1

    try:
        attrs[(ns_uri, "attr")]
        gik = 0
    except KeyError:
        gik = 1

    return attrs.getLength() == 0 and \
           attrs.getNames() == [] and \
           attrs.getQNames() == [] and \
           len(attrs) == 0 and \
           not attrs.has_key((ns_uri, "attr")) and \
           attrs.keys() == [] and \
           attrs.get((ns_uri, "attr")) is None and \
           attrs.get((ns_uri, "attr"), 25) == 25 and \
           attrs.items() == [] and \
           attrs.values() == [] and \
           gvk and gvqk and gnqk and gik and gqnk

def test_nsattrs_empty():
    return verify_empty_nsattrs(AttributesNSImpl({}, {}))

def test_nsattrs_wattr():
    attrs = AttributesNSImpl({(ns_uri, "attr") : "val"},
                             {(ns_uri, "attr") : "ns:attr"})

    return attrs.getLength() == 1 and \
           attrs.getNames() == [(ns_uri, "attr")] and \
           attrs.getQNames() == ["ns:attr"] and \
           len(attrs) == 1 and \
           attrs.has_key((ns_uri, "attr")) and \
           attrs.keys() == [(ns_uri, "attr")] and \
           attrs.get((ns_uri, "attr")) == "val" and \
           attrs.get((ns_uri, "attr"), 25) == "val" and \
           attrs.items() == [((ns_uri, "attr"), "val")] and \
           attrs.values() == ["val"] and \
           attrs.getValue((ns_uri, "attr")) == "val" and \
           attrs.getValueByQName("ns:attr") == "val" and \
           attrs.getNameByQName("ns:attr") == (ns_uri, "attr") and \
           attrs[(ns_uri, "attr")] == "val" and \
           attrs.getQNameByName((ns_uri, "attr")) == "ns:attr"


# ===== Main program

def make_test_output():
    parser = make_parser()
    result = StringIO()
    xmlgen = XMLGenerator(result)

    parser.setContentHandler(xmlgen)
    parser.parse(findfile("test.xml"))

    outf = open(findfile("test.xml.out"), "w")
    outf.write(result.getvalue())
    outf.close()

import sys
java_14 = sys.platform.startswith("java1.4")
del sys

items = locals().items()
items.sort()
for (name, value) in items:
    if name.startswith('test_expat') and java_14:
        #skip expat tests on java14 since the crimson parser is so crappy
        continue
    if name[:5] == "test_":
        confirm(value(), name)

if verbose:
    print "%d tests, %d failures" % (tests, len(failures))
if failures:
    raise TestFailed("%d of %d tests failed: %s"
                     % (len(failures), tests, ", ".join(failures)))
