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

import org.springframework.cloud.sleuth.api.ScopedSpan;
import org.springframework.cloud.sleuth.api.TraceContext;

class NoOpScopedSpan implements ScopedSpan {

	@Override
	public boolean isNoop() {
		return false;
	}

	@Override
	public TraceContext context() {
		return new NoOpTraceContext();
	}

	@Override
	public ScopedSpan name(String name) {
		return this;
	}

	@Override
	public ScopedSpan tag(String key, String value) {
		return this;
	}

	@Override
	public ScopedSpan annotate(String value) {
		return this;
	}

	@Override
	public ScopedSpan error(Throwable throwable) {
		return this;
	}

	@Override
	public void finish() {

	}

}