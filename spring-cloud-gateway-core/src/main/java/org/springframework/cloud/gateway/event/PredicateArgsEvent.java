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

package org.springframework.cloud.gateway.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

/**
 * 路由谓词参数事件
 *
 * 由路由定义定位器RouteDefinitionRouteLocator中转换路由谓词的时候发送事件（主要是权重相关的）
 * 在权重计算web过滤器WeightCalculatorWebFilter中监听事件并处理事件（非权重相关参数不处理直接返回）
 *
 */
public class PredicateArgsEvent extends ApplicationEvent {

	private final Map<String, Object> args;

	private String routeId;

	public PredicateArgsEvent(Object source, String routeId, Map<String, Object> args) {
		super(source);
		this.routeId = routeId;
		this.args = args;
	}

	public String getRouteId() {
		return routeId;
	}

	public Map<String, Object> getArgs() {
		return args;
	}

}
