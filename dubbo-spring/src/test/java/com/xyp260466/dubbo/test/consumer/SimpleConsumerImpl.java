/**
 * Copyright 2002-2016 xiaoyuepeng
 *
 * this is for @Provider Scan For Dubbo Export Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @author xiaoyuepeng <xyp260466@163.com>
 */
package com.xyp260466.dubbo.test.consumer;

import com.xyp260466.dubbo.annotation.Consumer;
import com.xyp260466.dubbo.test.provider.OtherService;
import com.xyp260466.dubbo.test.provider.SimpleProvider;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by xyp on 16-5-9.
 */
@Service("consumer")
public class SimpleConsumerImpl implements SimpleConsumer {

    @Consumer
    private SimpleProvider simpleProvider;


    @Consumer
    private OtherService otherService;

//    @Autowired
//    private ServiceImpl service;


    public String sayHello(String name) {


//        return service.doSome();
        return "SimpleConsumer.sayHello() : [ "+name+", SimpleProvider: "+ simpleProvider.providerMethod(name)+", otherService: "+otherService.getData("123")+" ]";
    }
}
