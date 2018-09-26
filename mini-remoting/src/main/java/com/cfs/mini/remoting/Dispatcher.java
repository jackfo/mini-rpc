package com.cfs.mini.remoting;

import com.cfs.mini.common.extension.SPI;
import com.cfs.mini.remoting.transport.dispatcher.all.AllDispatcher;

@SPI(AllDispatcher.NAME)
public interface Dispatcher {
}
