package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.alwaysTrue
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.LoadingProxyPool
import ai.platon.scent.proxy.pool.ProxyVendorLoader
import java.time.Duration

fun main() {
    val conf = ImmutableConfig()
    val proxyLoader = ProxyVendorLoader(conf)

    proxyLoader.updateProxies(Duration.ofSeconds(1))

    if (alwaysTrue()) {
        return
    }

    val proxyPool = LoadingProxyPool(proxyLoader, conf)

    var cmd = ""
    var q = false

    while (!q) {
        print("Get next proxy?y:")
        cmd = readLine()?:""
        cmd = cmd.trim().toLowerCase()

        when (cmd) {
            "y" -> println(proxyPool.take())
            "q" -> q = true
        }
    }
}
