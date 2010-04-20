"""Test file descriptor operations

Made for Jython.
"""
import errno
import os
import sys
import tempfile
import test.test_support as test_support
import unittest

class TestFilenoTestCase(unittest.TestCase):

    def setUp(self):
        self.filename = tempfile.mktemp()
        self.fp = open(self.filename, 'w+')
        self.fd = self.fp.fileno()

    def tearDown(self):
        if self.fp:
            self.fp.close()
        os.remove(self.filename)

    def test_ftruncate(self):
        self.fp.write('jython filenos')
        self.fp.flush()
        os.fsync(self.fd)
        self.assertEqual(os.path.getsize(self.filename), 14)
        os.ftruncate(self.fd, 8)
        self.assertEqual(os.path.getsize(self.filename), 8)
        os.ftruncate(self.fd, 0)
        self.assertEqual(os.path.getsize(self.filename), 0)

        self.fp.close()
        raises(IOError, 9, os.ftruncate, self.fd, 0)

    def test_lseek(self):
        self.assertEqual(os.lseek(self.fd, 0, 1), 0)
        os.write(self.fd, 'jython filenos')
        os.lseek(self.fd, 7, 0)
        self.assertEqual(os.read(self.fd, 7), 'filenos')
        self.fp.close()
        raises(OSError, 9, os.lseek, self.fd, 0, 1)

    def test_read(self):
        self.fp.write('jython filenos')
        self.fp.flush()
        self.fp.seek(0)
        self.assertEqual(os.read(self.fd, 7), 'jython ')
        self.assertEqual(os.read(self.fd, 99), 'filenos')
        self.fp.close()
        raises(OSError, 9, os.read, self.fd, 1)

    def test_write(self):
        os.write(self.fd, 'jython filenos')
        self.fp.seek(0)
        self.assertEqual(self.fp.read(), 'jython filenos')
        self.fp.close()
        raises(OSError, 9, os.write, self.fd, 'The Larch')


class TestOsOpenTestCase(unittest.TestCase):

    def setUp(self):
        self.filename = tempfile.mktemp()
        self.dir = None
        self.fd = None

    def tearDown(self):
        if self.fd:
            try:
                os.close(self.fd)
            except:
                pass
        if os.path.exists(self.filename):
            os.remove(self.filename)
        if self.dir:
            os.rmdir(self.dir)

    def test_open(self):
        # XXX: assert the mode of the file
        self.fd = os.open(self.filename, os.O_WRONLY | os.O_CREAT)
        self.assert_(os.path.exists(self.filename))
        os.write(self.fd, 'jython')
        os.close(self.fd)

        self.fd = os.open(self.filename, os.O_WRONLY | os.O_APPEND)
        os.write(self.fd, ' filenos')
        os.close(self.fd)
        fp = open(self.filename)
        self.assertEquals(fp.read(), 'jython filenos')
        fp.close()

        # falls back to read only without O_WRONLY/O_RDWR
        self.fd = os.open(self.filename, os.O_APPEND)
        raises(OSError, 9, os.write, self.fd, 'new')
        # Acts as append on windows (seeks to the end)
        os.lseek(self.fd, 0, 0)
        self.assertEquals(os.read(self.fd, len('jython filenos')), 'jython filenos')
        os.close(self.fd)

        # falls back to read only without O_WRONLY/O_RDWR
        self.fd = os.open(self.filename, os.O_CREAT)
        raises(OSError, 9, os.write, self.fd, 'new')
        self.assertEquals(os.read(self.fd, len('jython filenos')), 'jython filenos')
        os.close(self.fd)

        # interpreted as RDWR
        self.fd = os.open(self.filename, os.O_RDONLY | os.O_RDWR)
        os.write(self.fd, 'test')
        os.lseek(self.fd, 0, 0)
        self.assertEquals(os.read(self.fd, 4), 'test')
        os.close(self.fd)

    def test_open_truncate(self):
        fp = open(self.filename, 'w')
        fp.write('hello')
        fp.close()

        self.assertEquals(os.path.getsize(self.filename), 5)
        self.fd = os.open(self.filename, os.O_TRUNC | os.O_RDWR)
        self.assertEquals(os.path.getsize(self.filename), 0)
        os.write(self.fd, 'truncated')
        os.lseek(self.fd, 0, 0)
        self.assertEquals(os.read(self.fd, len('truncated')), 'truncated')
        os.close(self.fd)

        self.fd = os.open(self.filename, os.O_TRUNC | os.O_WRONLY)
        self.assertEquals(os.path.getsize(self.filename), 0)
        os.write(self.fd, 'write only truncated')
        raises(OSError, 9, os.read, self.fd, 99)
        os.close(self.fd)

        fd = open(self.filename)
        self.assertEquals(fd.read(), 'write only truncated')
        fd.close()

        # Both fail on Windows, errno 22
        """
        # falls back to read only without O_WRONLY/O_RDWR, but truncates
        self.fd = os.open(self.filename, os.O_TRUNC)
        self.assertEquals(os.path.getsize(self.filename), 0)
        raises(OSError, 9, os.write, self.fd, 'new')
        self.assertEquals(os.read(self.fd, 99), '')
        os.close(self.fd)

        fp = open(self.filename, 'w')
        fp.write('and ')
        fp.close()
        self.assertEquals(os.path.getsize(self.filename), 4)

        # append with no write falls back to read, but still truncates
        self.fd = os.open(self.filename, os.O_TRUNC | os.O_APPEND)
        self.assertEquals(os.path.getsize(self.filename), 0)
        raises(OSError, 9, os.write, self.fd, 'new')
        os.close(self.fd)

        fp = open(self.filename, 'w')
        fp.write('and ')
        fp.close()
        self.assertEquals(os.path.getsize(self.filename), 4)
        """

    def test_open_exclusive(self):
        self.assert_(not os.path.exists(self.filename))
        # fails without O_CREAT
        raises(OSError, (2, self.filename), os.open, self.filename, os.O_EXCL)
        self.assert_(not os.path.exists(self.filename))

        # creates, read only
        self.fd = os.open(self.filename, os.O_EXCL | os.O_CREAT)
        self.assert_(os.path.exists(self.filename))
        raises(OSError, 9, os.write, self.fd, 'jython')
        self.assertEquals(os.read(self.fd, 99), '')
        os.close(self.fd)

        # not exclusive unless creating
        os.close(os.open(self.filename, os.O_EXCL))
        raises(OSError, (17, self.filename),
               os.open, self.filename, os.O_CREAT | os.O_EXCL)
        raises(OSError, (17, self.filename),
               os.open, self.filename, os.O_CREAT | os.O_WRONLY | os.O_EXCL)
        raises(OSError, (17, self.filename),
               os.open, self.filename, os.O_CREAT | os.O_RDWR | os.O_EXCL)

        os.remove(self.filename)
        self.fd = os.open(self.filename, os.O_EXCL | os.O_RDWR | os.O_CREAT)
        os.write(self.fd, 'exclusive')
        os.lseek(self.fd, 0, 0)
        self.assertEquals(os.read(self.fd, len('exclusive')), 'exclusive')

    def test_open_sync(self):
        if not hasattr(os, 'O_SYNC'):
            return

        # Just ensure this works
        self.fd = os.open(self.filename, os.O_SYNC | os.O_WRONLY | os.O_CREAT)
        self.assert_(os.path.exists(self.filename))
        os.write(self.fd, 'jython')
        raises(OSError, 9, os.read, self.fd, 99)
        os.close(self.fd)
        os.remove(self.filename)

        self.fd = os.open(self.filename, os.O_SYNC | os.O_RDWR | os.O_CREAT)
        self.assert_(os.path.exists(self.filename))
        os.write(self.fd, 'jython')
        os.lseek(self.fd, 0, 0)
        self.assertEquals(os.read(self.fd, len('jython')), 'jython')
        os.close(self.fd)

    def test_open_sync_dir(self):
        if not hasattr(os, 'O_SYNC'):
            return

        self.dir = tempfile.mkdtemp()
        try:
            self.fd = os.open(self.dir, os.O_SYNC | os.O_RDWR)
        except OSError, ose:
            assert ose.errno == errno.EISDIR, ose.errno

    def test_bad_open(self):
        for mode in (os.O_WRONLY, os.O_WRONLY, os.O_RDWR):
            raises(OSError, (2, self.filename), os.open, self.filename, mode)

        open(self.filename, 'w').close()

        raises(OSError, (22, self.filename),
               os.open, self.filename, os.O_WRONLY | os.O_RDWR)


class TestOsFdopenTestCase(unittest.TestCase):

    def setUp(self):
        self.filename = tempfile.mktemp()
        self.fd = None

    def tearDown(self):
        if self.fd:
            try:
                os.close(self.fd)
            except:
                pass
        if os.path.exists(self.filename):
            os.remove(self.filename)

    def test_fdopen(self):
        origw_fp = open(self.filename, 'w')
        origw = origw_fp.fileno()
        origr_fp = open(self.filename, 'r')
        origr = origr_fp.fileno()

        # Mode must begin with rwa
        raises(ValueError, "invalid file mode 'b'",
               os.fdopen, origr, 'b')

        # Refuse modes the original file doesn't support
        # XXX: allowed on windows CPython
        """
        raises(OSError, '[Errno 22] Invalid argument',
               os.fdopen, origw, 'r')
        raises(OSError, '[Errno 22] Invalid argument',
               os.fdopen, origr, 'w')
               """

        fp = os.fdopen(origw, 'w')
        fp.write('fdopen')
        # Windows CPython doesn't raise an exception here
        #raises(IOError, '[Errno 9] Bad file descriptor',
        #       fp.read, 7)
        fp.close()

        fp = os.fdopen(origr)
        self.assertEquals(fp.read(), 'fdopen')
        # Windows CPython raises IOError [Errno 0] Error
        #raises(IOError, '[Errno 9] Bad file descriptor',
        #       fp.write, 'test')
        raises(IOError, None,
               fp.write, 'test')
        fp.close()

        # Windows CPython raises OSError [Errno 0] Error for both these
        #raises(OSError, '[Errno 9] Bad file descriptor',
        #       os.fdopen, origw, 'w')
        #raises(OSError, '[Errno 9] Bad file descriptor',
        #       os.fdopen, origr, 'r')
        raises(OSError, None,
               os.fdopen, origw, 'w')
        raises(OSError, None,
               os.fdopen, origr, 'r')

        # These all raise IO/OSErrors on FreeBSD
        try:
            origw_fp.close()
        except:
            pass
        try:
            origr_fp.close()
        except:
            pass
        try:
            os.close(origw)
        except:
            pass
        try:
            os.close(origr)
        except:
            pass


def raises(exc, expected, callable, *args):
    """Ensure the specified call raises exc.

    expected is compared against the exception message if not None. It
    can be a str, an errno or a 2 item tuple of errno/filename. The
    latter two being for comparison against EnvironmentErrors.
    """
    if expected:
        if isinstance(expected, str):
            msg = expected
        else:
            errno = expected[0] if isinstance(expected, tuple) else expected
            msg = '[Errno %d] %s' % (errno, os.strerror(errno))
            if isinstance(expected, tuple):
                msg += ': %r' % expected[1]
    try:
        callable(*args)
    except exc, val:
        if expected and str(val) != msg:
            raise test_support.TestFailed(
                "Message %r, expected %r" % (str(value), msg))
    else:
        raise test_support.TestFailed("Expected %s" % exc)


def test_main():
    test_support.run_unittest(TestFilenoTestCase,
                              TestOsOpenTestCase,
                              TestOsFdopenTestCase)

if __name__ == '__main__':
    test_main()
