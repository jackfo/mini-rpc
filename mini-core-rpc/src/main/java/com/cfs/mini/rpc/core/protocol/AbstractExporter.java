package com.cfs.mini.rpc.core.protocol;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.rpc.core.Exporter;
import com.cfs.mini.rpc.core.Invoker;

public class AbstractExporter<T> implements Exporter<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Invoker<T> invoker;

    private volatile boolean unexported = false;

    public AbstractExporter(Invoker<T> invoker){
        if(invoker==null){
            throw new IllegalStateException("当前Service的Invoker为null");
        }
        if(invoker.getInterface()==null){
            throw new IllegalStateException("service type == null");
        }
        if (invoker.getUrl() == null)
            throw new IllegalStateException("service url == null");
        this.invoker = invoker;
    }


    @Override
    public Invoker<T> getInvoker() {
        return invoker;
    }

    @Override
    public void unexport() {
        // 标记已经取消暴露
        if (unexported) {
            return;
        }
        unexported = true;
        // 销毁
        getInvoker().destroy();
    }

    public String toString() {
        return getInvoker().toString();
    }

}
