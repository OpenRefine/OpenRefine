"""Test chdir

Made for Jython.
"""
import imp
import os
import py_compile
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile
import zipimport
from test import test_support

COMPILED_SUFFIX = [suffix for suffix, mode, type in imp.get_suffixes()
                   if type == imp.PY_COMPILED][0]

WINDOWS = (os._name if test_support.is_jython else os.name) == 'nt'

CODE1 = """result = 'result is %r' % (100.0 * (3.0 / 5.0))"""
CODE1_RESULT = 'result is %r' % (100.0 * (3.0 / 5.0))

CODE2 = 'raise NotImplementedError()'

# The following are their corresponding Java code compiled with javac 1.5
"""
public class ChdirFooTest {
    public static String foo = "bar";
}
"""
CHDIR_FOO_CLASS = """\
eJxdjr1uwjAUhc8F8ktaQspKJTZgAHUuYkFiQnQAdXeCoUYhlkzoe1UdKnXoA/BQFddR1VYs91wf
fz4+5+/PLwAP6IRw0PIRB2ig7SHxcEeob7UmJIu9eBXjXBS78ao0qtg9EtyJKlQ5ZaY/eCY0Znoj
Ca2FKuTydEilWYs0Z8efZPkPGa70yWRyrqzfnr1slJlrvZbHcmQ/iOAj4LxUmAguPEL0nyHEfzWe
0r3MSvS4rMP9CYF9wVuNd47hGfKpy0qszvAD9FaBTZ5uZVow+kXvK4/vkto76tesjb2p9PYCbxA0
PQ==
""".decode('base64').decode('zlib')

"""
package chdir_bar_test;

public class ChdirBarTest {
    public static String bar = "foo";
}
"""
CHDIR_BAR_CLASS = """\
eJxdj71Ow0AQhGfjf2NIYugQSHRAgUWdiIJIVBEUiWijs3OEi4wtHQ7vhSiQKHgAHgoxthAgmt29
uW/n5j4+394BnGMvhod+iEEEF8MAaYBdgZMrK0ina/WkslJVq2zWWFOtRgJ/bCrTXJA5PrkVuJN6
qQX9qan09eYh13au8pJKOC7KbzKe1Rtb6CvT6sPJ/dLYS2Xn+rE5ax9IECKi311dJ/ARCPaLllkw
xKIhlf1dEQx+U93ka100OGJ2j98RRK0Bpx5nurLGPB2wC7t3+gp57sAtVr8THYLJD3rYabxLey9w
/rMu63Znv/MFLEM6Hg==
""".decode('base64').decode('zlib')

# This is a jar archive of the following Java code compiled with javac 1.5
"""
public class ChdirJyTest {
    public static String jy = "thon";
}
"""
CHDIR_JY_JAR = """\
eJwL8GZmEWHgAMLaXf3mDEiAk4GFwdc1xFHX089N/98pBgZmhgBvdg6QFBNUSQBOzSJADNfs6+jn
6eYaHKLn6/bZ98xpH29dvYu83rpa586c3xxkcMX4wdMiPS9fHU/fi6WrWDhnvJI8Ij1DO8NCTOTJ
Ei2L58/FpkxbIvZSo+KZ6uvMT0UfixjB7liq2l/iBrTFDcUdP3agukMQiJ0zUjKLvCpDUotL9JJz
EouLY/v2eh1xEGk9fzavzvx38pbQDcHczV/1zBi5I7fcSJif0p7iNMdhDsfviCTFiB8MdlMq1E9o
mX2/Y/n57uGff6r+MxS0LmYweyYnZnfMokBwp4VKxNc+gbWv31h2z/aZ+tGtPGiqd9PmSV6vsg+9
ePOn0cDv1lqFM9eDOnf/MxSPnfWU1/reqoy6EwmXv8Uq5i35KO65derdt3qi/r8uPWn/8KB4wroo
0UON3jP+JP54tP1obVy30+Mpq2/dmfdgNvM9nXuWbekH1ieHJW3Vv+egz2zx8CXPMsbDtlVGFX/E
I9dL/E33fVNy4aY5KHj+n9nN+QDoeUNGUPAwMokwoEYULApBsYwKUOIcXStyqIugaLPFEePIJoBi
B9lhgigmHMUWVwHerGwgSWYg3AOknzCCeAAyK9gW
""".decode('base64').decode('zlib')


class BaseChdirTestCase(unittest.TestCase):

    """Base TestCase for chdir tests.

    TEST_FILES is the number of available test files allocated for the
    test (residing in the first TEST_DIR).

    Their names are safe for use as a Python module name and available
    at self.filename1 - self.filenameN. os.path.basename(self.filenameN)
    is available at self.basename1 - self.basenameN.

    TEST_DIRS is the number of directories to be created by the fixture,
    available at self.dir1 - self.dirN. At least 1 is always created.

    FILE_SUFFIX is a suffix passed to mktemp for TEST_FILES.

    FIXTURE_CHDIR is whether or not the fixture should chdir to the
    first dir. It always changes back to the original working dir after
    the test regardless of this setting.

    SYSPATH is an iterable of entries to be added (if they don't already
    exist) to sys.path for the duration of the test.
    """

    TEST_FILES = 1
    TEST_DIRS = 1
    FILE_SUFFIX = ''
    FIXTURE_CHDIR = True
    SYSPATH = ()

    def setUp(self):
        for i in max(1, range(self.TEST_DIRS)):
            setattr(self, 'dir%i' % (i + 1), tempfile.mkdtemp())

        for i in range(self.TEST_FILES):
            filename = safe_mktemp(dir=self.dir1, suffix=self.FILE_SUFFIX)
            basename = os.path.basename(filename)
            setattr(self, 'filename%i' % (i + 1), filename)
            setattr(self, 'basename%i' % (i + 1), basename)

        self.orig_cwd = os.getcwd()
        self.orig_syspath = sys.path[:]
        sys.path.extend([path for path in self.SYSPATH
                         if path not in sys.path])

        if self.FIXTURE_CHDIR:
            os.chdir(self.dir1)

    def tearDown(self):
        try:
            try:
                for i in range(self.TEST_DIRS):
                    dir = getattr(self, 'dir%i' % (i + 1))
                    if os.path.exists(dir):
                        shutil.rmtree(dir)
            finally:
                for i in range(self.TEST_FILES):
                    filename = getattr(self, 'filename%i' % (i + 1))
                    if os.path.exists(filename):
                        os.remove(filename)
        finally:
            sys.path = self.orig_syspath
            os.chdir(self.orig_cwd)


class ChdirTestCase(BaseChdirTestCase):

    FIXTURE_CHDIR = False

    def setUp(self):
        super(ChdirTestCase, self).setUp()
        self.subdir1 = os.path.join(self.dir1, 'subdir1')
        self.subdir2 = os.path.join(self.dir1, 'subdir1', 'subdir2')
        os.makedirs(self.subdir2)

    def test_chdir(self):
        orig = os.path.realpath(os.getcwd())
        self.assertNotEqual(orig, self.dir1)
        os.chdir(self.dir1)
        cwd = os.path.realpath(os.getcwd())
        self.assertEqual(os.path.realpath(self.dir1), cwd)
        self.assertNotEqual(orig, cwd)

    def test_relative_chdir(self):
        top = os.path.dirname(self.dir1)
        os.chdir(top)
        os.chdir(os.path.basename(self.dir1))
        self.assertEqual(os.getcwd(), os.path.realpath(self.dir1))
        os.chdir('subdir1')
        self.assertEqual(os.getcwd(), os.path.realpath(self.subdir1))
        os.chdir('subdir2')
        self.assertEqual(os.getcwd(), os.path.realpath(self.subdir2))
        os.chdir('..')
        self.assertEqual(os.getcwd(), os.path.realpath(self.subdir1))
        os.chdir('..%s..' % os.sep)
        self.assertEqual(os.getcwd(), os.path.realpath(top))

    def test_invalid_chdir(self):
        raises(OSError,
               '[Errno 2] %s: %r' % (os.strerror(2), self.filename1),
               os.chdir, self.filename1)
        open(self.filename1, 'w').close()
        raises(OSError,
               '[Errno 20] %s: %r' % (os.strerror(20), self.filename1),
               os.chdir, self.filename1)


class WindowsChdirTestCase(BaseChdirTestCase):

    FIXTURE_CHDIR = False

    def setUp(self):
        super(WindowsChdirTestCase, self).setUp()
        self.subdir = os.path.join(self.dir1, 'Program Files')
        os.makedirs(self.subdir)

    def test_windows_chdir_dos_path(self):
        dos_name = os.path.join(self.dir1, 'progra~1')
        os.chdir(dos_name)
        self.assertEqual(os.getcwd(), os.path.realpath(dos_name))

    def test_windows_getcwd_ensures_drive_letter(self):
        drive = os.path.splitdrive(self.subdir)[0]
        os.chdir('\\')
        self.assertEqual(os.path.normcase(os.getcwd()),
                         os.path.normcase(os.path.join(drive, '\\')))

    def test_windows_chdir_slash_isabs(self):
        drive = os.path.splitdrive(os.getcwd())[0]
        os.chdir('/')
        self.assertEqual(os.path.normcase(os.getcwd()),
                         os.path.normcase(os.path.join(drive, '\\')))


class BaseImportTestCase(BaseChdirTestCase):

    FILE_SUFFIX = '.py'
    SYSPATH = ('',)

    def setUp(self):
        super(BaseImportTestCase, self).setUp()

        write(self.filename1, CODE1)

        self.mod_name = self.basename1[:-3]
        if self.mod_name in sys.modules:
            del sys.modules[self.mod_name]

    def tearDown(self):
        super(BaseImportTestCase, self).tearDown()
        if self.mod_name in sys.modules:
            del sys.modules[self.mod_name]


class ImportTestCase(BaseImportTestCase):

    def setUp(self):
        super(ImportTestCase, self).setUp()
        self.bytecode = os.path.join(self.dir1,
                                     self.mod_name + COMPILED_SUFFIX)

    def test_import_module(self):
        __import__(self.mod_name)
        self.assert_(self.mod_name in sys.modules)
        mod = sys.modules[self.mod_name]
        self.assertEqual(mod.__file__, self.basename1)
        self.assert_(os.path.exists(self.bytecode))

    def test_import_bytecode(self):
        py_compile.compile(self.basename1)
        self.assert_(os.path.exists(self.bytecode))

        os.remove(self.filename1)

        __import__(self.mod_name)
        self.assert_(self.mod_name in sys.modules)
        mod = sys.modules[self.mod_name]
        self.assertEqual(mod.__file__, self.mod_name + COMPILED_SUFFIX)

    def test_imp_find_module(self):
        module_info = imp.find_module(self.mod_name)
        self.assertEqual(module_info[1], self.basename1)
        module_info[0].close()

    def test_imp_load_module(self):
        module_info = imp.find_module(self.mod_name)
        self.assertEqual(module_info[1], self.basename1)
        mod = imp.load_module(self.mod_name, *module_info)
        self.assertEqual(mod.__file__, self.basename1)
        self.assert_(os.path.exists(self.bytecode))
        module_info[0].close()

    def test_imp_load_source(self):
        mod = imp.load_source(self.mod_name, self.basename1)
        self.assertEqual(mod.__file__, self.basename1)
        self.assert_(os.path.exists(self.bytecode))


class ImportPackageTestCase(BaseChdirTestCase):

    SYSPATH = ('',)

    def setUp(self):
        super(ImportPackageTestCase, self).setUp()

        self.package_name = 'chdir_test'
        self.package_path = os.path.join(self.dir1, self.package_name)
        os.mkdir(self.package_path)
        write(os.path.join(self.package_path, '__init__.py'), CODE1)

        self.bytecode = os.path.join(self.package_path,
                                     '__init__' + COMPILED_SUFFIX)
        self.relative_source = os.path.join(self.package_name, '__init__.py')

        if self.package_name in sys.modules:
            del sys.modules[self.package_name]

    def tearDown(self):
        super(ImportPackageTestCase, self).tearDown()
        if self.package_name in sys.modules:
            del sys.modules[self.package_name]

    def test_import_package(self):
        __import__(self.package_name)
        self.assert_(self.package_name in sys.modules)
        package = sys.modules[self.package_name]
        self.assertEqual(package.__path__, [self.package_name])
        self.assertEqual(package.__file__, self.relative_source)
        self.assert_(os.path.exists(self.bytecode))

    def test_imp_find_module_package(self):
        module_info = imp.find_module(self.package_name)
        self.assertEqual(module_info[1], self.package_name)

    def test_imp_load_module_package(self):
        module_info = imp.find_module(self.package_name)
        mod = imp.load_module(self.package_name, *module_info)
        self.assertEqual(mod.__file__, self.relative_source)
        self.assert_(os.path.exists(self.bytecode))

    def test_imp_load_source_package(self):
        mod = imp.load_source(self.package_name, self.relative_source)
        self.assertEqual(mod.__file__, self.relative_source)
        self.assert_(os.path.exists(self.bytecode))


class ZipimportTestCase(BaseImportTestCase):

    def setUp(self):
        super(ZipimportTestCase, self).setUp()

        zip = zipfile.ZipFile(self.filename1 + '.zip', 'w')
        zip.write(self.basename1)
        zip.close()
        os.remove(self.filename1)

    def test_zipimport(self):
        importer = zipimport.zipimporter(self.basename1 + '.zip')
        self.assertEqual(importer.archive, self.basename1 + '.zip')
        self.assertEqual(importer.prefix, '')
        self.assert_(self.basename1 in importer._files)

        # Ensure correct cache entry paths
        entry = importer._files[self.basename1]
        self.assertEqual(entry[0], os.path.join(self.basename1 + '.zip',
                                                self.basename1))

        # Ensure sane import machinery
        self.assertEqual(importer.find_module(self.mod_name), importer)

        # Ensure data lookup from the zip file
        self.assertEqual(importer.get_source(self.mod_name), CODE1)


class PyCompileTestCase(BaseImportTestCase):

    def test_compile(self):
        py_compile.compile(self.basename1)
        self.assert_(os.path.exists(self.filename1[:-3] + COMPILED_SUFFIX))

        __import__(self.mod_name)
        self.assert_(self.mod_name in sys.modules)
        mod = sys.modules[self.mod_name]
        self.assertEqual(mod.__file__, self.mod_name + COMPILED_SUFFIX)

    def test_compile_dest(self):
        py_compile.compile(self.basename1,
                           self.basename1[:-3] +
                           'chdir_test' + COMPILED_SUFFIX)
        self.assert_(os.path.exists(self.filename1[:-3] + 'chdir_test' +
                                    COMPILED_SUFFIX))

        mod_name = self.mod_name + 'chdir_test'
        __import__(mod_name)
        self.assert_(mod_name in sys.modules)
        mod = sys.modules[mod_name]
        self.assertEqual(mod.__file__, mod_name + COMPILED_SUFFIX)


class SubprocessTestCase(BaseChdirTestCase):

    TEST_DIRS = 2

    # Write out the external app's cwd to a file we'll specify in setUp
    COMMAND = '''\
"%s" -c "import os; fp = open(r'%%s', 'w'); fp.write(os.getcwd())"''' % \
        sys.executable

    def test_popen(self):
        os.popen(self._command()).read()
        self.assertEqual(read(self.filename1), os.getcwd())

        os.chdir(self.dir2)
        os.popen(self._command()).read()
        self.assertEqual(read(self.filename1), os.getcwd())

    def test_system(self):
        self.assertEqual(os.system(self._command()), 0)
        self.assertEqual(read(self.filename1), os.getcwd())

        os.chdir(self.dir2)
        self.assertEqual(os.system(self._command()), 0)
        self.assertEqual(read(self.filename1), os.getcwd())

    def test_subprocess(self):
        self.assertEqual(subprocess.call(self._command(), shell=True), 0)
        self.assertEqual(read(self.filename1), os.getcwd())

        os.chdir(self.dir2)
        self.assertEqual(subprocess.call(self._command(), shell=True), 0)
        self.assertEqual(read(self.filename1), os.getcwd())

    def _command(self):
        command = self.COMMAND % self.filename1
        if WINDOWS:
            command = '"%s"' % command
        return command


class ExecfileTestCase(BaseChdirTestCase):

    FIXTURE_CHDIR = False
    TEST_DIRS = 2

    def setUp(self):
        super(ExecfileTestCase, self).setUp()
        write(self.filename1, CODE1)

    def test_execfile(self):
        globals = {}
        execfile(self.filename1, globals)
        self.assertEqual(globals['result'], CODE1_RESULT)

        # filename1 lives in dir1
        os.chdir(self.dir1)

        globals = {}
        execfile(self.basename1, globals)
        self.assertEqual(globals['result'], CODE1_RESULT)

        os.chdir(self.dir2)
        raises(IOError, 2, execfile, self.basename1, globals)


class ExecfileTracebackTestCase(BaseChdirTestCase):

    def setUp(self):
        super(ExecfileTracebackTestCase, self).setUp()
        write(self.filename1, CODE2)

    def test_execfile_traceback(self):
        globals = {}
        try:
            execfile(self.basename1, globals)
        except NotImplementedError:
            tb = sys.exc_info()[2]
            self.assertEqual(tb.tb_next.tb_frame.f_code.co_filename,
                             self.basename1)


class ListdirTestCase(BaseChdirTestCase):

    TEST_DIRS = 2
    FIXTURE_CHDIR = False

    def setUp(self):
        super(ListdirTestCase, self).setUp()
        open(os.path.join(self.dir1, 'chdir_test1'), 'w').close()
        open(os.path.join(self.dir2, 'chdir_test2'), 'w').close()

    def test_listdir(self):
        dir1 = os.listdir(self.dir1)
        dir2 = os.listdir(self.dir2)

        os.chdir(self.dir1)
        self.assertEqual(os.listdir('.'), dir1, os.listdir(self.dir1))

        os.chdir(self.dir2)
        self.assertEqual(os.listdir('.'), dir2, os.listdir(self.dir2))


class DirsTestCase(BaseChdirTestCase):

    def test_makedirs(self):
        self.assert_(not os.path.exists(self.filename1))
        subdir = os.path.join(self.basename1, 'chdir_test')
        os.makedirs(subdir)
        self.assert_(os.path.isdir(self.filename1))
        self.assert_(os.path.isdir(os.path.join(self.filename1, 'chdir_test')))

    def test_mkdir(self):
        self.assert_(not os.path.exists(self.filename1))
        os.mkdir(self.basename1)
        self.assert_(os.path.isdir(self.filename1))

    def test_rmdir(self):
        os.mkdir(self.filename1)
        self.assert_(os.path.exists(self.filename1))
        self.assert_(os.path.exists(self.basename1))
        os.rmdir(self.basename1)
        self.assert_(not os.path.exists(self.filename1))

    def test_isdir(self):
        self.assert_(not os.path.isdir(self.basename1))
        os.mkdir(self.filename1)
        self.assert_(os.path.isdir(self.basename1))


class FilesTestCase(BaseChdirTestCase):

    def setUp(self):
        super(FilesTestCase, self).setUp()
        write(self.filename1, 'test')

    def test_remove(self):
        os.remove(self.basename1)
        self.assert_(not os.path.exists(self.filename1))

    def test_rename(self):
        os.rename(self.basename1, 'chdir_test')
        self.assert_(not os.path.exists(self.filename1))
        self.assert_(os.path.exists(os.path.join(self.dir1, 'chdir_test')))

    def test_stat(self):
        self.assertEqual(os.stat(self.basename1).st_size, 4)

    def test_utime(self):
        new_utime = os.stat(self.basename1).st_mtime + 100
        os.utime(self.basename1, (new_utime, new_utime))
        self.assertEqual(os.stat(self.basename1).st_mtime,
                         os.stat(self.filename1).st_mtime)

    def test_open(self):
        fp = open(self.basename1)
        self.assertEqual(fp.name, self.basename1)
        self.assert_(repr(fp).startswith("<open file '%s'" % self.basename1))
        self.assertEqual(fp.read(), 'test')
        fp.close()

    def test_os_open(self):
        fd = os.open(self.basename1, os.O_RDONLY)
        self.assertEqual(os.read(fd, 4), 'test')
        os.close(fd)

    def test_exists(self):
        self.assert_(os.path.exists(self.basename1))

    def test_isfile(self):
        self.assert_(os.path.isfile(self.basename1))

    def test_abspath(self):
        self.assertEqual(os.path.join(os.getcwd(), self.basename1),
                         os.path.abspath(self.basename1))

    def test_realpath(self):
        self.assertEqual(os.path.realpath(self.filename1),
                         os.path.realpath(self.basename1))

    def test_getsize(self):
        self.assertEqual(os.path.getsize(self.basename1), 4)

    def test_getmtime(self):
        self.assertEqual(os.path.getmtime(self.filename1),
                         os.path.getmtime(self.basename1))

    def test_getatime(self):
        self.assertEqual(os.path.getatime(self.filename1),
                         os.path.getatime(self.basename1))


class ImportJavaClassTestCase(BaseChdirTestCase):

    SYSPATH = ('',)

    def setUp(self):
        super(ImportJavaClassTestCase, self).setUp()

        write(os.path.join(self.dir1, 'ChdirFooTest.class'), CHDIR_FOO_CLASS)

        package_path = os.path.join(self.dir1, 'chdir_bar_test')
        os.mkdir(package_path)
        write(os.path.join(package_path, 'ChdirBarTest.class'),
              CHDIR_BAR_CLASS)

    def tearDown(self):
        super(ImportJavaClassTestCase, self).tearDown()
        if 'ChdirFooTest' in sys.modules:
            del sys.modules['ChdirFooTest']
        if 'chdir_bar_test' in sys.modules:
            del sys.modules['chdir_bar_test']

    def test_import_java_class(self):
        import ChdirFooTest
        self.assertEqual(ChdirFooTest.foo, 'bar')

    def test_import_java_package(self):
        import chdir_bar_test
        self.assertEqual(chdir_bar_test.ChdirBarTest.bar, 'foo')


class ImportJarTestCase(BaseChdirTestCase):

    SYSPATH = ('chdir-test.jar',)

    def setUp(self):
        super(ImportJarTestCase, self).setUp()
        write(os.path.join(self.dir1, 'chdir-test.jar'), CHDIR_JY_JAR)

    def tearDown(self):
        try:
            super(ImportJarTestCase, self).tearDown()
        except OSError:
            # XXX: Windows raises an error here when deleting the jar
            # because SyspathArchive holds onto its file handle (and you
            # can't delete a file in use on Windows). We may not want to
            # change this
            self.assert_(WINDOWS)
        if 'ChdirJyTest' in sys.modules:
            del sys.modules['ChdirJyTest']

    def test_import_jar(self):
        import ChdirJyTest
        self.assertEqual(ChdirJyTest.jy, 'thon')


def safe_mktemp(*args, **kwargs):
    """Return a temp filename similar to tempfile.mktemp()

    The 'safety' features:
    o ensures the filename is safe to use as a Python module name
    o ensures a file of the same name does not exist in the current
    working directory (to avoid false chdir positives -- extra paranoia)
    """
    while True:
        dir, basename = os.path.split(tempfile.mktemp(*args, **kwargs))
        basename = basename.replace('-', '')
        name = os.path.join(dir, basename)
        if not os.path.exists(name) and \
                not os.path.exists(os.path.join(os.getcwd(), basename)):
            return name


def raises(exc, expected, callable, *args):
    """Ensure the expected exception is raised"""
    try:
        callable(*args)
    except exc, msg:
        if expected is not None:
            if isinstance(expected, str):
                if str(msg) != expected:
                    raise test_support.TestFailed(
                        "Message %r, expected %r" % (str(msg), expected))
            elif isinstance(expected, int):
                if msg.errno != expected:
                    raise test_support.TestFailed(
                        "errno %r, expected %r" % (msg, expected))

    else:
        raise test_support.TestFailed("Expected %s" % exc)


def read(filename):
    """Read data from filename"""
    fp = open(filename)
    data = fp.read()
    fp.close()
    return data


def write(filename, data):
    """Write data to filename"""
    fp = open(filename, 'wb')
    fp.write(data)
    fp.close()


def test_main():
    tests = [ChdirTestCase,
             ImportTestCase,
             ImportPackageTestCase,
             ZipimportTestCase,
             PyCompileTestCase,
             ExecfileTestCase,
             ExecfileTracebackTestCase,
             ListdirTestCase,
             DirsTestCase,
             FilesTestCase]
    if WINDOWS:
        tests.append(WindowsChdirTestCase)
    if test_support.is_jython:
        tests.extend((ImportJavaClassTestCase,
                      ImportJarTestCase))
    if test_support.is_resource_enabled('subprocess'):
        tests.append(SubprocessTestCase)
    test_support.run_unittest(*tests)


if __name__ == '__main__':
    test_main()
