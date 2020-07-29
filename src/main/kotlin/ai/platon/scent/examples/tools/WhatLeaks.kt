package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.scent.tools.WhatLeaks
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    val log = LoggerFactory.getLogger(WhatLeaks::class.java)

    Systems.setPropertyIfAbsent(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
    Systems.setPropertyIfAbsent(CapabilityTypes.PROXY_ENABLE_DEFAULT_PROVIDERS, "true")

    WhatLeaks().use {
        it.check()
        if (it.isRTCLeaked) {
            log.info("Web RTC is on (leaked) | ${it.webRTC}")
        }

        Params(
                "WebRTC", it.get("General", "WebRTC"),
                "Ping", it.get("PROXY", "Ping")
        ).merge(it.toParams("General", "PORTS", "BLACKLISTS")).withLogger(log).info()

        readLine()
    }
}
