package com.cfs.mini.config;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.utils.*;
import com.cfs.mini.registry.RegistryFactory;
import com.cfs.mini.registry.RegistryService;
import com.mini.rpc.core.service.GenericService;
import com.mini.rpc.core.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
                    + NetUtils.getLocalHost()
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
