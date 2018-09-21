package com.cfs.mini.remoting.transport;

import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;

public class ChannelHandlerAdapter implements ChannelHandler {

    @Override public void connected(Channel channel) throws RemotingException { }

    @Override public void disconnected(Channel channel) throws RemotingException { }

    @Override public void sent(Channel channel, Object message) { }

    @Override public void received(Channel channel, Object message) throws RemotingException { }

    @Override public void caught(Channel channel, Throwable exception) throws RemotingException { }

}
