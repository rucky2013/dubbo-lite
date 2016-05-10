package com.xyp260466.dubbo.test;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.protocol.DubboProtocol;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xyp on 16-5-3.
 */
public class ReferTest {


    public ReferTest(String name, int i) {
        this.name = name;
        this.i = i;
    }

    private String name;
    private int i;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void main() {

        try {
            Protocol protocol = DubboProtocol.getProtocol();


            Simple invoker = protocol.refer(Simple.class, "127.0.0.1", 2880, 3600);

            Two invoker2 = protocol.refer(Two.class, "127.0.0.1", 2880, 3600);

            String s = invoker.sayHello(name);

            System.out.println("ID "+i+" : "+s);

            invoker.noBack("this is no back Message "+i);

            System.out.println("ID "+i+" : "+invoker.testBean(new SerializableBean(""+i, name, 18, new Date()), new NoSerializableBean(i, name, new Date())));


            Date d = new Date();
            System.out.println("ID "+i+" : "+d);
            System.out.println("ID "+i+" : "+invoker2.twoMethod(i, name, d));


        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        new ReferTest("xiaoming 1", 1).main();


    }

}
