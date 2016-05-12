/**
 * Copyright 2002-2016 xiaoyuepeng
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
package com.xyp260466.dubbo.creator;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;

/**
 * Created by xyp on 16-5-9.
 */
public class AutoBeanCreator implements SmartInstantiationAwareBeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware {


    public void setBeanClassLoader(ClassLoader classLoader) {

        System.out.println("setBeanClassLoader");

    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

        System.out.println("setBeanFactory");

    }

    public Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {

        System.out.println("predictBeanType");

        return null;
    }

    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {

        System.out.println("determineCandidateConstructors");

        return new Constructor<?>[0];
    }

    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {

        System.out.println("getEarlyBeanReference");

        return null;
    }

    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {


        System.out.println("postProcessBeforeInstantiation");

        return null;
    }

    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {

        System.out.println("postProcessAfterInstantiation");


        return false;
    }

    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        System.out.println("postProcessPropertyValues");

        return null;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {


        System.out.println("postProcessBeforeInitialization");

        return null;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        System.out.println("postProcessAfterInitialization");

        return null;
    }
}
