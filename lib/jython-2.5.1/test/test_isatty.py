import test.test_support, unittest
import os, popen2, subprocess, sys

def test_isatty(label, thingy):
    os_isatty = os.isatty(thingy.fileno())
    thingy_isatty = thingy.isatty()
    if 'in' in label: expected = stdin_isatty
    elif 'out' in label: expected = stdout_isatty
    elif 'err' in label: expected = stderr_isatty
    else: expected = False
    print '%11s: os.isatty=%.1s | .isatty=%.1s | expected=%.1s' % \
        (label, os_isatty, thingy_isatty, expected)
    assert expected == os_isatty == thingy_isatty, \
        'expected isatty would return %s on %s' % (expected, label)

def test_int_isatty(fd, expected):
    os_isatty = os.isatty(fd)
    print '%11s: os.isatty=%.1s | expected=%.1s' % \
        ('fd %d' % fd, os_isatty, expected)
    assert expected == os_isatty

def test_file_isatty(name):
    if not os.path.exists(name):
        return
    try:
        test_isatty(name, file(name))
    except IOError, e:
        print e # XXX Jython prints 'no such file or directory' - probably
                # 'permission denied' but Java doesn't understand?

def args_list(*args):
    return [sys.executable, __file__] + map(str, args)

class IsattyTest(unittest.TestCase):
    def check_call(self, *args, **kw):
        self.assertEqual(subprocess.check_call(args_list(*args), **kw), 0)

    def test_isatty(self):
        if os.name == 'java': # Jython doesn't allocate ptys here
            self.check_call(False, False, False)
            # XXX not sure how to test anything else
        else:
            self.check_call(True, True, True)
            self.check_call(False, True, True, stdin=subprocess.PIPE)
            self.check_call(True, False, True, stdout=subprocess.PIPE)
            self.check_call(True, True, False, stderr=subprocess.PIPE)

if __name__ == '__main__':
    if len(sys.argv) != 4:
        test.test_support.run_unittest(IsattyTest)
        sys.exit(0)

    stdin_isatty, stdout_isatty, stderr_isatty = map(lambda x: x == 'True',
                                                     sys.argv[1:])

    test_isatty('stdin',  sys.stdin)
    test_isatty('stdout', sys.stdout)
    test_isatty('stderr', sys.stderr)

    test_int_isatty(0, stdin_isatty)
    test_int_isatty(1, stdout_isatty)
    test_int_isatty(2, stderr_isatty)

    test_file_isatty('/dev/stdin')
    test_file_isatty('/dev/stdout')
    test_file_isatty('/dev/stderr')

    try:
        from java.lang import System
        test_isatty('System.in', file(getattr(System, 'in')))
        test_isatty('System.out', file(System.out, 'w'))
        test_isatty('System.err', file(System.err, 'w'))

        from java.io import FileDescriptor, FileInputStream, FileOutputStream
        fd_in = getattr(FileDescriptor, 'in')
        fd_out = FileDescriptor.out
        fd_err = FileDescriptor.err

        test_isatty('FIS(FD.in)', file(FileInputStream(fd_in)))
        test_isatty('FOS(FD.out)', file(FileOutputStream(fd_out)))
        test_isatty('FOS(FD.err)', file(FileOutputStream(fd_err)))
    except ImportError:
        pass

    test_file_isatty('/dev/null')

    sys.exit(0)
