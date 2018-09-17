package com.cfs.mini.registry.integration;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConfigUtils;
import com.cfs.mini.common.utils.NamedThreadFactory;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.Registry;
import com.cfs.mini.registry.RegistryFactory;
import com.cfs.mini.registry.RegistryService;
import com.cfs.mini.registry.support.ProviderConsumerRegTable;
import com.cfs.mini.rpc.core.*;
import com.cfs.mini.rpc.core.cluster.Cluster;
import com.cfs.mini.rpc.core.cluster.Configurator;
import com.cfs.mini.rpc.core.protocol.InvokerWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.cfs.mini.common.Constants.ACCEPT_FOREIGN_IP;
import static com.cfs.mini.common.Constants.QOS_ENABLE;
import static com.cfs.mini.common.Constants.QOS_PORT;

public class RegistryProtocol implements Protocol {


    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);

    private ProxyFactory proxyFactory;

    private Cluster cluster;

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }


    /**
     * 当前属性通过injectExtension方法,在SPI加载的时候进行注入
     * */
    private Protocol protocol;

    private static RegistryProtocol INSTANCE;

    private RegistryFactory registryFactory;

    public RegistryFactory getRegistryFactory() {
        return registryFactory;
    }

    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public RegistryProtocol() {
        INSTANCE = this;
    }

    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap();

    @Override
    public int getDefaultPort() {
        return 9090;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> originInvoker) throws RpcException {

        /**本地通过netty等形式将服务暴露*/
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker);

        /**获取相应的registryUrl*/
        URL registryUrl = getRegistryUrl(originInvoker);

        /**连接zookeeper等注册中心并进行相关注册*/
        final Registry registry = getRegistry(originInvoker);

        /**主要是移除了一些参数*/
        final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);

        /**检测当前服务是否需要注册到注册中心*/
        boolean register = registedProviderUrl.getParameter("register", true);

        /**将当前服务添加到一个全局MAP集合*/
        ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registedProviderUrl);


        if (register) {
            /**将相应URL注册到注册中心*/
            register(registryUrl, registedProviderUrl);
            /** 标记向本地注册表的注册服务提供者，已经注册*/
            ProviderConsumerRegTable.getProviderWrapper(originInvoker).setReg(true);
        }

        /**添加定义URL的监听器*/
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registedProviderUrl);
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);

        return new DestroyableExporter<T>(exporter, originInvoker, overrideSubscribeUrl, registedProviderUrl);

    }


    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY, // configurators
                        Constants.CHECK_KEY, String.valueOf(false)); // 订阅失败，不校验。因为，不需要检查。
    }

    public void register(URL registryUrl, URL registedProviderUrl) {
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.register(registedProviderUrl);
    }




    private URL getRegistedProviderUrl(final Invoker<?> originInvoker) {
        // 从注册中心的 export 参数中，获得服务提供者的 URL
        URL providerUrl = getProviderUrl(originInvoker);
        //The address you see at the registry
        return providerUrl.removeParameters(getFilteredKeys(providerUrl)) // 移除 .hide 为前缀的参数
                .removeParameter(Constants.MONITOR_KEY) // monitor
                .removeParameter(Constants.BIND_IP_KEY) // bind.ip
                .removeParameter(Constants.BIND_PORT_KEY) // bind.port
                .removeParameter(QOS_ENABLE) // qos.enable
                .removeParameter(QOS_PORT) // qos.port
                .removeParameter(ACCEPT_FOREIGN_IP); // qos.accept.foreign.ip
    }

    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> filteredKeys = new ArrayList<String>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
                    filteredKeys.add(entry.getKey());
                }
            }
            return filteredKeys.toArray(new String[filteredKeys.size()]);
        } else {
            return new String[]{};
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // 获得真实的注册中心的 URL
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);

        Registry registry = registryFactory.getRegistry(url);

        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        String group = qs.get(Constants.GROUP_KEY);

        if (group != null && group.length() > 0) {
           throw new RuntimeException("分组聚合暂时未实现");
        }
        // 执行服务引用
        return doRefer(cluster, registry, type, url);
    }

    /**
     * 根据URL获取相应的Invoker
     * */
    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        // 创建 RegistryDirectory 对象，并设置注册中心
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
//        directory.setRegistry(registry);
//        directory.setProtocol(protocol);
        // 创建订阅 URL
        // all attributes of REFER_KEY
        Map<String, String> parameters = new HashMap<String, String>(directory.getUrl().getParameters()); // 服务引用配置集合
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, parameters.remove(Constants.REGISTER_IP_KEY), 0, type.getName(), parameters);
        // 向注册中心注册自己（服务消费者）
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                    Constants.CHECK_KEY, String.valueOf(false))); // 不检查的原因是，不需要检查。
        }


        // 创建 Invoker 对象
        Invoker invoker = cluster.join(directory);

        return invoker;

    }
    @Override
    public void destroy() {

    }

    private Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return registryFactory.getRegistry(registryUrl);
    }


    /**
     * protocol.export(invokerDelegete) 会暴露服务
     * */
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker) {
        String key = getCacheKey(originInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                // 未暴露过，进行暴露服务
                if (exporter == null) {
                    // 创建 Invoker Delegate 对象
                    final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                    // 暴露服务，创建 Exporter 对象
                    // 使用 创建的Exporter对象 + originInvoker ，创建 ExporterChangeableWrapper 对象
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegete), originInvoker);
                    // 添加到 `bounds`
                    bounds.put(key, exporter);
                }
            }
        }
        return exporter;
    }



    /**
     * 如果协议是registry,则重新设置协议为默认
     * */
    private URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) { // protocol
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        return registryUrl;
    }

    public static class InvokerDelegete<T> extends InvokerWrapper<T> {

        /**
         * Invoker 对象
         *
         * 因为父类未提供 invoker 属性的获取方法，因此这里增加了和父类 invoker 一样的这个属性。
         */
        private final Invoker<T> invoker;

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegete(Invoker<T> invoker, URL url) {
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegete) {
                return ((InvokerDelegete<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }


    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        return providerUrl.removeParameters("dynamic", "enabled").toFullString();
    }

    private URL getProviderUrl(final Invoker<?> origininvoker) {
        String export = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY); // export
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }
        return URL.valueOf(export);
    }

    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final Invoker<T> originInvoker;
        /**
         * 暴露的 Exporter 对象
         */
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            // 移除出 `bounds`
            bounds.remove(key);
            // 取消暴露
            exporter.unexport();
        }
    }


    static private class DestroyableExporter<T> implements Exporter<T>{


        public static final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));

        private Exporter<T> exporter;
        private Invoker<T> originInvoker;
        private URL subscribeUrl;
        private URL registerUrl;

        public DestroyableExporter(Exporter<T> exporter, Invoker<T> originInvoker, URL subscribeUrl, URL registerUrl) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
            this.subscribeUrl = subscribeUrl;
            this.registerUrl = registerUrl;
        }

        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            Registry registry = RegistryProtocol.INSTANCE.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        int timeout = ConfigUtils.getServerShutdownTimeout();
                        if (timeout > 0) {
                            logger.info("Waiting " + timeout + "ms for registry to notify all consumers before unexport. Usually, this is called when you use MINI API");
                            Thread.sleep(timeout);
                        }
                        exporter.unexport();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            });

        }
    }




    private class OverrideListener implements NotifyListener {

        /**
         * 订阅 URL 对象
         */
        private final URL subscribeUrl;
        /**
         * 原始 Invoker 对象
         */
        private final Invoker originInvoker;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }


        @Override
        public synchronized void notify(List<URL> urls) {
            throw new RuntimeException("运行时异常");
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            throw new RuntimeException("运行时异常");

        }

        // Merge the urls of configurators
        private URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
            throw new RuntimeException("运行时异常");

        }
    }

}
