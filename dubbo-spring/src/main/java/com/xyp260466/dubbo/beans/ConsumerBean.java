/**
 * Copyright 2002-2016 xiaoyuepeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @author xiaoyuepeng <xyp260466@163.com>
 */
package com.xyp260466.dubbo.beans;

import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.protocol.DubboProtocol;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by xyp on 16-5-11.
 */
public class ConsumerBean implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {


    private String interfaceClass;
    private transient ApplicationContext applicationContext;

    public ConsumerBean(String interfaceClass){
        this.interfaceClass = interfaceClass;
    }

    public void destroy() throws Exception {

    }

    public Object getObject() throws Exception {
        return get();
    }

    public Class<?> getObjectType() {
        return ReflectUtils.forName(interfaceClass);
    }

    private Object get() throws Exception{
        Protocol protocol = DubboProtocol.getProtocol();
        return protocol.refer((Class)ReflectUtils.forName(interfaceClass), "127.0.0.1", 20880);
    }


    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        //afterPropertiesSet

    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
