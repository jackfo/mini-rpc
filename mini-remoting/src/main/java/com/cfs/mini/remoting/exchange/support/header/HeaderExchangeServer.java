package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.remoting.Server;
import com.cfs.mini.remoting.exchange.ExchangeServer;

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
}
