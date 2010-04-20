# test of reload -- PR#101 and related PR#128
#
# javac must be on your $PATH

import sys
import os
from java.lang import Runtime, System
rt = Runtime.getRuntime()

# make sure this directory doesn't appear on your CLASSPATH
tmpdir = '/tmp'                                   # TBD: Un*xism

# assert
cpath = System.getProperty('java.class.path')
for dir in cpath.split(os.pathsep):
    assert os.path.normpath(dir) <> tmpdir

sys.path.insert(0, tmpdir)
javafile = os.path.join(tmpdir, 'pr101j.java')

def makejavaclass(s):
    fp = open(javafile, 'w')
    fp.write('''
// Java side of the PR#101 test -- reload of a Java class
public class pr101j {
    public static String doit() {
        return "%s";
    }
}
''' % s)
    fp.close()
    proc = rt.exec('javac ' + javafile)
    status = proc.waitFor()
    if status <> 0:
        raise RuntimeError, 'javac process failed'

try:
    makejavaclass("first")
    import pr101j
    ret = pr101j.doit()
    if ret <> 'first':
        print 'unexpected first doit() result:', ret

    makejavaclass("second")
    pr101j = reload(pr101j)
    ret = pr101j.doit()
    if ret <> 'second':
        print 'unexpected second doit() result:', ret

    makejavaclass("third")
    pr101j = reload(pr101j)
    ret = pr101j.doit()
    if ret <> 'third':
        print 'unexpected third doit() result:', ret

finally:
    classfile = os.path.splitext(javafile)[0]+'.class'
    os.unlink(javafile)
    os.unlink(classfile)
