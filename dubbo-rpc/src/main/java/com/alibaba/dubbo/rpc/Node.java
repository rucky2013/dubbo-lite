/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc;
import com.alibaba.dubbo.common.URL;

public interface Node {

    /**
     * get url.
     * 
     * @return url.
     */
    URL getUrl();
    
    /**
     * is available.
     * 
     * @return available.
     */
    boolean isAvailable();

    /**
     * destroy.
     */
    void destroy();

}