from __future__ import generators
import unittest

#tests for deeply nested try/except/finally's

class FinallyTests(unittest.TestCase):
    def gen1(self):
        try:
            pass
        finally:
            yield 1
    def genContinue(self):
        for i in range(3):
            try:
                continue
            finally:
                yield i
    def genPass(self):
        for i in range(3):
            try:
                pass
            finally:
                yield i
    def genLocal(self):
        x = 1
        try:
            pass
        finally:
            yield x
    def genConditional(self):
        for i in range(3):
            x = 0
            try:
                if i == 2:
                    continue
                x = 1
            finally:
                for j in range(x, x + 2):
                    yield j
    def genTryExceptAroundFinally(self):
        try:
            for i in range(1):
                try:
                    for i in range(3):
                        try:
                            try:
                                1//0
                            finally:
                                yield i
                        except:
                            pass
                    1//0
                except:
                    yield 3
        except:
            pass
    def genNested(self):
        for i in range(2):
            try:
                continue
            finally:
                for j in range(2):
                    try:
                        pass
                    finally:
                        yield (i, j)
    def genNestedReversed(self):
        for i in range(2):
            try:
                pass
            finally:
                for j in range(2):
                    try:
                        continue
                    finally:
                        yield (i, j)
    def genNestedDeeply(self):
        for i in range(4):
            try:
                continue
            finally:
                for j in range(i):
                    try:
                        pass
                    finally:
                        for k in range(j):
                            try:
                                try:
                                    1//0
                                finally:
                                    yield (i, j, k)
                            except:
                                pass
    def genNestedTryExcept(self):
        for j in range(3):
            try:
                try:
                    1//0
                finally:
                    for k in range(3):
                        try:
                            1//0
                        finally:
                            yield (j, k)
            except:
                pass
    def genNestedDeeplyTryExcept(self):
        for i in range(3):
            try:
                try:
                    1//0
                finally:
                    for j in range(3):
                        try:
                            1//0
                        finally:
                            for k in range(3):
                                try:
                                    1//0
                                finally:
                                    yield (i, j, k)
            except:
                pass
    def testFinally(self):
        self.assertEquals([1], list(self.gen1()))
        self.assertEquals([0, 1, 2], list(self.genContinue()))
        self.assertEquals([0, 1, 2], list(self.genPass()))
        self.assertEquals([1], list(self.genLocal()))
        self.assertEquals(
            [1, 2, 1, 2, 0, 1],
            list(self.genConditional()))
        self.assertEquals([0, 1, 2, 3], list(self.genTryExceptAroundFinally()))
        self.assertEquals(
            [(0, 0), (0, 1), (1, 0), (1, 1)],
            list(self.genNested()))
        self.assertEquals(
            [(0, 0), (0, 1), (1, 0), (1, 1)],
            list(self.genNestedReversed()))
        self.assertEquals(
            [(2, 1, 0), (3, 1, 0), (3, 2, 0), (3, 2, 1)],
            list(self.genNestedDeeply()))
        self.assertEquals(
            [(0, 0), (1, 0), (2, 0)],
            list(self.genNestedTryExcept()))
        self.assertEquals(
            [(0, 0, 0), (1, 0, 0), (2, 0, 0)],
            list(self.genNestedDeeplyTryExcept()))

class TryExceptTests(unittest.TestCase):
    def genNestedExcept(self):
        for j in range(3):
            try:
                try:
                    1//0
                except ZeroDivisionError, e:
                    yield 1
                    raise e
            except ZeroDivisionError:
                pass
    def testExcept(self):
        self.assertEquals([1, 1, 1], list(self.genNestedExcept()))

if __name__ == "__main__":
    unittest.main()
