package com.cfs.mini.remoting.transport;

import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;

public class MultiMessageHandler extends AbstractChannelHandlerDelegate {


    public MultiMessageHandler(ChannelHandler handler) {
        super(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        throw new RuntimeException("..");
    }
}
