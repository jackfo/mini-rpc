package com.cfs.mini.remoting.transport;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;

/**
 * 解码相关句柄
 * */
public class DecodeHandler extends AbstractChannelHandlerDelegate{

    private static final Logger log = LoggerFactory.getLogger(DecodeHandler.class);

    public DecodeHandler(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException{


        handler.received(channel, message);
    }

    private void decode(Object message) {

    }

}
