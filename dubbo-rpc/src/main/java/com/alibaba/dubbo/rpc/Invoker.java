/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc;

public interface Invoker<T> extends Node {

    /**
     * get service interface.
     * 
     * @return service interface.
     */
    Class<T> getInterface();

    /**
     * invoke.
     * 
     * @param invocation
     * @return result
     * @throws RpcException
     */
    Result invoke(Invocation invocation) throws RpcException;

}