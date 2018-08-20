package com.cfs.mini.config.spring.schema;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ReflectUtils;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.config.ProtocolConfig;
import com.cfs.mini.config.ProviderConfig;
import com.cfs.mini.config.spring.ServiceBean;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


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
            // 给当前bean对应属性集合添加BeanDefine的值
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }


        if (ProtocolConfig.class.equals(beanClass)) {
            // <mini:service interface="com.alibaba.dubbo.demo.DemoService" protocol="dubbo" ref="demoService"/>
            // <mini:protocol id="dubbo" name="dubbo" port="20880"/>

            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
                    Object value = property.getValue();
//                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
//                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
//                    }
                }
            }

            // 处理 `<mini:service />` 的属性 `class`
        } else if (ServiceBean.class.equals(beanClass)) {
            // 处理 `class` 属性。例如  <mini:service id="sa" interface="com.alibaba.dubbo.demo.DemoService" class="com.alibaba.dubbo.demo.provider.DemoServiceImpl" >
            String className = element.getAttribute("class");
            if (className != null && className.length() > 0) {
                // 创建 Service 的 RootBeanDefinition 对象。相当于内嵌了 <bean class="com.alibaba.dubbo.demo.provider.DemoServiceImpl" />
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                // 解析 Service Bean 对象的属性
                //parseProperties(element.getChildNodes(), classDefinition);
                //设置ref属性为相关的BeanDefinitionHolder 根据指定的class 属性
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }

        } else if (ProviderConfig.class.equals(beanClass)) {

        }

        Set<String> props = new HashSet<String>();
        ManagedMap parameters = null;

        for(Method setter : beanClass.getMethods()) {
            //获取setter方法名
            String name = setter.getName();
            if (name.length() > 3 && name.startsWith("set") && Modifier.isPublic(setter.getModifiers()) && setter.getParameterTypes().length == 1) {
                Class<?> type = setter.getParameterTypes()[0];
                //将方法名添加到属性集合
                String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                props.add(property);


                //尝试根据方法名获取起getter方法 如果getter方法没有提供或者是私有则不继续向下处理
                Method getter = null;
                try {
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    try {
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                    }
                }
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    continue;
                }

                //TODO:针对parameters methods arguments参数没有做处理

                //从标签中获取属性值
                String value = element.getAttribute(property);

                if(value!=null){
                    value = value.trim();
                    if(value.length()>0){

                        Object reference;

                        //根据方法的参数类型,来决定对值进行封装的类型,所以在setter的时候好进行处理
                        if (isPrimitive(type)) {
                            // 兼容性处理
                            if ("async".equals(property) && "false".equals(value)
                                    || "timeout".equals(property) && "0".equals(value)
                                    || "delay".equals(property) && "0".equals(value)
                                    || "version".equals(property) && "0.0.0".equals(value)
                                    || "stat".equals(property) && "-1".equals(value)
                                    || "reliable".equals(property) && "false".equals(value)) {
                                // backward compatibility for the default value in old version's xsd
                                value = null;
                            }
                            reference = value;
                            // 处理在 `<mini:provider />` 或者 `<mini:service />` 上定义了 `protocol` 属性的 兼容性。
                        }else{
                            //从解析上下文获取是否包含改值对应的Bean,并且其必须是单例
                            if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                if (!refBean.isSingleton()) {
                                    throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                }
                            }
                            // 创建 RuntimeBeanReference ，指向 Service 的 Bean 对象
                            reference = new RuntimeBeanReference(value);
                        }

                        //设置beanDefine对应的属性值
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);

                    }
                }
            }
        }
        return beanDefinition;
    }


    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

//    @Override
//    protected void doParse(Element element, ParserContext parserContext,
//                           BeanDefinitionBuilder builder) {
//
//        System.out.println("你好");
//
//    }
    }
