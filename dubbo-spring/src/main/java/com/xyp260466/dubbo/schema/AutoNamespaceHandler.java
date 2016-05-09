package com.xyp260466.dubbo.schema;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by xyp on 16-5-9.
 */
public class AutoNamespaceHandler extends NamespaceHandlerSupport {
    private static final Logger logger = Logger.getLogger(AutoNamespaceHandler.class);

    public void init() {

        logger.info("Initialize Dubbo Namespace Handler.");

        registerBeanDefinitionParser("annotation-driven", new AutoBeanDefinitionParser());
    }
}
