package com.cfs.mini.remoting;

import com.cfs.mini.common.Resetable;

public interface Client extends Endpoint, Channel, Resetable {

    void reconnect() throws RemotingException;

    @Deprecated
    void reset(com.cfs.mini.common.Parameters parameters);

}
