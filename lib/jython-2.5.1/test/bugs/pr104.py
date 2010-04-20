def parrot(**args):
    pass

try:
    compile("parrot(voltage=5.0, 'expired')", '<string>', 'exec')
except SyntaxError:
    pass
else:
    print 'PR#104 regressed'
