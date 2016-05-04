/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc;

public interface Exporter<T> {
    
    /**
     * get invoker.
     * 
     * @return invoker
     */
    Invoker<T> getInvoker();
    
    /**
     * unexport.
     * 
     * <code>
     *     getInvoker().destroy();
     * </code>
     */
    void unexport();

}