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

/**
 * @author Marcin Grzejszczak
 */
public class TraceContinuingCallable<V> extends TraceCallable<V> implements Callable<V> {

	public TraceContinuingCallable(Tracer tracer, Callable<V> delegate) {
		super(tracer, delegate);
	}

	@Override
	protected Span startSpan() {
		return getTracer().continueSpan(getParent());
	}

	@Override
	protected void close(Span span) {
		getTracer().detach(span);
	}
}
