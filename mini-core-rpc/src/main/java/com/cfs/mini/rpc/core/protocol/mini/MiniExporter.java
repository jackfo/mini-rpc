package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.rpc.core.Exporter;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.protocol.AbstractExporter;

import java.util.Map;


/**
 * 构造形影暴露的Map
 * */
public class MiniExporter<T> extends AbstractExporter<T> {

    private final String key;

    private final Map<String,Exporter<?>> exporterMap;

    public MiniExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
        super(invoker);
        this.key = key;
        this.exporterMap = exporterMap;
    }

    @Override
    public void unexport() {
        // 取消暴露
        super.unexport();
        // 移除
        exporterMap.remove(key);
    }

}
