package com.xyp260466.dubbo.test;

import java.util.Date;

/**
 * Created by xyp on 16-5-5.
 */
public interface Two {

    NoSerializableBean twoMethod(int id, String name, Date date) throws Exception;

}
