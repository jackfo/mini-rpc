package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Server;
import com.cfs.mini.remoting.exchange.ExchangeServer;

import java.net.InetSocketAddress;

public class HeaderExchangeServer implements ExchangeServer {


    private final Server server;

    public HeaderExchangeServer(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server == null");
        }
        // 读取心跳相关配置
        this.server = server;

        //尚未做心跳处理
    }

    @Override
    public void reset(URL url) {

    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
