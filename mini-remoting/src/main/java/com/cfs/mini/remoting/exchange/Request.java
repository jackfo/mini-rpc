package com.cfs.mini.remoting.exchange;

import com.cfs.mini.common.utils.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class Request {

    /**心跳事件*/
    public static final String HEARTBEAT_EVENT = null;
    /**只读事件*/
    public static final String READONLY_EVENT = "R";
    /**请求编号自增序列*/
    private static final AtomicLong INVOKE_ID = new AtomicLong(0);
    /**请求编号*/
    private final long mId;
    /**rpc版本*/
    private String mVersion;
    /**是否异常请求*/
    private boolean mBroken = false;

    private Object mData;
    /**是否需要相应*/
    private boolean mTwoWay = true;
    /**心跳事件*/
    private boolean mEvent = false;

    private static long newId() {
        return INVOKE_ID.getAndIncrement();
    }

    private static String safeToString(Object data) {
        if (data == null) return null;
        String dataStr;
        try {
            dataStr = data.toString();
        } catch (Throwable e) {
            dataStr = "<Fail toString of " + data.getClass() + ", cause: " +
                    StringUtils.toString(e) + ">";
        }
        return dataStr;
    }

    public Request() {
        mId = newId();
    }

    public Request(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public boolean isTwoWay() {
        return mTwoWay;
    }

    public void setTwoWay(boolean twoWay) {
        mTwoWay = twoWay;
    }

    public boolean isEvent() {
        return mEvent;
    }

    public void setEvent(String event) {
        mEvent = true;
        mData = event;
    }

    public boolean isBroken() {
        return mBroken;
    }

    public void setBroken(boolean mBroken) {
        this.mBroken = mBroken;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object msg) {
        mData = msg;
    }

    public boolean isHeartbeat() {
        return mEvent && HEARTBEAT_EVENT == mData;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat) {
            setEvent(HEARTBEAT_EVENT);
        }
    }

    @Override
    public String toString() {
        return "Request [id=" + mId + ", version=" + mVersion + ", twoway=" + mTwoWay + ", event=" + mEvent
                + ", broken=" + mBroken + ", data=" + (mData == this ? "this" : safeToString(mData)) + "]";
    }

}
