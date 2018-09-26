package com.cfs.mini.remoting;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;
import com.cfs.mini.remoting.transport.dispatcher.all.AllDispatcher;

@SPI(AllDispatcher.NAME)
public interface Dispatcher {

    @Adaptive({Constants.DISPATCHER_KEY, "dispather", "channel.handler"})
        // The last two parameters are reserved for compatibility with the old configuration
    ChannelHandler dispatch(ChannelHandler handler, URL url);
}
