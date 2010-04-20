from test import test_support
import unittest
import pickle

#myint and its test taken from pickletester, the other "my" classes and tests
#      are based on it.
class myint(int):
    def __init__(self, x):
        self.str = str(x)

class mystr(str):
    def __init__(self, x):
        self.str = str(x)

class mytuple(tuple):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)

class myint(int):
    def __init__(self, x):
        self.str = str(x)


class TestSubclasses(unittest.TestCase):

    def test_float(self):
        class Spam(float):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0.0)
        self.assertEqual(str(Spam()), "hi")

    def test_complex(self):
        class Spam(complex):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0j)
        self.assertEqual(str(Spam()), "hi")

    def test_int(self):
        class Spam(int):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0)
        self.assertEqual(str(Spam()), "hi")

    def test_long(self):
        class Spam(long):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0L)
        self.assertEqual(str(Spam()), "hi")

    def test_tuple(self):
        class Spam(tuple):
            def __str__(self):
                return "hi"

        #XXX: subclasses of tuple are not coming out equal...
        #self.assertEqual(Spam(), ())
        self.assertEqual(str(Spam()), "hi")


    def test_str(self):
        class Spam(str):
            def eggs(self):
                return "I am eggs."

        self.assertEqual(Spam(), "")
        self.assertEqual(Spam().eggs(), "I am eggs.")

    def test_unicode(self):
        class Spam(unicode):
            def eggs(self):
                return "I am eggs."

        self.assertEqual(Spam(), "")
        self.assertEqual(Spam().eggs(), "I am eggs.")

    def test_classic(self):
        class Spam:
            def eggs(self):
                return "I am eggs."

        class SubSpam(Spam):
            pass

        class SubSubSpam(SubSpam):
            pass

        self.assertEqual(Spam().eggs(), "I am eggs.")
        self.assertEqual(SubSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubSpam().eggs(), "I am eggs.")

    def test_classic_multiple(self):
        class Spam:
            def eggs(self):
                return "I am eggs."

        class SubSpam(Spam):
            pass

        class SubSubStrSpam(SubSpam, str):
            pass

        class SubSubStrSpam2(str, SubSpam):
            pass

        self.assertEqual(Spam().eggs(), "I am eggs.")
        self.assertEqual(SubSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubStrSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubStrSpam2().eggs(), "I am eggs.")

    def test_newstyle(self):
        class Spam(object):
            def eggs(self):
                return "I am eggs."

        class SubSpam(Spam):
            pass

        class SubSubSpam(SubSpam):
            pass

        self.assertEqual(Spam().eggs(), "I am eggs.")
        self.assertEqual(SubSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubSpam().eggs(), "I am eggs.")

    def test_newstyle_multiple(self):
        class Spam(object):
            def eggs(self):
                return "I am eggs."

        class SubSpam(Spam):
            pass

        class SubSubStrSpam(SubSpam, str):
            pass

        class SubSubStrSpam2(str, SubSpam):
            pass

        self.assertEqual(Spam().eggs(), "I am eggs.")
        self.assertEqual(SubSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubStrSpam().eggs(), "I am eggs.")
        self.assertEqual(SubSubStrSpam2().eggs(), "I am eggs.")

    def picklecheck(self, picklee):
        s = pickle.dumps(picklee)
        y = pickle.loads(s)
        self.assertEqual(picklee, y)

    def test_pickle_builtins(self):
        #ignores cPickle for now.

        self.picklecheck(myint(4))
        self.picklecheck(mystr('x'))
        self.picklecheck(mytuple([1,2]))


def test_main():
    test_support.run_unittest(TestSubclasses)

if __name__ == "__main__":
    test_main()
