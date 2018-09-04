package com.cfs.mini.remoting.transport;

import com.cfs.mini.remoting.ChannelHandler;

public interface ChannelHandlerDelegate extends ChannelHandler {

    ChannelHandler getHandler();
}
