package com.xyp260466.dubbo.test;

import java.util.Date;

/**
 * Created by xyp on 16-5-5.
 */
public class TwoImpl implements Two{


    public NoSerializableBean twoMethod(int id, String name, Date date) throws Exception {
        return new NoSerializableBean(id, name, date);
    }
}
