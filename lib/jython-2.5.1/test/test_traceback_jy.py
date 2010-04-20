"""Misc traceback tests

Made for Jython.
"""
import sys
import traceback
import unittest
from test import test_support

if test_support.is_jython:
    from java.awt import EventQueue
    from java.lang import Runnable

class TracebackTestCase(unittest.TestCase):

    def test_tb_across_threads(self):
        if not test_support.is_jython:
            return

        # http://bugs.jython.org/issue1533624
        class PyRunnable(Runnable):
            def run(self):
                raise TypeError('this is only a test')
        try:
            EventQueue.invokeAndWait(PyRunnable())
        except TypeError:
            self.assertEqual(tb_info(),
                             [('test_tb_across_threads',
                               'EventQueue.invokeAndWait(PyRunnable())'),
                              ('run',
                               "raise TypeError('this is only a test')")])
        else:
            self.fail('Expected TypeError')

    def test_reraise(self):
        def raiser():
            raise Exception(), None, tb
        try:
            # Jython previously added raiser's frame to the traceback
            raiser()
        except Exception:
            self.assertEqual(tb_info(),
                             [('test_reraise', 'raiser()'),
                              ('<module>', "raise Exception('foo')")])

        else:
            self.fail('Expected Exception')

    def test_extract_stack(self):
        # http://bugs.jython.org/issue437809
        traceback.extract_stack()

    def test_except_around_raising_call(self):
        """[ #452526 ] traceback lineno is the except line"""
        from test import except_in_raising_code
        try:
            except_in_raising_code.foo()
        except NameError:
            tb = sys.exc_info()[2]
            self.assertEquals(6, tb.tb_next.tb_lineno)
        else:
            self.fail("Should've raised a NameError")

try:
    raise Exception('foo')
except Exception:
    tb = sys.exc_info()[2]


def tb_info():
    # [2:] ignores filename/lineno
    return [info[2:] for info in traceback.extract_tb(sys.exc_info()[2])]


def test_main():
    test_support.run_unittest(TracebackTestCase)


if __name__ == '__main__':
    test_main()
