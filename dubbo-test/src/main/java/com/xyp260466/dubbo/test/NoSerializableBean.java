package com.xyp260466.dubbo.test;

import java.util.Date;

/**
 * Created by xyp on 16-5-5.
 */
public class NoSerializableBean {

    private int a;
    private String b;
    private Date d;

    public NoSerializableBean() {
    }

    public NoSerializableBean(int a, String b, Date d) {
        this.a = a;
        this.b = b;
        this.d = d;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public Date getD() {
        return d;
    }

    public void setD(Date d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "NoSerializableBean{" +
                "a=" + a +
                ", b='" + b + '\'' +
                ", d=" + d +
                '}';
    }
}
