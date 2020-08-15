package ai.platon.scent.examples.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.h2.H2Db
import ai.platon.pulsar.ql.h2.H2DbConfig
import ai.platon.scent.ScentContext
import ai.platon.scent.common.message.ScentMiscMessageWriter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.h2.tools.SimpleResultSet
import java.sql.Connection
import java.sql.ResultSet
import java.util.concurrent.ArrayBlockingQueue

/**
 * The base class for all tests.
 */
open class CommonSqlExtractor(context: ScentContext): Crawler(context) {

    val messageWriter = session.pulsarContext.getBean<ScentMiscMessageWriter>()

    val sessionFactory = ai.platon.scent.ql.h2.H2SessionFactory::class.java.name
    val dbConfig = H2DbConfig().apply { memory = true; multiThreaded = true }
    val connection = H2Db(sessionFactory, dbConfig).getRandomConnection()
    val stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
    val connectionPool = ArrayBlockingQueue<Connection>(1000)
    val randomConnection get() = H2Db(sessionFactory, dbConfig).getRandomConnection()

    fun allocateDbConnections(concurrent: Int) {
        runBlocking {
            repeat(concurrent) { launch { connectionPool.add(randomConnection) } }
        }
    }

    fun execute(sql: String, printResult: Boolean = true, formatAsList: Boolean = false) {
        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
                val rs = stat.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs, asList = formatAsList))
                }
            } else {
                val r = stat.execute(sql)
                if (printResult) {
                    println(r)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun query(sql: String, printResult: Boolean = true, withHeader: Boolean = true): ResultSet {
        try {
            val rs = stat.executeQuery(sql)
            if (printResult) {
                println(ResultSetFormatter(rs, withHeader = withHeader))
            }
            return rs
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return SimpleResultSet()
    }

    override fun close() {
        connectionPool.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }
        super.close()
    }
}
