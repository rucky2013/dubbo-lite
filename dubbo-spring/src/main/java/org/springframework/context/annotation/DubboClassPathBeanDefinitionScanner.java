package org.springframework.context.annotation;

import com.xyp260466.dubbo.annotation.Provider;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * Created by xyp on 16-5-10.
 */
public class DubboClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner{

    public DubboClassPathBeanDefinitionScanner(ParserContext parserContext) {
        super(parserContext.getRegistry(), false);
        XmlReaderContext readerContext = parserContext.getReaderContext();
        setResourceLoader(readerContext.getResourceLoader());
        setEnvironment(parserContext.getDelegate().getEnvironment());
        setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
        setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());
        addIncludeFilter(new AnnotationTypeFilter(Provider.class));
    }

    public Set<BeanDefinitionHolder> scanComplete(String... basePackages){
        return doScan(basePackages);
    }
}
