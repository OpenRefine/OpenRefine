# encoding: utf-8

import sys
JYTHON = sys.platform.startswith("java")

import doctest

import xml.parsers.expat as expat
from xml.etree.ElementTree import *

def jython(function):
    if JYTHON:
        return function
    else:
        return None

class sortdict(dict):
    def __repr__(self):
        items = self.items()
        items.sort()
        pairs = ["%r: %r" % pair for pair in items]
        return "{%s}" % ", ".join(pairs)
    __str__ = __repr__


class Outputter:
    def StartElementHandler(self, name, attrs):
        print 'Start element:\n   ', repr(name), sortdict(attrs)

    def EndElementHandler(self, name):
        print 'End element:\n   ', repr(name)

    def CharacterDataHandler(self, data):
        data = data.strip()
        if data:
            print 'Character data:'
            print '   ', repr(data)

    def ProcessingInstructionHandler(self, target, data):
        print 'PI:\n   ', repr(target), repr(data)

    def StartNamespaceDeclHandler(self, prefix, uri):
        print 'NS decl:\n   ', repr(prefix), repr(uri)

    def EndNamespaceDeclHandler(self, prefix):
        print 'End of NS decl:\n   ', repr(prefix)

    def StartCdataSectionHandler(self):
        print 'Start of CDATA section'

    def EndCdataSectionHandler(self):
        print 'End of CDATA section'

    def CommentHandler(self, text):
        print 'Comment:\n   ', repr(text)

    def NotationDeclHandler(self, *args):
        name, base, sysid, pubid = args
        print 'Notation declared:', args

    def UnparsedEntityDeclHandler(self, *args):
        entityName, base, systemId, publicId, notationName = args
        print 'Unparsed entity decl:\n   ', args

    def NotStandaloneHandler(self, userData):
        print 'Not standalone'
        return 1

    def ExternalEntityRefHandler(self, *args):
        context, base, sysId, pubId = args
        print 'External entity ref:', args[1:]
        return 1

    def DefaultHandler(self, userData):
        pass

    def DefaultHandlerExpand(self, userData):
        pass

_=  """
    >>> data = '''\
    ... <?xml version="1.0" encoding="iso-8859-1" standalone="no"?>
    ... <?xml-stylesheet href="stylesheet.css"?>
    ... <!-- comment data -->
    ... <!DOCTYPE quotations SYSTEM "quotations.dtd" [
    ... <!ELEMENT root ANY>
    ... <!NOTATION notation SYSTEM "notation.jpeg">
    ... <!ENTITY acirc "&#226;">
    ... <!ENTITY external_entity SYSTEM "entity.file">
    ... <!ENTITY unparsed_entity SYSTEM "entity.file" NDATA notation>
    ... %unparsed_entity;
    ... ]>
    ...
    ... <root attr1="value1" attr2="value2&#8000;">
    ... <myns:subelement xmlns:myns="http://www.python.org/namespace">
    ...      Contents of subelements
    ... </myns:subelement>
    ... <sub2><![CDATA[contents of CDATA section]]></sub2>
    ... &external_entity;
    ... </root>
    ... '''
    """

def test_utf8():
    """
    Source: test_pyexpat.py
    Changes: replaced tabs with spaces in Outputter to ease doctest integration

    >>> out = Outputter()
    >>> parser = expat.ParserCreate(namespace_separator='!')
    >>> HANDLER_NAMES = [
    ...     'StartElementHandler', 'EndElementHandler',
    ...     'CharacterDataHandler',
    ...     'ProcessingInstructionHandler',
    ...     'UnparsedEntityDeclHandler', 'NotationDeclHandler',
    ...     'StartNamespaceDeclHandler', 'EndNamespaceDeclHandler',
    ...     'CommentHandler', 'StartCdataSectionHandler',
    ...     'EndCdataSectionHandler',
    ...     'DefaultHandler', 'DefaultHandlerExpand',
    ...     #'NotStandaloneHandler',
    ...     'ExternalEntityRefHandler'
    ...     ]
    >>> for name in HANDLER_NAMES:
    ...     setattr(parser, name, getattr(out, name))

    >>> data = '''\\
    ... <?xml version="1.0" encoding="iso-8859-1" standalone="no"?>
    ... <?xml-stylesheet href="stylesheet.css"?>
    ... <!-- comment data -->
    ... <!DOCTYPE quotations SYSTEM "quotations.dtd" [
    ... <!ELEMENT root ANY>
    ... <!NOTATION notation SYSTEM "notation.jpeg">
    ... <!ENTITY acirc "&#226;">
    ... <!ENTITY external_entity SYSTEM "entity.file">
    ... <!ENTITY unparsed_entity SYSTEM "entity.file" NDATA notation>
    ... %unparsed_entity;
    ... ]>
    ...
    ... <root attr1="value1" attr2="value2&#8000;">
    ... <myns:subelement xmlns:myns="http://www.python.org/namespace">
    ...      Contents of subelements
    ... </myns:subelement>
    ... <sub2><![CDATA[contents of CDATA section]]></sub2>
    ... &external_entity;
    ... </root>
    ... '''

    #Produce UTF-8 output
    #>>> parser.returns_unicode = 0
    #>>> try:
    #...     parser.Parse(data, 1)
    #... except expat.error:
    #...     print '** Error', parser.ErrorCode, expat.ErrorString(parser.ErrorCode)
    #...     print '** Line', parser.ErrorLineNumber
    #...     print '** Column', parser.ErrorColumnNumber
    #...     print '** Byte', parser.ErrorByteIndex
    #PI:
        #'xml-stylesheet' 'href="stylesheet.css"'
    #Comment:
        #' comment data '
    #Notation declared: ('notation', None, 'notation.jpeg', None)
    #Unparsed entity decl:
        #('unparsed_entity', None, 'entity.file', None, 'notation')
    #Start element:
        #'root' {'attr1': 'value1', 'attr2': 'value2\\xe1\\xbd\\x80'}
    #NS decl:
        #'myns' 'http://www.python.org/namespace'
    #Start element:
        #'http://www.python.org/namespace!subelement' {}
    #Character data:
        #'Contents of subelements'
    #End element:
        #'http://www.python.org/namespace!subelement'
    #End of NS decl:
        #'myns'
    #Start element:
        #'sub2' {}
    #Start of CDATA section
    #Character data:
        #'contents of CDATA section'
    #End of CDATA section
    #End element:
        #'sub2'
    #External entity ref: (None, 'entity.file', None)
    #End element:
        #'root'
    #1

    >>> parser = expat.ParserCreate(namespace_separator='!')
    >>> parser.returns_unicode = 1
    >>> for name in HANDLER_NAMES:
    ...     setattr(parser, name, getattr(out, name))
    >>> try:
    ...     parser.Parse(data, 1)
    ... except expat.error:
    ...     print '** Line', parser.ErrorLineNumber
    ...     print '** Column', parser.ErrorColumnNumber
    ...     print '** Byte', parser.ErrorByteIndex #doctest: +REPORT_UDIFF
    PI:
        u'xml-stylesheet' u'href="stylesheet.css"'
    Comment:
        u' comment data '
    Notation declared: (u'notation', None, u'notation.jpeg', None)
    Unparsed entity decl:
        (u'unparsed_entity', None, u'entity.file', None, u'notation')
    Start element:
        u'root' {u'attr1': u'value1', u'attr2': u'value2\u1f40'}
    NS decl:
        u'myns' u'http://www.python.org/namespace'
    Start element:
        u'http://www.python.org/namespace!subelement' {}
    Character data:
        u'Contents of subelements'
    End element:
        u'http://www.python.org/namespace!subelement'
    End of NS decl:
        u'myns'
    Start element:
        u'sub2' {}
    Start of CDATA section
    Character data:
        u'contents of CDATA section'
    End of CDATA section
    End element:
        u'sub2'
    External entity ref: (None, u'entity.file', None)
    End element:
        u'root'
    1
    """


def test_import_as_pyexpat():
    """
    >>> import pyexpat as expat
    >>> expat #doctest: +ELLIPSIS
    <module 'pyexpat' from ...>
    """


def test_errors_submodule():
    """
    >>> import xml.parsers.expat as expat
    >>> expat.errors
    <module 'pyexpat.errors' (built-in)>
    >>> dir(expat.errors) #doctest: +ELLIPSIS
    ['XML_ERROR_ABORTED', ..., 'XML_ERROR_XML_DECL', '__doc__', '__name__']
    >>> expat.errors.XML_ERROR_ABORTED
    'parsing aborted'
    >>> expat.errors.XML_ERROR_XML_DECL
    'XML declaration not well-formed'
    """

def test_model_submodule():
    """
    >>> import xml.parsers.expat as expat
    >>> expat.model
    <module 'pyexpat.model' (built-in)>
    >>> print sortdict(expat.model.__dict__)
    {'XML_CQUANT_NONE': 0, 'XML_CQUANT_OPT': 1, 'XML_CQUANT_PLUS': 3, 'XML_CQUANT_REP': 2, 'XML_CTYPE_ANY': 2, 'XML_CTYPE_CHOICE': 5, 'XML_CTYPE_EMPTY': 1, 'XML_CTYPE_MIXED': 3, 'XML_CTYPE_NAME': 4, 'XML_CTYPE_SEQ': 6, '__doc__': 'Constants used to interpret content model information.', '__name__': 'pyexpat.model'}
    """

def test_parse_only_xml_data():
    """
    Source: test_pyexpat.py, see also: http://python.org/sf/1296433
    Changes:
      - replaced 'iso8859' encoding with 'ISO-8859-1',
      - added isfinal=True keyword argument to Parse call (as in this port,
        the data is not processed until it is fully available).
    With these changes, the test still crashes CPython 2.5.

    >>> import xml.parsers.expat as expat
    >>> # xml = "<?xml version='1.0' encoding='iso8859'?><s>%s</s>" % ('a' * 1025)

    This one doesn't crash:
    >>> xml = "<?xml version='1.0'?><s>%s</s>" % ('a' * 10000)

    >>> def handler(text):
    ...     raise Exception
    >>> parser = expat.ParserCreate()
    >>> parser.CharacterDataHandler = handler
    >>> try:
    ...     parser.Parse(xml, True)
    ... except:
    ...     pass
    """


def test_namespace_separator():
    """
    Source: test_pyexpat.py

    Tests that make sure we get errors when the namespace_separator value
    is illegal, and that we don't for good values:

    >>> from xml.parsers.expat import ParserCreate

    >>> p = ParserCreate()
    >>> p = ParserCreate(namespace_separator=None)
    >>> p = ParserCreate(namespace_separator=' ')
    >>> p = ParserCreate(namespace_separator=42) #doctest: +ELLIPSIS
    Traceback (most recent call last):
    ...
    TypeError: ...
    >>> p = ParserCreate(namespace_separator='too long') #doctest: +ELLIPSIS
    Traceback (most recent call last):
    ...
    ValueError: ...

    ParserCreate() needs to accept a namespace_separator of zero length
    to satisfy the requirements of RDF applications that are required
    to simply glue together the namespace URI and the localname.  Though
    considered a wart of the RDF specifications, it needs to be supported.

    See XML-SIG mailing list thread starting with
    http://mail.python.org/pipermail/xml-sig/2001-April/005202.html

    >>> p = ParserCreate(namespace_separator='') # too short
"""


def test_interning_machinery():
    """
    Source: test_pyexpat.py

    >>> from xml.parsers.expat import ParserCreate

    >>> p = ParserCreate()
    >>> L = []
    >>> def collector(name, *args):
    ...     L.append(name)
    >>> p.StartElementHandler = collector
    >>> p.EndElementHandler = collector
    >>> p.Parse("<e> <e/> <e></e> </e>", 1)
    1
    >>> tag = L[0]
    >>> len(L)
    6
    >>> all(tag is entry for entry in L)
    True
    """


def test_exception_from_callback():
    """
    Source: test_pyexpat.py

    >>> from xml.parsers.expat import ParserCreate

    >>> def StartElementHandler(name, attrs):
    ...     raise RuntimeError(name)

    >>> parser = ParserCreate()
    >>> parser.StartElementHandler = StartElementHandler
    >>> try:
    ...     parser.Parse("<a><b><c/></b></a>", 1)
    ... except RuntimeError, e:
    ...     pass
    >>> e.args[0] == "a"
    True
    """


def test_with_and_without_namespace():
    """
    >>> from xml.parsers.expat import ParserCreate

    >>> xml = '''<root
    ...            xmlns="http://www.python.org"
    ...            xmlns:python="http://www.python.org"
    ...            python:a="1" b="2">
    ...            <python:sub1/>
    ...            <sub2 xmlns=""/>
    ...          </root>'''
    >>> def handler(name, attributes):
    ...     attributes = sorted(attributes.items())
    ...     print name
    ...     for attr in attributes:
    ...         print "  %s = %r" % attr

    >>> parser = ParserCreate()
    >>> parser.StartElementHandler = handler
    >>> _ = parser.Parse(xml, True)
    root
      b = u'2'
      python:a = u'1'
      xmlns = u'http://www.python.org'
      xmlns:python = u'http://www.python.org'
    python:sub1
    sub2
      xmlns = u''

    >>> parser = ParserCreate(namespace_separator="|")
    >>> parser.StartElementHandler = handler
    >>> _ = parser.Parse(xml, True)
    http://www.python.org|root
      b = u'2'
      http://www.python.org|a = u'1'
    http://www.python.org|sub1
    sub2
    """

def test_unicode_bug():
    """
    Regression introduced by revision 28

    >>> doc = XML("<doc>&#x8230;</doc>")
    >>> doc.text
    u'\u8230'
    """

def test_DTD():
    """
    >>> xml = '''<!DOCTYPE doc [
    ...          <!ELEMENT doc (any|empty|text|mixed|opt|many|plus)>
    ...          <!ELEMENT any ANY>
    ...          <!ELEMENT empty EMPTY>
    ...          <!ELEMENT text (#PCDATA)>
    ...          <!ELEMENT sequence (_sequence)>
    ...          <!ELEMENT _sequence (any,any)>
    ...          <!ELEMENT mixed (#PCDATA|any)*>
    ...          <!ELEMENT opt (empty)?>
    ...          <!ELEMENT many (empty)*>
    ...          <!ELEMENT plus (empty)+>
    ...          ]>
    ...          <doc><text>content</text></doc>
    ...       '''
    >>> parser = expat.ParserCreate()
    >>> def handler(header, *args):
    ...     def _handler(*args):
    ...         print header + ":", args
    ...     return _handler
    >>> parser.ElementDeclHandler = handler("ELEMENT")
    >>> parser.AttlistDeclHandler = handler("ATTRIBUTE")
    >>> parser.EntityDeclHandler = handler("ENTITY")
    >>> parser.NotationDeclHandler = handler("NOTATION")
    >>> parser.UnparsedEntityDeclHandler = handler("UNPARSED")
    >>> parser.Parse(xml, True)
    ELEMENT: (u'doc', (5, 0, None, ((4, 0, u'any', ()), (4, 0, u'empty', ()), (4, 0, u'text', ()), (4, 0, u'mixed', ()), (4, 0, u'opt', ()), (4, 0, u'many', ()), (4, 0, u'plus', ()))))
    ELEMENT: (u'any', (2, 0, None, ()))
    ELEMENT: (u'empty', (1, 0, None, ()))
    ELEMENT: (u'text', (3, 0, None, ()))
    ELEMENT: (u'sequence', (6, 0, None, ((4, 0, u'_sequence', ()),)))
    ELEMENT: (u'_sequence', (6, 0, None, ((4, 0, u'any', ()), (4, 0, u'any', ()))))
    ELEMENT: (u'mixed', (3, 2, None, ((4, 0, u'any', ()),)))
    ELEMENT: (u'opt', (6, 1, None, ((4, 0, u'empty', ()),)))
    ELEMENT: (u'many', (6, 2, None, ((4, 0, u'empty', ()),)))
    ELEMENT: (u'plus', (6, 3, None, ((4, 0, u'empty', ()),)))
    1
    """

def test_entity():
    """

    TODO: need a fallback for entity-resolver so that empty source is returned.

    >>> xml = ''' <!DOCTYPE doc SYSTEM "external.dtd" [
    ...           <!ENTITY ext-entity SYSTEM "external-entity">
    ...           ]>
    ...           <doc>&ext-entity;&in-ext-dtd-entity;</doc>'''
    >>> parser = expat.ParserCreate()
    >>> parser.Parse(xml, True)
    1

    EXPAT OH MY ! When applicable (internal entities), the CharacterDataHandler
    callback will override DefaultHandlerExpand, but it WON'T override
    DefaultHandler. On the other hand, the DefaultHandlerExpand callback WILL
    override DefaultHandler ... More tests todo here ...

    >>> xml = '''<!DOCTYPE doc SYSTEM "external.dtd" [
    ...          <!ENTITY ext-entity SYSTEM "external-entity">
    ...          <!ENTITY int-entity "internal">
    ...          ]>
    ...           <doc>&int-entity;&ext-entity;&in-ext-dtd-entity;</doc>'''
    >>> parser = expat.ParserCreate()
    >>> def handler(header):
    ...     def _handler(*args):
    ...         print header + ":", args
    ...         return 1
    ...     return _handler
    >>> parser.CharacterDataHandler = handler("text")
    >>> parser.DefaultHandler = handler("default")
    >>> parser.Parse(xml, True) #doctest: +ELLIPSIS
    default: ...
    default: (u'&int-entity;',)
    default: (u'&ext-entity;',)
    default: (u'&in-ext-dtd-entity;',)
    ...
    1

    EXPAT OH MY ! When applicable (internal entities), the CharacterDataHandler
    callback will override DefaultHandlerExpand, but it WON'T override
    DefaultHandler. On the other hand, the DefaultHandlerExpand callback WILL
    override DefaultHandler ... More tests todo here ...
    """

def test_resolve_entity_handlers():
    """
    >>> xml = '''<!DOCTYPE doc [
    ...          <!ENTITY entity SYSTEM "entity">
    ...          ]>
    ...          <doc>&entity;</doc>'''
    >>> def handler(header):
    ...     def _handler(*args):
    ...         print header + ":", args
    ...         return 1
    ...     return _handler

    >>> parser = expat.ParserCreate()
    >>> parser.ExternalEntityRefHandler = handler("ExternalEntityRefHandler")
    >>> parser.Parse(xml, True)
    ExternalEntityRefHandler: (u'entity', None, u'entity', None)
    1
    """

def handler(name, header="XML>", returns=None):
    def _handler(*args):
        if len(args) == 1:
            args = "(%r)" % args[0]
        else:
            args = str(args)
        print header, name + "%s" % args
        return returns
    return _handler

def parse(xml, *handlers):
    parser = expat.ParserCreate()
    for name in handlers:
        if name == "ExternalEntityRefHandler":
            returns = 1
        else:
            returns = None
        setattr(parser, name, handler(name, returns=returns))
    parser.Parse(xml, True)

def test_internal_entities():
    """
    >>> xml = '''<!DOCTYPE doc [
    ...          <!ENTITY entity "entity-content">
    ...          ]>
    ...          <doc>&entity;</doc>'''

    >>> parse(xml)

    >>> parse(xml, "CharacterDataHandler")
    XML> CharacterDataHandler(u'entity-content')

    >>> parse(xml, "DefaultHandler") #doctest: +ELLIPSIS
    XML> ...DefaultHandler(u'&entity;')...

    >>> parse(xml, "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...DefaultHandlerExpand(u'entity-content')...

    # Uhu ?
    >>> parse(xml, "CharacterDataHandler",
    ...            "DefaultHandler") #doctest: +ELLIPSIS
    XML> ...DefaultHandler(u'&entity;')...

    >>> parse(xml, "CharacterDataHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...CharacterDataHandler(u'entity-content')...

    >>> parse(xml, "DefaultHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...DefaultHandlerExpand(u'entity-content')...

    >>> parse(xml, "CharacterDataHandler",
    ...            "DefaultHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...CharacterDataHandler(u'entity-content')...
    """

def test_external_entities():
    """
    >>> xml = '''<!DOCTYPE doc [
    ...          <!ENTITY entity PUBLIC "http://entity-web" "entity-file">
    ...          ]>
    ...          <doc>&entity;</doc>'''

    >>> parse(xml)

    >>> parse(xml, "ExternalEntityRefHandler")
    XML> ExternalEntityRefHandler(u'entity', None, u'entity-file', u'http://entity-web')

    >>> parse(xml, "DefaultHandler") #doctest: +ELLIPSIS
    XML> ...DefaultHandler(u'&entity;')...

    >>> parse(xml, "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...DefaultHandlerExpand(u'&entity;')...

    >>> parse(xml, "ExternalEntityRefHandler",
    ...            "DefaultHandler") #doctest: +ELLIPSIS
    XML> ...ExternalEntityRefHandler(u'entity', None, u'entity-file', u'http://entity-web')...

    >>> parse(xml, "ExternalEntityRefHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...ExternalEntityRefHandler(u'entity', None, u'entity-file', u'http://entity-web')...

    >>> parse(xml, "DefaultHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...DefaultHandlerExpand(u'&entity;')...

    >>> parse(xml, "ExternalEntityRefHandler",
    ...            "DefaultHandler",
    ...            "DefaultHandlerExpand") #doctest: +ELLIPSIS
    XML> ...ExternalEntityRefHandler(u'entity', None, u'entity-file', u'http://entity-web')...
    """

def test_undefined_entities():
    """
    >>> xml = "<doc>&entity;</doc>"
    >>> parse(xml)
    Traceback (most recent call last):
    ...
    ExpatError: undefined entity: line 1, column 5
    """

def locate(parser, name):
    def _handler(*args):
        print name, parser.CurrentLineNumber, parser.CurrentColumnNumber
    return _handler

def test_current_location():
    """
    >>> xml = '''<doc>text<tag/>text<tag></tag>
    ... <tag></tag>
    ... text<tag/>
    ... </doc>'''
    >>> parser = expat.ParserCreate()
    >>> parser.CharacterDataHandler = locate(parser, "TEXT:")
    >>> parser.StartElementHandler = locate(parser, "START:")
    >>> parser.EndElementHandler = locate(parser, "END:")
    >>> _ = parser.Parse(xml, True) #doctest: +ELLIPSIS
    START: 1 0
    TEXT: 1 5...
    START: 1 9
    END: 1 15
    TEXT: 1 15...
    START: 1 19
    END: 1 24
    TEXT: 1 30...
    START: 2 0
    END: 2 5
    TEXT: 2 11...
    START: 3 4
    END: 3 10
    TEXT: 3 10...
    END: 4 0

    >>> xml = '''<doc>
    ... start tag after some text<tag/>
    ... <elt></elt><tag/>
    ... <elt/><tag/>
    ... </doc>'''
    >>> parser = expat.ParserCreate()
    >>> parser.CharacterDataHandler = locate(parser, "TEXT:")
    >>> parser.StartElementHandler = locate(parser, "START:")
    >>> parser.EndElementHandler = locate(parser, "END:")
    >>> _ = parser.Parse(xml, True) #doctest: +ELLIPSIS
    START: 1 0
    TEXT: 1 5...
    START: 2 25
    END: 2 31
    TEXT: 2 31...
    START: 3 0
    END: 3 5
    START: 3 11
    END: 3 17
    TEXT: 3 17...
    START: 4 0
    END: 4 6
    START: 4 6
    END: 4 12
    TEXT: 4 12...
    END: 5 0
    """


def test_error_location():
    """
    Source: selftest.py, ElementTree 1.3a3
    Changes: removed dependencies in ElementTree, added one extra test

    >>> def error(xml):
    ...     p = expat.ParserCreate()
    ...     try:
    ...         p.Parse(xml, True)
    ...     except expat.ExpatError, e:
    ...         return e.lineno, e.offset

    >>> error("foo")
    (1, 0)
    >>> error("<tag>&foo;</tag>")
    (1, 5)
    >>> error("foobar<")
    (1, 6)
    >>> error("<doc>text<doc")
    (1, 9)
    """

@jython
def test_resolveEntity():
    """
    # TODO: test that 'skipEntity' works.

    >>> # Jython
    >>> from org.python.core.util import StringUtil
    >>> from jarray import array

    >>> # Java Standard Edition
    >>> from org.xml.sax import *
    >>> from org.xml.sax.ext import *
    >>> from org.xml.sax.helpers import *
    >>> from java.io import ByteArrayInputStream

    >>> xml = '''<!DOCTYPE doc
    ... [<!ENTITY entity SYSTEM "entity-file">
    ... ]>
    ... <doc>&entity;</doc>
    ... '''

    >>> def empty_source():
    ...     _source = InputSource()
    ...     byte_stream = ByteArrayInputStream(array([], "b"))
    ...     _source.setByteStream(byte_stream)
    ...     return _source

    >>> class Handler(EntityResolver2):
    ...     def getExternalSubset(self, name, baseURI):
    ...         return None
    ...     def resolveEntity(self, name, publicId, baseURI, systemId):
    ...         print "Entity name:", name
    ...         return empty_source()

    >>> def main():
    ...     sax_parser = "org.apache.xerces.parsers.SAXParser"
    ...     reader = XMLReaderFactory.createXMLReader(sax_parser)
    ...     entity_resolver2 = "http://xml.org/sax/features/use-entity-resolver2"
    ...     enabled = reader.getFeature(entity_resolver2)
    ...     print "Entity-Resolver2 enabled:", enabled
    ...     handler = Handler()
    ...     reader.setEntityResolver(handler)
    ...     bytes = StringUtil.toBytes(xml)
    ...     byte_stream = ByteArrayInputStream(bytes)
    ...     source = InputSource(byte_stream)
    ...     reader.parse(source)

    >>> main()
    Entity-Resolver2 enabled: True
    Entity name: entity
    """

if __name__ == "__main__":
    doctest.testmod()
