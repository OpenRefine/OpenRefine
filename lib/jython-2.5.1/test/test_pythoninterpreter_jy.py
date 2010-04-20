# -*- coding: utf-8 -*-
import java.io.StringWriter
import sys
import traceback
import unittest
import test.test_support


def exec_code_in_pi(function, out, err, locals=None):
    """Runs code in a separate context: (thread, PySystemState, PythonInterpreter)"""

    def function_context():
        from org.python.core import Py
        from org.python.util import PythonInterpreter
        from org.python.core import PySystemState

        ps = PySystemState()
        pi = PythonInterpreter({}, ps)
        if locals:
            pi.setLocals(locals)
        pi.setOut(out)
        pi.setErr(err)
        try:
            pi.exec(function.func_code)
        except:
            print '-'*60
            traceback.print_exc(file=sys.stdout)
            print '-'*60


    import threading
    context = threading.Thread(target=function_context)
    context.start()
    context.join()


class InterpreterTest(unittest.TestCase):

    # in these tests, note the promotion to unicode by java.io.Writer,
    # because these are character-oriented streams. caveat emptor!

    def test_pi_out_unicode(self):
        source_text = [
            u'Some text',
            'Plain text',
            u'\u1000\u2000\u3000\u4000',
            # Some language names from wikipedia
            u'Català · Česky · Dansk · Deutsch · English · Español · Esperanto · Français · Bahasa Indonesia · Italiano · Magyar · Nederlands · 日本語 · Norsk (bokmål) · Polski · Português · Русский · Română · Slovenčina · Suomi · Svenska · Türkçe · Українська · Volapük · 中文',
            ]

        def f():
            global text
            for x in text:
                print x
        out = java.io.StringWriter()
        err = java.io.StringWriter()
        exec_code_in_pi(f, out, err, {'text': source_text})
        output_text = out.toString().splitlines()
        for source, output in zip(source_text, output_text):
            self.assertEquals(source, output)

    def test_pi_out(self):
        def f():
            print 42
        out = java.io.StringWriter()
        err = java.io.StringWriter()
        exec_code_in_pi(f, out, err)
        self.assertEquals(u"42\n", out.toString())

    def test_more_output(self):
        def f():
            for i in xrange(42):
                print "*" * i
        out = java.io.StringWriter()
        err = java.io.StringWriter()
        exec_code_in_pi(f, out, err)
        output = out.toString().splitlines()
        for i, line in enumerate(output):
            self.assertEquals(line, u'*' * i)
        self.assertEquals(42, len(output))



def test_main():
    test.test_support.run_unittest(InterpreterTest)

if __name__ == "__main__":
    test_main()
