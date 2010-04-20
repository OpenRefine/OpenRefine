import sys
import StringIO
import unittest

from xml.sax import saxutils
from xml.sax import make_parser
from xml.sax.handler import feature_namespaces

from test import test_support

file = StringIO.StringIO("""<collection>
  <comic title="Sandman" number='62'>
    <writer>Neil Gaiman</writer>
    <penciller pages='1-9,18-24'>Glyn Dillon</penciller>
    <penciller pages="10-17">Charles Vess</penciller>
  </comic>
  <comic title="Shade, the Changing Man" number="7">
    <writer>Peter Milligan</writer>
    <penciller>Chris Bachalo</penciller>
  </comic>
</collection>""")

class FindIssue(saxutils.DefaultHandler):
    def __init__(self, title, number):
        self.search_title, self.search_number = title, number
        self.match = 0

    def startElement(self,name,attrs):
        global match
        if name != 'comic' : return

        title = attrs.get('title', None)
        number = attrs.get('number',None)
        if title == self.search_title and number == self.search_number:
            self.match += 1

class SimpleSaxTest(unittest.TestCase):
    def test_find_issue(self):
        parser = make_parser()
        parser.setFeature(feature_namespaces,0)
        dh = FindIssue('Sandman', '62')
        parser.setContentHandler(dh)
        parser.parse(file)
        self.assertEquals(1, dh.match)
def test_main():
    test_support.run_unittest(SimpleSaxTest)

if __name__ == "__main__":
    test_main()
