
# Jython Database Specification API 2.0
#
# $Id: jndi.py 6647 2009-08-10 17:23:22Z fwierzbicki $
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

"""
        This script is used to bind a JNDI reference for testing purposes only.
"""
from java.util import Hashtable
from org.gjt.mm.mysql import MysqlDataSource
from javax.naming import Context, InitialContext, NameAlreadyBoundException

env = Hashtable()
env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory")

ds = MysqlDataSource()
ds.setServerName("localhost")
ds.setDatabaseName("ziclix")
ds.setPort(3306)

ctx = InitialContext(env)
try:
    try:
        ctx.bind("/jdbc/mysqldb", ds)
    except NameAlreadyBoundException, e:
        ctx.unbind("/jdbc/mysqldb")
        ctx.bind("/jdbc/mysqldb", ds)
finally:
    ctx.close()

print "bound [%s] at /jdbc/mysqldb" % (ds)
