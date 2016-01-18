/*
 * Copyright 2013-2015 the original author or authors.
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

package sample;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author Spencer Gibb
 */
@Component
public class SampleBackground {

	@Autowired
	private TraceManager traceManager;
	@Autowired
	private Random random;

	@SneakyThrows
	@Async
	public void background() {
		int millis = random.nextInt(1000);
		Thread.sleep(millis);
		this.traceManager.addAnnotation("background-sleep-millis", String.valueOf(millis));
	}

}
