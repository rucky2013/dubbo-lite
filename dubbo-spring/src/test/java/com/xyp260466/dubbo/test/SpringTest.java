package com.xyp260466.dubbo.test;
import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by xyp on 16-5-9.
 */
public class SpringTest {
    private static final Logger logger = Logger.getLogger(SpringTest.class);

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
        logger.info("spring container starting...");

        new ClassPathXmlApplicationContext("spring-provider.xml");
    }


}
