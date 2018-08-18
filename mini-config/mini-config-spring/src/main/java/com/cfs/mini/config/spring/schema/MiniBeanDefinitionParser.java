package com.cfs.mini.config.spring.schema;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
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

        logger.info("解析MiniBeanDefinitionParser的parse");

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        //解析配置对象id的 若不存在 则生成
        String id = element.getAttribute("id");

        if ((id == null || id.length() == 0) && required) {
            // 生成 id 。不同的配置对象，会存在不同。
            String generatedBeanName = element.getAttribute("name");
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                //TODO:加了协议之后加载
            }
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            // 若 id 已存在，通过自增序列，解决重复。
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        if (id != null && id.length() > 0) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("该bean的id已经存在 所以重复了" + id);
            }

            // 将这个bean以id的方式添加到注册表
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            // 设置 Bean 的 `id` 属性值
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        return beanDefinition;
    }

//    @Override
//    protected void doParse(Element element, ParserContext parserContext,
//                           BeanDefinitionBuilder builder) {
//
//        System.out.println("你好");
//
//    }
    }
