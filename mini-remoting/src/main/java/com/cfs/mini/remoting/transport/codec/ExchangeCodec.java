package com.cfs.mini.remoting.transport.codec;

import com.cfs.mini.common.io.Bytes;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.transport.AbstractCodec;
import org.jboss.netty.buffer.ChannelBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExchangeCodec extends AbstractCodec {


    private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80; // 128
    protected static final byte FLAG_TWOWAY = (byte) 0x40; // 64
    protected static final byte FLAG_EVENT = (byte) 0x20; // 32
    protected static final int SERIALIZATION_MASK = 0x1f; // 31

    public Short getMagicCode() {
        return MAGIC;
    }


    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {

    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        throw new RuntimeException("decode异常");
    }
}
