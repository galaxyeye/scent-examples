package ai.platon.scent.examples.component

import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.scent.ScentSession
import ai.platon.scent.parse.html.JdbcSinkSqlExtractor
import ai.platon.scent.parse.html.JdbcSinkSqlExtractorConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Depth1Crawler can not see JdbcSinkSqlExtractor, so we have to initialize it here
 * */
@Component
class ParserInitializer {
    private val log = LoggerFactory.getLogger(ParserInitializer::class.java)

    @Autowired
    lateinit var session: ScentSession

    @Value("\${sync.dest.jdbc.driver}")
    var sinkJdbcDriver = ""
    @Value("\${sync.dest.jdbc.url}")
    var sinkJdbcUrl = ""
    @Value("\${sync.dest.jdbc.username}")
    var sinkJdbcUsername = ""
    @Value("\${sync.dest.jdbc.password}")
    var sinkJdbcPassword = ""
    @Value("\${sync.batch.size}")
    var minSyncBatchSize = 40
    @Value("\${extract.store.page.model}")
    var storePageModel = false

    @Autowired
    lateinit var parseFilters: ParseFilters

    val params: Params
        get() = ParserInitializer::class.java.declaredFields
                .filter { it.isAnnotationPresent(Value::class.java) }
                .onEach { it.isAccessible = true }
                .associate { it.name to it.get(this) }
                .let { Params.of(it) }

    fun report() {
        params.withLogger(log).info()
    }

    fun addSqlExtractor(
            urlPattern: String,
            minContentSize: Int,
            minNumNonBlankFields: Int,
            sqlTemplateResource: String,
            sinkTableName: String
    ) {
        val jsseConfig = JdbcSinkSqlExtractorConfig(
                sinkJdbcDriver = sinkJdbcDriver,
                sinkJdbcUrl = sinkJdbcUrl,
                sinkJdbcUsername = sinkJdbcUsername,
                sinkJdbcPassword = sinkJdbcPassword,
                sinkTableName = sinkTableName,
                minSyncBatchSize = minSyncBatchSize,
                minNumNonBlankFields = minNumNonBlankFields
        )

        val extractor = session.pulsarContext.getBean<JdbcSinkSqlExtractor>()
        extractor.session = session.pulsarSession
        extractor.jsseConfig = jsseConfig
        extractor.urlRegex = urlPattern.toRegex()
        extractor.minContentSize = minContentSize
        extractor.sqlTemplateResource = sqlTemplateResource
        extractor.storePageModel = storePageModel

        parseFilters.addLast(extractor)

        log.info("Added extractor | {}", extractor)
    }
}
