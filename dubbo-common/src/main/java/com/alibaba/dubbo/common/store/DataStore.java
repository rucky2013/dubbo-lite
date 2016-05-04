/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.common.store;

import java.util.Map;

public interface DataStore {

    /**
     * return a snapshot value of componentName
     */
    Map<String,Object> get(String componentName);

    Object get(String componentName, String key);

    void put(String componentName, String key, Object value);

    void remove(String componentName, String key);

}
