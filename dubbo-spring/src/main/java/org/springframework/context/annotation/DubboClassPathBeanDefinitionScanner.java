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
