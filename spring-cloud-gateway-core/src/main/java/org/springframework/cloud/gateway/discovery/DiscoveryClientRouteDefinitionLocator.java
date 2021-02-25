/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.gateway.discovery;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.style.ToStringCreator;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * 服务发现客户端路由定义定位器
 * TODO: change to RouteLocator? use java dsl
 *
 * @author Spencer Gibb
 */
public class DiscoveryClientRouteDefinitionLocator implements RouteDefinitionLocator {

	private static final Log log = LogFactory
			.getLog(DiscoveryClientRouteDefinitionLocator.class);

	/**
	 * 配置
	 */
	private final DiscoveryLocatorProperties properties;

	/**
	 * 路由id前缀
	 */
	private final String routeIdPrefix;

	/**
	 * SimpleEvaluationContext - 针对不需要SpEL语言语法的全部范围并且应该受到有意限制的表达式类别，公开Spal语言特性和配置选项的子集。示例包括但不限于数据绑定表达式，基于属性的过滤器等
	 */
	private final SimpleEvaluationContext evalCtxt;

	private Flux<List<ServiceInstance>> serviceInstances;

	/**
	 * Kept for backwards compatibility. You should use the reactive discovery client.
	 * @param discoveryClient the blocking discovery client 阻塞的服务发现客户端
	 * @param properties the configuration properties 配置属性
	 * @deprecated kept for backwards compatibility
	 */
	@Deprecated
	public DiscoveryClientRouteDefinitionLocator(DiscoveryClient discoveryClient,
			DiscoveryLocatorProperties properties) {
		this(discoveryClient.getClass().getSimpleName(), properties);
		serviceInstances = Flux
				.defer(() -> Flux.fromIterable(discoveryClient.getServices()))
				.map(discoveryClient::getInstances)
				.subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * @param discoveryClient 反应式服务发现客户端
	 * @param properties      配置属性
	 */
	public DiscoveryClientRouteDefinitionLocator(ReactiveDiscoveryClient discoveryClient,
			DiscoveryLocatorProperties properties) {
		this(discoveryClient.getClass().getSimpleName(), properties);
		serviceInstances = discoveryClient.getServices()
				.flatMap(service -> discoveryClient.getInstances(service).collectList());
	}

	/**
	 *
	 * @param discoveryClientName 服务发现客户端类名 simpleName
	 * @param properties          配置属性
	 */
	private DiscoveryClientRouteDefinitionLocator(String discoveryClientName,
			DiscoveryLocatorProperties properties) {
		this.properties = properties;
		// 如果有配置路由id前缀
		if (StringUtils.hasText(properties.getRouteIdPrefix())) {
			routeIdPrefix = properties.getRouteIdPrefix();
		}
		// 没配置路由id前缀则使用 discoveryClientName + "_" 作为路由id前缀
		else {
			routeIdPrefix = discoveryClientName + "_";
		}
		evalCtxt = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods()
				.build();
	}

	/**
	 * 获取路由定义
	 * @return
	 */
	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		// SpEL语言解析器
		SpelExpressionParser parser = new SpelExpressionParser();
		// 是否包含el表达式的el表达式
		Expression includeExpr = parser
				.parseExpression(properties.getIncludeExpression());
		// url的el表达式
		Expression urlExpr = parser.parseExpression(properties.getUrlExpression());

		Predicate<ServiceInstance> includePredicate;
		// includeExpression 如果为空 或者 为 true字符串 则为true
		if (properties.getIncludeExpression() == null
				|| "true".equalsIgnoreCase(properties.getIncludeExpression())) {
			includePredicate = instance -> true;
		}
		else {
			includePredicate = instance -> {
				Boolean include = includeExpr.getValue(evalCtxt, instance, Boolean.class);
				if (include == null) {
					return false;
				}
				return include;
			};
		}

		return serviceInstances.filter(instances -> !instances.isEmpty())
				.map(instances -> instances.get(0)).filter(includePredicate)
				.map(instance -> {
					RouteDefinition routeDefinition = buildRouteDefinition(urlExpr,
							instance);

					final ServiceInstance instanceForEval = new DelegatingServiceInstance(
							instance, properties);

					for (PredicateDefinition original : this.properties.getPredicates()) {
						PredicateDefinition predicate = new PredicateDefinition();
						predicate.setName(original.getName());
						for (Map.Entry<String, String> entry : original.getArgs()
								.entrySet()) {
							// 根据表达式 获取到谓词value的字符串
							String value = getValueFromExpr(evalCtxt, parser,
									instanceForEval, entry);
							predicate.addArg(entry.getKey(), value);
						}
						routeDefinition.getPredicates().add(predicate);
					}

					for (FilterDefinition original : this.properties.getFilters()) {
						FilterDefinition filter = new FilterDefinition();
						filter.setName(original.getName());
						for (Map.Entry<String, String> entry : original.getArgs()
								.entrySet()) {
							// 根据表达式 获取到过滤器value的字符串
							String value = getValueFromExpr(evalCtxt, parser,
									instanceForEval, entry);
							filter.addArg(entry.getKey(), value);
						}
						routeDefinition.getFilters().add(filter);
					}

					return routeDefinition;
				});
	}

	/**
	 * build路由定义
	 * @param urlExpr
	 * @param serviceInstance
	 * @return
	 */
	protected RouteDefinition buildRouteDefinition(Expression urlExpr,
			ServiceInstance serviceInstance) {
		String serviceId = serviceInstance.getServiceId();
		RouteDefinition routeDefinition = new RouteDefinition();
		// 设置路由id
		routeDefinition.setId(this.routeIdPrefix + serviceId);
		String uri = urlExpr.getValue(this.evalCtxt, serviceInstance, String.class);
		// 设置路由uri
		routeDefinition.setUri(URI.create(uri));
		// add instance metadata
		routeDefinition.setMetadata(new LinkedHashMap<>(serviceInstance.getMetadata()));
		return routeDefinition;
	}

	/**
	 * 根据表达式过滤el表达式的值
	 * @param evalCtxt
	 * @param parser
	 * @param instance
	 * @param entry
	 * @return
	 */
	String getValueFromExpr(SimpleEvaluationContext evalCtxt, SpelExpressionParser parser,
			ServiceInstance instance, Map.Entry<String, String> entry) {
		try {
			Expression valueExpr = parser.parseExpression(entry.getValue());
			return valueExpr.getValue(evalCtxt, instance, String.class);
		}
		catch (ParseException | EvaluationException e) {
			if (log.isDebugEnabled()) {
				log.debug("Unable to parse " + entry.getValue(), e);
			}
			throw e;
		}
	}

	/**
	 * 服务实例 委托delegate模式
	 */
	private static class DelegatingServiceInstance implements ServiceInstance {

		final ServiceInstance delegate;

		private final DiscoveryLocatorProperties properties;

		private DelegatingServiceInstance(ServiceInstance delegate,
				DiscoveryLocatorProperties properties) {
			this.delegate = delegate;
			this.properties = properties;
		}

		@Override
		public String getServiceId() {
			if (properties.isLowerCaseServiceId()) {
				return delegate.getServiceId().toLowerCase();
			}
			return delegate.getServiceId();
		}

		@Override
		public String getHost() {
			return delegate.getHost();
		}

		@Override
		public int getPort() {
			return delegate.getPort();
		}

		@Override
		public boolean isSecure() {
			return delegate.isSecure();
		}

		@Override
		public URI getUri() {
			return delegate.getUri();
		}

		@Override
		public Map<String, String> getMetadata() {
			return delegate.getMetadata();
		}

		@Override
		public String getScheme() {
			return delegate.getScheme();
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("delegate", delegate)
					.append("properties", properties).toString();
		}

	}

}
