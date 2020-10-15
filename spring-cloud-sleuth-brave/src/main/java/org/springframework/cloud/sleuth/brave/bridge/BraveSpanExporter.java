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

package org.springframework.cloud.sleuth.brave.bridge;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;

import org.springframework.cloud.sleuth.api.exporter.ReportedSpan;
import org.springframework.cloud.sleuth.api.exporter.SpanExporter;

public class BraveSpanExporter extends SpanHandler implements SpanExporter {

	@Override
	public boolean export(ReportedSpan span) {
		return false;
	}

	@Override
	public boolean end(TraceContext context, MutableSpan span, Cause cause) {
		return super.end(context, span, cause);
	}

}