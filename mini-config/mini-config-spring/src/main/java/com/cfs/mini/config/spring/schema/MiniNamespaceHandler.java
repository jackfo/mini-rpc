package com.cfs.mini.config.spring.schema;

import com.cfs.mini.config.ApplicationConfig;
import com.cfs.mini.config.ProtocolConfig;
import com.cfs.mini.config.ProviderConfig;
import com.cfs.mini.config.spring.ServiceBean;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

//
public class MiniNamespaceHandler extends NamespaceHandlerSupport {

    static {
        //TODO:小康 检查版本
    }

    @Override
    public void init() {
        //注册相应的bean定义解析器,通过MiniBeanDefinitionParser 完成配置文件与Bean对象之间的解析过程
        registerBeanDefinitionParser("application", new MiniBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("service", new MiniBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("provider", new MiniBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("protocol", new MiniBeanDefinitionParser(ProtocolConfig.class, true));
    }
}
