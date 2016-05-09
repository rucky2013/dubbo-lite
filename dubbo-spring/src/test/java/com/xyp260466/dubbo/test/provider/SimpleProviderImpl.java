package com.xyp260466.dubbo.test.provider;
import com.xyp260466.dubbo.annotation.Provider;

/**
 * Created by xyp on 16-5-9.
 */
@Provider
public class SimpleProviderImpl implements SimpleProvider {

    public String providerMethod(String parameter) {
        return "SimpleProvider Result: "+parameter;
    }

}
