package com.xyp260466.dubbo.test.provider;
import com.xyp260466.dubbo.annotation.Interface;

/**
 * Created by xyp on 16-5-9.
 */
@Interface
public interface SimpleProvider {

    String providerMethod(String parameter);

}
