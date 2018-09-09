package com.cfs.mini.config.spring.extension;

import com.cfs.mini.common.extension.ExtensionFactory;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import org.springframework.context.ApplicationContext;

import java.util.Set;

public class SpringExtensionFactory implements ExtensionFactory {

    /**
     *
     * Spring Context 集合
     */
    private static final Set<ApplicationContext> contexts = new ConcurrentHashSet<ApplicationContext>();


    public static void addApplicationContext(ApplicationContext context) {
        contexts.add(context);
    }

    public static void removeApplicationContext(ApplicationContext context) {
        contexts.remove(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> type, String name) {
        for (ApplicationContext context : contexts) {
            if (context.containsBean(name)) {
                // 获得属性
                Object bean = context.getBean(name);
                // 判断类型
                if (type.isInstance(bean)) {
                    return (T) bean;
                }
            }
        }
        return null;
    }
}
