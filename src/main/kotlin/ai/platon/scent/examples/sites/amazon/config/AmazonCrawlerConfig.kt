package ai.platon.scent.examples.sites.amazon.config

import ai.platon.scent.ScentContext
import ai.platon.scent.ScentSession
import ai.platon.scent.context.ScentContexts
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.context.annotation.*

@Configuration
@ImportResource("classpath:scent-beans/app-context.xml")
@PropertySource(value = ["classpath:application.properties", "classpath:config/sites/amazon/application-amazon.properties"])
class AmazonCrawlerConfig {

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    fun createSession(): ScentSession {
        return ScentContexts.createSession()
    }
}
