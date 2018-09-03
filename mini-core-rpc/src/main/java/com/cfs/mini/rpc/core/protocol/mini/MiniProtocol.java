package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Exporter;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.protocol.AbstractProtocol;

public class MiniProtocol extends AbstractProtocol {
    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        throw new RuntimeException("运行时异常");


        return null;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return null;
    }
}
