package com.xyp260466.dubbo.test.provider;

import org.springframework.stereotype.Service;

/**
 * Created by xyp on 16-5-11.
 */
@Service
public class SpringServiceImpl implements SpringService{


    public String doSomething(){
        return "just do it!";
    }



}
