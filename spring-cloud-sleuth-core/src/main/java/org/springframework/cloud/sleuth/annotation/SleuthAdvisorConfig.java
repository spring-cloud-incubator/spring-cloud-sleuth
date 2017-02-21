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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Custom pointcut advisor that picks all classes / interfaces that
 * have the {@link NewSpan}.
 *
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
class SleuthAdvisorConfig  extends AbstractPointcutAdvisor implements
		IntroductionAdvisor, BeanFactoryAware {
	private static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

	private Advice advice;

	private Pointcut pointcut;

	private BeanFactory beanFactory;

	@PostConstruct
	public void init() {
		this.pointcut = buildPointcut();
		this.advice = buildAdvice();
		if (this.advice instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
		}
	}

	/**
	 * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public ClassFilter getClassFilter() {
		return pointcut.getClassFilter();
	}

	@Override
	public Class<?>[] getInterfaces() {
		return new Class[] {};
	}

	@Override
	public void validateInterfaces() throws IllegalArgumentException {
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	protected Advice buildAdvice() {
		return new SleuthInterceptor();
	}

	/**
	 * Calculate a pointcut for the given retry annotation types, if any.
	 *
	 * @return the applicable Pointcut object, or {@code null} if none
	 */
	protected Pointcut buildPointcut() {
		return new AnnotationClassOrMethodOrArgsPointcut();
	}

	/**
	 *
	 */
	private final class AnnotationClassOrMethodOrArgsPointcut extends
			DynamicMethodMatcherPointcut {

		private final DynamicMethodMatcherPointcut methodResolver;

		AnnotationClassOrMethodOrArgsPointcut() {
			this.methodResolver = new DynamicMethodMatcherPointcut() {
				@Override public boolean matches(Method method, Class<?> targetClass,
						Object... args) {
					if (SleuthAnnotationUtils.isMethodAnnotated(method)) {
						if (log.isDebugEnabled()) {
							log.debug("Found a method with @NewSpan annotation");
						}
						return true;
					}
					if (SleuthAnnotationUtils.hasAnnotatedParams(method, args)) {
						if (log.isDebugEnabled()) {
							log.debug("Found annotated arguments of the method");
						}
						return true;
					}
					return false;
				}
			};
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			return getClassFilter().matches(targetClass) || this.methodResolver.matches(method, targetClass, args);
		}

		@Override public ClassFilter getClassFilter() {
			return new AnnotationClassOrMethodFilter(NewSpan.class);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AnnotationClassOrMethodOrArgsPointcut)) {
				return false;
			}
			AnnotationClassOrMethodOrArgsPointcut otherAdvisor = (AnnotationClassOrMethodOrArgsPointcut) other;
			return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver);
		}

	}

	private final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

		private final AnnotationMethodsResolver methodResolver;

		AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
			super(annotationType, true);
			this.methodResolver = new AnnotationMethodsResolver(annotationType);
		}

		@Override
		public boolean matches(Class<?> clazz) {
			return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
		}

	}

	private static class AnnotationMethodsResolver {

		private Class<? extends Annotation> annotationType;

		public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
			this.annotationType = annotationType;
		}

		public boolean hasAnnotatedMethods(Class<?> clazz) {
			final AtomicBoolean found = new AtomicBoolean(false);
			ReflectionUtils.doWithMethods(clazz,
					new ReflectionUtils.MethodCallback() {
						@Override
						public void doWith(Method method) throws IllegalArgumentException,
								IllegalAccessException {
							if (found.get()) {
								return;
							}
							Annotation annotation = AnnotationUtils.findAnnotation(method,
									annotationType);
							if (annotation != null) { found.set(true); }
						}
					});
			return found.get();
		}

	}
}

class SleuthInterceptor  implements IntroductionInterceptor, BeanFactoryAware  {

	private BeanFactory beanFactory;
	private SpanCreator spanCreator;
	private Tracer tracer;

	@Override public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (method == null) {
			return invocation.proceed();
		}
		Method mostSpecificMethod = AopUtils
				.getMostSpecificMethod(method, invocation.getThis().getClass());
		NewSpan annotation = SleuthAnnotationUtils.findAnnotation(mostSpecificMethod);
		if (annotation == null) {
			return invocation.proceed();
		}
		Span span = null;
		boolean hasLog = StringUtils.hasText(annotation.log());
		try {
			span = spanCreator().createSpan(invocation, annotation);
			if (hasLog) {
				span.logEvent(annotation.log() + ".start");
			}
			return invocation.proceed();
		} finally {
			if (span != null) {
				if (hasLog) {
					span.logEvent(annotation.log() + ".end");
				}
				tracer().close(span);
			}
		}
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

	private SpanCreator spanCreator() {
		if (this.spanCreator == null) {
			this.spanCreator = this.beanFactory.getBean(SpanCreator.class);
		}
		return this.spanCreator;
	}

	@Override public boolean implementsInterface(Class<?> intf) {
		return true;
	}

	@Override public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
