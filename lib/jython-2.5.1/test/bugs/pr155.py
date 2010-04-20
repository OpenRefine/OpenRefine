# a test for PR#155, %g conversion differs from CPython.

def test(fmt, expected, input=1.0):
    x = fmt % input
    if x <> expected:
        print 'PR#155: fmt conversion failed.',
        print '(got %s, expected %s)' % (x, expected)


test('%g', '1')
test('%#g', '1.00000')
test('%#.3g', '1.00')
test('%#.10g', '1.000000000')
