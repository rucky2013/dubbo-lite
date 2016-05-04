/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc.remoting;


public interface ResponseCallback {

    /**
     * done.
     * 
     * @param response
     */
    void done(Object response);

    /**
     * caught exception.
     * 
     * @param exception
     */
    void caught(Throwable exception);

}