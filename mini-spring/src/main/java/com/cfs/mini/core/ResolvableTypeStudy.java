package com.cfs.mini.core;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Map;

public class ResolvableTypeStudy {
    public static void main(String[] args) {

        //1.获取类的类型
        ResolvableType resolvableType = ResolvableType.forClass(Map.class);
        Type type = resolvableType.getType();
        System.out.println(type.getTypeName());



    }
}

