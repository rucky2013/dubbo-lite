package com.xyp260466.dubbo.beans;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.protocol.DubboProtocol;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Created by xyp on 16-5-10.
 */
public class ProviderBean implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener, BeanNameAware {
    private static final Logger logger = Logger.getLogger(ProviderBean.class);

    private String id;
    private String interfaceClass;
    private transient String name;
    private transient ApplicationContext applicationContext;
    private Object ref;

    public ProviderBean(){
    }

    public void setBeanName(String name) {
        this.name = name;
    }

    public void destroy() throws Exception {

    }

    public void afterPropertiesSet() throws Exception {
        //afterPropertiesSet
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String getName() {
        return name;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        //onApplicationEvent
        Protocol protocol = DubboProtocol.getProtocol();
        protocol.export(ref, (Class)ReflectUtils.forName(interfaceClass), 20880);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    public String getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
}
