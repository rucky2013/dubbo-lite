/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc.remoting;

public interface Client extends Endpoint, Channel {

    /**
     * reconnect.
     */
    void reconnect() throws RemotingException;
    
}