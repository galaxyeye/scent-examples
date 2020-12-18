package ai.platon.scent.examples.misc.mysql

import ai.platon.scent.common.sql.SqlUtils
import java.sql.DriverManager

object MysqlCon {
    fun testConnect() = try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val url = "jdbc:mysql://mysql0:3306/mallbigdata_us_test?autoReconnect=true&DB_CLOSE_ON_EXIT=false"
        val con = DriverManager.getConnection(url, "mytest", "123456@")
        // here sonoo is database name, root is username and password
        val stmt = con.createStatement()
        val rs = stmt.executeQuery("select 1+1, 2*6, 'end'")
        while (rs.next()) println(rs.getString(1) + "  " + rs.getString(2) + "  " + rs.getString(3))
        con.close()
    } catch (e: Exception) {
        println(e)
    }

    fun convertSql() {
        val sql = SqlUtils.loadConvertSql("/sql/x-items-convert.sql")
        println(sql)
    }
}

fun main() {
    MysqlCon.testConnect()
}
