package com.cfs.mini.registry.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.registry.Registry;
import com.cfs.mini.registry.RegistryFactory;
import com.cfs.mini.registry.RegistryService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractRegistryFactory implements RegistryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);


    private static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<String, Registry>();

    private static final ReentrantLock LOCK = new ReentrantLock();

    @Override
    public Registry getRegistry(URL url) {

        //已经路由到具体协议 则有些参数需要修改一下了
        url = url.setPath(RegistryService.class.getName())
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName())
                .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY);

        String key = url.toServiceString();

        LOCK.lock();

        try {
            Registry registry = REGISTRIES.get(key);

            if (registry != null) {
                return registry;
            }


            registry = createRegistry(url);

            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            // 添加到缓存
            REGISTRIES.put(key, registry);
            return registry;

        }finally {
            LOCK.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);

    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    public static void destroyAll() {
        LOCK.lock();
        try {
            // 销毁
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            // 清空缓存
            REGISTRIES.clear();
        } finally {
            // 释放锁
            // Release the lock
            LOCK.unlock();
        }
    }
}
