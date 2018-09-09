package com.cfs.rpc.consumer;

import com.cfs.rpc.service.ISay;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/mini-consumer.xml"});
        context.start();
        ISay iSay = (ISay)context.getBean("isay");
        iSay.sayHello();
    }
}
