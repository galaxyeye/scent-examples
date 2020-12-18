package ai.platon.scent.examples.sites.platonic

import ai.platon.scent.context.withContext
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun main() {
    val seed = "http://www.platonic.fun/"
    val args = "-i 1s -ii 1s -ol a[href~=item] -tl 100"

    withContext {
        val session = it.createSession()
        val document = session.loadDocument(seed, args)

        val solutions = document.select("#solutions div.row > div").map {
            val id = it.id()
            val title = it.selectFirst(".title").text()
            val description = it.selectFirst(".description").text()
            val content = it.selectFirst(".description ~ p").text()

            mapOf("id" to id, "title" to title, "description" to description, "content" to content)
        }

        println(jacksonObjectMapper().writeValueAsString(solutions))
    }
}
