package com.xyp260466.dubbo.test;

import com.alibaba.dubbo.rpc.protocol.DubboProtocol;

/**
 * Created by xyp on 16-4-28.
 */
public class ExportTest {

    public static void main(String[] args) {
        new Thread(new Runnable() {
            public void run() {
                while (true){
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        }).start();
        new DubboProtocol().export(new SimpleImpl(), Simple.class, 2880);


    }


}
