package com.xyp260466.dubbo.test;

import com.alibaba.dubbo.rpc.Protocol;
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
        System.out.println("export starting...");

        //initialize a protocol-
        Protocol protocol = DubboProtocol.getProtocol();

        //export a service
        protocol.export(new SimpleImpl(), Simple.class, 2880);

        //export a second service
        protocol.export(new TwoImpl(), Two.class, 2880);

        System.out.println("export complete.");


    }


}
