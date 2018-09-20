package com.cfs.mini.registry.integration;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.Registry;
import com.cfs.mini.rpc.core.*;
import com.cfs.mini.rpc.core.cluster.Configurator;
import com.cfs.mini.rpc.core.cluster.ConfiguratorFactory;
import com.cfs.mini.rpc.core.cluster.Router;
import com.cfs.mini.rpc.core.cluster.directory.AbstractDirectory;
import com.cfs.mini.rpc.core.protocol.InvokerWrapper;
import com.cfs.mini.rpc.core.support.ClusterUtils;
import com.cfs.mini.rpc.core.support.RpcUtils;

import java.util.*;

public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);


    private volatile Map<String, List<Invoker<T>>> methodInvokerMap;


    private volatile boolean forbidden = false;

    /**调用的服务类*/
    private final Class<T> serviceType;

    /**url与服务提供者的映射关系*/
    private volatile Map<String, Invoker<T>> urlInvokerMap;

    /**注册中心的服务类*/
    private final String serviceKey;

    private volatile List<Configurator> configurators;

    private static final ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();

    /**
     * 服务方法数组
     */
    private final String[] serviceMethods;

    private volatile URL overrideDirectoryUrl;

    /**
     * 注册中心的 Protocol 对象
     */
    private Protocol protocol; // Initialization at the time of injection, the assertion is not null
    /**
     * 注册中心
     */
    private Registry registry;

    /**服务提供者的缓存集合*/
    private volatile Set<URL> cachedInvokerUrls;

    private final Map<String, String> queryMap;

    private final URL directoryUrl;


    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void subscribe(URL url) {
        // 设置消费者 URL
        setConsumerUrl(url);
        // 向注册中心，发起订阅
        registry.subscribe(url, this);
    }


    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if (serviceType == null) {
            throw new IllegalArgumentException("service type is null.");
        }
        if (url.getServiceKey() == null || url.getServiceKey().length() == 0) {
            throw new IllegalArgumentException("registry serviceKey is null.");
        }
        //设置服务类型 即接口名
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        this.overrideDirectoryUrl = this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        String methods = queryMap.get(Constants.METHODS_KEY);
        this.serviceMethods = methods == null ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }


    @Override
    public void notify(List<URL> urls) {
        /**服务提供者*/
        List<URL> invokerUrls = new ArrayList<URL>();
        /**服务路由*/
        List<URL> routerUrls = new ArrayList<URL>();
        /**服务配置*/
        List<URL> configuratorUrls = new ArrayList<URL>();
        for (URL url : urls) {
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            if (Constants.ROUTERS_CATEGORY.equals(category) || Constants.ROUTE_PROTOCOL.equals(protocol)) {
                routerUrls.add(url);
            } else if (Constants.CONFIGURATORS_CATEGORY.equals(category) || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
                configuratorUrls.add(url);
            } else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
                invokerUrls.add(url);
            } else {
                logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
            }
        }

        if (!configuratorUrls.isEmpty()) {
            this.configurators = toConfigurators(configuratorUrls);
        }


        //TODO:处理路由规则URL集合

        /**处理服务提供者集合*/
        refreshInvoker(invokerUrls);
    }


    private void refreshInvoker(List<URL> invokerUrls) {
        //如果只存在一个空协议则禁止访问
        if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null
                && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            // 设置禁止访问
            this.forbidden = true; // Forbid to access
            // methodInvokerMap 置空
            this.methodInvokerMap = null; // Set the method invoker map to null
            // 销毁所有 Invoker 集合
            destroyAllInvokers();
        }else{
            //设置允许访问
            this.forbidden = false;

            // 引用老的 urlInvokerMap
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
            // 传入的 invokerUrls 为空，说明是路由规则或配置规则发生改变，此时 invokerUrls 是空的，直接使用 cachedInvokerUrls 。
            if (invokerUrls.isEmpty() && this.cachedInvokerUrls != null) {
                invokerUrls.addAll(this.cachedInvokerUrls);
                // 传入的 invokerUrls 非空，更新 cachedInvokerUrls 。
            } else {
                this.cachedInvokerUrls = new HashSet<URL>();
                this.cachedInvokerUrls.addAll(invokerUrls); //Cached invoker urls, convenient for comparison //缓存invokerUrls列表，便于交叉对比
            }

            if (invokerUrls.isEmpty()) {
                return;
            }

            //将传入的invokerUrls转化为新的invoker
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);

            /**转化新的对应methodInvokerMap*/
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap);

            this.methodInvokerMap = newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;

        }
    }

    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        // 创建新的 `methodInvokerMap`
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();

        // 创建 Invoker 集合
        List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();

        /**获取所有的Map进行处理*/
        if (invokersMap != null && invokersMap.size() > 0) {
            // 循环每个服务提供者 Invoker
            for (Invoker<T> invoker : invokersMap.values()) {
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY); // methods
                if (parameter != null && parameter.length() > 0) {
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        // 循环每个方法，按照方法名为维度，聚合到 `methodInvokerMap` 中
                        for (String method : methods) {
                            if (method != null && method.length() > 0 && !Constants.ANY_VALUE.equals(method)) { // 当服务提供者的方法为 "*" ，代表泛化调用
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }

                invokersList.add(invoker);
            }
        }
        List<Invoker<T>> newInvokersList = route(invokersList, null);

        // 添加 `newInvokersList` 到 `newMethodInvokerMap` 中，表示该服务提供者的全量 Invoker 集合
        newMethodInvokerMap.put(Constants.ANY_VALUE, newInvokersList);

        // 循环，基于每个方法路由，匹配合适的 Invoker 集合
        if (serviceMethods != null && serviceMethods.length > 0) {
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (methodInvokers == null || methodInvokers.isEmpty()) {
                    methodInvokers = newInvokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }

        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);

    }


    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        // 创建 Invocation 对象
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        // 获得 Router 数组
        List<Router> routers = getRouters();
        // 根据路由规则，筛选 Invoker 集合
        if (routers != null) {
            for (Router router : routers) {
                if (router.getUrl() != null) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }


    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        // 新的 `newUrlInvokerMap`
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
        // 若为空，直接返回
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }

        // 已初始化的服务器提供 URL 集合
        Set<String> keys = new HashSet<String>();
        // 获得引用服务的协议
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);

        for(URL providerUrl:urls){
            if(queryProtocols!=null&&queryProtocols.length()>0){
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(",");
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)) {
                        accept = true;
                        break;
                    }
                }
                if (!accept) {
                    continue;
                }
            }

            if(Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())){
                continue;
            }

            // 忽略，若应用程序不支持该协议
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost()
                        + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }

            URL url = mergeUrl(providerUrl);

            String key = url.toFullString();

            if (keys.contains(key)) {
                continue;
            }

            keys.add(key);

            // 如果服务端 URL 发生变化，则重新 refer 引用
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);

            if(invoker == null){
                try{
                    // 判断是否开启
                    boolean enabled;
                    if (url.hasParameter(Constants.DISABLED_KEY)) {
                        enabled = !url.getParameter(Constants.DISABLED_KEY, false);
                    } else {
                        enabled = url.getParameter(Constants.ENABLED_KEY, true);
                    }
                    // 若开启，创建 Invoker 对象
                    if (enabled) {
                        // 注意，引用服务
                        invoker = new InvokerDelegate<T>(protocol.refer(serviceType, url), url, providerUrl);
                    }

                }catch (Throwable t){
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + url + ")" + t.getMessage(), t);
                }

                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(key, invoker);
                }
            } else { // 在缓存中，直接使用缓存的 Invoker 对象，添加到 newUrlInvokerMap 中
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;

    }


    /**
     * Merge url parameters. the order is: override > -D >Consumer > Provider
     *
     * 合并 URL 参数，优先级为配置规则 > 服务消费者配置 > 服务提供者配置
     *
     * @param providerUrl 服务提供者 URL
     * @return 合并后的 URL
     */
    private URL mergeUrl(URL providerUrl) {
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap);

        // 合并配置规则
        List<Configurator> localConfigurators = this.configurators; // local reference
        if (localConfigurators != null && !localConfigurators.isEmpty()) {
            for (Configurator configurator : localConfigurators) {
                providerUrl = configurator.configure(providerUrl);
            }
        }

        // 不检查连接是否成功，总是创建 Invoker ！因为，启动检查，只有启动阶段需要。此时在检查，已经没必要了。
        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false)); // Do not check whether the connection is successful or not, always create Invoker!

        // The combination of directoryUrl and override is at the end of notify, which can't be handled here
        // 仅合并提供者参数，因为 directoryUrl 与 override 合并是在 notify 的最后，这里不能够处理
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters()); // Merge the provider side parameters

        // 【忽略】因为是对 1.0 版本的兼容
        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0)
                && "dubbo".equals(providerUrl.getProtocol())) { // Compatible version 1.0
            //fix by tony.chenl DUBBO-44
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }

        // 返回服务提供者 URL
        return providerUrl;
    }

    public void destroyAllInvokers(){
        Map<String,Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
        if(localUrlInvokerMap!=null){
            for(Invoker invoker:new ArrayList<>(localUrlInvokerMap.values())){
                invoker.destroy();
            }
            localUrlInvokerMap.clear();
        }
        /**将方法与Invoker之间的映射置为空*/
        methodInvokerMap = null;
    }

    /**
     * 将overrideURL 转换为 map，供重新 refer 时使用.
     * 每次下发全部规则，全部重新组装计算
     *
     * @param urls 契约：
     *             </br>1.override://0.0.0.0/...(或override://ip:port...?anyhost=true)&para1=value1...表示全局规则(对所有的提供者全部生效)
     *             </br>2.override://ip:port...?anyhost=false 特例规则（只针对某个提供者生效）
     *             </br>3.不支持override://规则... 需要注册中心自行计算.
     *             </br>4.不带参数的override://0.0.0.0/ 表示清除override
     *
     * @return Configurator 集合
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {
        // 忽略，若配置规则 URL 集合为空
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        // 创建 Configurator 集合
        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());

        for(URL url:urls){
            // 若协议为 `empty://` ，意味着清空所有配置规则，因此返回空 Configurator 集合
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }

            Map<String, String> override = new HashMap<String, String>(url.getParameters());
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0) {
                configurators.clear();
                continue;
            }
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        // 排序
        Collections.sort(configurators);
        return configurators;
    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {
        if (forbidden) {
            // 1. No service provider 2. Service providers are disabled
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION,
                    "No provider available from registry " + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey() + " on consumer " +  NetUtils.getLocalHost()
                            + " use mini version " + Version.getVersion() + ", please check status of providers(disabled, not registered or in blacklist).");
        }
        List<Invoker<T>> invokers = null;
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap; // local reference
        // 获得 Invoker 集合
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            // 获得方法名、方法参数
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);
            // 【第一】可根据第一个参数枚举路由
            if (args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
//                invokers = localMethodInvokerMap.get(methodName + "." + args[0]); // The routing can be enumerated according to the first parameter
                invokers = localMethodInvokerMap.get(methodName + args[0]); // The routing can be enumerated according to the first parameter
            }
            // 【第二】根据方法名获得 Invoker 集合
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(methodName);
            }
            // 【第三】使用全量 Invoker 集合。例如，`#$echo(name)` ，回声方法
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
            // 【第四】使用 `methodInvokerMap` 第一个 Invoker 集合。防御性编程。
            if (invokers == null) {
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()) {
                    invokers = iterator.next();
                }
            }
        }
        return invokers == null ? new ArrayList<Invoker<T>>(0) : invokers;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    private static class InvokerDelegate<T> extends InvokerWrapper<T> {

        /**
         * 服务提供者 URL
         *
         * 未经过配置合并
         */
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }

    }

    /**
     * Invoker 排序器，根据 URL 升序
     */
    private static class InvokerComparator implements Comparator<Invoker<?>> {

        /**
         * 单例
         */
        private static final InvokerComparator comparator = new InvokerComparator();

        private InvokerComparator() {
        }

        public static InvokerComparator getComparator() {
            return comparator;
        }

        @Override
        public int compare(Invoker<?> o1, Invoker<?> o2) {
            return o1.getUrl().toString().compareTo(o2.getUrl().toString());
        }

    }
}
