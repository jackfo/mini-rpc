package com.cfs.mini.remoting.exchange;

import com.cfs.mini.remoting.RemotingException;

public interface ExchangeChannel {

    ExchangeHandler getExchangeHandler();

    ResponseFuture request(Object request, int timeout) throws RemotingException;
}
