package com.cfs.mini.remoting.transport.dispatcher.all;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Dispatcher;

public class AllDispatcher implements Dispatcher {

    public static final String NAME = "all";

    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return new AllChannelHandler(handler, url);
    }
}
