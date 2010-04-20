import unittest
from test import test_support

# xxx - forces a skip in the case we haven't built ctypes_test module in ant (which is not yet a task as of now)

try:
    import _rawffi
    _rawffi.CDLL("ctypes_test")
except:
    raise ImportError

class RawFFITestCase(unittest.TestCase):

    def setUp(self):
        self.libc_name = "c"
        self.lib_name = "ctypes_test"

    def test_libload(self):
        import _rawffi
        _rawffi.CDLL(self.libc_name)

    def test_libc_load(self):
        import _rawffi
        _rawffi.get_libc()

    def test_getattr(self):
        import _rawffi
        libc = _rawffi.get_libc()
        func = libc.ptr('rand', [], 'i')
        assert libc.ptr('rand', [], 'i') is func # caching
        assert libc.ptr('rand', [], 'l') is not func
        assert isinstance(func, _rawffi.FuncPtr)
        self.assertRaises(AttributeError, getattr, libc, "xxx")

    def test_short_addition(self):
        import _rawffi
        lib = _rawffi.CDLL(self.lib_name)
        short_add = lib.ptr('add_shorts', ['h', 'h'], 'H')
        A = _rawffi.Array('h')
        arg1 = A(1, autofree=True)
        arg2 = A(1, autofree=True)
        arg1[0] = 1
        arg2[0] = 2
        res = short_add(arg1, arg2)
        assert res[0] == 3
        # this does not apply to this version of memory allocation
        #arg1.free()
        #arg2.free()

def test_main():
    tests = [RawFFITestCase,
             ]
    test_support.run_unittest(*tests)

if __name__ == '__main__':
    test_main()
