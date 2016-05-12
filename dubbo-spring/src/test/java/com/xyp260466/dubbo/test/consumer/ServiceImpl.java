package com.xyp260466.dubbo.test.consumer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * Created by xyp on 16-5-11.
 */
@Component
public class ServiceImpl implements FactoryBean {

    public String doSome(){
        return "123";
    }

    public Object getObject() throws Exception {
//        new Throwable().printStackTrace();
        return new ServiceImpl();
    }

    public Class<?> getObjectType() {
        return ServiceImpl.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
