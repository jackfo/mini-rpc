/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cfs.mini.common.utils;


import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;

import java.util.*;

public class UrlUtils {

    /**
     * 解析单个 URL ，将 `defaults` 里的参数，合并到 `address` 中。
     *
     * 合并的逻辑如下：
     *
     * 我们可以把 `address` 认为是 url ；`defaults` 认为是 defaultURL 。
     * 若 url 有不存在的属性时，从 defaultURL 获得对应的属性，设置到 url 中。
     *
     * @param address 地址
     * @param defaults 默认参数集合
     * @return URL
     */
    public static URL parseURL(String address, Map<String, String> defaults) {
        if (address == null || address.length() == 0) {
            return null;
        }
        // 以 Zookeeper 注册中心，配置集群的例子如下：
        // 第一种，<mini:registry address="zookeeper://10.20.153.10:2181?backup=10.20.153.11:2181,10.20.153.12:2181"/>
        // 第二种，<mini:registry protocol="zookeeper" address="10.20.153.10:2181,10.20.153.11:2181,10.20.153.12:2181"/>
        String url;
        if (address.contains("://")) { // 第一种
            url = address;
        } else { // 第二种
            String[] addresses = Constants.COMMA_SPLIT_PATTERN.split(address); // 按照 逗号 拆分
            url = addresses[0];
            if (addresses.length > 1) {
                StringBuilder backup = new StringBuilder();
                for (int i = 1; i < addresses.length; i++) {
                    if (i > 1) {
                        backup.append(",");
                    }
                    backup.append(addresses[i]);
                }
                url += "?" + Constants.BACKUP_KEY + "=" + backup.toString();
            }
        }
        // 从 `defaults` 中，获得 "protocol" "username" "password" "host" "port" "path" 到 `defaultXXX` 属性种。
        // 因为，在 mini URL 中，这几个是独立的属性，不在 `mini.parameters` 属性中。
        String defaultProtocol = defaults == null ? null : defaults.get("protocol");
        if (defaultProtocol == null || defaultProtocol.length() == 0) { // 如果地址没有协议缺省为 mini
            defaultProtocol = "mini";
        }
        String defaultUsername = defaults == null ? null : defaults.get("username");
        String defaultPassword = defaults == null ? null : defaults.get("password");
        int defaultPort = StringUtils.parseInteger(defaults == null ? null : defaults.get("port"));
        String defaultPath = defaults == null ? null : defaults.get("path");
        Map<String, String> defaultParameters = defaults == null ? null : new HashMap<String, String>(defaults);
        if (defaultParameters != null) { // 需要移除，因为这几个是独立属性。
            defaultParameters.remove("protocol");
            defaultParameters.remove("username");
            defaultParameters.remove("password");
            defaultParameters.remove("host");
            defaultParameters.remove("port");
            defaultParameters.remove("path");
        }
        // 创建 mini URL 。
        URL u = URL.valueOf(url);
        // 若 `u` 的属性存在非空的情况下，从 `defaultXXX` 属性，赋值到 `u` 的属性中。
        boolean changed = false; // 是否改变，即从 `defaultXXX` 属性，赋值到 `u` 的属性中。
        String protocol = u.getProtocol();
        String username = u.getUsername();
        String password = u.getPassword();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        Map<String, String> parameters = new HashMap<String, String>(u.getParameters());
        if ((protocol == null || protocol.length() == 0) && defaultProtocol.length() > 0) {
            changed = true;
            protocol = defaultProtocol;
        }
        if ((username == null || username.length() == 0) && defaultUsername != null && defaultUsername.length() > 0) {
            changed = true;
            username = defaultUsername;
        }
        if ((password == null || password.length() == 0) && defaultPassword != null && defaultPassword.length() > 0) {
            changed = true;
            password = defaultPassword;
        }
        /*if (u.isAnyHost() || u.isLocalHost()) {
            changed = true;
            host = NetUtils.getLocalHost();
        }*/
        if (port <= 0) {
            if (defaultPort > 0) {
                changed = true;
                port = defaultPort;
            } else { // 如果地址没有端口缺省为9090。FROM http://mini.io/books/mini-user-book/references/xml/mini-registry.html 文档。
                changed = true;
                port = 9090;
            }
        }
        if (path == null || path.length() == 0) {
            if (defaultPath != null && defaultPath.length() > 0) {
                changed = true;
                path = defaultPath;
            }
        }
        if (defaultParameters != null && defaultParameters.size() > 0) {
            for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
                String key = entry.getKey();
                String defaultValue = entry.getValue();
                if (defaultValue != null && defaultValue.length() > 0) {
                    String value = parameters.get(key);
                    if (value == null || value.length() == 0) {
                        changed = true;
                        parameters.put(key, defaultValue);
                    }
                }
            }
        }
        // 若改变，创建新的 mini URL 。
        if (changed) {
            u = new URL(protocol, username, password, host, port, path, parameters);
        }
        return u;
    }

    /**
     * 解析多个 URL ，将 `defaults` 里的参数，合并到 `address` 中。
     *
     * 合并的逻辑如下：
     *
     * 我们可以把 `address` 认为是 url ；`defaults` 认为是 defaultURL 。
     * 若 url 有不存在的属性时，从 defaultURL 获得对应的属性，设置到 url 中。
     *
     * @param address 地址
     * @param defaults 默认参数集合
     * @return URL
     */
    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        // 拆分注册中心地址，按照逗号或者分号。
        if (address == null || address.length() == 0) {
            return null;
        }
        String[] addresses = Constants.REGISTRY_SPLIT_PATTERN.split(address);
        if (addresses == null || addresses.length == 0) {
            return null; //here won't be empty
        }
        List<URL> registries = new ArrayList<URL>();
        for (String addr : addresses) {
            // 解析 URL ，将 `defaults` 里的参数，合并到 `addr` 中。
            registries.add(parseURL(addr, defaults));
        }
        return registries;
    }

    // 芋艿，可无视；测试类使用
    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String group = params.get("group");
                    String version = params.get("version");
                    //params.remove("group");
                    //params.remove("version");
                    String name = serviceName;
                    if (group != null && group.length() > 0) {
                        name = group + "/" + name;
                    }
                    if (version != null && version.length() > 0) {
                        name = name + ":" + version;
                    }
                    Map<String, String> newUrls = newRegister.get(name);
                    if (newUrls == null) {
                        newUrls = new HashMap<String, String>();
                        newRegister.put(name, newUrls);
                    }
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    // 芋艿，可无视；测试类使用
    public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String group = params.get("group");
                String version = params.get("version");
                //params.remove("group");
                //params.remove("version");
                String name = serviceName;
                if (group != null && group.length() > 0) {
                    name = group + "/" + name;
                }
                if (version != null && version.length() > 0) {
                    name = name + ":" + version;
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    // 芋艿，可无视；测试类使用
    public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (serviceName.contains(":") || serviceName.contains("/")) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String name = serviceName;
                    int i = name.indexOf('/');
                    if (i >= 0) {
                        params.put("group", name.substring(0, i));
                        name = name.substring(i + 1);
                    }
                    i = name.lastIndexOf(':');
                    if (i >= 0) {
                        params.put("version", name.substring(i + 1));
                        name = name.substring(0, i);
                    }
                    Map<String, String> newUrls = newRegister.get(name);
                    if (newUrls == null) {
                        newUrls = new HashMap<String, String>();
                        newRegister.put(name, newUrls);
                    }
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    // 芋艿，可无视；测试类使用
    public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (serviceName.contains(":") || serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String name = serviceName;
                int i = name.indexOf('/');
                if (i >= 0) {
                    params.put("group", name.substring(0, i));
                    name = name.substring(i + 1);
                }
                i = name.lastIndexOf(':');
                if (i >= 0) {
                    params.put("version", name.substring(i + 1));
                    name = name.substring(0, i);
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    // 芋艿，可无视；测试类使用
    public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
        if (notify != null && notify.size() > 0) {
            Map<String, Map<String, String>> newNotify = new HashMap<String, Map<String, String>>();
            for (Map.Entry<String, Map<String, String>> entry : notify.entrySet()) {
                String serviceName = entry.getKey();
                Map<String, String> serviceUrls = entry.getValue();
                if (!serviceName.contains(":") && !serviceName.contains("/")) {
                    if (serviceUrls != null && serviceUrls.size() > 0) {
                        for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                            String url = entry2.getKey();
                            String query = entry2.getValue();
                            Map<String, String> params = StringUtils.parseQueryString(query);
                            String group = params.get("group");
                            String version = params.get("version");
                            // params.remove("group");
                            // params.remove("version");
                            String name = serviceName;
                            if (group != null && group.length() > 0) {
                                name = group + "/" + name;
                            }
                            if (version != null && version.length() > 0) {
                                name = name + ":" + version;
                            }
                            Map<String, String> newUrls = newNotify.get(name);
                            if (newUrls == null) {
                                newUrls = new HashMap<String, String>();
                                newNotify.put(name, newUrls);
                            }
                            newUrls.put(url, StringUtils.toQueryString(params));
                        }
                    }
                } else {
                    newNotify.put(serviceName, serviceUrls);
                }
            }
            return newNotify;
        }
        return notify;
    }

    // 芋艿，可无视；测试类使用
    //compatible for mini-2.0.0
    public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
        if (forbid != null && !forbid.isEmpty()) {
            List<String> newForbid = new ArrayList<String>();
            for (String serviceName : forbid) {
                if (!serviceName.contains(":") && !serviceName.contains("/")) {
                    for (URL url : subscribed) {
                        if (serviceName.equals(url.getServiceInterface())) {
                            newForbid.add(url.getServiceKey());
                            break;
                        }
                    }
                } else {
                    newForbid.add(serviceName);
                }
            }
            return newForbid;
        }
        return forbid;
    }

    // 芋艿，可无视；未调用
    public static URL getEmptyUrl(String service, String category) {
        String group = null;
        String version = null;
        int i = service.indexOf('/');
        if (i > 0) {
            group = service.substring(0, i);
            service = service.substring(i + 1);
        }
        i = service.lastIndexOf(':');
        if (i > 0) {
            version = service.substring(i + 1);
            service = service.substring(0, i);
        }
        return URL.valueOf(Constants.EMPTY_PROTOCOL + "://0.0.0.0/" + service + "?"
                + Constants.CATEGORY_KEY + "=" + category
                + (group == null ? "" : "&" + Constants.GROUP_KEY + "=" + group)
                + (version == null ? "" : "&" + Constants.VERSION_KEY + "=" + version));
    }

    public static boolean isMatchCategory(String category, String categories) {
        if (categories == null || categories.length() == 0) {
            return Constants.DEFAULT_CATEGORY.equals(category);
        } else if (categories.contains(Constants.ANY_VALUE)) {
            return true;
        } else if (categories.contains(Constants.REMOVE_VALUE_PREFIX)) {
            return !categories.contains(Constants.REMOVE_VALUE_PREFIX + category);
        } else {
            return categories.contains(category);
        }
    }

    public static boolean isMatch(URL consumerUrl, URL providerUrl) {
        // interface
        String consumerInterface = consumerUrl.getServiceInterface();
        String providerInterface = providerUrl.getServiceInterface();
        if (!(Constants.ANY_VALUE.equals(consumerInterface) || StringUtils.isEquals(consumerInterface, providerInterface))) {
            return false;
        }

        // category
        if (!isMatchCategory(providerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY), consumerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY))) {
            return false;
        }

        // enable
        if (!providerUrl.getParameter(Constants.ENABLED_KEY, true) && !Constants.ANY_VALUE.equals(consumerUrl.getParameter(Constants.ENABLED_KEY))) {
            return false;
        }

        // consumer
        String consumerGroup = consumerUrl.getParameter(Constants.GROUP_KEY);
        String consumerVersion = consumerUrl.getParameter(Constants.VERSION_KEY);
        String consumerClassifier = consumerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);
        // provider
        String providerGroup = providerUrl.getParameter(Constants.GROUP_KEY);
        String providerVersion = providerUrl.getParameter(Constants.VERSION_KEY);
        String providerClassifier = providerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);
        // 匹配 group、version、classifier
        return (Constants.ANY_VALUE.equals(consumerGroup) || StringUtils.isEquals(consumerGroup, providerGroup) || StringUtils.isContains(consumerGroup, providerGroup))
                && (Constants.ANY_VALUE.equals(consumerVersion) || StringUtils.isEquals(consumerVersion, providerVersion))
                && (consumerClassifier == null || Constants.ANY_VALUE.equals(consumerClassifier) || StringUtils.isEquals(consumerClassifier, providerClassifier));
    }

    public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
        // 以美元符 `$` 开头，表示引用参数
        if (param != null && pattern.startsWith("$")) {
            pattern = param.getRawParameter(pattern.substring(1));
        }
        // 匹配
        return isMatchGlobPattern(pattern, value);
    }

    public static boolean isMatchGlobPattern(String pattern, String value) {
        // 全匹配
        if ("*".equals(pattern)) {
            return true;
        }
        // 全部为空，匹配
        if ((pattern == null || pattern.length() == 0) && (value == null || value.length() == 0)) {
            return true;
        }
        // 有一个为空，不匹配
        if ((pattern == null || pattern.length() == 0) || (value == null || value.length() == 0)) {
            return false;
        }

        // 支持 * 的通配
        int i = pattern.lastIndexOf('*');
        // doesn't find "*"
        if (i == -1) {
            return value.equals(pattern);
        }
        // "*" is at the end
        else if (i == pattern.length() - 1) {
            return value.startsWith(pattern.substring(0, i));
        }
        // "*" is at the beginning
        else if (i == 0) {
            return value.endsWith(pattern.substring(i + 1));
        }
        // "*" is in the middle
        else {
            String prefix = pattern.substring(0, i);
            String suffix = pattern.substring(i + 1);
            return value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    /**
     * 判断服务键是否匹配
     *
     * @param pattern 匹配 URL
     * @param value 被匹配 URL
     * @return 是否
     */
    public static boolean isServiceKeyMatch(URL pattern, URL value) {
                // interface
        return pattern.getParameter(Constants.INTERFACE_KEY).equals(value.getParameter(Constants.INTERFACE_KEY))
                // group
                && isItemMatch(pattern.getParameter(Constants.GROUP_KEY), value.getParameter(Constants.GROUP_KEY))
                // version
                && isItemMatch(pattern.getParameter(Constants.VERSION_KEY), value.getParameter(Constants.VERSION_KEY));
    }

    /**
     * Check if the given value matches the given pattern. The pattern supports wildcard "*".
     *
     * @param pattern pattern
     * @param value   value
     * @return true if match otherwise false
     */
    static boolean isItemMatch(String pattern, String value) {
        if (pattern == null) {
            return value == null;
        } else {
            return "*".equals(pattern) || pattern.equals(value);
        }
    }
}