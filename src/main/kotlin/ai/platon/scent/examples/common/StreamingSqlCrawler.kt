package ai.platon.scent.examples.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.ScentContext
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import kotlinx.coroutines.*
import oshi.SystemInfo
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

open class StreamingSqlCrawler(
        private val urls: Sequence<String>,
        private val options: LoadOptions = LoadOptions.create(),
        private val context: ScentContext
): CommonSqlExtractor(context) {
    companion object {
        private val metricRegistry = SharedMetricRegistries.getOrCreate(AppConstants.DEFAULT_METRICS_NAME)
        private val numRunningTasks = AtomicInteger()

        init {
            metricRegistry.register(prependReadableClassName(this,"runningTasks"), object: Gauge<Int> {
                override fun getValue(): Int = numRunningTasks.get()
            })
        }
    }

    private val conf = session.sessionConfig
    private val numPrivacyContexts = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)
    private val fetchConcurrency = numPrivacyContexts * conf.getInt(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, AppContext.NCPU)

    private val systemInfo = SystemInfo()
    // OSHI cached the value, so it's fast and safe to be called frequently
    private val availableMemory get() = systemInfo.hardware.memory.available
    private val requiredMemory = 500 * 1024 * 1024L // 500 MiB
    private val memoryRemaining get() = availableMemory - requiredMemory
    private val taskTimeout = Duration.ofMinutes(5)
    private val numTasks = AtomicInteger()
    var onLoadComplete: (WebPage) -> Unit = {}

    open suspend fun run() {
        supervisorScope {
            urls.forEachIndexed { j, url ->
                numTasks.incrementAndGet()

                // update fetch concurrency on command
                if (numRunningTasks.get() == fetchConcurrency) {
                    val path = AppPaths.TMP_CONF_DIR.resolve("fetch-concurrency-override")
                    if (Files.exists(path)) {
                        val concurrencyOverride = Files.readAllLines(path).firstOrNull()?.toIntOrNull()?:fetchConcurrency
                        if (concurrencyOverride != fetchConcurrency) {
                            session.sessionConfig.setInt(CapabilityTypes.FETCH_CONCURRENCY, concurrencyOverride)
                        }
                    }
                }

                while (isAppActive && numRunningTasks.get() >= fetchConcurrency) {
                    delay(1000)
                }

                while (isAppActive && memoryRemaining < 0) {
                    if (j % 20 == 0) {
                        log.info("$j.\tnumRunning: {}, availableMemory: {}, requiredMemory: {}, shortage: {}",
                                numRunningTasks,
                                Strings.readableBytes(availableMemory),
                                Strings.readableBytes(requiredMemory),
                                Strings.readableBytes(abs(memoryRemaining))
                        )
                        session.pulsarContext.clearCaches()
                        System.gc()
                    }
                    delay(1000)
                }

                if (!isAppActive) {

                    return@supervisorScope
                }

                var page: WebPage? = null
                var exception: Throwable? = null
                numRunningTasks.incrementAndGet()
                val context = Dispatchers.Default + CoroutineName("w")
                launch(context) {
                    withTimeout(taskTimeout.toMillis()) {
                        page = session.runCatching { loadDeferred(url, options) }
                                .onFailure { exception = it; log.warn("Load failed - $it") }
                                .getOrNull()
                        page?.also {
                            // TODO: do this before completed page report
                            onLoadComplete(it)
                        }
                        numRunningTasks.decrementAndGet()
                    }
                }

                if (exception is ProxyVendorUntrustedException) {
                    log.error(exception?.message?:"Unexpected error")
                    return@supervisorScope
                }
            }
        }

        log.info("All done. Total $numTasks tasks")
    }
}
