package com.cfs.mini.config.spring.schema;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;



public class MiniBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(MiniBeanDefinitionParser.class);

    private final Class<?> beanClass;

    private final boolean required;

    public MiniBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }


    /**
     * 解析相应的bean定义
     * */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {



        return null;
    }
}
