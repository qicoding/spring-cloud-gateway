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

package org.springframework.cloud.gateway.filter.ratelimit;

import java.util.Map;

import org.springframework.cloud.gateway.event.FilterArgsEvent;
import org.springframework.cloud.gateway.support.AbstractStatefulConfigurable;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationListener;
import org.springframework.core.style.ToStringCreator;
import org.springframework.validation.Validator;

public abstract class AbstractRateLimiter<C> extends AbstractStatefulConfigurable<C>
		implements RateLimiter<C>, ApplicationListener<FilterArgsEvent> {

	private String configurationPropertyName;

	private ConfigurationService configurationService;

	@Deprecated
	protected AbstractRateLimiter(Class<C> configClass, String configurationPropertyName,
			Validator validator) {
		super(configClass);
		this.configurationPropertyName = configurationPropertyName;
		this.configurationService = new ConfigurationService();
		this.configurationService.setValidator(validator);
	}

	protected AbstractRateLimiter(Class<C> configClass, String configurationPropertyName,
			ConfigurationService configurationService) {
		super(configClass);
		this.configurationPropertyName = configurationPropertyName;
		this.configurationService = configurationService;
	}

	protected String getConfigurationPropertyName() {
		return configurationPropertyName;
	}

	@Deprecated
	protected Validator getValidator() {
		if (this.configurationService != null) {
			return this.configurationService.getValidator();
		}
		return null;
	}

	@Deprecated
	public void setValidator(Validator validator) {
		if (this.configurationService != null) {
			this.configurationService.setValidator(validator);
		}
	}

	protected void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	/**
	 * 监听处理过滤器参数事件
	 * @param event
	 */
	@Override
	public void onApplicationEvent(FilterArgsEvent event) {
		Map<String, Object> args = event.getArgs();

		// 如果参数为空 或者 不包含限流相关参数 则直接返回
		if (args.isEmpty() || !hasRelevantKey(args)) {
			return;
		}

		String routeId = event.getRouteId();

		C routeConfig = newConfig();
		if (this.configurationService != null) {
			this.configurationService.with(routeConfig)
					.name(this.configurationPropertyName).normalizedProperties(args)
					.bind();
		}
		getConfig().put(routeId, routeConfig);
	}

	/**
	 * 判断是否包含限流相关参数
	 * @param args
	 * @return
	 */
	private boolean hasRelevantKey(Map<String, Object> args) {
		return args.keySet().stream()
				.anyMatch(key -> key.startsWith(configurationPropertyName + "."));
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("configurationPropertyName", configurationPropertyName)
				.append("config", getConfig()).append("configClass", getConfigClass())
				.toString();
	}

}
