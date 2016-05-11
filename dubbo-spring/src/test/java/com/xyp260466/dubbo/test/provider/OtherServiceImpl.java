package com.xyp260466.dubbo.test.provider;

import com.xyp260466.dubbo.annotation.Provider;

import javax.annotation.Resource;

/**
 * Created by xyp on 16-5-11.
 */
@Provider
public class OtherServiceImpl implements OtherService{

    @Resource
    private SpringService springService;


    public String getData(String parameter){


        return springService.doSomething() + " world!";
    }

}
