package com.cfs.mini.common.compiler;

import com.cfs.mini.common.extension.SPI;



/**
 * 编译器接口
 * */
@SPI("javassist")
public interface Compiler {

    /**
     * Compile java source code.
     *
     * 编译 Java 代码字符串
     *
     * @param code        Java source code
     *                    Java 代码字符串
     * @param classLoader classloader
     *                    类加载器
     * @return Compiled class
     *                    编译后的类
     */
    Class<?> compile(String code, ClassLoader classLoader);

}