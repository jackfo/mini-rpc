package com.cfs.mini.remoting;


import com.cfs.mini.common.Constants;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SPI
public interface Codec {

    Object NEED_MORE_INPUT = new Object();

    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, OutputStream output, Object message) throws IOException;


    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, InputStream input) throws IOException;
}

