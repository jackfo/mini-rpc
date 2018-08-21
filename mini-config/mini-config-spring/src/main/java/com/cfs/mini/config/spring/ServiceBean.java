package com.cfs.mini.config.spring;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.config.ProtocolConfig;
import com.cfs.mini.config.ProviderConfig;
import com.cfs.mini.config.ServiceConfig;
import com.cfs.mini.config.spring.extension.SpringExtensionFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ServiceBean<T> extends ServiceConfig implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanNameAware {

    private Logger logger = LoggerFactory.getLogger(ServiceBean.class);


    private transient ApplicationContext applicationContext;

    private static transient ApplicationContext SPRING_CONTEXT;


    /**
     * 不会序列化的字段
     * */
    private transient String beanName;

    private transient boolean supportedApplicationListener;

    /**
     * 继承了应用
     * */
    public void setApplicationContext(ApplicationContext applicationContext) {
        logger.info("setApplicationContext");
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
        //添加相应的应用监听器
        if (applicationContext != null) {
            SPRING_CONTEXT = applicationContext;
            try {
                //在这里使用反射的原因是applicationContext这个接口不直接具有addApplicationListener这个方法,在它的具体实现类中存在
                //所以这里需要通过反射来获取相应的方法,并进行相应的调用  前提子类必须实现这个方法
                //TODO:思考这样做的好处为什么
                Method method = applicationContext.getClass().getMethod("addApplicationListener", new Class<?>[]{ApplicationListener.class}); // backward compatibility to spring 2.0.1
                method.invoke(applicationContext, new Object[]{this});
                supportedApplicationListener = true;
            } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext) {
                    try {
                        Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", new Class<?>[]{ApplicationListener.class}); // backward compatibility to spring 2.0.1
                        if (!method.isAccessible()) {
                            method.setAccessible(true);
                        }
                        method.invoke(applicationContext, new Object[]{this});
                        supportedApplicationListener = true;
                    } catch (Throwable t2) {
                    }
                }
            }
        }
    }

    /**
     * 这个监听方法是与上面setApplicationContext配套
     * 在这里开始准备暴露服务了
     * */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
//        if (isDelay() && !isExported() && !isUnexported()) {
//            if (logger.isInfoEnabled()) {
//                logger.info("The service ready on spring started. service: " + getInterface());
//            }
//            export();
//        }
        export();
        System.out.println("准备开始直接暴露服务了");
    }

    /**
     * 继承了BeanNameAware接口则需要实现这个方法,并且在初始化Bean的时候会调用当前方法
     * */
    @Override
    public void setBeanName(String name) {
        logger.info("setBeanName");
        this.beanName = name;
    }

    @Override
    public void destroy() throws Exception {
        logger.info("destroy");
    }

    /**
     * Service的Provider等其它属性都是在这个位置进行设置
     * 设置了解析标签的属性注入之后
     * 则开始进行服务暴露
     * */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("afterPropertiesSet");
        if(getProvider()==null){
            //获取所有在上下文中的ProviderConfig,其中k是beanName
            Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class, false, false);

            if(providerConfigMap!=null&&providerConfigMap.size()>0){
                //找到所有协议配置文件
                Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);

                //在protocolConfigMap为空的情况下,需要根据providerConfig来解析protocolConfig,并进行注入
                if( (protocolConfigMap!=null||protocolConfigMap.size()==0) && providerConfigMap.size()>1){

                    List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
                    for (ProviderConfig config : providerConfigMap.values()) {
                        if (config.isDefault() != null && config.isDefault().booleanValue()) {
                            providerConfigs.add(config);
                        }
                    }
                    if (!providerConfigs.isEmpty()) {
                        setProviders(providerConfigs);
                    }
                }
            }else{
                /**
                 * 走到else分支表示存在protocolConfig
                 * 根据providerConfigMap中找出默认的providerConfig
                 * 如果默认值为空或者isDefault的布尔值为真则当前这个即为默认的providerConfig 这种情况不能存在两个
                 * 之后将其注入
                 * */
                ProviderConfig providerConfig = null;
                for (ProviderConfig config : providerConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (providerConfig != null) {
                            throw new IllegalStateException("Duplicate provider configs: " + providerConfig + " and " + config);
                        }
                        providerConfig = config;
                    }
                }
                if (providerConfig != null) {
                    setProvider(providerConfig);
                }
            }
        }


        //TODO:解析Module
        //TODO:解析注册中心配置
        //TODO:解析监视器
        //TODO:解析协议配置文件
        //TODO:设置其path,即网络上相应的路径,通过beanName进行相应的注入
        //TODO:如果不是延迟的话 直接进行暴露

        //如果具有应用监听器且延迟时间为null则进行延迟
        if (!isDelay()) {
            export();
        }

    }

    /**
     * 延迟时间的获取策略是先通过service来获取
     * 如果Service不存在则通过provider来进行获取
     *
     * 验证是直接监听器模式或者delay为空或者-1
     * */
    private boolean isDelay() {
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (delay == null && provider != null) {
            delay = provider.getDelay();
        }
        return supportedApplicationListener && (delay == null || delay == -1);
    }




}
