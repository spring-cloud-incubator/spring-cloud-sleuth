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

package org.springframework.cloud.sleuth.instrument.kafka;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.springframework.cloud.sleuth.propagation.Propagator;

public class TracingKafkaPropagatorSetter implements Propagator.Setter<ProducerRecord<?, ?>> {

	private static final Log log = LogFactory.getLog(TracingKafkaPropagatorSetter.class);

	@Override
	public void set(ProducerRecord<?, ?> carrier, String key, String value) {
		carrier.headers().add(key, value.getBytes());
	}

}
