package com.cfs.mini.rpc.core.protocol;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.rpc.core.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractInvoker<T> implements Invoker<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**接口类型*/
    private final Class<T> type;

    /**服务URL*/
    private final URL url;

    private final Map<String, String> attachment;

    private volatile boolean available = true;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractInvoker(Class<T> type, URL url) {
        this(type, url, (Map<String, String>) null);
    }

    public AbstractInvoker(Class<T> type, URL url, String[] keys) {
        this(type, url, convertAttachment(url, keys));
    }

    public AbstractInvoker(Class<T> type, URL url, Map<String, String> attachment) {
        if (type == null)
            throw new IllegalArgumentException("service type == null");
        if (url == null)
            throw new IllegalArgumentException("service url == null");
        this.type = type;
        this.url = url;
        this.attachment = attachment == null ? null : Collections.unmodifiableMap(attachment);
    }

    /**
     * 将指向参数的值添加到 {@link #attachment} 中
     *
     * @param url URL 对象
     * @param keys 指定参数集合
     * @return 集合
     */
    private static Map<String, String> convertAttachment(URL url, String[] keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        Map<String, String> attachment = new HashMap<String, String>();
        for (String key : keys) {
            String value = url.getParameter(key);
            if (value != null && value.length() > 0) {
                attachment.put(key, value);
            }
        }
        return attachment;
    }


    @Override
    public boolean isAvailable() {
        return available;
    }

    public Class<T> getInterface() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        setAvailable(false);
    }
    protected void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public Result invoke(Invocation inv) throws RpcException {
        RpcInvocation invocation = (RpcInvocation) inv;
        try {
            return doInvoke(invocation);
        }catch (Throwable t){
            t.printStackTrace();
        }

        throw new RpcException("远程调用异常");
    }

    protected abstract Result doInvoke(Invocation invocation) throws Throwable;
}
