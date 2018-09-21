package com.cfs.mini.remoting.buffer;

import org.jboss.netty.buffer.ChannelBufferFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public interface ChannelBuffer {

    int capacity();

    void clear();

    ChannelBuffer copy();

    ChannelBuffer copy(int index, int length);

    void discardReadBytes();

    void ensureWritableBytes(int writableBytes);

    boolean equals(Object o);

    ChannelBufferFactory factory();

    byte getByte(int index);

    void getBytes(int index, byte[] dst);

    void getBytes(int index, byte[] dst, int dstIndex, int length);

    void getBytes(int index, ByteBuffer dst);

    void getBytes(int index, ChannelBuffer dst);

    void getBytes(int index, ChannelBuffer dst, int length);

    void getBytes(int index, ChannelBuffer dst, int dstIndex, int length);

    void getBytes(int index, OutputStream dst, int length) throws IOException;


}
