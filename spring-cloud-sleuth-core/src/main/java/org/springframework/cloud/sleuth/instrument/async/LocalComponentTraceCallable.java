/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.async;

import java.util.concurrent.Callable;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.TraceKeys;

/**
 *
 * Callable that starts a span that is a local component span.
 *
 * @author Marcin Grzejszczak
 */
public class LocalComponentTraceCallable<V> extends LocalComponentTraceDelegate<Callable<V>> implements Callable<V> {

	public LocalComponentTraceCallable(Tracer tracer, TraceKeys traceKeys, Callable<V> delegate) {
		super(tracer, traceKeys, delegate);
	}

	public LocalComponentTraceCallable(Tracer tracer, TraceKeys traceKeys, Callable<V> delegate, String name) {
		super(tracer, traceKeys, delegate, name);
	}

	@Override
	public V call() throws Exception {
		Span span = startSpan();
		try {
			return this.getDelegate().call();
		}
		finally {
			close(span);
		}
	}
}
