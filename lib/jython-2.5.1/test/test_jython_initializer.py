import os
import subprocess
import sys
import unittest
from test import test_support

class TestUsingInitializer(unittest.TestCase):

    def test_syspath_initializer(self):
        fn = test_support.findfile('check_for_initializer_in_syspath.py')
        env = dict(CLASSPATH='tests/data/initializer',
                   PATH=os.environ.get('PATH', ''))
        self.assertEquals(0, subprocess.call([sys.executable, fn], env=env))

def test_main():
    test_support.run_unittest(TestUsingInitializer)

if __name__ == "__main__":
    test_main()
