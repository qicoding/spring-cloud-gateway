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

import org.springframework.context.ApplicationEvent;

/**
 * 缓存请求body的事件
 *
 * 当使用RetryGatewayFilterFactory路由过滤器时，会在该过滤器发送一个缓存请求body的事件
 * 然后在全局过滤器AdaptCachedBodyGlobalFilter中会监听该事件，监听到之后会把该路由的请求body缓存起来，便于后面重试使用
 */
public class EnableBodyCachingEvent extends ApplicationEvent {

	private final String routeId;

	public EnableBodyCachingEvent(Object source, String routeId) {
		super(source);
		this.routeId = routeId;
	}

	public String getRouteId() {
		return this.routeId;
	}

}
