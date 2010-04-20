
# Jython Database Specification API 2.0
#
# $Id: zxtest.py 6647 2009-08-10 17:23:22Z fwierzbicki $
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

from com.ziclix.python.sql import zxJDBC
from java.util import Calendar, Date as JDate

import tempfile, os, time, runner

class zxCoreTestCase(runner.SQLTestCase):

    def setUp(self):
        runner.SQLTestCase.setUp(self)
        self.db = self.connect()
        self.db.autocommit = 0

    def tearDown(self):
        self.db.close()
        runner.SQLTestCase.tearDown(self)

    def connect(self):
        factory = runner.__imp__(self.factory.classname)
        args = map(lambda x: x[1], self.factory.arguments)
        connect = getattr(factory, self.factory.method)
        return apply(connect, args, self.factory.keywords)

    def cursor(self, *args, **kws):
        c = apply(self.db.cursor, args, kws)
        if hasattr(self, "datahandler"):
            c.datahandler = self.datahandler(c.datahandler)
        return c

class zxJDBCTestCase(zxCoreTestCase):

    def setUp(self):
        zxCoreTestCase.setUp(self)

        c = self.cursor()

        try:
            c.execute("drop table zxtesting")
            self.db.commit()
        except:
            self.db.rollback()

        try:
            c.execute("create table zxtesting (id int not null, name varchar(32), state varchar(32), primary key (id))")
            self.db.commit()
            c.execute("insert into zxtesting (id, name, state) values (1, 'test0', 'il')")
            c.execute("insert into zxtesting (id, name, state) values (2, 'test1', 'wi')")
            c.execute("insert into zxtesting (id, name, state) values (3, 'test2', 'tx')")
            c.execute("insert into zxtesting (id, name, state) values (4, 'test3', 'co')")
            c.execute("insert into zxtesting (id, name, state) values (5, 'test4', 'il')")
            c.execute("insert into zxtesting (id, name, state) values (6, 'test5', 'ca')")
            c.execute("insert into zxtesting (id, name, state) values (7, 'test6', 'wi')")
            self.db.commit()
        finally:
            c.close()

    def tearDown(self):

        c = self.cursor()
        try:
            try:
                c.execute("drop table zxtesting")
            except:
                self.db.rollback()
        finally:
            c.close()

        zxCoreTestCase.tearDown(self)

class zxAPITestCase(zxJDBCTestCase):

    def testConnection(self):
        """testing connection"""
        assert self.db, "invalid connection"

    def testAutocommit(self):
        """testing autocommit functionality"""
        if self.db.__connection__.getMetaData().supportsTransactions():
            self.db.autocommit = 1
            self.assertEquals(1, self.db.__connection__.getAutoCommit())
            self.db.autocommit = 0
            self.assertEquals(0, self.db.__connection__.getAutoCommit())

    def testSimpleQuery(self):
        """testing simple queries with cursor.execute(), no parameters"""
        c = self.cursor()
        try:
            c.execute("select count(*) from zxtesting")
            f = c.fetchall()
            assert len(f) == 1, "expecting one row"
            c.execute("select * from zxtesting")
            data = c.fetchone()
            assert len(f) == 1, "expecting one row"
            assert data[0] == 1, "expected [1] rows, got [%d]" % (data[0])
        finally:
            c.close()

    def testNoneQuery(self):
        """testing that executing None doesn't fail"""
        c = self.cursor()
        try:
            c.execute(None)
        finally:
            c.close()

    def _test_preparedstatement(self, dynamic):
        c = self.cursor(dynamic)
        try:
            p = c.prepare("select * from zxtesting where id = ?")
            for i in range(1, 8):
                c.execute(p, (i,))
                data = c.fetchall()
                self.assertEquals(1, len(data))
            assert not p.closed
            p.close()
            assert p.closed
            self.assertRaises(zxJDBC.ProgrammingError, c.execute, p, (1,))
        finally:
            c.close()

    def testStaticPrepare(self):
        """testing the prepare() functionality for static cursors"""
        self._test_preparedstatement(0)

    def testDynamicPrepare(self):
        """testing the prepare() functionality for dynamic cursors"""
        self._test_preparedstatement(1)

    def _test_cursorkeywords(self, *args, **kws):
        c = self.cursor(*args, **kws)
        try:
            c.execute("select * from zxtesting")
            data = c.fetchmany(1)
            assert len(data) == 1, "expecting one row"
        finally:
            c.close()

    def testCursorKeywords(self):
        """testing the creation of a cursor with keywords"""
        self._test_cursorkeywords(dynamic=1)
        self._test_cursorkeywords(dynamic=1,
                rstype=zxJDBC.TYPE_SCROLL_INSENSITIVE,
                rsconcur=zxJDBC.CONCUR_READ_ONLY
        )
        self._test_cursorkeywords(1,
                rstype=zxJDBC.TYPE_SCROLL_INSENSITIVE,
                rsconcur=zxJDBC.CONCUR_READ_ONLY
        )
        self._test_cursorkeywords(1,zxJDBC.TYPE_SCROLL_INSENSITIVE,zxJDBC.CONCUR_READ_ONLY)
        self.assertRaises(TypeError, self.cursor, 1, zxJDBC.TYPE_SCROLL_INSENSITIVE)

    def testFileLikeCursor(self):
        """testing the cursor as a file-like object"""
        c = self.cursor()
        try:
            print >> c, "insert into zxtesting (id, name, state) values (100, 'test100', 'wa')"
            print >> c, "insert into zxtesting (id, name, state) values (101, 'test101', 'co')"
            print >> c, "insert into zxtesting (id, name, state) values (102, 'test102', 'or')"
            self.db.commit()
        finally:
            c.close()

        c = self.cursor()
        try:
            c.execute("select * from zxtesting where id in (100, 101, 102)")
            f = c.fetchall()
            self.assertEquals(3, len(f))
        finally:
            c.close()

    def testIteration(self):
        """testing the iteration protocol"""
        c = self.cursor()
        try:
            # first with a for loop
            cnt = 0
            c.execute("select * from zxtesting")
            for a in c:
                self.assertEquals(3, len(a))
                cnt += 1
            self.assertEquals(7, cnt)
            # then with a while loop
            cnt = 0
            c.execute("select * from zxtesting")
            while 1:
                try:
                    self.assertEquals(3, len(c.next()))
                except StopIteration:
                    break
                cnt += 1
            self.assertEquals(7, cnt)
        finally:
            c.close()

    def testClosingCursor(self):
        """testing that a closed cursor throws an exception"""
        c = self.cursor()
        try:
            c.execute("select * from zxtesting")
        finally:
            c.close()
        self.assertRaises(zxJDBC.ProgrammingError, c.execute, ("select * from zxtesting",))

    def testClosingConnectionWithOpenCursors(self):
        """testing that a closed connection closes any open cursors"""
        c = self.cursor()
        d = self.cursor()
        e = self.cursor()
        self.db.close()
        # open a new connection so the tearDown can run
        self.db = self.connect()
        self.assertRaises(zxJDBC.ProgrammingError, c.execute, ("select * from zxtesting",))
        self.assertRaises(zxJDBC.ProgrammingError, d.execute, ("select * from zxtesting",))
        self.assertRaises(zxJDBC.ProgrammingError, e.execute, ("select * from zxtesting",))

    def testNativeSQL(self):
        """testing the connection's ability to convert sql"""
        sql = self.db.nativesql("select * from zxtesting where id = ?")
        assert sql is not None
        assert len(sql) > 0

    def testTables(self):
        """testing cursor.tables()"""
        c = self.cursor()
        try:
            c.tables(None, None, None, None)
            # let's look for zxtesting
            found = 0
            while not found:
                try:
                    found = "zxtesting" == c.next()[2].lower()
                except StopIteration:
                    break
            assert found, "expected to find 'zxtesting'"
            c.tables(None, None, "zxtesting", None)
            self.assertEquals(1, len(c.fetchall()))
            c.tables(None, None, "zxtesting", ("TABLE",))
            self.assertEquals(1, len(c.fetchall()))
            c.tables(None, None, "zxtesting", ("table",))
            self.assertEquals(1, len(c.fetchall()))
        finally:
            c.close()

    def testColumns(self):
        """testing cursor.columns()"""
        c = self.cursor()
        try:
            # deliberately copied so as to produce useful line numbers

            c.columns(None, None, "zxtesting", None)
            f = c.fetchall()
            self.assertEquals(3, c.rowcount)
            f.sort(lambda x, y: cmp(x[3], y[3]))
            self.assertEquals("name", f[1][3].lower())

            # if the db engine handles mixed case, then don't ask about a different
            #  case because it will fail
            if not self.db.__connection__.getMetaData().storesMixedCaseIdentifiers():
                c.columns(None, None, "ZXTESTING", None)
                f = c.fetchall()
                self.assertEquals(3, c.rowcount)
                f.sort(lambda x, y: cmp(x[3], y[3]))
                self.assertEquals("name", f[1][3].lower())
        finally:
            c.close()

    def testBestRow(self):
        """testing bestrow which finds the optimal set of columns that uniquely identify a row"""
        c = self.cursor()
        try:
            # we're really just testing that this doesn't blow up
            c.bestrow(None, None, "zxtesting")
            f = c.fetchall()
            if f: # we might as well see that it worked
                self.assertEquals(1, len(f))
        finally:
            c.close()

    def testTypeInfo(self):
        """testing cursor.gettypeinfo()"""
        c = self.cursor()
        try:
            c.gettypeinfo()
            f = c.fetchall()
            assert f is not None, "expected some type information, got None"
            # this worked prior to the Fetch re-write, now the client will have to bear the burden, sorry
            #c.gettypeinfo(zxJDBC.INTEGER)
            #f = c.fetchall()
            #assert f[0][1] == zxJDBC.INTEGER, "expected [%d], got [%d]" % (zxJDBC.INTEGER, f[0][1])
        finally:
            c.close()

    def _test_scrolling(self, dynamic=0):
        if self.vendor.scroll:
            c = self.cursor(dynamic,
                    rstype=getattr(zxJDBC, self.vendor.scroll),
                    rsconcur=zxJDBC.CONCUR_READ_ONLY
            )
        else:
            c = self.cursor(dynamic)

        try:
            # set everything up
            c.execute("select id, name, state from zxtesting order by id")
            self.assertEquals(1, c.fetchone()[0])
            self.assertEquals(2, c.fetchone()[0])
            self.assertEquals(3, c.fetchone()[0])
            # move back two and fetch the row again
            c.scroll(-2)
            self.assertEquals(2, c.fetchone()[0])
            # move to the fifth row (0-based indexing)
            c.scroll(4, "absolute")
            self.assertEquals(5, c.fetchone()[0])
            # move back to the start
            c.scroll(-5)
            self.assertEquals(1, c.fetchone()[0])
            # move to the end
            c.scroll(6, "absolute")
            self.assertEquals(7, c.fetchone()[0])
            # make sure we get an IndexError
            self.assertRaises(IndexError, c.scroll, 1, "relative")
            self.assertRaises(IndexError, c.scroll, -1, "absolute")
            self.assertRaises(zxJDBC.ProgrammingError, c.scroll, 1, "somethingelsealltogether")
        finally:
            c.close()

    def testDynamicCursorScrolling(self):
        """testing the ability to scroll a dynamic cursor"""
        self._test_scrolling(1)

    def testStaticCursorScrolling(self):
        """testing the ability to scroll a static cursor"""
        self._test_scrolling(0)

    def _test_rownumber(self, dynamic=0):
        if self.vendor.scroll:
            c = self.cursor(dynamic,
                    rstype=getattr(zxJDBC, self.vendor.scroll),
                    rsconcur=zxJDBC.CONCUR_READ_ONLY
            )
        else:
            c = self.cursor(dynamic)

        try:
            if not dynamic:
                # a dynamic cursor doesn't know if any rows really exist
                # maybe the 'possibility' of rows should change .rownumber to 0?
                c.execute("select * from zxtesting where 1=0")
                self.assertEquals(0, c.rownumber)
            c.execute("select * from zxtesting")
            self.assertEquals(0, c.rownumber)
            c.next()
            self.assertEquals(1, c.rownumber)
            c.next()
            self.assertEquals(2, c.rownumber)
            c.scroll(-1)
            self.assertEquals(1, c.rownumber)
            c.scroll(2, "absolute")
            self.assertEquals(2, c.rownumber)
            c.scroll(6, "absolute")
            self.assertEquals(6, c.rownumber)
        finally:
            c.close()
        self.assertEquals(None, c.rownumber)

    def testStaticRownumber(self):
        """testing a static cursor's rownumber"""
        self._test_rownumber(0)

    def testDynamicRownumber(self):
        """testing a dynamic cursor's rownumber"""
        self._test_rownumber(1)

    def _test_rowcount(self, dynamic=0):
        if self.vendor.scroll:
            c = self.cursor(dynamic,
                    rstype=getattr(zxJDBC, self.vendor.scroll),
                    rsconcur=zxJDBC.CONCUR_READ_ONLY
            )
        else:
            c = self.cursor(dynamic)

        try:
            c.execute("select * from zxtesting")
            c.next()
            c.next()
            c.next()
            if dynamic:
                # dynamic cursors only know about the number of rows encountered
                self.assertEquals(3, c.rowcount)
            else:
                self.assertEquals(7, c.rowcount)
            c.scroll(-1)
            # make sure they don't change just because we scrolled backwards
            if dynamic:
                # dynamic cursors only know about the number of rows encountered
                self.assertEquals(3, c.rowcount)
            else:
                self.assertEquals(7, c.rowcount)
        finally:
            c.close()

    def testStaticRowcount(self):
        """testing a static cursor's rowcount"""
        self._test_rowcount(0)

    def testDynamicRowcount(self):
        """testing a dynamic cursor's rowcount"""
        self._test_rowcount(1)

    def testTableTypeInfo(self):
        """testing cursor.gettabletypeinfo()"""
        c = self.cursor()
        try:
            c.gettabletypeinfo()
            c.fetchall()
            assert c.rowcount > 0, "expected some table types"
        finally:
            c.close()

    def testTupleParams(self):
        """testing the different ways to pass params to execute()"""
        c = self.cursor()
        try:
            self.assertRaises(zxJDBC.ProgrammingError, c.execute, "select * from zxtesting where id = ?", params=4)
            c.execute("select * from zxtesting where id = ?", params=[4])
            c.execute("select * from zxtesting where id = ?", params=(4,))
        finally:
            c.close()

    def testConnectionAttribute(self):
        """testing the getting and setting of cursor.connection"""
        c = self.cursor()
        try:
            from com.ziclix.python.sql import PyConnection
            assert isinstance(c.connection, PyConnection), "expected PyConnection"
            self.assertRaises(TypeError, setattr, (c, "connection", None), None)
        finally:
            c.close()

    def testFetchMany(self):
        """testing cursor.fetchmany()"""
        c = self.cursor()
        try:
            c.execute("select * from zxtesting")
            data = c.fetchmany(6)
            assert len(data) == 6, "expected [6] rows, got [%d]" % (len(data))
            c.execute("select * from zxtesting")
            data = c.fetchmany(16)
            assert len(data) == 7, "expected [7] rows, got [%d]" % (len(data))
        finally:
            c.close()

    def testQueryWithParameter(self):
        """testing query by parameter"""
        c = self.cursor()
        try:
            c.execute("select name from zxtesting where state = ?", [("il",)], {0:zxJDBC.VARCHAR})
            data = c.fetchall()
            assert len(data) == 2, "expected [2] rows, got [%d]" % (len(data))
            c.execute("select name from zxtesting where state = ?", [("co",)], {0:zxJDBC.VARCHAR})
            data = c.fetchall()
            assert len(data) == 1, "expected [1] row, got [%d]" % (len(data))
        finally:
            c.close()

    def testInsertWithFile(self):
        """testing insert with file"""
        assert self.has_table("texttable"), "missing attribute texttable"
        fp = open(tempfile.mktemp(), "w")
        c = self.cursor()
        try:
            try:
                c.execute(self.table("texttable")[1])
                data = fp.name * 300
                data = data[:3500]
                fp.write(data)
                fp.flush()
                fp.close()
                fp = open(fp.name, "r")
                c.execute("insert into %s (a, b) values (?, ?)" % (self.table("texttable")[0]), [(0, fp)], {1:zxJDBC.LONGVARCHAR})
                self.db.commit()
                c.execute("select b from %s" % (self.table("texttable")[0]))
                f = c.fetchall()
                assert len(f) == 1, "expected [1] row, got [%d]" % (len(f))
                assert len(f[0][0]) == len(data), "expected [%d], got [%d]" % (len(data), len(f[0][0]))
                assert data == f[0][0], "failed to retrieve the same text as inserted"
            except Exception, e:
                raise e
        finally:
            c.execute("drop table %s" % (self.table("texttable")[0]))
            c.close()
            self.db.commit()
            fp.close()
            os.remove(fp.name)

    def calendar(self):
        c = Calendar.getInstance()
        c.setTime(JDate())
        return c

    def testDate(self):
        """testing creation of Date"""

        # Java uses milliseconds and Python uses seconds, so adjust the time accordingly
        # seeded with Java
        c = self.calendar()
        o = zxJDBC.DateFromTicks(c.getTime().getTime() / 1000L)
        v = zxJDBC.Date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DATE))
        assert o.equals(v), "incorrect date conversion using java, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

        # seeded with Python
        t = time.time()
        l = time.localtime(t)
        o = zxJDBC.DateFromTicks(t)
        v = zxJDBC.Date(l[0], l[1], l[2])
        assert o.equals(v), "incorrect date conversion, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

    def testTime(self):
        """testing creation of Time"""

        # Java uses milliseconds and Python uses seconds, so adjust the time accordingly

        # seeded with Java
        c = self.calendar()
        o = zxJDBC.TimeFromTicks(c.getTime().getTime() / 1000L)
        v = zxJDBC.Time(c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
        assert o.equals(v), "incorrect date conversion using java, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

        # seeded with Python
        #t = time.time()
        #l = time.localtime(t)
        #o = zxJDBC.TimeFromTicks(t)
        #v = zxJDBC.Time(l[3], l[4], l[5])
        #assert o.equals(v), "incorrect date conversion using python, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

    def testTimestamp(self):
        """testing creation of Timestamp"""

        # Java uses milliseconds and Python uses seconds, so adjust the time accordingly

        # seeded with Java
        c = self.calendar()
        o = zxJDBC.TimestampFromTicks(c.getTime().getTime() / 1000L)
        v = zxJDBC.Timestamp(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DATE),
                c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
        assert o.equals(v), "incorrect date conversion using java, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

        # seeded with Python
        #t = time.time()
        #l = time.localtime(t)
        #o = zxJDBC.TimestampFromTicks(t)
        #v = zxJDBC.Timestamp(l[0], l[1], l[2], l[3], l[4], l[5])
        #assert o.equals(v), "incorrect date conversion using python, got [%ld], expected [%ld]" % (v.getTime(), o.getTime())

    def _test_precision(self, (tabname, sql), diff, values, attr):

        try:
            c = self.cursor()
            try:
                c.execute("drop table %s" % (tabname))
                self.db.commit()
            except:
                self.db.rollback()
        finally:
            c.close()

        try:
            c = self.cursor()
            c.execute(sql)
            c.execute("insert into %s (a, b) values (?, ?)" % (tabname), map(lambda x: (0, x), values))
            c.execute("select a, b from %s" % (tabname))
            f = c.fetchall()
            assert len(values) == len(f), "mismatched result set length"
            for i in range(0, len(f)):
                v = values[i]
                if attr: v = getattr(v, attr)()
                msg = "expected [%0.10f], got [%0.10f] for index [%d] of [%d]" % (v, f[i][1], (i+1), len(f))
                assert diff(f[i][1], values[i]) < 0.01, msg
            self.db.commit()
        finally:
            c.close()
            try:
                c = self.cursor()
                try:
                    c.execute("drop table %s" % (tabname))
                    self.db.commit()
                except:
                    self.db.rollback()
            finally:
                c.close()

    def testFloat(self):
        """testing value of float"""
        assert self.has_table("floattable"), "missing attribute floattable"
        values = [4.22, 123.44, 292.09, 33.2, 102.00, 445]
        self._test_precision(self.table("floattable"), lambda x, y: x-y, values, None)

    def testBigDecimal(self):
        """testing value of BigDecimal"""
        assert self.has_table("floattable"), "missing attribute floattable"
        from java.math import BigDecimal
        values = [BigDecimal(x).setScale(2, BigDecimal.ROUND_UP) for x in [4.22, 123.44, 292.09, 33.2, 102.00, 445]]
        self._test_precision(self.table("floattable"), lambda x, y, b=BigDecimal: b(x).subtract(y).doubleValue(), values, "doubleValue")

    def testBigDecimalConvertedToDouble(self):
        """testing value of BigDecimal when converted to double"""
        assert self.has_table("floattable"), "missing attribute floattable"
        from java.math import BigDecimal
        values = [BigDecimal(x).setScale(2, BigDecimal.ROUND_UP) for x in [4.22, 123.44, 292.09, 33.2, 102.00, 445]]
        self._test_precision(self.table("floattable"), lambda x, y: x - y.doubleValue(), values, "doubleValue")

    def testNextset(self):
        """testing nextset"""
        c = self.cursor()
        try:
            c.execute("select * from zxtesting where id = ?", [(3,), (4,)])
            f = c.fetchall()
            assert f, "expected results, got None"
            assert len(f) == 1, "expected [1], got [%d]" % (len(f))
            assert c.nextset(), "expected next set, got None"
            f = c.fetchall()
            assert f, "expected results after call to nextset(), got None"
            assert len(f) == 1, "expected [1], got [%d]" % (len(f))
        finally:
            c.close()

    def testJavaUtilList(self):
        """testing parameterized values in a java.util.List"""
        c = self.cursor()
        try:
            from java.util import LinkedList
            a = LinkedList()
            a.add((3,))
            c.execute("select * from zxtesting where id = ?", a)
            f = c.fetchall()
            assert len(f) == 1, "expected [1], got [%d]" % (len(f))
        finally:
            c.close()

    def testUpdateCount(self):
        """testing update count functionality"""
        c = self.cursor()
        try:
            c.execute("insert into zxtesting values (?, ?, ?)", [(500, 'bz', 'or')])
            assert c.updatecount == 1, "expected [1], got [%d]" % (c.updatecount)
            c.execute("select * from zxtesting")
            self.assertEquals(None, c.updatecount)
            # there's a *feature* in the mysql engine where it returns 0 for delete if there is no
            #  where clause, regardless of the actual value.  using a where clause forces it to calculate
            #  the appropriate value
            c.execute("delete from zxtesting where 1>0")
            assert c.updatecount == 8, "expected [8], got [%d]" % (c.updatecount)
            c.execute("update zxtesting set name = 'nothing'")
            self.assertEquals(0, c.updatecount)
        finally:
            c.close()

    def _test_time(self, (tabname, sql), factory, values, _type, _cmp=cmp, datahandler=None):
        c = self.cursor()
        if datahandler: c.datahandler = datahandler(c.datahandler)
        try:
            c.execute(sql)
            dates = map(lambda x, f=factory: apply(f, x), values)
            for a in dates:
                c.execute("insert into %s values (1, ?)" % (tabname), [(a,)], {0:_type})
            self.db.commit()

            c.execute("select * from %s where b = ?" % (tabname), [(dates[0],)], {0:_type})
            f = c.fetchall()
            assert len(f) == 1, "expected length [1], got [%d]" % (len(f))
            assert _cmp(f[0][1], dates[0]) == 0, "expected date [%s], got [%s]" % (str(dates[0]), str(f[0][1]))

            c.execute("delete from %s where b = ?" % (tabname), [(dates[1],)], {0:_type})
            self.db.commit()

            c.execute("select * from %s" % (tabname))
            f = c.fetchall()
            self.assertEquals(len(f), len(dates) - 1)
        finally:
            c.execute("drop table %s" % (tabname))
            c.close()
            self.db.commit()

    def testUpdateSelectByDate(self):
        """testing insert, update, query and delete by java.sql.Date"""
        assert self.has_table("datetable"), "missing attribute datetable"
        def _cmp_(x, y):
            xt = (x.getYear(), x.getMonth(), x.getDay())
            yt = (y.getYear(), y.getMonth(), y.getDay())
            return not xt == yt
        values = [(1996, 6, 22), (2000, 11, 12), (2000, 1, 12), (1999, 9, 24)]
        self._test_time(self.table("datetable"), zxJDBC.Date, values, zxJDBC.DATE, _cmp_)

    def testUpdateSelectByTime(self):
        """testing insert, update, query and delete by java.sql.Time"""
        assert self.has_table("timetable"), "missing attribute timetable"
        def _cmp_(x, y):
            xt = (x.getHours(), x.getMinutes(), x.getSeconds())
            yt = (y.getHours(), y.getMinutes(), y.getSeconds())
            return not xt == yt
        values = [(10, 11, 12), (3, 1, 12), (22, 9, 24)]
        self._test_time(self.table("timetable"), zxJDBC.Time, values, zxJDBC.TIME, _cmp_)

    def testUpdateSelectByTimestamp(self):
        """testing insert, update, query and delete by java.sql.Timestamp"""
        assert self.has_table("timestamptable"), "missing attribute timestamptable"
        def _cmp_(x, y):
            xt = (x.getYear(), x.getMonth(), x.getDay(), x.getHours(), x.getMinutes(), x.getSeconds())
            yt = (y.getYear(), y.getMonth(), y.getDay(), y.getHours(), y.getMinutes(), y.getSeconds())
            return not xt == yt
        values = [(1996, 6, 22, 10, 11, 12), (2000, 11, 12, 3, 1, 12), (2001, 1, 12, 4, 9, 24)]
        self._test_time(self.table("timestamptable"), zxJDBC.Timestamp, values, zxJDBC.TIMESTAMP, _cmp_)

    def testOrderOfArgsMaxRowsOnly(self):
        """testing execute with max rows only"""
        c = self.cursor()
        try:

            # maxrows only (SAPDB doesn't support maxrows as of version 7.2.0)
            c.execute("select * from zxtesting", maxrows=3)
            f = c.fetchall()
            assert len(f) == 3, "expected length [3], got [%d]" % (len(f))

        finally:
            c.close()
            self.db.commit()

    def testOrderOfArgs(self):
        """testing execute with different argument orderings"""
        c = self.cursor()
        try:

            # bindings and params flipped
            c.execute("select * from zxtesting where id = ?", bindings={0:zxJDBC.INTEGER}, params=[(3,)])
            f = c.fetchall()
            assert len(f) == 1, "expected length [1], got [%d]" % (len(f))

            # bindings and params flipped, empty params
            c.execute("select * from zxtesting where id = ?", bindings={}, params=[(3,)])
            f = c.fetchall()
            assert len(f) == 1, "expected length [1], got [%d]" % (len(f))

            # bindings and params flipped, empty params, empty bindings
            c.execute("select * from zxtesting where id = 3", bindings={}, params=[])
            f = c.fetchall()
            assert len(f) == 1, "expected length [1], got [%d]" % (len(f))

        finally:
            c.close()
            self.db.commit()

    def testMaxrows(self):
        """testing maxrows"""
        c = self.cursor()
        try:

            c.execute("select * from zxtesting", maxrows=3)
            self.assertEquals(3, len(c.fetchall()))

            c.execute("select * from zxtesting where id > ?", (1,), maxrows=3)
            self.assertEquals(3, len(c.fetchall()))

            c.execute("select count(*) from zxtesting")
            f = c.fetchall()
            num = f[0][0]

            c.execute("select * from zxtesting", maxrows=0)
            self.assertEquals(num, len(c.fetchall()))

        finally:
            c.close()
            self.db.commit()

    def testPrimaryKey(self):
        """testing for primary key information"""
        c = self.cursor()
        try:
            c.primarykeys(None, None, "zxtesting")
            f = c.fetchall()
            assert len(f) == 1, "expected [1], got [%d]" % (len(f))
            assert f[0][3].lower() == "id", "expected [id], got [%s]" % (f[0][3])
        finally:
            c.close()
            self.db.commit()

    def testForeignKey(self):
        """testing for foreign key information"""
        pass

    def testIndexInfo(self):
        """testing index information"""
        c = self.cursor()
        try:
            c.statistics(None, None, "zxtesting", 0, 0)
            f = c.fetchall()
            assert f is not None, "expected some values"
            # filter out any indicies with name None
            f = filter(lambda x: x[5], f)
            assert len(f) == 1, "expected [1], got [%d]" % (len(f))
        finally:
            c.close()

    def _test_fetching(self, dynamic=0):
        c = self.cursor(dynamic)
        try:
            # make sure None if the result is an empty result set
            c.execute("select * from zxtesting where 1<0")
            self.assertEquals(None, c.fetchone())
            # make sure an empty sequence if the result is an empty result set
            c.execute("select * from zxtesting where 1<0")
            self.assertEquals([], c.fetchmany())
            # make sure an empty sequence if the result is an empty result set
            c.execute("select * from zxtesting where 1<0")
            self.assertEquals([], c.fetchall())
            # test some arraysize features
            c.execute("select * from zxtesting")
            f = c.fetchmany()
            assert len(f) == c.arraysize, "expecting [%d] rows, got [%d]" % (c.arraysize, len(f))
            c.execute("select * from zxtesting")
            c.arraysize = 4
            f = c.fetchmany()
            assert len(f) == 4, "expecting [4] rows, got [%d]" % (len(f))
            c.execute("select * from zxtesting")
            c.arraysize = -1
            f = c.fetchmany()
            assert len(f) == 7, "expecting [7] rows, got [%d]" % (len(f))
        finally:
            c.close()

    def testStaticFetching(self):
        """testing various static fetch methods"""
        self._test_fetching(0)

    def testDynamicFetching(self):
        """testing various dynamic fetch methods"""
        self._test_fetching(1)

    def testFetchingBeforeExecute(self):
        """testing fetch methods before execution"""
        c = self.cursor()
        try:
            try:
                c.fetchall()
            except zxJDBC.Error, e:
                pass
            else:
                self.fail("excepted exception calling fetchall() prior to execute()")
        finally:
            c.close()

    def testBindingsWithNoParams(self):
        """testing bindings with no params"""
        c = self.cursor()
        try:
            self.assertRaises(zxJDBC.ProgrammingError, c.execute, "select * from zxtesting", {0:zxJDBC.INTEGER})

            # test an inappropriate value for a binding
            self.assertRaises(zxJDBC.ProgrammingError, c.execute, "select * from zxtesting", {0:{}})
        finally:
            c.close()

    def testDynamicCursor(self):
        """testing dynamic cursor queries"""
        c = self.cursor(1)
        try:
            c.execute("select * from zxtesting")
            f = c.fetchmany(4)
            assert len(f) == 4, "expected [4] rows, got [%d]" % (len(f))
        finally:
            c.close()

    def testRowid(self):
        """testing the autoincrement facilities of the different handlers"""
        assert self.has_table("autoincrementtable"), "no autoincrement table"

        c = self.cursor()
        assert c.lastrowid == None, "expected initial lastrowid to be None"

        try:

            tabname, sql = self.table("autoincrementtable")
            c.execute(sql)

            c.execute("insert into %s (b) values (?)" % (tabname), [(0,)])
            assert c.lastrowid is not None, "lastrowid is None"

            try:
                for idx in range(c.lastrowid + 1, c.lastrowid + 25):
                    c.execute("insert into %s (b) values (?)" % (tabname), [(idx,)])
                    assert c.lastrowid is not None, "lastrowid is None"
                    self.assertEquals(idx, c.lastrowid)
            except:
                self.db.rollback()

        finally:
            if self.has_table("post_autoincrementtable"):
                try:
                    sequence, sql = self.table("post_autoincrementtable")
                    c.execute(sql)
                    self.db.commit()
                except:
                    self.db.rollback()

            try:
                c.execute("drop table %s" % (tabname))
                self.db.commit()
            except:
                self.db.rollback()

            self.db.commit()
            c.close()

    def _test_fetchapi(self, dynamic=0):
        """Test the public Java API for Fetch"""
        from com.ziclix.python.sql import Fetch, WarningListener
        cur = self.cursor()
        try:
            c = self.db.__connection__
            stmt = c.prepareStatement("select * from zxtesting where id < ?")
            stmt.setInt(1, 5)
            rs = stmt.executeQuery()
            fetch = Fetch.newFetch(cur.datahandler, dynamic)
            class WL(WarningListener):
                def warning(self, event):
                    raise event.getWarning()
            wl = WL()
            fetch.addWarningListener(wl)
            # the RS is closed by Fetch
            fetch.add(rs)

            if not dynamic:
                self.assertEquals(4, fetch.getRowCount())
            assert fetch.fetchone()
            assert fetch.fetchmany(2)
            assert fetch.fetchall()
            assert not fetch.fetchall()
            self.assertEquals(4, fetch.getRowCount())
            assert fetch.removeWarningListener(wl)
            fetch.close()
            stmt.close()
        finally:
            cur.close()

    def testStaticFetchAPI(self):
        """Test static Java Fetch API"""
        self._test_fetchapi(0)

    def testDynamicFetchAPI(self):
        """Test dynamic Java Fetch API"""
        self._test_fetchapi(1)

class LOBTestCase(zxJDBCTestCase):

    def _test_blob(self, obj=0):
        assert self.has_table("blobtable"), "no blob table"
        tabname, sql = self.table("blobtable")
        fn = tempfile.mktemp()
        fp = None

        c = self.cursor()
        try:

            hello = ("hello",) * 1024

            c.execute(sql)
            self.db.commit()

            from java.io import FileOutputStream, FileInputStream, ObjectOutputStream, ObjectInputStream, ByteArrayInputStream

            fp = FileOutputStream(fn)
            oos = ObjectOutputStream(fp)
            oos.writeObject(hello)
            fp.close()

            fp = FileInputStream(fn)
            blob = ObjectInputStream(fp)
            value = blob.readObject()
            fp.close()
            assert hello == value, "unable to serialize properly"

            if obj == 1:
                fp = open(fn, "rb")
            else:
                fp = FileInputStream(fn)

            c.execute("insert into %s (a, b) values (?, ?)" % (tabname), [(0, fp)], {1:zxJDBC.BLOB})
            self.db.commit()

            c.execute("select * from %s" % (tabname))
            f = c.fetchall()
            bytes = f[0][1]
            blob = ObjectInputStream(ByteArrayInputStream(bytes)).readObject()

            assert hello == blob, "blobs are not equal"

        finally:
            c.execute("drop table %s" % (tabname))
            c.close()
            self.db.commit()
            if os.path.exists(fn):
                if fp:
                    fp.close()
                os.remove(fn)

    def testBLOBAsString(self):
        """testing BLOB as string"""
        self._test_blob()

    def testBLOBAsPyFile(self):
        """testing BLOB as PyFile"""
        self._test_blob(1)

    def _test_clob(self, asfile=0):
        assert self.has_table("clobtable"), "no clob table"
        tabname, sql = self.table("clobtable")

        c = self.cursor()
        try:
            hello = "hello" * 1024 * 10

            c.execute(sql)
            self.db.commit()

            if asfile:
                fp = open(tempfile.mktemp(), "w")
                fp.write(hello)
                fp.flush()
                fp.close()
                obj = open(fp.name, "r")
            else:
                obj = hello

            c.execute("insert into %s (a, b) values (?, ?)" % (tabname), [(0, obj)], {1:zxJDBC.CLOB})
            c.execute("select * from %s" % (tabname), maxrows=1)
            f = c.fetchall()
            assert len(f) == 1, "expected [%d], got [%d]" % (1, len(f))
            assert hello == f[0][1], "clobs are not equal"

        finally:
            c.execute("drop table %s" % (tabname))
            c.close()
            self.db.commit()
            if asfile:
                obj.close()
                os.remove(obj.name)

    def testCLOBAsString(self):
        """testing CLOB as string"""
        self._test_clob(0)

    def testCLOBAsPyFile(self):
        """testing CLOB as PyFile"""
        self._test_clob(1)

class BCPTestCase(zxJDBCTestCase):

    def testCSVPipe(self):
        """testing the CSV pipe"""
        from java.io import PrintWriter, FileWriter
        from com.ziclix.python.sql.pipe import Pipe
        from com.ziclix.python.sql.pipe.db import DBSource
        from com.ziclix.python.sql.pipe.csv import CSVSink

        try:
            src = self.connect()
            fn = tempfile.mktemp(suffix="csv")
            writer = PrintWriter(FileWriter(fn))
            csvSink = CSVSink(writer)

            c = self.cursor()
            try:
                c.execute("insert into zxtesting (id, name, state) values (?, ?, ?)", [(1000, 'this,has,a,comma', 'and a " quote')])
                c.execute("insert into zxtesting (id, name, state) values (?, ?, ?)", [(1001, 'this,has,a,comma and a "', 'and a " quote')])
                # ORACLE has a problem calling stmt.setObject(index, null)
                c.execute("insert into zxtesting (id, name, state) values (?, ?, ?)", [(1010, '"this,has,a,comma"', None)], {2:zxJDBC.VARCHAR})
                self.db.commit()
            finally:
                self.db.rollback()
                c.close()

            dbSource = DBSource(src, c.datahandler.__class__, "zxtesting", None, None, None)

            cnt = Pipe().pipe(dbSource, csvSink) - 1 # ignore the header row

        finally:
            writer.close()
            src.close()
            os.remove(fn)

    def testDBPipe(self):
        """testing the DB pipe"""
        from com.ziclix.python.sql.pipe import Pipe
        from com.ziclix.python.sql.pipe.db import DBSource, DBSink

        try:
            src = self.connect()
            dst = self.connect()

            c = self.cursor()
            c.execute("create table zxtestingbcp (id int not null, name varchar(20), state varchar(2), primary key (id))")
            self.db.commit()

            c.execute("select count(*) from zxtesting")
            one = c.fetchone()[0]
            c.close()

            dbSource = DBSource(src, c.datahandler.__class__, "zxtesting", None, None, None)
            dbSink = DBSink(dst, c.datahandler.__class__, "zxtestingbcp", None, None, 1)

            cnt = Pipe().pipe(dbSource, dbSink) - 1 # ignore the header row

            c = self.cursor()
            c.execute("select count(*) from zxtestingbcp")
            two = c.fetchone()[0]
            c.execute("delete from zxtestingbcp")
            self.db.commit()
            c.close()

            assert one == two, "expected [%d] rows in destination, got [%d] (sql)" % (one, two)
            assert one == cnt, "expected [%d] rows in destination, got [%d] (bcp)" % (one, cnt)

            # this tests the internal assert in BCP.  we need to handle the case where we exclude
            # all the rows queried (based on the fact no columns exist) but rows were fetched
            # also make sure (eg, Oracle) that the column name case is ignored
            dbSource = DBSource(src, c.datahandler.__class__, "zxtesting", None, ["id"], None)
            dbSink = DBSink(dst, c.datahandler.__class__, "zxtestingbcp", ["id"], None, 1)

            self.assertRaises(zxJDBC.Error, Pipe().pipe, dbSource, dbSink)

            params = [(4,)]

            dbSource = DBSource(src, c.datahandler.__class__, "zxtesting", "id > ?", None, params)
            dbSink = DBSink(dst, c.datahandler.__class__, "zxtestingbcp", None, None, 1)

            cnt = Pipe().pipe(dbSource, dbSink) - 1 # ignore the header row

            c = self.cursor()
            c.execute("select count(*) from zxtesting where id > ?", params)
            one = c.fetchone()[0]
            c.execute("select count(*) from zxtestingbcp")
            two = c.fetchone()[0]
            c.close()
            assert one == two, "expected [%d] rows in destination, got [%d] (sql)" % (one, two)
            assert one == cnt, "expected [%d] rows in destination, got [%d] (bcp)" % (one, cnt)

        finally:
            try:
                c = self.cursor()
                try:
                    c.execute("drop table zxtestingbcp")
                    self.db.commit()
                except:
                    self.db.rollback()
            finally:
                c.close()

            try:
                src.close()
            except:
                src = None
            try:
                dst.close()
            except:
                dst = None

    def testBCP(self):
        """testing bcp parameters and functionality"""
        from com.ziclix.python.sql.util import BCP

        import dbexts

        try:
            src = self.connect()
            dst = self.connect()

            c = self.cursor()
            c.execute("create table zxtestingbcp (id int not null, name varchar(20), state varchar(2), primary key (id))")
            self.db.commit()

            c.execute("select count(*) from zxtesting")
            one = c.fetchone()[0]
            c.close()

            b = BCP(src, dst)
            if hasattr(self, "datahandler"):
                b.sourceDataHandler = self.datahandler
                b.destinationDataHandler = self.datahandler
            cnt = b.bcp("zxtesting", toTable="zxtestingbcp")

            c = self.cursor()
            c.execute("select count(*) from zxtestingbcp")
            two = c.fetchone()[0]
            c.execute("delete from zxtestingbcp")
            self.db.commit()
            c.close()

            assert one == two, "expected [%d] rows in destination, got [%d] (sql)" % (one, two)
            assert one == cnt, "expected [%d] rows in destination, got [%d] (bcp)" % (one, cnt)

            # this tests the internal assert in BCP.  we need to handle the case where we exclude
            # all the rows queried (based on the fact no columns exist) but rows were fetched
            # also make sure (eg, Oracle) that the column name case is ignored
            self.assertRaises(zxJDBC.Error, b.bcp, "zxtesting", toTable="zxtestingbcp", include=["id"], exclude=["id"])

            params = [(4,)]
            cnt = b.bcp("zxtesting", "id > ?", params, toTable="zxtestingbcp")

            c = self.cursor()
            c.execute("select count(*) from zxtesting where id > ?", params)
            one = c.fetchone()[0]
            c.execute("select count(*) from zxtestingbcp")
            two = c.fetchone()[0]
            c.close()
            assert one == two, "expected [%d] rows in destination, got [%d] (sql)" % (one, two)
            assert one == cnt, "expected [%d] rows in destination, got [%d] (bcp)" % (one, cnt)

        finally:
            try:
                c = self.cursor()
                try:
                    c.execute("drop table zxtestingbcp")
                    self.db.commit()
                except:
                    self.db.rollback()
            finally:
                c.close()

            try:
                src.close()
            except:
                src = None
            try:
                dst.close()
            except:
                dst = None
