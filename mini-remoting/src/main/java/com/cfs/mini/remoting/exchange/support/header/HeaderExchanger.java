package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.*;
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
     * 与具体的服务暴露方进行连接
     * mini://172.17.46.50:20880/com.cfs.rpc.service.ISay?anyhost=true&check=false&codec=exchange&id=isay&interface=com.cfs.rpc.service.ISay&interface.name=com.cfs.rpc.service.ISay&methods=sayHello&mini=2.0.0&pid=16128&register.ip=172.17.46.50&remote.timestamp=1537525885522&side=consumer&singleton=false&timestamp=1537525950129
     * */
    @Override
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        HeaderExchangeHandler headerExchangeHandler = new HeaderExchangeHandler(handler);
        //创建解码句柄
        DecodeHandler decodeHandler =  new DecodeHandler(headerExchangeHandler);

        //进行绑定
        Client client =  Transporters.connect(url,decodeHandler);

        return new HeaderExchangeClient(client,true);

    }
}
