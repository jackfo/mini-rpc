package com.cfs.rpc.service.impl;

import com.cfs.rpc.service.ISay;

public class SayImpl implements ISay {
    @Override
    public void sayHello() {
        System.out.println("提供暴露接口sayHello");
    }
}
