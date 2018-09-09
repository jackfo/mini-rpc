package com.cfs.mini.config;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.utils.ConfigUtils;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.common.utils.UrlUtils;
import com.cfs.mini.registry.RegistryFactory;
import com.cfs.mini.registry.RegistryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cfs.mini.common.utils.NetUtils.getLocalHost;

public  abstract class AbstractInterfaceConfig extends AbstractMethodConfig {


    protected String stub;

    public String getStub() {
        return stub;
    }

    public void setStub(String stub) {
        this.stub = stub;
    }


    /**
     * 解析应用配置
     *
     * */
    protected ApplicationConfig application;

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }


    protected List<RegistryConfig> registries;

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    /**
     *
     * */
    protected MonitorConfig monitor;

    /**
     * 检验接口是否存在配置文件中方法
     * */
    protected void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
         if(interfaceClass == null ){
             throw new IllegalStateException("interface not allow null!");
         }

        //验证类是否是一个接口
         if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }

        /**
         * 检验接口是否存在当前方法,如果接口不存在这个方法,那么是无法进行暴露的
         * 这种情况是防止配置是否显示配置了一个在接口中不存在的方法
         * */
        if(methods !=null && !methods.isEmpty()){
             for(MethodConfig methodConfig:methods){
                 String methodName = methodConfig.getName();
                 if (methodName == null || methodName.length() == 0) {
                     throw new IllegalStateException("<mini:method> name attribute is required! Please check: <dubbo:service interface=\"" + interfaceClass.getName() + "\" ... ><dubbo:method name=\"\" ... /></<dubbo:reference>");
                 }
                 boolean hasMethod = false;
                 for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
                     if (method.getName().equals(methodName)) {
                         hasMethod = true;
                         break;
                     }
                 }
                 if (!hasMethod) {
                     throw new IllegalStateException("The interface " + interfaceClass.getName() + " not found method " + methodName);
                 }
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
                        //设置相应的协议
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

    /**
     * 检查registries 如果registries会通过系统属性再进行获取,以及不存在则会扔出异常
     * */
    protected void checkRegistry() {
        // 当 RegistryConfig 对象数组为空时，若有 `mini.registry.address` 配置，进行创建。
        // for backward compatibility 向后兼容
        if (registries == null || registries.isEmpty()) {
            String address = ConfigUtils.getProperty("mini.registry.address");
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
                    + ", Please add <mini:registry address=\"...\" /> to your spring config. If you want unregister, please set <dubbo:service registry=\"N/A\" />");
        }

        // 读取环境变量和 properties 配置到 RegistryConfig 对象数组。
        for (RegistryConfig registryConfig : registries) {
            appendProperties(registryConfig);
        }
    }




}
