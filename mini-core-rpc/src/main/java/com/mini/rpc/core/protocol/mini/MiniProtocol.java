package com.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.URL;
import com.mini.rpc.core.Exporter;
import com.mini.rpc.core.Invoker;
import com.mini.rpc.core.RpcException;
import com.mini.rpc.core.protocol.AbstractProtocol;

public class MiniProtocol extends AbstractProtocol {
    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return null;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return null;
    }
}
