package com.cfs.mini.remoting.exchange;

import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;

public interface ExchangeHandler extends ChannelHandler{

    Object reply(ExchangeChannel channel, Object request) throws RemotingException;
}
