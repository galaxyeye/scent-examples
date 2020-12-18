package ai.platon.scent.examples.tools

import ai.platon.pulsar.ql.h2.H2Db
import ai.platon.pulsar.ql.h2.H2DbConfig
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException

fun main() {
    val dbConf = H2DbConfig(user = "", password = "")
    val db = H2Db(conf = dbConf)
    val conn = db.getConnection("metabase.db")
    val stat = conn.createStatement()
    // Dump from a h2 database
    // echo "SCRIPT TO 'db-dump.sql'" > dump/query.sql
    // Use java -cp pulsar-h2-1.5.3-SNAPSHOT.jar org.h2.tools.RunScript -url "jdbc:h2:file:./metabase.db" -script dump/query.sql -showResults
    val path = Paths.get("/home/vincent/dumps/dom.head1000.sql")
    val sqls = Files.readString(path).split(";\n")
            .asSequence()
            .filterNot { it.startsWith("-- ") }
            .filterNot { it.isBlank() }
            .filterNot { it.contains("SYSTEM_LOB_STREAM") }
    sqls.forEachIndexed { i, sql ->
        try {
            stat.execute(sql)
        } catch (e: SQLException) {
            println(">>>")
            println(sql)
            println("<<<")
        }
    }
}
