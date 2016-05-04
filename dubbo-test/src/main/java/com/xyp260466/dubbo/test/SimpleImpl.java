package com.xyp260466.dubbo.test;

/**
 * Created by xyp on 16-4-28.
 */
public class SimpleImpl implements Simple{


    public String sayHello(String name) {

        return "hello! "+ name;
    }

    public void noBack(String text) {
        System.out.println("noBack Method Called. "+text);
    }
}
