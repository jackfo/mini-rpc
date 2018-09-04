package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.exchange.ExchangeChannel;

/**
 * 交换通道
 * */
public class HeaderExchangeChannel implements ExchangeChannel {

    private final Channel channel;

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeChannel.class);

    HeaderExchangeChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        this.channel = channel;
    }




}
