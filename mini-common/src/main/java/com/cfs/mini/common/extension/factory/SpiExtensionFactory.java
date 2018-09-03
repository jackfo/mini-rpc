package com.cfs.mini.common.extension.factory;

import com.cfs.mini.common.extension.ExtensionFactory;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.extension.SPI;

public class SpiExtensionFactory implements ExtensionFactory {
    @Override
    public <T> T getExtension(Class<T> type, String name) {
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) { // 校验是 @SPI
            // 加载拓展接口对应的 ExtensionLoader 对象
            ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
            // 加载拓展对象
            if (!loader.getSupportedExtensions().isEmpty()) {
                return loader.getAdaptiveExtension();
            }
        }
        return null;
    }
}
