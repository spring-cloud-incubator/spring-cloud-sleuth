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

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.TraceKeys;

/**
 *
 * Runnable that starts a span that is a local component span.
 *
 * @author Marcin Grzejszczak
 */
public class LocalComponentTraceRunnable extends LocalComponentTraceDelegate<Runnable> implements Runnable {

	public LocalComponentTraceRunnable(Tracer tracer, TraceKeys traceKeys, Runnable delegate) {
		super(tracer, traceKeys, delegate);
	}

	public LocalComponentTraceRunnable(Tracer tracer, TraceKeys traceKeys, Runnable delegate, String name) {
		super(tracer, traceKeys, delegate, name);
	}

	@Override
	public void run() {
		Span span = startSpan();
		try {
			this.getDelegate().run();
		}
		finally {
			close(span);
		}
	}
}
