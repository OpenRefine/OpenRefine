"""Test the readonly dict wrapper dictproxy

Made for Jython.
"""
import sys
import unittest
from test import test_support

class DictproxyTestCase(unittest.TestCase):

    def test_dictproxy(self):
        proxy = type.__dict__
        first_key = iter(proxy).next()

        self.assert_(isinstance(first_key, str))
        self.assert_(first_key in proxy)
        self.assert_(proxy.has_key(first_key))
        self.assertEqual(proxy[first_key], proxy.get(first_key))
        self.assertEqual(proxy.get('NOT A KEY', 'foo'), 'foo')

        proxy_len = len(proxy)
        self.assert_(isinstance(proxy_len, int) and proxy_len > 2)
        self.assert_(proxy_len == len(proxy.keys()) == len(proxy.items()) ==
                     len(proxy.values()) == len(list(proxy.iterkeys())) ==
                     len(list(proxy.iteritems())) ==
                     len(list(proxy.itervalues())))
        self.assert_(isinstance(proxy.items()[0], tuple))
        self.assert_(isinstance(proxy.iteritems().next(), tuple))

        copy = proxy.copy()
        self.assert_(proxy is not copy)
        self.assertEqual(len(proxy), len(copy))

    def test_dictproxy_equality(self):
        self.assertEqual(type.__dict__, type.__dict__)
        self.assertEqual(type.__dict__, type.__dict__.copy())
        self.assertEqual(type.__dict__, dict(type.__dict__))
        self.assertEqual(cmp(type.__dict__, type.__dict__), 0)
        self.assertEqual(cmp(type.__dict__, type.__dict__.copy()), 0)
        self.assertEqual(cmp(type.__dict__, dict(type.__dict__)), 0)


def test_main():
    test_support.run_unittest(DictproxyTestCase)


if __name__ == '__main__':
    test_main()
