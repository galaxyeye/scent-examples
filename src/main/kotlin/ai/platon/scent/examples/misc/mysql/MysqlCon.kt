package ai.platon.scent.examples.misc.mysql

import ai.platon.scent.common.SqlUtils
import java.sql.DriverManager

object MysqlCon {
    fun testConnect(args: Array<String>) = try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val con = DriverManager.getConnection("jdbc:mysql://47.103.79.201:3306/mallbigdata_us", "mytest", "123456@")
        // here sonoo is database name, root is username and password
        val stmt = con.createStatement()
        val rs = stmt.executeQuery("select * from asin")
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
    MysqlCon.convertSql()
}
