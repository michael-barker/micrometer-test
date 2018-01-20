package com.metrics.test.metricstest

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.dropwizard.DropwizardConfig
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.spring.web.client.MetricsRestTemplateCustomizer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.AsyncRestTemplate
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class MetricsTestApplication {
    @Bean
    fun restTemplate(metricsRegRestTemplateCustomizer: MetricsRestTemplateCustomizer) =
        RestTemplate().also {
            metricsRegRestTemplateCustomizer.customize(it)
        }

    @Bean
    fun asyncRestTemplate(metricsRegRestTemplateCustomizer: MetricsRestTemplateCustomizer) =
        AsyncRestTemplate().also {
            // TODO Instrument AsyncRestTemplate
            // metricsRegRestTemplateCustomizer.customize(it)
        }

    // TODO Do we need Dropwizard or is SimpleMeterRegistry enough?
    // Without Dropwizard then we will probably have to setup Timers to publish percentiles.
    @Bean
    fun dropWizardMeterRegistry() = DropwizardMeterRegistry(
        object : DropwizardConfig {
            override fun get(k: String?) = null
            override fun prefix() = null
        },
        KrogerHierarchicalNameMapper(),
        Clock.SYSTEM
    )
}

fun main(args: Array<String>) {
    SpringApplication.run(MetricsTestApplication::class.java, *args)
}
