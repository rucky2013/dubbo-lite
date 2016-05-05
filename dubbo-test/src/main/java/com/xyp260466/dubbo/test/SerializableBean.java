package com.xyp260466.dubbo.test;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xyp on 16-5-4.
 */
public class SerializableBean implements Serializable{

    private String id;
    private String name;
    private int age;
    private Date d;

    public SerializableBean() {
    }

    public SerializableBean(String id, String name, int age, Date d) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.d = d;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getD() {
        return d;
    }

    public void setD(Date d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "SerializableBean{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", d=" + d +
                '}';
    }
}
