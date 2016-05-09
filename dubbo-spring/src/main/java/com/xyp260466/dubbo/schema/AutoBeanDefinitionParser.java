package com.xyp260466.dubbo.schema;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Created by xyp on 16-5-9.
 */
public class AutoBeanDefinitionParser extends ComponentScanBeanDefinitionParser {
    private static final Logger logger = Logger.getLogger(AutoBeanDefinitionParser.class);

    public BeanDefinition parse(Element element, ParserContext parserContext) {

        logger.info("Parse Dubbo Services.");


        return null;
    }
}
