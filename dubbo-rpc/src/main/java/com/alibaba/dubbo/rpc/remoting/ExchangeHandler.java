/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc.remoting;


public interface ExchangeHandler extends ChannelHandler {

    /**
     * reply.
     * 
     * @param channel
     * @param request
     * @return response
     * @throws RemotingException
     */
    Object reply(Channel channel, Object request) throws RemotingException;

}