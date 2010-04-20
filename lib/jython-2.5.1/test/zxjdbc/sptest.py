
# Jython Database Specification API 2.0
#
# $Id: sptest.py 6647 2009-08-10 17:23:22Z fwierzbicki $
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

from zxtest import zxCoreTestCase

class OracleSPTest(zxCoreTestCase):

    def setUp(self):
        zxCoreTestCase.setUp(self)

        c = self.cursor()

        try:
            try:
                c.execute("drop table sptest")
            except:
                self.db.rollback()
            try:
                c.execute("create table sptest (x varchar2(20))")
                c.execute("create or replace procedure procnone is begin insert into sptest values ('testing'); end;")
                c.execute("create or replace procedure procin (y in varchar2) is begin insert into sptest values (y); end;")
                c.execute("create or replace procedure procout (y out varchar2) is begin y := 'tested'; end;")
                c.execute("create or replace procedure procinout (y out varchar2, z in varchar2) is begin insert into sptest values (z); y := 'tested'; end;")
                c.execute("create or replace function funcnone return varchar2 is begin return 'tested'; end;")
                c.execute("create or replace function funcin (y varchar2) return varchar2 is begin return y || y; end;")
                c.execute("create or replace function funcout (y out varchar2) return varchar2 is begin y := 'tested'; return 'returned'; end;")
                self.db.commit()
            except:
                self.db.rollback()
                self.fail("procedure creation failed")

            self.proc_errors("PROC")
            self.proc_errors("FUNC")

        finally:
            c.close()

    def tearDown(self):
        zxCoreTestCase.tearDown(self)

    def proc_errors(self, name):
        c = self.cursor()
        try:
            c.execute("select * from user_errors where name like '%s%%'" % (name.upper()))
            errors = c.fetchall()
            try:
                assert not errors, "found errors"
            except AssertionError, e:
                print "printing errors:"
                for a in errors:
                    print a
                raise e
        finally:
            c.close()

    def testCursor(self):
        c = self.cursor()
        try:

            c.execute("insert into sptest values ('a')")
            c.execute("insert into sptest values ('b')")
            c.execute("insert into sptest values ('c')")
            c.execute("insert into sptest values ('d')")
            c.execute("insert into sptest values ('e')")

            c.execute("""
                    CREATE OR REPLACE PACKAGE types
                    AS
                            TYPE ref_cursor IS REF CURSOR;
                    END;
            """)

            c.execute("""
                    CREATE OR REPLACE FUNCTION funccur(v_x IN VARCHAR)
                            RETURN types.ref_cursor
                    AS
                            funccur_cursor types.ref_cursor;
                    BEGIN
                            OPEN funccur_cursor FOR
                                    SELECT x FROM sptest WHERE x < v_x;
                            RETURN funccur_cursor;
                    END;
            """)

            self.proc_errors("funccur")

            c.callproc("funccur", ("z",))
            data = c.fetchall()
            self.assertEquals(5, len(data))
            c.callproc("funccur", ("c",))
            data = c.fetchall()
            self.assertEquals(2, len(data))

        finally:
            c.close()

    def testProcin(self):
        c = self.cursor()
        try:
            params = ["testProcin"]
            c.callproc("procin", params)
            self.assertEquals([], c.fetchall())
            c.execute("select * from sptest")
            self.assertEquals(1, len(c.fetchall()))
        finally:
            c.close()

    def testProcinout(self):
        c = self.cursor()
        try:
            params = [None, "testing"]
            c.callproc("procinout", params)
            data = c.fetchone()
            assert data is None, "data was not None"
            c.execute("select * from sptest")
            data = c.fetchone()
            self.assertEquals("testing", data[0])
            self.assertEquals("tested", params[0])
        finally:
            c.close()

    def testFuncnone(self):
        c = self.cursor()
        try:
            c.callproc("funcnone")
            data = c.fetchone()
            assert data is not None, "data was None"
            self.assertEquals(1, len(data))
            self.assertEquals("tested", data[0])
        finally:
            c.close()

    def testFuncin(self):
        c = self.cursor()
        try:
            params = ["testing"]
            c.callproc("funcin", params)
            self.assertEquals(1, c.rowcount)
            data = c.fetchone()
            assert data is not None, "data was None"
            self.assertEquals(1, len(data))
            self.assertEquals("testingtesting", data[0])
        finally:
            c.close()

    def testCallingWithKws(self):
        c = self.cursor()
        try:
            params = ["testing"]
            c.callproc("funcin", params=params)
            self.assertEquals(1, c.rowcount)
            data = c.fetchone()
            assert data is not None, "data was None"
            self.assertEquals(1, len(data))
            self.assertEquals("testingtesting", data[0])
        finally:
            c.close()

    def testFuncout(self):
        c = self.cursor()
        try:
            params = [None]
            c.callproc("funcout", params)
            data = c.fetchone()
            assert data is not None, "data was None"
            self.assertEquals(1, len(data))
            self.assertEquals("returned", data[0])
            self.assertEquals("tested", params[0].strip())
        finally:
            c.close()

    def testMultipleFetch(self):
        """testing the second fetch call to a callproc() is None"""
        c = self.cursor()
        try:
            c.callproc("funcnone")
            data = c.fetchone()
            assert data is not None, "data was None"
            data = c.fetchone()
            assert data is None, "data was not None"
        finally:
            c.close()

class SQLServerSPTest(zxCoreTestCase):

    def testProcWithResultSet(self):
        c = self.cursor()
        try:
            for a in (("table", "sptest"), ("procedure", "sp_proctest")):
                try:
                    c.execute("drop %s %s" % (a))
                except:
                    pass

            c.execute("create table sptest (a int, b varchar(32))")
            c.execute("insert into sptest values (1, 'hello')")
            c.execute("insert into sptest values (2, 'there')")
            c.execute("insert into sptest values (3, 'goodbye')")

            c.execute(""" create procedure sp_proctest (@A int) as select a, b from sptest where a <= @A """)
            self.db.commit()

            c.callproc("sp_proctest", (2,))
            data = c.fetchall()
            self.assertEquals(2, len(data))
            self.assertEquals(2, len(c.description))
            assert c.nextset() is not None, "expected an additional result set"
            data = c.fetchall()
            self.assertEquals(1, len(data))
            self.assertEquals(1, len(c.description))
        finally:
            c.close()

#       def testSalesByCategory(self):
#               c = self.cursor()
#               try:
#                       c.execute("use northwind")
#                       c.callproc(("northwind", "dbo", "SalesByCategory"), ["Seafood", "1998"])
#                       data = c.fetchall()
#                       assert data is not None, "no results from SalesByCategory"
#                       assert len(data) > 0, "expected numerous results"
#               finally:
#                       c.close()
