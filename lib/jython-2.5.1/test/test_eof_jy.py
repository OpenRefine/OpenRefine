import unittest
from test import test_support

class TestEof(unittest.TestCase):
    """
    Oddities originally found in Django involving whitespace and newlines or
    lack thereof at the end of files.  I can't use __builtin__.compile()
    because newlines get added and hide these problems, so I have opted to
    import Python files containing these oddities.
    """

    def test_indented_no_newline(self):
        try:
            import eof_fodder1
        except ImportError, cause:
            self.fail(cause)

    def test_trailing_ws_no_newline(self):
        try:
            import eof_fodder2
        except ImportError, cause:
            self.fail(cause)

    def test_trailing_ws(self):
        try:
            import eof_fodder3
        except ImportError, cause:
            self.fail(cause)

    def test_empty(self):
        try:
            import eof_fodder4
        except ImportError, cause:
            self.fail(cause)

    def test_just_a_comment_no_newline(self):
        try:
            import eof_fodder5
        except ImportError, cause:
            self.fail(cause)

    def test_junky_ws_after_indent(self):
        try:
            import eof_fodder6
        except ImportError, cause:
            self.fail(cause)

    def test_trailing_paren(self):
        try:
            import badsyntax_eof1
        except SyntaxError, cause:
            self.assertEquals(cause.lineno, 5)

#==============================================================================

def test_main(verbose=None):
    test_classes = [TestEof]
    test_support.run_unittest(*test_classes)

if __name__ == "__main__":
    test_main(verbose=True)
