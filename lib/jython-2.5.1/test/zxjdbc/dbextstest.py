
# Jython Database Specification API 2.0
#
# $Id: dbextstest.py 6647 2009-08-10 17:23:22Z fwierzbicki $
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

import dbexts, runner, tempfile, os
from random import random

class dbextsTestCase(runner.SQLTestCase):

    def setUp(self):
        template = """
                [default]
                name=dbexts_test

                [jdbc]
                name=dbexts_test
                url=%s
                user=%s
                pwd=%s
                driver=%s
        """

        args = {}
        for arg in self.factory.arguments:
            args[arg[0]] = arg[1]

        template = template % (args["url"], args["usr"], args["pwd"], args["driver"])
        if hasattr(self, "datahandler"):
            template += "\tdatahandler=%s" % (self.datahandler.__name__)
        template = os.linesep.join(template.split())

        try:
            fp = open(tempfile.mktemp(), "w")
            fp.write(template)
            fp.close()
            self.db = dbexts.dbexts(cfg=fp.name)
            self.db.verbose = 0
            for table in ("one", "two"):
                try: self.db.raw("drop table %s" % (table))
                except: pass
            self.db.raw("create table one (a int, b int, c varchar(32))")
            self.db.raw("create table two (a int, b int, c varchar(32))")
        finally:
            try:
                os.remove(fp.name)
            except:
                pass

    def tearDown(self):
        self.db.raw("drop table one")
        self.db.raw("drop table two")
        self.db.close()

    def testChoose(self):
        """testing choose()"""
        r = dbexts.choose(1, 4, 5)
        assert r == 4, "choose failed, expected 4, got %d" % r

    def _insertInto(self, table, num):
        for i in range(0, num):
            self.db.raw("insert into %s (a, b, c) values (?, ?, ?)" % (table), [(i, random()*100+i, "%s" % (random()*100+i))])

    def testSqlFailure(self):
        """testing isql with sql exception"""
        try:
            self.db.isql("select * from __garbage__")
            self.fail("expected SQL exception")
        except:
            pass

    def testSqlFailureWithBeginCommit(self):
        """testing failure with begin/commit"""
        failed = 0
        c = self.db.begin()
        try:
            try:
                c.execute("select * from __garbage__")
            except:
                failed = 1
        finally:
            self.db.commit(c)

        if not failed:
            self.fail("expected SQL exception")

    def testSqlWithBeginCommit(self):
        """testing begin/commit"""
        self._insertInto("two", 30)
        c = self.db.begin()
        c.execute("select * from two")
        f = c.fetchall()
        c.close()
        self.db.commit()
        assert len(f) == 30, "expected [30], got [%d]" % (len(f))

    def testQueryMultipleReturnSets(self):
        """testing multiple return sets"""
        self._insertInto("two", 30)
        h, r = self.db.raw("select * from two where a = ?", [(0,), (3,)])
        assert len(r) == 2, "expected [2], got [%d]" % (len(r))

    def testUpdateCount(self):
        """testing update count"""

        self._insertInto("one", 45)
        self.db.raw("delete from one where a > ?", [(12,)])
        self.assertEquals(32, self.db.updatecount)

    def testQueryWithMaxRows(self):
        """testing query with max rows"""

        self._insertInto("one", 45)
        self.db.raw("select * from one", maxrows=3)
        self.assertEquals(3, len(self.db.results))
        self.db.raw("select * from one where a > ?", [(12,)], maxrows=3)
        self.assertEquals(3, len(self.db.results))

    def testBulkcopy(self):
        """testing bcp"""

        self._insertInto("two", 3)

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a'])
        assert len(bcp.columns) == 1, "one column should be specified, [%d] found" % (len(bcp.columns))

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a', 'b', 'c'], exclude=['a'])
        assert len(bcp.columns) == 2, "expected two columns, found [%d]" % (len(bcp.columns))
        a = filter(lambda x, c=bcp.columns: x in c, ['b', 'c'])
        assert a, "expecting ['b', 'c'], found %s" % (str(a))

        class _executor:
            def __init__(self, table, cols):
                self.cols = cols
                if cols:
                    self.sql = "insert into %s (%s) values (%s)" % (table, ",".join(self.cols), ",".join(("?",) * len(self.cols)))
                else:
                    self.sql = "insert into %s values (%%s)" % (table)
            def execute(self, db, rows, bindings):
                assert len(rows) > 0, "must have at least one row"
                if self.cols:
                    sql = self.sql
                else:
                    sql = self.sql % (",".join(("?",) * len(rows[0])))

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a'], executor=_executor)
        done = bcp.transfer(self.db)
        assert done == 3, "expecting three rows to be handled but not inserted, found [%d]" % (done)

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a'])
        done = bcp.transfer(self.db)
        assert done == 3, "expecting three rows to be inserted, found [%d]" % (done)

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a'])
        bcp.rowxfer([200])
        bcp.rowxfer([201])
        bcp.rowxfer([202])
        bcp.rowxfer([203])
        done = bcp.batch()
        assert done == 4, "expecting four rows to be inserted, found [%d]" % (done)
        bcp.rowxfer([300])
        bcp.rowxfer([401])
        bcp.rowxfer([502])
        bcp.rowxfer([603])
        done = bcp.batch()
        assert done == 4, "expecting four rows to be inserted, found [%d]" % (done)
        bcp.rowxfer([205])
        bcp.rowxfer([210])
        done = bcp.done()
        assert done == 2, "expecting two rows to be inserted, found [%d]" % (done)
        assert bcp.total == 10, "expecting 10 rows to be inserted, found [%d]" % (bcp.total)

        bcp = self.db.bulkcopy("dbexts_test", "two", include=['a'])
        done = bcp.transfer(self.db)
        assert done == 16, "expecting sixteen rows to be inserted, found [%d]" % (done)

    def testTable(self):
        """testing dbexts.table(tabname)"""
        self.db.table("one")
        assert self.db.results is not None, "results were None"
        self.assertEquals(3, len(self.db.results))
        self.db.table()
        found = 0
        for a in self.db.results:
            if a[2].lower() in ("one", "two"): found += 1
        self.assertEquals(2, found)

    def testOut(self):
        """testing dbexts.out"""
        self.db.verbose = 1
        fp = open(tempfile.mktemp(), "w")
        try:
            self.db.out = fp
            self.db.raw("insert into one (a, b) values (?, ?)", [(1, 2), (3, 4)])
            self.db.isql("select * from one")
            self.db.verbose = 0
            fp.close()
            fp = open(fp.name, "r")
            data = fp.read()
            assert len(data), "expected file to contain output"
        finally:
            fp.close()
            os.remove(fp.name)

    def testResultSetWrapper(self):
        """testing result set wrapper"""
        from dbexts import ResultSet
        self._insertInto("two", 30)
        h, r = self.db.raw("select * from two where a in (?, ?, ?, ?) order by a", [(12,15,17,8)])
        assert len(r) == 4, "expected [4], got [%d]" % (len(r))
        rs = ResultSet(map(lambda x: x[0], h), r)
        assert len(rs[0]) == 3, "expected [3], got [%d]" % (len(rs[0]))
        assert rs[0]['a'] == 8, "expected [8], got [%s]" % (rs[0]['a'])
        assert rs[0]['A'] == 8, "expected [8], got [%s]" % (rs[0]['A'])
        assert len(rs[0]['b':]) == 2, "expected [2], got [%s]" % (len(rs[0]['b':]))
        assert len(rs[0]['a':'b']) == 1, "expected [1], got [%s]" % (len(rs[0]['a':'b']))

    def testMultipleResultSetConcatentation(self):
        """testing multiple result sets with some resulting in None"""
        self._insertInto("two", 30)
        # first is non None
        h, r = self.db.raw("select * from two where a = ?", [(12,),(8001,),(15,),(17,),(8,),(9001,)])
        assert len(r) == 4, "expected [4], got [%d]" % (len(r))
        # first is None
        h, r = self.db.raw("select * from two where a = ?", [(1200,),(8001,),(15,),(17,),(8,),(9001,)])
        assert len(r) == 3, "expected [3], got [%d]" % (len(r))

    def testBulkcopyWithDynamicColumns(self):
        """testing bcp with dynamic column names"""

        self.testBulkcopy()

        bcp = self.db.bulkcopy("dbexts_test", "two", exclude=['a'])
        assert len(bcp.columns) == 2, "expected two columns, found [%d]" % (len(bcp.columns))
        a = filter(lambda x, c=bcp.columns: x in c, ['b', 'c'])
        assert a == ['b', 'c'], "expecting ['b', 'c'], found %s" % (str(a))

        bcp = self.db.bulkcopy("dbexts_test", "two")
        done = bcp.transfer(self.db)
        assert done == 32, "expecting thirty two rows to be inserted, found [%d]" % (done)

    def testAutocommit(self):
        """testing the autocommit functionality"""
        for u in (0, 1):
            self.db.autocommit = u
            try:
                self.db.isql("select * from one")
            except Exception, e:
                fail("failed autocommit query with u=[%d], v=[%d]" % (u, v))
            for v in (0, 1):
                self.db.db.autocommit = v
                try:
                    self.db.isql("select * from one")
                except Exception, e:
                    self.fail("failed autocommit query with u=[%d], v=[%d]" % (u, v))

    def testPrepare(self):
        """testing the handling of a prepared statement"""
        self._insertInto("one", 10)
        p = self.db.prepare("select * from one")
        self.db.isql(p)
        self.db.isql(p)
        p.close()
        assert p.closed
