package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.Client;
import com.cfs.mini.remoting.exchange.ExchangeChannel;
import com.cfs.mini.remoting.exchange.ExchangeClient;

public class HeaderExchangeClient implements ExchangeClient {


    private final Client client;

    /**信息交换通道*/
    private final ExchangeChannel channel;

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeClient.class);

    public HeaderExchangeClient(Client client,boolean needHeartbeat){

        if(client==null){
            throw new IllegalArgumentException("client == null");
        }

        this.client = client;
        // 创建 HeaderExchangeChannel 对象
        this.channel = new HeaderExchangeChannel(client);


        //String mini = client.getUrl().getParameter(Constants.MINI_VERSION_KEY);

        //TODO:心跳机制
    }


}
