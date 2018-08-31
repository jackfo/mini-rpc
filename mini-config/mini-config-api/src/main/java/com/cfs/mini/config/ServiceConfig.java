package com.cfs.mini.config;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.bytecode.Wrapper;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.utils.*;
import com.cfs.mini.monitor.MonitorFactory;
import com.cfs.mini.monitor.MonitorService;
import com.cfs.mini.registry.RegistryFactory;
import com.cfs.mini.registry.RegistryService;
import com.mini.rpc.core.Invoker;
import com.mini.rpc.core.Protocol;
import com.mini.rpc.core.ProxyFactory;
import com.mini.rpc.core.cluster.ConfiguratorFactory;
import com.mini.rpc.core.service.GenericService;
import com.mini.rpc.core.support.ProtocolUtils;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.cfs.mini.common.utils.NetUtils.getAvailablePort;
import static com.cfs.mini.common.utils.NetUtils.getLocalHost;
import static com.cfs.mini.common.utils.NetUtils.isInvalidLocalHost;

public class ServiceConfig<T> extends AbstractServiceConfig{


    private String interfaceName;
    /**
     * {@link #interfaceName} 对应的接口类
     *
     * 非配置
     */
    private Class<?> interfaceClass;

    /**
     * 延迟服务暴露线程池,一个定时的线程池
     * */
    private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MiniServiceDelayExporter", true));


    private T ref;


    /**取消服务暴露*/
    private transient volatile boolean unexported;

    /**为true表示服务已经暴露*/
    private transient volatile boolean exported;

    protected String version;

    /**当前服务配置的方法*/
    private List<MethodConfig> methods;


    /**是否泛化*/
    private volatile String generic;

    protected List<RegistryConfig> registries;

    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    private String path;

    protected String token;

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;


    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();


    /**生成的随机端口*/
    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();


    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? (String) null : interfaceClass.getName());
    }

    /**
     * 开始暴露服务
     * */
    public synchronized void export() {

        //表示Service的export属性为空 则设置为Provider的export属性
        if(provider!=null){
            if (export == null) {
                export = provider.getExport();
            }
        }

        //如果export为空或者为false则表示不暴露服务
        if (export != null && !export) {
            return;
        }

        // 延迟暴露
        if (delay != null && delay > 0) {
            delayExportExecutor.schedule(new Runnable() {
                public void run() {
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS);
            // 立即暴露
        } else {
            doExport();
        }
    }

    /**
     *
     * */
    protected synchronized void doExport() {
        //如果已经取消暴露,则当前服务不能暴露了
        //todo 哪些情况下会取消服务暴露 以及取消暴露在框架执行的哪个过程
        if (unexported) {
            throw new IllegalStateException("Already unexported!");
        }

        if (exported) {
            return;
        }
        //既然开始暴露服务了,就需要将这个过程设置为true
        exported = true;

        //进行相应的接口校验
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<mini:service interface=\"\" /> interface not allow null!");
        }

        //TODO:拼接一些属性设置到ProviderConfig 根据properties文件


        if(provider!=null){

        }

        //泛化接口的实现
        if(ref instanceof GenericService){
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        }else{
            try {
                //加载当前类到加载器中
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            // 校验接口和方法
            checkInterfaceAndMethods(interfaceClass, methods);
            // 校验指向的 service 对象
            checkRef();
            generic = Boolean.FALSE.toString();
        }


        //处理服务接口端本地代理
        if(stub != null){
            if("true".equals(stub)){
                stub = interfaceName+"Stub";
            }
            Class<?> stubClass=null;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }

        //校验ApplicationConfig
        checkApplication();
        // 校验 RegistryConfig 配置。
        checkRegistry();
        // 校验 ProtocolConfig 配置数组。
        checkProtocol();
        // 读取环境变量和 properties 配置到 ServiceConfig 对象。
        appendProperties(this);
        // 校验 Stub 和 Mock 相关的配置
        //checkStubAndMock(interfaceClass);
        // 服务路径，缺省为接口名
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }


    }


    private void doExportUrls() {
        List<URL> registryURLs = loadRegistries(true);
        /**
         * 想每一个注册中心暴露服务
         * */
        for (ProtocolConfig protocolConfig : protocols) {
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }


    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs){

        //获取协议名
        String name = protocolConfig.getName();

        if(name==null||name.length()==0){
            name = "mini";
        }
        Map<String, String> map = new HashMap<String, String>();

        //添加相应的参数
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);
        map.put(Constants.MINI_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }

        appendParameters(map, application);
        //appendParameters(map, module);
        appendParameters(map, provider, Constants.DEFAULT_KEY); // ProviderConfig ，为 ServiceConfig 的默认属性，因此添加 `default` 属性前缀。
        appendParameters(map, protocolConfig);
        appendParameters(map, this);

        if (methods != null && !methods.isEmpty()) {
            for (MethodConfig method : methods) {
                // 将 MethodConfig 对象，添加到 `map` 集合中。
                appendParameters(map, method, method.getName());
                // 当 配置了 `MethodConfig.retry = false` 时，强制禁用重试
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                // 将ArgumentConfig对象数组，添加到 `map` 集合中。
                List<ArgumentConfig> arguments = method.getArguments();
                if (arguments != null && !arguments.isEmpty()) {
                    for (ArgumentConfig argument : arguments) {
                        // convert argument type
                        if (argument.getType() != null && argument.getType().length() > 0) {

                            //获取接口类的所有方法,起参数类型和方法中像是参数类型相同,则将参数添加进去
                            Method[] methods = interfaceClass.getMethods();
                            if (methods != null && methods.length > 0) {
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    // target the method, and get its signature
                                    if (methodName.equals(method.getName())) { // 找到指定方法
                                        Class<?>[] argTypes = methods[i].getParameterTypes();
                                        // one callback in the method
                                        if (argument.getIndex() != -1) { // 指定单个参数的位置 + 类型
                                            if (argTypes[argument.getIndex()].getName().equals(argument.getType())) {
                                                // 将 ArgumentConfig 对象，添加到 `map` 集合中。
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex()); // `${methodName}.${index}`
                                            } else {
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            // multiple callbacks in the method
                                            for (int j = 0; j < argTypes.length; j++) {
                                                Class<?> argClazz = argTypes[j];
                                                if (argClazz.getName().equals(argument.getType())) {
                                                    // 将 ArgumentConfig 对象，添加到 `map` 集合中。
                                                    appendParameters(map, argument, method.getName() + "." + j); // `${methodName}.${index}`
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) { // 多余的判断，因为 `argument.getIndex() == -1` 。
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (argument.getIndex() != -1) { // 指定单个参数的位置
                            // 将 ArgumentConfig 对象，添加到 `map` 集合中。
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex()); // `${methodName}.${index}`
                        } else {
                            throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        if (ProtocolUtils.isGeneric(generic)) {
            map.put("generic", generic);
            map.put("methods", Constants.ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision); // 修订本
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames(); // 获得方法数组
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            } else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }




        if (!ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) { // true || default 时，UUID 随机生成
                map.put("token", UUID.randomUUID().toString());
            } else {
                map.put("token", token);
            }
        }
        // 协议为 injvm 时，不注册，不通知。
        if ("injvm".equals(protocolConfig.getName())) {
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }

        // export service
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }

        // host、port
        String host = this.findConfigedHosts(protocolConfig, registryURLs, map);
        Integer port = this.findConfigedPorts(protocolConfig, name, map);

        // 创建 Dubbo URL 对象
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        String scope = url.getParameter(Constants.SCOPE_KEY);


        if (!Constants.SCOPE_NONE.equalsIgnoreCase(scope)) {
            //如果不是远程则进行本地暴露
            if (!Constants.SCOPE_REMOTE.equalsIgnoreCase(scope)) {
                //TODO:本地暴露待实现
            }

            //远程暴露
            if (!Constants.SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                if (registryURLs != null && !registryURLs.isEmpty()) {
                    for(URL registryURL:registryURLs){
                        /**检测参数是否是动态暴露*/
                        url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));

                        //获得监控中心URL
                        URL monitorUrl = loadMonitor(registryURL);

                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }

                        if (logger.isInfoEnabled()) {
                            logger.info("Register mini service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                        }

                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));

                    }
                }else{

                }

            }
        }


    }


    /**
     * 加载监控中心URL
     * */
    protected URL loadMonitor(URL registryURL) {
        if(monitor == null){
            String monitorAddress = ConfigUtils.getProperty("mini.monitor.address");
            String monitorProtocol = ConfigUtils.getProperty("mini.monitor.protocol");

            if ((monitorAddress == null || monitorAddress.length() == 0) && (monitorProtocol == null || monitorProtocol.length() == 0)) {
                return null;
            }

            monitor = new MonitorConfig();
            if (monitorAddress != null && monitorAddress.length() > 0) {
                monitor.setAddress(monitorAddress);
            }
            if (monitorProtocol != null && monitorProtocol.length() > 0) {
                monitor.setProtocol(monitorProtocol);
            }
        }

        appendProperties(monitor);
        // 添加 `interface` `dubbo` `timestamp` `pid` 到 `map` 集合中
        Map<String, String> map = new HashMap<String, String>();
        map.put(Constants.INTERFACE_KEY, MonitorService.class.getName());
        map.put("dubbo", Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        // 将 MonitorConfig ，添加到 `map` 集合中。
        appendParameters(map, monitor);
        // 获得地址
        String address = monitor.getAddress();
        String sysaddress = System.getProperty("dubbo.monitor.address");
        if (sysaddress != null && sysaddress.length() > 0) {
            address = sysaddress;
        }


        if (ConfigUtils.isNotEmpty(address)) {
            // 若不存在 `protocol` 参数，默认 "mini" 添加到 `map` 集合中。
            if (!map.containsKey(Constants.PROTOCOL_KEY)) {
                if (ExtensionLoader.getExtensionLoader(MonitorFactory.class).hasExtension("logstat")) {
                    map.put(Constants.PROTOCOL_KEY, "logstat");
                } else {
                    map.put(Constants.PROTOCOL_KEY, "mini");
                }
            }
            // 解析地址，创建 Dubbo URL 对象。
            return UrlUtils.parseURL(address, map);
            // 从注册中心发现监控中心地址
        } else if (Constants.REGISTRY_PROTOCOL.equals(monitor.getProtocol()) && registryURL != null) {
            return registryURL.setProtocol("dubbo").addParameter(Constants.PROTOCOL_KEY, "registry").addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map));
        }
        return null;
    }

    /**
     * 通过配置文件找主机ip
     * 主机ip获取优先级   系统属性>protocolConfig>providerConfig>本机hostname对应ip>注册中心ip>本机第一个活跃ip
     * */
    private String findConfigedHosts(ProtocolConfig protocolConfig, List<URL> registryURLs, Map<String, String> map) {
        boolean anyhost = false;


        String hostToBind = getValueFromConfig(protocolConfig, Constants.MINI_IP_TO_BIND);

        /**
         * 如果不为空,不合法则需要扔出异常了
         * */
        if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
            throw new IllegalArgumentException("Specified invalid bind ip from property:" + Constants.MINI_IP_TO_BIND + ", value:" + hostToBind);
        }

        /**
         * 如果系统中没有,则需要通过其他方法进行获取了
         * */
        if(hostToBind==null || hostToBind.length()==0){


            hostToBind = protocolConfig.getHost();
            if (provider != null && (hostToBind == null || hostToBind.length() == 0)) {
                hostToBind = provider.getHost();
            }


            if(isInvalidLocalHost(hostToBind)){
                anyhost = true;
                try{
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                }catch (UnknownHostException e){
                    logger.warn(e.getMessage(), e);
                }

                if (isInvalidLocalHost(hostToBind)) {
                    if (registryURLs != null && !registryURLs.isEmpty()) {
                        for (URL registryURL : registryURLs) {
                            try {
                                Socket socket = new Socket();
                                try {
                                    SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                    socket.connect(addr, 1000);
                                    hostToBind = socket.getLocalAddress().getHostAddress();
                                    break;
                                } finally {
                                    try {
                                        socket.close();
                                    } catch (Throwable e) {
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn(e.getMessage(), e);
                            }
                        }
                    }

                    if (isInvalidLocalHost(hostToBind)) {
                        hostToBind = getLocalHost();
                    }

                }
            }

        }


        map.put(Constants.BIND_IP_KEY, hostToBind);

        // registry ip is not used for bind ip by default
        String hostToRegistry = getValueFromConfig(protocolConfig, Constants.MINI_IP_TO_REGISTRY);
        if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.MINI_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        } else if (hostToRegistry == null || hostToRegistry.length() == 0) {
            // bind ip is used as registry ip by default
            hostToRegistry = hostToBind;
        }

        map.put(Constants.ANYHOST_KEY, String.valueOf(anyhost));

        return hostToRegistry;
    }

    /**
     * 获取端口的规则
     * */
    private Integer findConfigedPorts(ProtocolConfig protocolConfig, String name, Map<String, String> map) {
        // 第一优先级，从环境变量，获得绑定的 Port 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
        // parse bind port from environment
        String port = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_BIND);
        Integer portToBind = parsePort(port);

        // if there's no bind port found from environment, keep looking up.
        if (portToBind == null) {
            // 第二优先级，从 ProtocolConfig 获得 Port 。
            portToBind = protocolConfig.getPort();
            if (provider != null && (portToBind == null || portToBind == 0)) {
                portToBind = provider.getPort();
            }
            // 第三优先级，获得协议对应的缺省端口，
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
            if (portToBind == null || portToBind == 0) {
                portToBind = defaultPort;
            }
            // 第四优先级，随机获得端口
            if (portToBind <= 0) {
                portToBind = getRandomPort(name); // 先从缓存中获得端口
                if (portToBind == null || portToBind < 0) {
                    // 获得可用端口
                    portToBind = getAvailablePort(defaultPort);
                    // 添加到缓存
                    putRandomPort(name, portToBind);
                }
                logger.warn("Use random available port(" + portToBind + ") for protocol " + name);
            }
        }

        // save bind port, used as url's key later
        map.put(Constants.BIND_PORT_KEY, String.valueOf(portToBind));

        // 获得 `portToRegistry` ，默认使用 `portToBind` 。可强制指定，参见仓库 https://github.com/dubbo/dubbo-docker-sample
        // registry port, not used as bind port by default
        String portToRegistryStr = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_REGISTRY);
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (portToRegistry == null) {
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    /**
     * 添加随机端口到缓存中
     *
     * @param protocol 协议名
     * @param port 端口
     */
    private static void putRandomPort(String protocol, Integer port) {
        protocol = protocol.toLowerCase();
        if (!RANDOM_PORT_MAP.containsKey(protocol)) {
            RANDOM_PORT_MAP.put(protocol, port);
        }
    }




    private static Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase();
        if (RANDOM_PORT_MAP.containsKey(protocol)) {
            return RANDOM_PORT_MAP.get(protocol);
        }
        return Integer.MIN_VALUE;
    }

    private Integer parsePort(String configPort) {
        Integer port = null;
        if (configPort != null && configPort.length() > 0) {
            try {
                Integer intPort = Integer.parseInt(configPort);
                if (isInvalidPort(intPort)) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
                port = intPort;
            } catch (Exception e) {
                throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
            }
        }
        return port;
    }



    public static boolean isInvalidPort(int port) {
        return port <= MIN_PORT || port > MAX_PORT;
    }

    /**
     * 从协议配置对象解析对应的配置项
     *
     * @param protocolConfig 协议配置对象
     * @param key 配置项
     * @return 值
     */
    private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
        String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        if (port == null || port.length() == 0) {
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    /**
     * 加载注册中心数组
     * */
    protected List<URL> loadRegistries(boolean provider) {
        //加载之前依旧做一个检查
        checkRegistry();

        //创建注册中心的URL数组
        List<URL> registryList = new ArrayList<URL>();

        if(registries!=null&&!registries.isEmpty()){
            for(RegistryConfig config:registries){

                String address = config.getAddress();

                if(address==null||address.length()==0){
                    address = Constants.ANYHOST_VALUE;
                }

                /**如果系统参数存在,优先使用系统参数*/
                String sysaddress = System.getProperty("mini.registry.address");
                if (sysaddress != null && sysaddress.length() > 0) {
                    address = sysaddress;
                }

                if(address.length()>0&&!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)){
                    Map<String,String> map = new HashMap<>();
                    //将各种参数查看进去
                    appendParameters(map, application);
                    appendParameters(map, config);
                    map.put("path", RegistryService.class.getName());
                    map.put("mini", Version.getVersion());
                    map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));

                    if (ConfigUtils.getPid() > 0) {
                        map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
                    }

                    if (!map.containsKey("protocol")) {
                        //通过SPi机制检测决定是使用远程还是本地注册中心
                        if (ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension("remote")) { // "remote"
                            map.put("protocol", "remote");
                        } else {
                            map.put("protocol", "mini");
                        }
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);
                    // 循环 `url` ，设置 "registry" 和 "protocol" 属性。
                    for (URL url : urls) {
                        // 设置 `registry=${protocol}` 和 `protocol=registry` 到 URL
                        url = url.addParameter(Constants.REGISTRY_KEY, url.getProtocol());
                        url = url.setProtocol(Constants.REGISTRY_PROTOCOL);
                        // 添加到结果
                        if ((provider && url.getParameter(Constants.REGISTER_KEY, true)) // 服务提供者 && 注册 https://dubbo.gitbooks.io/dubbo-user-book/demos/subscribe-only.html
                                || (!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))) { // 服务消费者 && 订阅 https://dubbo.gitbooks.io/dubbo-user-book/demos/registry-only.html
                            registryList.add(url);
                        }
                    }

                }

            }
        }
        return registryList;
    }

    /**
     * 校验 ProtocolConfig 配置数组。
     * 实际上，会拼接属性配置（环境变量 + properties 属性）到 ProtocolConfig 对象数组。
     */
    private void checkProtocol() {
        // 当 ProtocolConfig 对象数组为空时，优先使用 `ProviderConfig.protocols` 。其次，进行创建。
        if ((protocols == null || protocols.isEmpty())
                && provider != null) {
            setProtocols(provider.getProtocols());
        }
        // backward compatibility 向后兼容
        if (protocols == null || protocols.isEmpty()) {
            setProtocol(new ProtocolConfig());
        }
        // 拼接属性配置（环境变量 + properties 属性）到 ProtocolConfig 对象数组
        for (ProtocolConfig protocolConfig : protocols) {
            if (StringUtils.isEmpty(protocolConfig.getName())) {
                protocolConfig.setName("mini");
            }
            appendProperties(protocolConfig);
        }
    }
    /**
     * 检验application
     * 首先根据XML标签解析,如果标签不存在则从西戎属性中获取
     *
     * */
    protected void checkApplication(){

        if (application == null) {
            String applicationName = ConfigUtils.getProperty("mini.application.name");
            if (applicationName != null && applicationName.length() > 0) {
                application = new ApplicationConfig();
            }
        }

        if (application == null) {
            throw new IllegalStateException(
                    "No such application config! Please add <mini:application name=\"...\" /> to your spring config.");
        }

        //给相应的配置文件setter方法注入属性
        appendProperties(application);

        String wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, wait.trim());
        } else {
            wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (wait != null && wait.trim().length() > 0) {
                System.setProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }
    }

    protected void checkRegistry() {
        // 当 RegistryConfig 对象数组为空时，若有 `dubbo.registry.address` 配置，进行创建。
        // for backward compatibility 向后兼容
        if (registries == null || registries.isEmpty()) {
            String address = ConfigUtils.getProperty("dubbo.registry.address");
            if (address != null && address.length() > 0) {
                registries = new ArrayList<RegistryConfig>();
                String[] as = address.split("\\s*[|]+\\s*");
                for (String a : as) {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setAddress(a);
                    registries.add(registryConfig);
                }
            }
        }
        if ((registries == null || registries.isEmpty())) {
            throw new IllegalStateException((getClass().getSimpleName().startsWith("Reference")
                    ? "No such any registry to refer service in consumer "
                    : "No such any registry to export service in provider ")
                    + getLocalHost()
                    + " use dubbo version "
                    + Version.getVersion()
                    + ", Please add <dubbo:registry address=\"...\" /> to your spring config. If you want unregister, please set <dubbo:service registry=\"N/A\" />");
        }
        // 读取环境变量和 properties 配置到 RegistryConfig 对象数组。
        for (RegistryConfig registryConfig : registries) {
            appendProperties(registryConfig);
        }
    }

    private ProviderConfig provider;

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }


    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }

    @Deprecated
    private static final List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());

        //根据provider获取协议相关属性
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }

        return protocols;
    }

    @Deprecated
    private static final ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
//        protocol.setName(provider.getProtocol().getName());
//        protocol.setServer(provider.getServer());
//        protocol.setClient(provider.getClient());
//        protocol.setCodec(provider.getCodec());
//        protocol.setHost(provider.getHost());
//        protocol.setPort(provider.getPort());
//        protocol.setPath(provider.getPath());
//        protocol.setPayload(provider.getPayload());
//        protocol.setThreads(provider.getThreads());
//        protocol.setParameters(provider.getParameters());
        return protocol;
    }


    /**
     * 检验ref
     * 保证ref不能为空
     * 并且interfaceClass是一个实例
     * 保证ref对象实现了interfaceClass接口
     * */
    private void checkRef() {
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }




}
