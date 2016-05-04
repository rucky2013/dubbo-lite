/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc.remoting;


public interface ExchangeHandlerDelegate extends ExchangeHandler {
    ChannelHandler getHandler();
}