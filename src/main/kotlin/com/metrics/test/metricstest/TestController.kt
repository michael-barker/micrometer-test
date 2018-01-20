package com.metrics.test.metricstest

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.AsyncRestTemplate
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.concurrent.CompletableFuture

private typealias FutureMetricsMap = CompletableFuture<ResponseEntity<String>>

@EnableAsync
@RestController
class TestController(
    private val restTemplate: RestTemplate,
    private val asyncRestTemplate: AsyncRestTemplate,
    private val objectMapper: ObjectMapper
) {
    @RequestMapping("/foo/{responseCode}", method = [GET, POST])
    fun foo(@PathVariable responseCode: Int) =
        ResponseEntity(getRequestMetersAsString(), HttpStatus.valueOf(responseCode))

    @RequestMapping("/bar/{responseCode}", method = [GET, POST])
    fun bar(@PathVariable responseCode: Int) =
        ResponseEntity(getRequestMetersAsString(), HttpStatus.valueOf(responseCode))

    @RequestMapping("/future/{responseCode}", method = [GET, POST])
    fun future(@PathVariable responseCode: Int): FutureMetricsMap =
        CompletableFuture.supplyAsync {
            ResponseEntity(getRequestMetersAsString(), HttpStatus.valueOf(responseCode))
        }

    @RequestMapping("/upstream/{responseCode}", method = [GET, POST])
    fun upstream(
        @PathVariable responseCode: Int,
        @RequestParam(required = false) downstreamResponseCode: Int?,
        @RequestParam(required = false) useAsyncRestTemplate: Boolean?
    ): ResponseEntity<String> {
        makeDownstreamRequest(useAsyncRestTemplate, downstreamResponseCode ?: responseCode)
        return ResponseEntity(getRequestMetersAsString(), HttpStatus.valueOf(responseCode))
    }

    @RequestMapping("/downstream/{responseCode}", method = [GET, POST])
    fun downstream(@PathVariable responseCode: Int) =
        ResponseEntity<Unit>(HttpStatus.valueOf(responseCode))

    private fun makeDownstreamRequest(useAsyncRestTemplate: Boolean?, downstreamCode: Int) {
        if (useAsyncRestTemplate == true) {
            asyncRestTemplate.getForEntity(
                "http://localhost:8080/downstream/{responseCode}",
                Unit::class.java,
                downstreamCode
            )
        } else {
            restTemplate.getForEntity(
                "http://localhost:8080/downstream/{responseCode}",
                Unit::class.java,
                downstreamCode
            )
        }
    }

    private fun getRequestMetersAsString(): String {
//        (Metrics.globalRegistry.registries.first{ it is SimpleMeterRegistry } as SimpleMeterRegistry)
//            .meters
//            .forEach {
//                println("${it.id}::${it.type()}: ${it.measure()}")
//            }

        Thread.sleep((50..200).random().toLong())
        val dropWizardMeterRegistry =
            Metrics
                .globalRegistry
                .registries
                .first { it is DropwizardMeterRegistry } as DropwizardMeterRegistry

        return dropWizardMeterRegistry
            .dropwizardRegistry
            .metrics
            .filter { it.key.contains("provider=kroger") }
            .map { metric ->
                """
                |${metric.key}
                |   ${objectMapper.writeValueAsString(metric.value)}
                """.trimMargin()
            }.joinToString("\n\n")
    }
}

private object RandomRangeSingleton : Random()

private fun ClosedRange<Int>.random() = RandomRangeSingleton.nextInt((endInclusive + 1) - start) + start