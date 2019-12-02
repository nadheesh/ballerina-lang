/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.http.websocket.server;

import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.websocket.WebSocketConstants;
import org.ballerinalang.net.http.websocket.WebSocketService;
import org.ballerinalang.net.http.websocket.WebSocketUtil;

/**
 * WebSocket service for service dispatching.
 */
public class WebSocketServerService extends WebSocketService {

    private String[] negotiableSubProtocols = null;
    private String basePath;
    private int maxFrameSize = WebSocketConstants.DEFAULT_MAX_FRAME_SIZE;
    private int idleTimeoutInSeconds = 0;
    private HttpResource upgradeResource;

    public WebSocketServerService(ObjectValue service, Scheduler scheduler) {
        super(service, scheduler);
        populateConfigs();
    }

    public WebSocketServerService(String httpBasePath, HttpResource upgradeResource, ObjectValue service,
                                  Scheduler scheduler) {
        this(service, scheduler);
        setBasePathWithUpgradePath(httpBasePath, upgradeResource);
        this.upgradeResource = upgradeResource;
    }

    private void setBasePathWithUpgradePath(String httpBasePath, HttpResource upgradeResource) {
        MapValue resourceConfigAnnotation = HttpResource.getResourceConfigAnnotation(upgradeResource.getBalResource());
        MapValue webSocketConfig =
                resourceConfigAnnotation.getMapValue(HttpConstants.ANN_CONFIG_ATTR_WEBSOCKET_UPGRADE);
        String upgradePath = webSocketConfig.getStringValue(HttpConstants.ANN_WEBSOCKET_ATTR_UPGRADE_PATH);
        setBasePathToServiceObj(httpBasePath.concat(upgradePath));
    }

    private void populateConfigs() {
        MapValue<String, Object> configAnnotation = getServiceConfigAnnotation();
        if (configAnnotation != null) {
            negotiableSubProtocols = WebSocketUtil.findNegotiableSubProtocols(configAnnotation);
            idleTimeoutInSeconds = WebSocketUtil.findIdleTimeoutInSeconds(configAnnotation);
            maxFrameSize = WebSocketUtil.findMaxFrameSize(configAnnotation);
        }
        // This will be overridden if there is an upgrade path
        setBasePathToServiceObj(findFullWebSocketUpgradePath(configAnnotation));
    }

    @SuppressWarnings(WebSocketConstants.UNCHECKED)
    private MapValue<String, Object> getServiceConfigAnnotation() {
        return (MapValue<String, Object>) service.getType().getAnnotation(
                HttpConstants.PROTOCOL_PACKAGE_HTTP, WebSocketConstants.WEBSOCKET_ANNOTATION_CONFIGURATION);
    }

    public String getName() {
        if (service != null) {
            // With JBallerina this is the way to get the key
            String name = HttpUtil.getServiceName(service);
            return name.startsWith(HttpConstants.DOLLAR) ? "" : name;
        }
        return null;
    }

    public String[] getNegotiableSubProtocols() {
        if (negotiableSubProtocols == null) {
            return new String[0];
        }
        return negotiableSubProtocols.clone();
    }

    public HttpResource getUpgradeResource() {
        return upgradeResource;
    }

    public int getIdleTimeoutInSeconds() {
        return idleTimeoutInSeconds;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setBasePathToServiceObj(String basePath) {
        service.addNativeData(WebSocketConstants.NATIVE_DATA_BASE_PATH, basePath);
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    private String findFullWebSocketUpgradePath(MapValue config) {
        String path = null;
        if (config != null) {
            String basePathVal = config.getStringValue(WebSocketConstants.ANNOTATION_ATTR_PATH);
            if (!basePathVal.trim().isEmpty()) {
                path = HttpUtil.sanitizeBasePath(basePathVal);
            }
        }
        if (path == null) {
            path = "/".concat(getName());
        }
        return path;
    }
}