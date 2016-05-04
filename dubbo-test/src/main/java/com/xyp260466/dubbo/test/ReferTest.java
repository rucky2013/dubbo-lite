package com.xyp260466.dubbo.test;
import com.alibaba.dubbo.rpc.protocol.DubboProtocol;

/**
 * Created by xyp on 16-5-3.
 */
public class ReferTest {

    public static void main(String[] args) {

        Simple invoker = new DubboProtocol().refer(Simple.class, "127.0.0.1", 2880);

        String s = invoker.sayHello("xiaoming");

        invoker.noBack("this is no back Message");

        System.out.println(s);

    }

}
