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
