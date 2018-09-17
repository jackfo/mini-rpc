package com.cfs.rpc.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class Provider {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/mini-provider.xml"});
        context.setValidating(false);
        context.start();
        System.in.read();
    }
}
