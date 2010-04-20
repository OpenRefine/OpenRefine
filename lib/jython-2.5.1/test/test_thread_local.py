#! /usr/bin/env python
""" Simple test script for Thread.local
"""
from thread import _local as local
import unittest
from test import test_support
import threading

class ThreadLocalTestCase(unittest.TestCase):

    def test_two_locals(self):
        '''Ensures that two locals in the same thread have separate dicts.'''
        first = local()
        first.x = 7
        second = local()
        second.x = 12
        self.assertEquals(7, first.x)
        self.assertEquals(12, second.x)

    def test_local(self):
        mydata = local()
        mydata.number = 42
        self.assertEqual(mydata.number,42)
        self.assertEqual(mydata.__dict__,{'number': 42})
        mydata.__dict__.setdefault('widgets', [])
        self.assertEqual(mydata.widgets,[])
        log=[]

        def f():
            items = mydata.__dict__.items()
            items.sort()
            log.append(items)
            mydata.number = 11
            log.append(mydata.number)

        thread = threading.Thread(target=f)
        thread.start()
        thread.join()
        self.assertEqual(log,[[], 11])
        self.assertEqual(mydata.number,42)

    def test_subclass_local(self):
        def f():
            items = mydata.__dict__.items()
            items.sort()
            log.append(items)
            mydata.number = 11
            log.append(mydata.number)

        class MyLocal(local):
            number = 2
            initialized = False
            def __init__(self, **kw):
                if self.initialized:
                    raise SystemError('__init__ called too many times')
                self.initialized = True
                self.__dict__.update(kw)
            def squared(self):
                return self.number ** 2

        class SubSubLocal(MyLocal):
            pass

        mydata = MyLocal(color='red')
        self.assertEqual(mydata.number,2)
        self.assertEqual(mydata.color,'red')
        del mydata.color
        log=[]
        self.assertEqual(mydata.squared(),4)
        thread = threading.Thread(target=f)
        thread.start()
        thread.join()
        self.assertEqual(log,[[('color', 'red'), ('initialized', True)], 11])
        self.assertEqual(mydata.number,2)
        self.assertRaises(TypeError, local, 'any arguments')
        SubSubLocal(color='red')

        def accessColor():
            mydata.color

        self.assertRaises(AttributeError,accessColor)

        class MyLocal(local):
            __slots__ = 'number'

        mydata = MyLocal()
        mydata.number = 42
        mydata.color = 'red'
        thread = threading.Thread(target=f)
        thread.start()
        thread.join()
        self.assertEqual(mydata.number,11)

def test_main():
    test_support.run_unittest(ThreadLocalTestCase)

if __name__ == "__main__":
    test_main()
