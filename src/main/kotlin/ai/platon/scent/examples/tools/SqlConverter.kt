package ai.platon.scent.examples.tools

import org.apache.commons.lang3.StringUtils

object SqlConverter {
    fun createSql2extractSql(createSql: String): String {
        val prefix = "select\n"
        val postfix = "\nfrom load_and_select(@url, '')"
        return createSql.split("\n")
                .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
                .map { StringUtils.substringBetween(it.trim(), "`", "`") }
                .map { "    dom_first_text(dom, 'div') as `$it`" }
                .joinToString(",\n", prefix, postfix) { it }
    }
}
