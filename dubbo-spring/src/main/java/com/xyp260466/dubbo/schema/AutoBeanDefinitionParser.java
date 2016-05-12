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
package com.xyp260466.dubbo.schema;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.xyp260466.dubbo.annotation.Provider;
import com.xyp260466.dubbo.annotation.Interface;
import com.xyp260466.dubbo.beans.ProviderBean;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.DubboConsumerAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.context.annotation.DubboClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.Set;

/**
 * Created by xyp on 16-5-9.
 */
public class AutoBeanDefinitionParser extends ComponentScanBeanDefinitionParser {

    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";

    public static final String CONSUMER_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalDubboConsumerAnnotationProcessor";

    private static final Logger logger = Logger.getLogger(AutoBeanDefinitionParser.class);

    public BeanDefinition parse(Element element, ParserContext parserContext) {

        logger.info("Parse Dubbo Services.");

        //do with provider
        String[] basePackages = StringUtils.tokenizeToStringArray(element.getAttribute(BASE_PACKAGE_ATTRIBUTE),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        Assert.notEmpty(basePackages, "At least one base package must be specified");

        DubboClassPathBeanDefinitionScanner scanner = new DubboClassPathBeanDefinitionScanner(parserContext);

        Set<BeanDefinitionHolder> beanDefinitions = scanner.scanComplete(basePackages);

        registerComponents(parserContext.getReaderContext(), beanDefinitions, element);


        for(BeanDefinitionHolder holder : beanDefinitions){

            RootBeanDefinition beanDefinition = new RootBeanDefinition();
            beanDefinition.setBeanClass(ProviderBean.class);
            beanDefinition.setLazyInit(false);

            ScannedGenericBeanDefinition srcDefinition = (ScannedGenericBeanDefinition) holder.getBeanDefinition();
            //process
            RootBeanDefinition classDefinition = new RootBeanDefinition();

            String beanClass = srcDefinition.getBeanClassName();

            classDefinition.setBeanClass(ReflectUtils.forName(beanClass));
            classDefinition.setLazyInit(false);

            beanDefinition.setBeanClassName(ProviderBean.class.getName());

            AnnotationMetadata metadata = srcDefinition.getMetadata();

            Map<String, Object> attributes = metadata.getAnnotationAttributes(Provider.class.getName());

            String[] interfaces = metadata.getInterfaceNames();

            if(interfaces.length == 0){
                throw new IllegalStateException("Class [ "+beanClass+" ] No Interfaces Detected!");
            }else {
                String interfaceTarget = null;
                if(interfaces.length > 1){
                    for(String interfaceStr : interfaces){
                        Class clazz = ReflectUtils.forName(interfaceStr);
                        if(clazz.isAnnotationPresent(Interface.class)){
                            interfaceTarget = interfaceStr;
                        }
                    }
                }else{
                    interfaceTarget = interfaces[0];
                }
                if(interfaceTarget == null){
                    throw new IllegalStateException("Class [ "+beanClass+" ] Cannot Find suitable Interface!");
                }else {
                    String id = interfaceTarget;
                    int counter = 2;
                    while(parserContext.getRegistry().containsBeanDefinition(id)) {
                        id = interfaceTarget + (counter ++);
                    }
                    if (parserContext.getRegistry().containsBeanDefinition(id))  {
                        throw new IllegalStateException("Duplicate spring bean id " + id);
                    }
                    parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
                    beanDefinition.getPropertyValues().addPropertyValue("id", id);
                    beanDefinition.getPropertyValues().addPropertyValue("interfaceClass", interfaceTarget);

                    String refId = interfaceTarget;
                    if (attributes.size() > 0 && attributes.get("value") != null) {
                        refId = attributes.get("value").toString();
                    }
                    beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, refId));

                }
            }
        }

        //do with consumer
        RootBeanDefinition def = new RootBeanDefinition(DubboConsumerAnnotationBeanPostProcessor.class);
        def.setSource(parserContext.getReaderContext().extractSource(element));
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        parserContext.getReaderContext().getRegistry().registerBeanDefinition(CONSUMER_ANNOTATION_PROCESSOR_BEAN_NAME, def);

        return null;
    }

    protected void registerComponents(
            XmlReaderContext readerContext, Set<BeanDefinitionHolder> beanDefinitions, Element element) {

        Object source = readerContext.extractSource(element);
        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);

        for (BeanDefinitionHolder beanDefHolder : beanDefinitions) {
            compositeDef.addNestedComponent(new BeanComponentDefinition(beanDefHolder));
        }

        // Register annotation config processors, if necessary.
        Set<BeanDefinitionHolder> processorDefinitions =
                AnnotationConfigUtils.registerAnnotationConfigProcessors(readerContext.getRegistry(), source);
        for (BeanDefinitionHolder processorDefinition : processorDefinitions) {
            compositeDef.addNestedComponent(new BeanComponentDefinition(processorDefinition));
        }

        readerContext.fireComponentRegistered(compositeDef);
    }
}
