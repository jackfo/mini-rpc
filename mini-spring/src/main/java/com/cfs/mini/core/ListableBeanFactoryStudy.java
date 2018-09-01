package com.cfs.mini.core;


import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import java.util.HashMap;
import java.util.Map;

class Student{
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

public class ListableBeanFactoryStudy {

    public static void main(String[] args) {
        DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();

        beanDefinition.setBeanClassName(Student.class.getName());

        Map<String,Object> propertyMap = new HashMap<>();
        propertyMap.put("name","jack");
        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.addPropertyValues(propertyMap);
        beanDefinition.setPropertyValues(mutablePropertyValues);

        beanDefinition.setAttribute("name","jack");
        defaultListableBeanFactory.registerBeanDefinition("student",beanDefinition);
        Student student = (Student) defaultListableBeanFactory.getBean("student");

        System.out.println(student.getName());

        /**
         * 获取指定类型的所有BeanName
         * */
        String[] beanNameType = defaultListableBeanFactory.getBeanNamesForType(Student.class);
        for(String beanName:beanNameType){
            System.out.println(beanName);
        }

        defaultListableBeanFactory.setSerializationId("serialId");
        System.out.println(defaultListableBeanFactory.getSerializationId());



    }
}
