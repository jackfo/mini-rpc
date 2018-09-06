package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Server;
import com.cfs.mini.remoting.Transporters;
import com.cfs.mini.remoting.exchange.ExchangeClient;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.exchange.ExchangeServer;
import com.cfs.mini.remoting.exchange.Exchanger;
import com.cfs.mini.remoting.transport.DecodeHandler;

public class HeaderExchanger implements Exchanger {

    public static final String NAME = "header";


    /**
     * bind是服务端绑定
     * */
    @Override
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        ChannelHandler exchangeHandler = new HeaderExchangeHandler(handler);
        ChannelHandler decodeHandler = new DecodeHandler(exchangeHandler);
        Server server = Transporters.bind(url,decodeHandler);
        HeaderExchangeServer headerExchangeServer = new HeaderExchangeServer(server);
        return headerExchangeServer;
    }

    /**
     * connect是客户端连接
     * */
    @Override
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
       throw new RuntimeException("运行异常");
    }
}
