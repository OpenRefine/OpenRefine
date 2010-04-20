"""Test handling of newlines via file's read and readline

Made for Jython.
"""
import os
import sys
import tempfile
import test.test_support as test_support
import unittest

assert not os.linesep == '\r', ('os.linesep of  %r is not supported' %
                                os.linesep)

LF = os.linesep == '\n'
CRLF = os.linesep == '\r\n'

CRLF_TEST = 'CR\rLF\nCRLF\r\nEOF'

if sys.platform.startswith('java'):
    from org.python.core.io import TextIOBase
    READAHEAD_SIZE = TextIOBase.CHUNK_SIZE
else:
    READAHEAD_SIZE = 300

class BaseTestCase(unittest.TestCase):

    data = CRLF_TEST
    write_mode = 'wb'
    mode = 'r'
    bufsize = -1

    def setUp(self):
        self.filename = tempfile.mktemp()
        self.write_fp = open(self.filename, self.write_mode, self.bufsize)
        self.write_fp.write(self.data)
        self.write_fp.flush()
        self.fp = open(self.filename, self.mode, self.bufsize)

    def tearDown(self):
        if self.write_fp:
            self.write_fp.close()
        if self.fp:
            self.fp.close()
        os.remove(self.filename)


class ReadBinaryNewlinesTestCase(BaseTestCase):

    mode = 'rb'

    def test_binary_read(self):
        read(self.fp, CRLF_TEST)

        self.fp.seek(0)
        read(self.fp, CRLF_TEST, len(CRLF_TEST))

        self.fp.seek(0)
        read(self.fp, 'CR\r', 3)
        read(self.fp, 'LF\n', 3)
        read(self.fp, 'CRLF\r\n', 6)
        read(self.fp, 'EOF', 3)

    def test_binary_readline(self):
        readline(self.fp, 'CR\rLF\n')
        readline(self.fp, 'CRLF\r\n')
        readline(self.fp, 'EOF')

        self.fp.seek(0)
        readline(self.fp, 'CR\rLF\n', 6)
        readline(self.fp, 'CRLF\r\n', 6)
        readline(self.fp, 'EOF', 3)


if LF:
    class ReadTextNewlinesTestCase(ReadBinaryNewlinesTestCase):

        mode = 'r'

else:
    class ReadTextNewlinesTestCase(BaseTestCase):

        def test_text_read(self):
            read(self.fp, 'CR\rLF\nCRLF\nEOF')

            self.fp.seek(0)
            read(self.fp, 'CR\rLF\nCRLF\nEOF', len('CR\rLF\nCRLF\nEOF'))

            self.fp.seek(0)
            read(self.fp, 'CR\r', 3)
            read(self.fp, 'LF\n', 3)
            read(self.fp, 'CRLF\n', 5)
            read(self.fp, 'EOF', 3)

        def test_text_readline(self):
            readline(self.fp, 'CR\rLF\n')
            readline(self.fp, 'CRLF\n')
            readline(self.fp, 'EOF')

            self.fp.seek(0)
            readline(self.fp, 'CR\rLF\n', 6)
            readline(self.fp, 'CRLF\n', 5)
            readline(self.fp, 'EOF', 3)


class ReadTextBasicBoundaryTestCase(BaseTestCase):

    data = 'CR\r'
    read_data = 'CR\r'

    def test_read_basic_boundary(self):
        read(self.fp, self.read_data)
        self.fp.seek(0)
        read(self.fp, self.read_data, 3)

    def test_readline_basic_boundary(self):
        readline(self.fp, self.read_data)
        self.fp.seek(0)
        readline(self.fp, self.read_data, 3)


class ReadUniversalBasicBoundaryTestCase(ReadTextBasicBoundaryTestCase):

    mode = 'U'
    read_data = 'CR\n'


class BinaryReadaheadBoundaryTestCase(BaseTestCase):

    mode = 'rb'
    data = 'foo\n' + ('x' * READAHEAD_SIZE)

    def test_read_boundary(self):
        readline(self.fp, 'foo\n')
        read(self.fp, 'x' * READAHEAD_SIZE, READAHEAD_SIZE)

    def test_readline_boundary(self):
        readline(self.fp, 'foo\n')
        readline(self.fp, 'x' * READAHEAD_SIZE, READAHEAD_SIZE)


class TextReadaheadBoundaryTestCase(BinaryReadaheadBoundaryTestCase):

    mode = 'r'


class UniversalReadaheadBoundaryTestCase(BinaryReadaheadBoundaryTestCase):

    mode = 'U'


class TextReadaheadBoundary2TestCase(BaseTestCase):
    """For CRLF platforms only"""

    data = ('x' * (READAHEAD_SIZE + 1)) + '\r\n'

    def test_read_boundary2(self):
        read(self.fp, 'x' * (READAHEAD_SIZE + 1), READAHEAD_SIZE + 1)
        read(self.fp, '\n')


class UniversalReadaheadBoundary2TestCase(TextReadaheadBoundary2TestCase):

    mode = 'U'


class TextReadaheadBoundary3TestCase(BaseTestCase):
    """For CRLF platforms only"""

    mode = 'r'
    data = ('x' * (READAHEAD_SIZE - 1)) + '\r\n'

    def test_read_boundary3(self):
        size = READAHEAD_SIZE - 1
        read(self.fp, 'x' * size, size)
        read(self.fp, '\n')
        self.fp.seek(0)
        read(self.fp, ('x' * size) + '\n', READAHEAD_SIZE)

    def test_readline_boundary3(self):
        size = READAHEAD_SIZE - 1
        readline(self.fp, 'x' * size, size)
        readline(self.fp, '\n')

    def test_read_boundary3_with_extra(self):
        self.write_fp.seek(0, 2)
        self.write_fp.write('foo')
        self.write_fp.flush()
        self.write_fp.seek(0)

        size = READAHEAD_SIZE - 1
        read(self.fp, ('x' * size) + '\nfoo', READAHEAD_SIZE + 3)


class UniversalReadaheadBoundary3TestCase(TextReadaheadBoundary3TestCase):

    mode = 'U'


class TextReadaheadBoundary4TestCase(BaseTestCase):
    """For CRLF platforms only"""

    mode = 'r'
    data = ('x' * (READAHEAD_SIZE - 2)) + '\n\r'

    def test_read_boundary4(self):
        readline(self.fp, 'x' * (READAHEAD_SIZE - 2) + '\n')

        self.write_fp.write('\n')
        self.write_fp.flush()

        read(self.fp, '\n')

    def test_readline_boundary4(self):
        readline(self.fp, 'x' * (READAHEAD_SIZE - 2) + '\n')

        self.write_fp.write('\n')
        self.write_fp.flush()

        readline(self.fp, '\n')


class UniversalReadaheadBoundary4TestCase(TextReadaheadBoundary4TestCase):

    mode = 'U'
    data = ('x' * (READAHEAD_SIZE - 2)) + '\n\r'


class TextReadaheadBoundary5TestCase(BaseTestCase):
    """For CRLF platforms only"""

    mode = 'r'
    data = 'foobar\n' + ('x' * (READAHEAD_SIZE + 1))

    def test_boundary5(self):
        readline(self.fp, 'foobar\n')
        read(self.fp, 'x' * (READAHEAD_SIZE + 1), READAHEAD_SIZE + 1)


class UniversalReadaheadBoundary5TestCase(TextReadaheadBoundary5TestCase):

    mode = 'U'


class TextCRAtReadheadBoundaryTestCase(BaseTestCase):
    """For CRLF platforms only"""

    data = ('x' * (READAHEAD_SIZE - 1)) + '\rfoo'
    read_data = data

    def test_readline_cr_at_boundary(self):
        readline(self.fp, self.read_data, len(self.read_data))
        self.fp.seek(0)
        readline(self.fp, self.read_data)


class TextCRAtReadheadBoundary2TestCase(TextCRAtReadheadBoundaryTestCase):
    """For CRLF platforms only"""

    data = ('x' * (READAHEAD_SIZE - 1)) + '\r' + ('x' * 300)
    read_data = data


class UniversalCRAtReadaheadBoundaryTestCase(BaseTestCase):

    mode = 'U'
    bufsize = 0
    data = ('-' * 1023) + '\r\n' + ('-' * 10233)

    def test_read_cr_at_boundary(self):
        # Used to raise a BufferOverflowException w/ bufsize of 0
        read(self.fp, ('-' * 1023) + '\n', 1024)


class WriteTextNewlinesTestCase(BaseTestCase):

    write_mode = 'w'
    mode = 'rb'

    def test_text_written(self):
        if LF:
            readline(self.fp, 'CR\rLF\n')
            readline(self.fp, 'CRLF\r\n')
        elif CRLF:
            readline(self.fp, 'CR\rLF\r\n')
            readline(self.fp, 'CRLF\r\r\n')
        readline(self.fp, 'EOF')


class ReadUniversalNewlinesTestCase(BaseTestCase):

    mode = 'rU'

    def test_read(self):
        read(self.fp, 'CR\nLF\nCRLF\nEOF')

        self.fp.seek(0)
        read(self.fp, 'CR\nLF\nCRLF\nEOF', 14)

    def test_readline(self):
        readline(self.fp, 'CR\n')
        assert self.fp.newlines == None, repr(self.fp.newlines)
        readline(self.fp, 'LF\n')
        assert self.fp.newlines == ('\r', '\n'), repr(self.fp.newlines)
        readline(self.fp, 'CRLF\n')
        assert self.fp.newlines == ('\r', '\n'), repr(self.fp.newlines)
        readline(self.fp, 'EOF')
        assert self.fp.newlines == ('\r', '\n', '\r\n'), repr(self.fp.newlines)

        self.fp.seek(0)
        readline(self.fp, 'CR\n', 3)
        readline(self.fp, 'LF\n', 3)
        readline(self.fp, 'CRLF\n', 5)
        readline(self.fp, 'EOF', 3)

    def test_seek(self):
        # Ensure seek doesn't confuse CRLF newline identification
        self.fp.seek(6)
        readline(self.fp, 'CRLF\n')
        assert self.fp.newlines == None
        self.fp.seek(5)
        readline(self.fp, '\n')
        assert self.fp.newlines == '\n'


class WriteUniversalNewlinesTestCase(unittest.TestCase):

    def test_fails(self):
        try:
            open(tempfile.mktemp(), 'wU')
        except ValueError:
            pass
        else:
            raise AssertionError("file mode 'wU' did not raise a "
                                 "ValueError")


def read(fp, data, size=-1):
    line = fp.read(size)
    assert line == data, 'read: %r expected: %r' % (line, data)


def readline(fp, data, size=-1):
    line = fp.readline(size)
    assert line == data, 'readline: %r expected: %r' % (line, data)


def test_main():
    tests = [ReadBinaryNewlinesTestCase,
             ReadTextNewlinesTestCase,
             ReadTextBasicBoundaryTestCase,
             ReadUniversalBasicBoundaryTestCase,
             BinaryReadaheadBoundaryTestCase,
             TextReadaheadBoundaryTestCase,
             UniversalReadaheadBoundaryTestCase,
             UniversalReadaheadBoundary2TestCase,
             UniversalReadaheadBoundary3TestCase,
             UniversalReadaheadBoundary4TestCase,
             UniversalReadaheadBoundary5TestCase,
             UniversalCRAtReadaheadBoundaryTestCase,
             WriteTextNewlinesTestCase,
             ReadUniversalNewlinesTestCase,
             WriteUniversalNewlinesTestCase]
    if CRLF:
        tests.extend([TextReadaheadBoundary2TestCase,
                      TextReadaheadBoundary3TestCase,
                      TextReadaheadBoundary4TestCase,
                      TextReadaheadBoundary5TestCase,
                      TextCRAtReadheadBoundaryTestCase,
                      TextCRAtReadheadBoundary2TestCase])
    test_support.run_unittest(*tests)

if __name__ == '__main__':
    test_main()
