package com.cfs.rpc.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Provider {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/mini-provider.xml"});
        context.setValidating(false);
        context.start();
    }
}
