"""Extra unittests for ast in Jython.  Look to integrate into CPython in the
future"""

import unittest
import ast
from test import test_support

def srcExprToTree(source, kind='exec'):
    return compile(source, '<module>', kind, ast.PyCF_ONLY_AST)

class TestCompile(unittest.TestCase):

    def test_compile_ast(self):
        node = srcExprToTree("1/2")
        compile(node, "<string>", 'exec')

    def test_alias_trim(self):
        node = srcExprToTree("import os. path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = srcExprToTree("import os .path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = srcExprToTree("import os . path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

    def test_cmpop(self):
        expr = srcExprToTree('a < b < c', 'eval')
        compare = expr.body
        self.assert_(isinstance(compare.ops[0], ast.Lt))
        self.assert_(isinstance(compare.comparators[0], ast.Name))
        self.assert_(isinstance(compare.ops[1], ast.Lt))
        self.assert_(isinstance(compare.comparators[1], ast.Name))
        self.assert_(isinstance(compare.ops[1:][0], ast.Lt))
        self.assert_(isinstance(compare.comparators[1:][0], ast.Name))
        z = zip( compare.ops[1:], compare.comparators[1:])
        self.assert_(isinstance(z[0][0], ast.Lt))
        self.assert_(isinstance(z[0][1], ast.Name))

    def test_empty_init(self):
        # Jython 2.5.0 did not allow empty constructors for many ast node types
        # but CPython ast nodes do allow this.  For the moment, I don't see a
        # reason to allow construction of the super types (like ast.AST and
        # ast.stmt) as well as the op types that are implemented as enums in
        # Jython (like boolop), but I've left them in but commented out for
        # now.  We may need them in the future since CPython allows this, but
        # it may fall under implementation detail.

        #ast.AST()
        ast.Add()
        ast.And()
        ast.Assert()
        ast.Assign()
        ast.Attribute()
        ast.AugAssign()
        ast.AugLoad()
        ast.AugStore()
        ast.BinOp()
        ast.BitAnd()
        ast.BitOr()
        ast.BitXor()
        ast.BoolOp()
        ast.Break()
        ast.Call()
        ast.ClassDef()
        ast.Compare()
        ast.Continue()
        ast.Del()
        ast.Delete()
        ast.Dict()
        ast.Div()
        ast.Ellipsis()
        ast.Eq()
        ast.Exec()
        ast.Expr()
        ast.Expression()
        ast.ExtSlice()
        ast.FloorDiv()
        ast.For()
        ast.FunctionDef()
        ast.GeneratorExp()
        ast.Global()
        ast.Gt()
        ast.GtE()
        ast.If()
        ast.IfExp()
        ast.Import()
        ast.ImportFrom()
        ast.In()
        ast.Index()
        ast.Interactive()
        ast.Invert()
        ast.Is()
        ast.IsNot()
        ast.LShift()
        ast.Lambda()
        ast.List()
        ast.ListComp()
        ast.Load()
        ast.Lt()
        ast.LtE()
        ast.Mod()
        ast.Module()
        ast.Mult()
        ast.Name()
        ast.Not()
        ast.NotEq()
        ast.NotIn()
        ast.Num()
        ast.Or()
        ast.Param()
        ast.Pass()
        ast.Pow()
        ast.Print()
        ast.RShift()
        ast.Raise()
        ast.Repr()
        ast.Return()
        ast.Slice()
        ast.Store()
        ast.Str()
        ast.Sub()
        ast.Subscript()
        ast.Suite()
        ast.TryExcept()
        ast.TryFinally()
        ast.Tuple()
        ast.UAdd()
        ast.USub()
        ast.UnaryOp()
        ast.While()
        ast.With()
        ast.Yield()
        ast.alias()
        ast.arguments()
        #ast.boolop()
        #ast.cmpop()
        ast.comprehension()
        #ast.excepthandler()
        #ast.expr()
        #ast.expr_context()
        ast.keyword()
        #ast.mod()
        #ast.operator()
        #ast.slice()
        #ast.stmt()
        #ast.unaryop()

#==============================================================================

def test_main(verbose=None):
    test_classes = [TestCompile]
    test_support.run_unittest(*test_classes)

if __name__ == "__main__":
    test_main(verbose=True)
