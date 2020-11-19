/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.otel.bridge;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.api.BaggageManager;
import org.springframework.cloud.sleuth.autoconfig.SleuthBaggageProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class Slf4jSpanProcessor implements SpanProcessor, ApplicationListener {

	private static final Log log = LogFactory.getLog(Slf4jSpanProcessor.class);

	private final SleuthBaggageProperties sleuthBaggageProperties;

	private final BeanFactory beanFactory;

	private BaggageManager baggageManager;

	public Slf4jSpanProcessor(SleuthBaggageProperties sleuthBaggageProperties, BeanFactory beanFactory) {
		this.sleuthBaggageProperties = sleuthBaggageProperties;
		this.beanFactory = beanFactory;
	}

	private BaggageManager baggageManager() {
		if (this.baggageManager == null) {
			this.baggageManager = this.beanFactory.getBean(BaggageManager.class);
		}
		return this.baggageManager;
	}

	@Override
	public void onStart(Context parent, ReadWriteSpan span) {
		onStart(span.getSpanContext().getTraceIdAsHexString(), span.getSpanContext().getSpanIdAsHexString());
	}

	private void onStart(String traceId, String spanId) {
		MDC.put("traceId", traceId);
		MDC.put("spanId", spanId);
		flushAllBaggageEntries();
	}

	@Override
	public boolean isStartRequired() {
		return true;
	}

	@Override
	public void onEnd(ReadableSpan span) {
		MDC.remove("traceId");
		MDC.remove("spanId");
		removeAllBaggageEntries();
	}

	@Override
	public boolean isEndRequired() {
		return true;
	}

	@Override
	public CompletableResultCode shutdown() {
		onEnd(null);
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode forceFlush() {
		flushAllBaggageEntries();
		return CompletableResultCode.ofSuccess();
	}

	private void flushAllBaggageEntries() {
		onEachCorrelatedBaggageEntry(e -> MDC.put(e.getKey(), e.getValue()));
	}

	private void onEachCorrelatedBaggageEntry(Consumer<Map.Entry<String, String>> consumer) {
		if (this.sleuthBaggageProperties.isCorrelationEnabled()) {
			List<String> correlationFields = lowerCaseCorrelationFields();
			baggageManager().getAllBaggage().entrySet().stream()
					.filter(e -> correlationFields.contains(e.getKey().toLowerCase())).forEach(consumer);
		}
	}

	private void removeAllBaggageEntries() {
		onEachCorrelatedBaggageEntry(e -> MDC.remove(e.getKey()));
	}

	private List<String> lowerCaseCorrelationFields() {
		return this.sleuthBaggageProperties.getCorrelationFields().stream().map(String::toLowerCase)
				.collect(Collectors.toList());
	}

	private void onBaggageChanged(OtelBaggageInScope.BaggageChanged event) {
		if (log.isTraceEnabled()) {
			log.trace("Got baggage changed event [" + event + "]");
		}
		if (this.sleuthBaggageProperties.isCorrelationEnabled()
				&& lowerCaseCorrelationFields().contains(event.name.toLowerCase())) {
			if (log.isTraceEnabled()) {
				log.trace("Correlation enabled and baggage with name [" + event.name
						+ "] is present on the list of correlated fields");
			}
			MDC.put(event.name, event.value);
		}
	}

	private void onScopeChanged(OtelCurrentTraceContext.ScopeChanged event) {
		if (log.isTraceEnabled()) {
			log.trace("Got scope changed event [" + event + "]");
		}
		if (event.span != null) {
			onStart(event.span.getSpanContext().getTraceIdAsHexString(),
					event.span.getSpanContext().getSpanIdAsHexString());
		}
	}

	private void onScopeClosed(OtelCurrentTraceContext.ScopeClosed event) {
		if (log.isTraceEnabled()) {
			log.trace("Got scope closed event [" + event + "]");
		}
		onEnd(null);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof OtelBaggageInScope.BaggageChanged) {
			onBaggageChanged((OtelBaggageInScope.BaggageChanged) event);
		}
		else if (event instanceof OtelCurrentTraceContext.ScopeChanged) {
			onScopeChanged((OtelCurrentTraceContext.ScopeChanged) event);
		}
		else if (event instanceof OtelCurrentTraceContext.ScopeClosed) {
			onScopeClosed((OtelCurrentTraceContext.ScopeClosed) event);
		}
	}

}
