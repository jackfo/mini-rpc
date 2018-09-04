package com.cfs.mini.remoting.exchange;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.support.header.HeaderExchanger;

@SPI(HeaderExchanger.NAME)
public interface Exchanger {


    @Adaptive({Constants.EXCHANGER_KEY})
    ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException;

    /**
     * connect.
     *
     * 连接一个服务器，即创建一个客户端
     *
     * @param url server url 服务器地址
     * @param handler 数据交换处理器
     * @return message channel 客户端
     */
    @Adaptive({Constants.EXCHANGER_KEY})
    ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException;


}
