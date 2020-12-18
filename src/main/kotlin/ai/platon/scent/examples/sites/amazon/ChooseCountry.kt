package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.scent.context.withContext

fun main(args: Array<String>) {
    val chooseLanguageUrl = "https://www.amazon.com/gp/customer-preferences/select-language"
    var portalUrl = "https://www.amazon.com/"
    var loadArguments = "-i 1s -retry"

    var i = 0
    while (i++ < args.size - 1) {
        if (args[i] == "-url") portalUrl = args[i++]
        if (args[i] == "-args") loadArguments = args[i++]
    }

    val options = LoadOptions.parse(loadArguments)
    val chooseLanguageExpressions = """
document.querySelector("input[value=en_US]").click();
document.querySelector("span#icp-btn-save input[type=submit]").click();
    """.trimIndent()

//    val zipcode = listOf("10001", "10002", "10003", "10004", "90002", "90003", "90004", "90005").shuffled().first()
    // New York City
    val zipcode = listOf("10002", "10002", "10003", "10004", "10005", "10006").shuffled().first()
    val chooseDistrictExpressions = ResourceLoader.readString("sites/amazon/js/choose-district.js")
            .replace("10001", zipcode)
            .split(";\n")
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("// ") }
            .joinToString(";\n")

    withContext { cx ->
        System.clearProperty(CapabilityTypes.BROWSER_LAUNCH_SUPERVISOR_PROCESS)
        System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyContextIdGenerator")

        val unmodifiedConfig = (cx as AbstractPulsarContext).unmodifiedConfig.unbox()
//        unmodifiedConfig.unset(CapabilityTypes.BROWSER_LAUNCH_SUPERVISOR_PROCESS)
//        unmodifiedConfig.set(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

        val session = cx.createSession().apply { disableCache() }
        options.retryFailed = true

        // 1. warn up
        val page = session.load(portalUrl, options)
        page.protocolStatus = ProtocolStatus.STATUS_NOTFETCHED
        page.crawlStatus = CrawlStatus.STATUS_UNFETCHED

        var document = session.parse(page)
        var text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        println("Current area: $text")

        // 2. choose language
        unmodifiedConfig.set(CapabilityTypes.FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE, chooseLanguageExpressions)
        session.load(chooseLanguageUrl, options)

        // 3. choose district
        unmodifiedConfig.set(CapabilityTypes.FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE, chooseDistrictExpressions)
        session.load(portalUrl, options)

        // 4. check the result
        unmodifiedConfig.unset(CapabilityTypes.FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE)
        document = session.loadDocument(portalUrl, options)

        text = document.selectFirstOrNull("#nav-tools a span.icp-nav-flag")?.attr("class") ?: "(unknown)"
        println("Current country: $text")

        text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        println("Current area: $text")
        val path = session.export(document)
        println("Exported to file://$path")
    }
}
