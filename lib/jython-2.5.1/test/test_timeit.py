import timeit
import unittest
import test.test_support

# some uninspired unit tests extracted from the docs for timeit;
# changed number=10000 so we don't spend too much time testing this
# module in the regrtest

class TestTimeit(unittest.TestCase):

    def test_oct(self):
        timing = timeit.Timer('for i in xrange(10): oct(i)', 'gc.enable()').timeit(number=10000)
        self.assertTrue(timing > 0.)
        timing_vector = timeit.Timer('for i in xrange(10): oct(i)').repeat(number=10000)
        self.assertEqual(len(timing_vector), 3)
        self.assertTrue(min(timing_vector) > 0.)

    def test_str(self):
        s = """\
            try:
                str.__nonzero__
            except AttributeError:
                pass
            """
        t = timeit.Timer(stmt=s)
        self.assertTrue(t.timeit(number=10000) > 0.)

        timing_vector = t.repeat(number=10000, repeat=10)
        self.assertEqual(len(timing_vector), 10)
        self.assertTrue(min(timing_vector) > 0.)


def test_main():
    test.test_support.run_unittest(TestTimeit)

if __name__ == "__main__":
    test_main()
