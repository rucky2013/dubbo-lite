package com.xyp260466.dubbo.test;

import com.xyp260466.dubbo.test.consumer.SimpleConsumer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by xyp on 16-5-9.
 */
public class SpringTest {

    public static void main(String[] args) {

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-consumer.xml");

//        SimpleConsumer simpleConsumer = (SimpleConsumer) context.getBean("consumer");
//
//        System.out.println(simpleConsumer.sayHello("123"));



    }


}
