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

package org.springframework.cloud.sleuth.api.noop;

import org.springframework.cloud.sleuth.api.Span;
import org.springframework.cloud.sleuth.api.TraceContext;

class NoOpSpan implements Span {

	@Override
	public boolean isNoop() {
		return true;
	}

	@Override
	public TraceContext context() {
		return new NoOpTraceContext();
	}

	@Override
	public Span start() {
		return this;
	}

	@Override
	public Span name(String name) {
		return this;
	}

	@Override
	public Span event(String value) {
		return this;
	}

	@Override
	public Span tag(String key, String value) {
		return this;
	}

	@Override
	public Span error(Throwable throwable) {
		return this;
	}

	@Override
	public void end() {

	}

	@Override
	public void abandon() {

	}

}
