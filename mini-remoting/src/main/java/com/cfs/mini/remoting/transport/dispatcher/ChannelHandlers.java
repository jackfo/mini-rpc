package com.cfs.mini.remoting.transport.dispatcher;


import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.transport.MultiMessageHandler;

/**
 * 通道处理器工厂
 * */
public class ChannelHandlers {

    /**
     * 单例
     */
    private static ChannelHandlers INSTANCE = new ChannelHandlers();

    protected ChannelHandlers() {
    }

    public static ChannelHandler wrap(ChannelHandler handler, URL url) {
        return ChannelHandlers.getInstance().wrapInternal(handler, url);
    }

    protected static ChannelHandlers getInstance() {
        return INSTANCE;
    }

    static void setTestingChannelHandlers(ChannelHandlers instance) { // for testing
        INSTANCE = instance;
    }

    protected ChannelHandler wrapInternal(ChannelHandler handler, URL url) {
        return new MultiMessageHandler(handler);
    }

}