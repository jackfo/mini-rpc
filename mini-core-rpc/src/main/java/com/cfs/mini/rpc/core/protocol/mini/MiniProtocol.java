package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.exchange.ExchangeServer;
import com.cfs.mini.rpc.core.Exporter;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.protocol.AbstractProtocol;
import com.cfs.mini.rpc.core.support.ProtocolUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiniProtocol extends AbstractProtocol {

    /**暴露的MAP映射*/
    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    protected static String serviceKey(URL url) {
        return ProtocolUtils.serviceKey(url);
    }

    /**通信服务器的集合*/
    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>();

    @Override
    public int getDefaultPort() {
        return 0;
    }

    /**
     * 根据相应的ServiceKey将invoker封装进去添加到Map
     * */
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();

        String key = serviceKey(url);

        MiniExporter miniExporter = new MiniExporter(invoker,key,exporterMap);

        exporterMap.put(key,miniExporter);

        ExchangeServer server;

        try {
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * 启动通信服务器
     * */
    private void openServer(URL url) {
        String key = url.getAddress();
        //查看当前URL是否是一个服务
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);

        if(isServer){
            ExchangeServer server = serverMap.get(key);
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return null;
    }
}
