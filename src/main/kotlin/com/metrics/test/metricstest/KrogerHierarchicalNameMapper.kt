package com.metrics.test.metricstest

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.core.instrument.util.HierarchicalNameMapper

class KrogerHierarchicalNameMapper : HierarchicalNameMapper {

    private var inboundHttpRequestsMetricName = "http.server.requests"
    private var outboundHttpRequestsMetricName = "http.client.requests"

    override fun toHierarchicalName(id: Meter.Id, convention: NamingConvention): String =
        when (id.name) {
            inboundHttpRequestsMetricName -> buildInboundHttpRequestName(id)
            outboundHttpRequestsMetricName -> buildOutboundHttpRequestName(id)
            else -> HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention)
        }

    private fun buildInboundHttpRequestName(id: Meter.Id): String {
        val method = id.tags.first { it.key == "method" }.value
        val uri = id.tags.first { it.key == "uri" }.value
        val status = id.tags.first { it.key == "status" }.value
        return "provider=kroger,destination=$method::$uri,result=${status.mapStatusToMetricResult()},direction=IN,protocol=http"
    }

    private fun buildOutboundHttpRequestName(id: Meter.Id): String {
        val method = id.tags.first { it.key == "method" }.value
        val uri = id.tags.first { it.key == "uri" }.value
        val status = id.tags.first { it.key == "status" }.value
        return "provider=kroger,destination=$method::$uri,result=${status.mapStatusToMetricResult()},direction=OUT,protocol=http"
    }

    // TODO Extract this so it's configurable
    // TODO Investigate RestTemplateExchangeTagsProvider as an alternative solution for response codes
    private fun String.mapStatusToMetricResult() =
        when(this.toInt()) {
            in 200..399 -> "SUCCESS"
            in 400..499 -> "BAD_REQUEST"
            503 -> "TIMEOUT"
            in 500..599 -> "FAILURE"
            else -> "UNKNOWN"
        }
}
