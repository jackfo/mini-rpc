package com.cfs.mini.remoting.exchange.suport;

import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.ExchangeChannel;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.transport.ChannelHandlerAdapter;


public abstract class ExchangeHandlerAdapter extends ChannelHandlerAdapter
        implements ExchangeHandler {

    @Override
    public Object reply(ExchangeChannel channel, Object msg) throws RemotingException {
        return null;
    }
}
