package com.cfs.mini.config.spring;

import com.cfs.mini.config.ReferenceConfig;
import com.cfs.mini.config.spring.extension.SpringExtensionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {


    private static final long serialVersionUID = 213195494150089726L;

    private transient ApplicationContext applicationContext;

    /**
     * 添加当亲服务调用的应用
     * */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }


    public ReferenceBean() {
        super();
    }

    @Override
    public void destroy() throws Exception {

    }



    @Override
    public Object getObject() throws Exception {
        return get();
    }



    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }


}
