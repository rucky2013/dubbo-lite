package com.xyp260466.dubbo.test;

import com.alibaba.dubbo.rpc.protocol.DubboProtocol;
import com.xyp260466.dubbo.test.provider.SimpleProvider;
import org.apache.log4j.Logger;

/**
 * Created by xyp on 16-5-10.
 */
public class CallSpringService {
    private static final Logger logger = Logger.getLogger(CallSpringService.class);

    public static void main(String[] args) {
        logger.info("Start Calling Spring Dubbo Service......");


        SimpleProvider simpleProvider = DubboProtocol.getProtocol().refer(SimpleProvider.class, "127.0.0.1", 20880);

        System.out.println("Spring Dubbo Service Result: "+simpleProvider.providerMethod("xiaoming"));




    }


}
