from java.util import ArrayList, List, Vector

from copy import copy

import unittest
import test.test_support

class CollectionProxyTest(unittest.TestCase):
    def _perform_op(self, value, op_func):
        """
        Perform an operation

            value - the value to operate on
            op_func - the function that applies the operation to value

        Returns:
            the result of calling op_func, OR the exception that was raised in op_func
        """
        try:
            return op_func(value)
        except Exception, e:
            return type(e)

    def check_list(self, control, results, initial):
        for result in results:
            try:
                len(result)
            except:
                print result
            self.assertEquals(len(control), len(result), "%s is wrong for %s" % (type(result), initial))
            for pvalue, jvalue in zip(control, result):
                self.assertEquals(pvalue, jvalue)

    def _list_op_test(self, initial_value, op_func, check_value):
        """
        Tests a list operation

        Ensures that performing an operation on:
            - a python list
            - a java.util.List instance

        givens the same result in both cases
        """
        lists = [list(initial_value), ArrayList(initial_value), Vector(initial_value)]

        results = [self._perform_op(l, op_func) for l in lists]
        self.check_list(lists[0], lists[1:], initial_value)
        if check_value or not isinstance(results[0], list):
            for r in results[1:]:
                self.assertEquals(results[0], r)
        else:
            self.check_list(results[0], results[1:], initial_value)

    def test_get_integer(self):
        initial_value = range(0, 5)

        for i in xrange(-7, 7):
            self._list_op_test(initial_value, lambda xs: xs[i], True)

    def test_set_integer(self):
        initial_value = range(0, 5)

        def make_op_func(index):
            def _f(xs):
                xs[index] = 100
            return _f

        for i in xrange(-7, 7):
            self._list_op_test(initial_value, make_op_func(i), True)

    def test_set_slice(self):
        initial_value = range(0, 10)

        def make_op_func(i, j, k, v):
            def _f(xs):
                xs[i:j:k] = v
            return _f

        for i in xrange(-12, 12):
            for j in xrange(-12, 12):
                for k in xrange(-12, 12):
                    self._list_op_test(initial_value, make_op_func(i, j, k, []), True)
                    self._list_op_test(initial_value, make_op_func(i, j, k, range(0,2)), True)
                    self._list_op_test(initial_value, make_op_func(i, j, k, range(0,4)), True)
                    self._list_op_test(initial_value, make_op_func(i, j, k, xrange(0,2)), True)

    def test_del_integer(self):
        initial_value = range(0,5)

        def make_op_func(index):
            def _f(xs):
                del xs[index]
            return _f

        for i in xrange(-7, 7):
            self._list_op_test(initial_value, make_op_func(i), True)

    def test_del_slice(self):
        initial_value = range(0,10)

        def make_op_func(i, j, k):
            def _f(xs):
                del xs[i:j:k]
            return _f

        for i in xrange(-12, 12):
            for j in xrange(-12, 12):
                for k in xrange(-12, 12):
                    self._list_op_test(initial_value, make_op_func(i, j, k), True)

    def test_len(self):
        jlist = ArrayList()
        jlist.addAll(range(0, 10))

        self.assert_(len(jlist) == 10)

    def test_iter(self):
        jlist = ArrayList()
        jlist.addAll(range(0, 10))

        i = iter(jlist)

        x = list(i)

        self.assert_(x == range(0, 10))

    def test_override_len(self):
        class MyList (ArrayList):
            def __len__(self):
                return self.size() + 1;

        m = MyList()
        m.addAll(range(0,10))

        self.assert_(len(m) == 11)

    def test_override_iter(self):
        class MyList (ArrayList):
            def __iter__(self):
                return iter(self.subList(0, self.size() - 1));


        m = MyList()
        m.addAll(range(0,10))
        i = iter(m)
        x = list(i)

        self.assert_(x == range(0, 9))

    def test_override_getsetdelitem(self):
        # Create an ArrayList subclass that provides some silly overrides for get/set/del item
        class MyList (ArrayList):
            def __getitem__(self, key):
                return self.get(key) * 2;

            def __setitem__(self, key, value):
                return self.set(key, value * 2);

            def __delitem__(self, key):
                self.add(84)


        m = MyList()
        m.addAll(range(0,10))

        self.assert_(m[1] == 2)
        self.assert_(m.get(1) == 1)

        m[0] = 3
        self.assert_(m.get(0) == 6)
        self.assert_(m[0] == 12)

        del m[0]
        self.assert_(m.size() == 11)
        self.assert_(m.get(10) == 84)

def test_main():
    test.test_support.run_unittest(CollectionProxyTest)

if __name__ == "__main__":
    test_main()
