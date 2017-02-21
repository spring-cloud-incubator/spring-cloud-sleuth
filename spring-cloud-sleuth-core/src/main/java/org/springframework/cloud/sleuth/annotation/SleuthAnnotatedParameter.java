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
package org.springframework.cloud.sleuth.annotation;

/**
 * A container class that holds information about the parameter
 * of the annotated method argument.
 *
 * @author Christian Schwerdtfeger
 * @since 1.2.0
 */
class SleuthAnnotatedParameter {

	private int parameterIndex;

	private SpanTag annotation;

	private Object argument;

	SpanTag getAnnotation() {
		return this.annotation;
	}

	void setAnnotation(SpanTag annotation) {
		this.annotation = annotation;
	}

	Object getArgument() {
		return this.argument;
	}

	void setArgument(Object argument) {
		this.argument = argument;
	}

	int getParameterIndex() {
		return this.parameterIndex;
	}

	void setParameterIndex(int parameterIndex) {
		this.parameterIndex = parameterIndex;
	}

}
