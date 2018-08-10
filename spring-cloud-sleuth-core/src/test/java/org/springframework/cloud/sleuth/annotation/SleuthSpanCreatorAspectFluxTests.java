/*
 * Copyright 2013-2018 the original author or authors.
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

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.util.ArrayListSpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import zipkin2.Annotation;
import zipkin2.reporter.Reporter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.sleuth.annotation.SleuthSpanCreatorAspectFluxTests.TestBean.TEST_STRING1;
import static org.springframework.cloud.sleuth.annotation.SleuthSpanCreatorAspectFluxTests.TestBean.TEST_STRING2;

@SpringBootTest(classes = SleuthSpanCreatorAspectFluxTests.TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SleuthSpanCreatorAspectFluxTests {
	
	@Autowired TestBeanInterface testBean;
	@Autowired Tracer tracer;
	@Autowired ArrayListSpanReporter reporter;
	
	@Before
	public void setup() {
		this.reporter.clear();
		testBean.reset();
	}
	
	@Test
	public void shouldCreateSpanWhenAnnotationOnInterfaceMethod() {
		Flux<String> flux = this.testBean.testMethod();

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("test-method");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWhenAnnotationOnClassMethod() {
		Flux<String> flux = this.testBean.testMethod2();

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("test-method2");
		then(spans.get(0).duration()).isNotZero();
	}
	
	@Test
	public void shouldCreateSpanWithCustomNameWhenAnnotationOnClassMethod() {
		Flux<String> flux = this.testBean.testMethod3();

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method3");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWithCustomNameWhenAnnotationOnInterfaceMethod() {
		Flux<String> flux = this.testBean.testMethod4();

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method4");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWithTagWhenAnnotationOnInterfaceMethod() {
		// tag::execution[]
		Flux<String> flux = this.testBean.testMethod5("test");

		// end::execution[]
		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method5");
		then(spans.get(0).tags()).containsEntry("testTag", "test");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWithTagWhenAnnotationOnClassMethod() {
		Flux<String> flux = this.testBean.testMethod6("test");

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method6");
		then(spans.get(0).tags()).containsEntry("testTag6", "test");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWithLogWhenAnnotationOnInterfaceMethod() {
		Flux<String> flux = this.testBean.testMethod8("test");

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method8");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldCreateSpanWithLogWhenAnnotationOnClassMethod() {
		Flux<String> flux = this.testBean.testMethod9("test");

		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("custom-name-on-test-method9");
		then(spans.get(0).tags())
				.containsEntry("class", "TestBean")
				.containsEntry("method", "testMethod9");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldContinueSpanWithLogWhenAnnotationOnInterfaceMethod() {
		Span span = this.tracer.nextSpan().name("foo");

		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span.start())) {
			Flux<String> flux = this.testBean.testMethod10("test");

			verifyNoSpansUntilFluxComplete(flux);
		} finally {
			span.finish();
		}

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("foo");
		then(spans.get(0).tags())
				.containsEntry("customTestTag10", "test");
		then(spans.get(0).annotations()
				.stream().map(Annotation::value).collect(Collectors.toList()))
				.contains("customTest.before", "customTest.after");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldStartAndCloseSpanOnContinueSpanIfSpanNotSet() {
		Flux<String> flux = this.testBean.testMethod10("test");
		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("test-method10");
		then(spans.get(0).tags())
				.containsEntry("customTestTag10", "test");
		then(spans.get(0).annotations()
				.stream().map(Annotation::value).collect(Collectors.toList()))
				.contains("customTest.before", "customTest.after");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldContinueSpanWhenKeyIsUsedOnSpanTagWhenAnnotationOnInterfaceMethod() {
		Span span = this.tracer.nextSpan().name("foo");

		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span.start())) {
			Flux<String> flux = this.testBean.testMethod10_v2("test");

			verifyNoSpansUntilFluxComplete(flux);
		} finally {
			span.finish();
		}

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("foo");
		then(spans.get(0).tags())
				.containsEntry("customTestTag10", "test");
		then(spans.get(0).annotations()
				.stream().map(Annotation::value).collect(Collectors.toList()))
				.contains("customTest.before", "customTest.after");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldContinueSpanWithLogWhenAnnotationOnClassMethod() {
		Span span = this.tracer.nextSpan().name("foo");

		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span.start())) {
			// tag::continue_span_execution[]
			Flux<String> flux = this.testBean.testMethod11("test");
			// end::continue_span_execution[]
			verifyNoSpansUntilFluxComplete(flux);
		} finally {
			span.finish();
		}

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("foo");
		then(spans.get(0).tags())
				.containsEntry("class", "TestBean")
				.containsEntry("method", "testMethod11")
				.containsEntry("customTestTag11", "test");
		then(spans.get(0).annotations()
				.stream().map(Annotation::value).collect(Collectors.toList()))
				.contains("customTest.before", "customTest.after");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldAddErrorTagWhenExceptionOccurredInNewSpan() {
		try {
			Flux<String> flux = this.testBean.testMethod12("test");

			then(this.reporter.getSpans()).isEmpty();

			flux.toIterable().iterator().next();
		} catch (RuntimeException ignored) {
		}

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("test-method12");
		then(spans.get(0).tags())
				.containsEntry("testTag12", "test")
				.containsEntry("error", "test exception 12");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldAddErrorTagWhenExceptionOccurredInContinueSpan() {
		Span span = this.tracer.nextSpan().name("foo");

		try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span.start())) {
			// tag::continue_span_execution[]
			Flux<String> flux = this.testBean.testMethod13();

			then(this.reporter.getSpans()).isEmpty();

			flux.toIterable().iterator().next();
			// end::continue_span_execution[]
		} catch (RuntimeException ignored) {
		} finally {
			span.finish();
		}

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("foo");
		then(spans.get(0).tags())
				.containsEntry("error", "test exception 13");
		then(spans.get(0).annotations()
				.stream().map(Annotation::value).collect(Collectors.toList()))
				.contains("testMethod13.before", "testMethod13.afterFailure",
						"testMethod13.after");
		then(spans.get(0).duration()).isNotZero();
	}

	@Test
	public void shouldNotCreateSpanWhenNotAnnotated() {
		Flux<String> flux = this.testBean.testMethod7();
		verifyNoSpansUntilFluxComplete(flux);

		List<zipkin2.Span> spans = new ArrayList<>(this.reporter.getSpans());
		then(spans).isEmpty();
	}

	@Test
	public void shouldReturnNewSpanFromTraceContext() {
		Flux<Long> flux = this.testBean.newSpanInTraceContext();
	    Long newSpanId = flux.blockFirst();

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("span-in-trace-context");
		then(spans.get(0).id()).isEqualTo(Long.toHexString(newSpanId));
	}

	@Test
	public void shouldReturnNewSpanFromSubscriberContext() {
		Flux<Long> flux = this.testBean.newSpanInSubscriberContext();
    	Long newSpanId = flux.blockFirst();

		List<zipkin2.Span> spans = this.reporter.getSpans();
		then(spans).hasSize(1);
		then(spans.get(0).name()).isEqualTo("span-in-subscriber-context");
		then(spans.get(0).id()).isEqualTo(Long.toHexString(newSpanId));
	}

	private void verifyNoSpansUntilFluxComplete(Flux<String> flux) {
		Iterator<String> iterator = flux.toIterable().iterator();

		then(this.reporter.getSpans()).isEmpty();
		testBean.proceed();

		String result1 = iterator.next();
		then(result1).isEqualTo(TEST_STRING1);
		then(this.reporter.getSpans()).isEmpty();

		testBean.proceed();
		String result2 = iterator.next();

		then(result2).isEqualTo(TEST_STRING2);
	}

	protected interface TestBeanInterface {

		// tag::annotated_method[]
		@NewSpan
		Flux<String> testMethod();
		// end::annotated_method[]

		Flux<String> testMethod2();

		@NewSpan(name = "interfaceCustomNameOnTestMethod3")
		Flux<String> testMethod3();

		// tag::custom_name_on_annotated_method[]
		@NewSpan("customNameOnTestMethod4")
		Flux<String> testMethod4();
		// end::custom_name_on_annotated_method[]

		// tag::custom_name_and_tag_on_annotated_method[]
		@NewSpan(name = "customNameOnTestMethod5")
		Flux<String> testMethod5(@SpanTag("testTag") String param);
		// end::custom_name_and_tag_on_annotated_method[]

		Flux<String> testMethod6(String test);

		Flux<String> testMethod7();

		@NewSpan(name = "customNameOnTestMethod8")
		Flux<String> testMethod8(String param);

		@NewSpan(name = "testMethod9")
		Flux<String> testMethod9(String param);

		@ContinueSpan(log = "customTest")
		Flux<String> testMethod10(@SpanTag(value = "testTag10") String param);

		@ContinueSpan(log = "customTest")
		Flux<String> testMethod10_v2(@SpanTag(key = "testTag10") String param);

		// tag::continue_span[]
		@ContinueSpan(log = "testMethod11")
		Flux<String> testMethod11(@SpanTag("testTag11") String param);
		// end::continue_span[]

		@NewSpan
		Flux<String> testMethod12(@SpanTag("testTag12") String param);

		@ContinueSpan(log = "testMethod13")
		Flux<String> testMethod13();

		@ContinueSpan
		Flux<String> testMethod14(String param);

		@NewSpan(name = "spanInTraceContext")
		Flux<Long> newSpanInTraceContext();

		@NewSpan(name = "spanInSubscriberContext")
		Flux<Long> newSpanInSubscriberContext();

		void proceed();

		void reset();
	}
	
	protected static class TestBean implements TestBeanInterface {

		public static final String TEST_STRING1 = "Test String 1";
		public static final String TEST_STRING2 = "Test String 2";

		private AtomicReference<CompletableFuture<Void>> proceed
				= new AtomicReference<>(new CompletableFuture<>());
		private Flux<String> testFlux = Flux.defer(() -> Flux.just(TEST_STRING1, TEST_STRING2))
				.delayUntil(s -> Mono.fromFuture(proceed.get()))
				.doOnNext(s -> proceed.set(new CompletableFuture<>()));

		@Override
		public void reset(){
			proceed.set(new CompletableFuture<>());
		}

		public void proceed(){
			proceed.get().complete(null);
		}

		@Override
		public Flux<String> testMethod() {
			return testFlux;
		}

		@NewSpan
		@Override
		public Flux<String> testMethod2() {
			return testFlux;
		}

		// tag::name_on_implementation[]
		@NewSpan(name = "customNameOnTestMethod3")
		@Override
		public Flux<String> testMethod3() {
			return testFlux;
		}
		// end::name_on_implementation[]

		@Override
		public Flux<String> testMethod4() {
			return testFlux;
		}
		
		@Override
		public Flux<String> testMethod5(String test) {
			return testFlux;
		}

		@NewSpan(name = "customNameOnTestMethod6")
		@Override
		public Flux<String> testMethod6(@SpanTag("testTag6") String test) {
			return testFlux;
		}

		@Override
		public Flux<String> testMethod7() {
			return testFlux;
		}

		@Override
		public Flux<String> testMethod8(String param) {
			return testFlux;
		}

		@NewSpan(name = "customNameOnTestMethod9")
		@Override
		public Flux<String> testMethod9(String param) {
			return testFlux;
		}

		@Override
		public Flux<String> testMethod10(@SpanTag(value = "customTestTag10") String param) {
			return testFlux;
		}

		@Override
		public Flux<String> testMethod10_v2(@SpanTag(key = "customTestTag10") String param) {
			return testFlux;
		}

		@ContinueSpan(log = "customTest")
		@Override
		public Flux<String> testMethod11(@SpanTag("customTestTag11") String param) {
			return testFlux;
		}

		@Override
		public Flux<String> testMethod12(String param) {
			return Flux.defer(() -> Flux.error(new RuntimeException("test exception 12")));
		}

		@Override
		public Flux<String> testMethod13() {
			return Flux.defer(() -> Flux.error(new RuntimeException("test exception 13")));
		}

		@Override
		public Flux<String> testMethod14(String param) {
			return Flux.just(TEST_STRING1, TEST_STRING2);
		}

		@Override
		public Flux<Long> newSpanInTraceContext() {
			return Flux.defer(() -> {
				final TraceContext traceContext = Tracing.current().currentTraceContext().get();
				return Flux.just(traceContext.spanId());
			});
		}

		@Override
		public Flux<Long> newSpanInSubscriberContext() {
			return Mono.subscriberContext()
					.flatMapMany(context -> {
						final TraceContext traceContext = Tracing.current().currentTraceContext().get();
						return Flux.just(traceContext.spanId());
					});
		}
	}
	
	@Configuration
	@EnableAutoConfiguration
	protected static class TestConfiguration {

		@Bean
		public TestBeanInterface testBean() {
			return new TestBean();
		}

		@Bean Reporter<zipkin2.Span> spanReporter() {
			return new ArrayListSpanReporter();
		}

		@Bean Sampler alwaysSampler() {
			return Sampler.ALWAYS_SAMPLE;
		}
	}
}
