package com.cfs.mini.common.extension.factory;

import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.ExtensionFactory;
import com.cfs.mini.common.extension.ExtensionLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Adaptive
public class AdaptiveExtensionFactory implements ExtensionFactory {

    private final List<ExtensionFactory> factories;


    public AdaptiveExtensionFactory(){
        ExtensionLoader<ExtensionFactory> extensionLoader = ExtensionLoader.getExtensionLoader(ExtensionFactory.class);
        List<ExtensionFactory> list = new ArrayList<>();

        Set<String> extensionLoaderSet = extensionLoader.getSupportedExtensions();
        for(String name:extensionLoaderSet){
            list.add(extensionLoader.getExtension(name));
        }
        factories = Collections.unmodifiableList(list);
    }

    /**
     *
     * */
    @Override
    public <T> T getExtension(Class<T> type, String name) {
        for (ExtensionFactory factory : factories) {
            T extension = factory.getExtension(type, name);
            if (extension != null) {
                return extension;
            }
        }
        return null;
    }
}


