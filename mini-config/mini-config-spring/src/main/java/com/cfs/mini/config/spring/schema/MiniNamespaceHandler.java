package com.cfs.mini.config.spring.schema;

import com.cfs.mini.config.ApplicationConfig;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

//
public class MiniNamespaceHandler extends NamespaceHandlerSupport {

    static {
        //TODO:小康 检查版本
    }

    @Override
    public void init() {
        //注册相应的bean定义解析器
        registerBeanDefinitionParser("application", new MiniBeanDefinitionParser(ApplicationConfig.class, true));
    }
}
