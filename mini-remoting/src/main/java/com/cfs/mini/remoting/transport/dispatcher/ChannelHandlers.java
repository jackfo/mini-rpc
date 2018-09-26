package com.cfs.mini.remoting.transport.dispatcher;


import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Dispatcher;
import com.cfs.mini.remoting.exchange.support.header.HeartbeatHandler;
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
        return new MultiMessageHandler(
                new HeartbeatHandler(
                        ExtensionLoader.getExtensionLoader(Dispatcher.class).getAdaptiveExtension().dispatch(handler, url)
                )
        );
    }

}