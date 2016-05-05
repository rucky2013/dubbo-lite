package com.xyp260466.dubbo.test;

/**
 * Created by xyp on 16-4-28.
 */
public class SimpleImpl implements Simple{


    public String sayHello(String name) {
        System.out.println("Method [sayHello] Called. Parameter: "+name);
        return "hello! "+ name;
    }

    public void noBack(String text) {
        System.out.println("Method [noBack] Called. Parameter: "+text);
    }

    public String testBean(SerializableBean serializableBean, NoSerializableBean noSerializableBean) {
        System.out.println("Method [testBean] Called. Parameter: " + serializableBean + ", "+ noSerializableBean);
        return "Test Success! SerializableBean Name is : "+ serializableBean.getName();
    }
}
