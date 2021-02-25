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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.core.style.ToStringCreator;

/**
 * 服务发现配置参数类
 */
@ConfigurationProperties("spring.cloud.gateway.discovery.locator")
public class DiscoveryLocatorProperties {

	/**
	 * 启用DiscoveryClient网关集成的标志
	 */
	/** Flag that enables DiscoveryClient gateway integration. */
	private boolean enabled = false;

	/**
	 * routeId的前缀，如果有配置前缀如 test1111_，则routeId为： test1111_service1
	 * 没配置时 默认为 DiscoveryClient.getClass().getSimpleName() + "_". 服务ID将被添加以创建routeId， 如： ReactiveCompositeDiscoveryClient_service1
	 */
	/**
	 * The prefix for the routeId, defaults to discoveryClient.getClass().getSimpleName()
	 * + "_". Service Id will be appended to create the routeId.
	 */
	private String routeIdPrefix;

	/**
	 * 将评估是否在网关集成中包括服务的SpEL表达式，默认为：true。
	 */
	/**
	 * SpEL expression that will evaluate whether to include a service in gateway
	 * integration or not, defaults to: true.
	 */
	private String includeExpression = "true";

	/**
	 * 为每个路由创建uri的SpEL表达式，默认为：'lb://'+serviceId
	 */
	/**
	 * SpEL expression that create the uri for each route, defaults to: 'lb://'+serviceId.
	 */
	private String urlExpression = "'lb://'+serviceId";

	/**
	 * 谓词和过滤器中使serviceId小写，默认为false。
	 * 当eureka自动将serviceId大写时，此方法很有用。 因此 MYSERIVCE 将与 /myservice/** 匹配
	 */
	/**
	 * Option to lower case serviceId in predicates and filters, defaults to false. Useful
	 * with eureka when it automatically uppercases serviceId. so MYSERIVCE, would match
	 * /myservice/**
	 */
	private boolean lowerCaseServiceId = false;

	/**
	 * 谓词列表
	 */
	private List<PredicateDefinition> predicates = new ArrayList<>();

	/**
	 * 过滤器
	 */
	private List<FilterDefinition> filters = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getRouteIdPrefix() {
		return routeIdPrefix;
	}

	public void setRouteIdPrefix(String routeIdPrefix) {
		this.routeIdPrefix = routeIdPrefix;
	}

	public String getIncludeExpression() {
		return includeExpression;
	}

	public void setIncludeExpression(String includeExpression) {
		this.includeExpression = includeExpression;
	}

	public String getUrlExpression() {
		return urlExpression;
	}

	public void setUrlExpression(String urlExpression) {
		this.urlExpression = urlExpression;
	}

	public boolean isLowerCaseServiceId() {
		return lowerCaseServiceId;
	}

	public void setLowerCaseServiceId(boolean lowerCaseServiceId) {
		this.lowerCaseServiceId = lowerCaseServiceId;
	}

	public List<PredicateDefinition> getPredicates() {
		return predicates;
	}

	public void setPredicates(List<PredicateDefinition> predicates) {
		this.predicates = predicates;
	}

	public List<FilterDefinition> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterDefinition> filters) {
		this.filters = filters;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("enabled", enabled)
				.append("routeIdPrefix", routeIdPrefix)
				.append("includeExpression", includeExpression)
				.append("urlExpression", urlExpression)
				.append("lowerCaseServiceId", lowerCaseServiceId)
				.append("predicates", predicates).append("filters", filters).toString();
	}

}
