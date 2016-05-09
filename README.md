# dubbo-lite
Dubbo 高性能升级版本，简单，高效

基于DUBBO 2.5.3构造

主要升级及改动变化

1、去除所有坑余代码及不必要的扩展<br/>
2、分为 dubbo-common 与 dubbo-rpc两大主要模块<br/>
3、NIO网络传输层升级为Apache Mina（保留dubbo原始mina版本）<br/>
4、代理生成采用 dubbo-2.5.3原生 Javassist框架<br/>
5、序列化采用高性能框架 Protostuff-1.3.3 （相比hession提升90%，秒杀java-built-in）<br/>
6、保留部分核心<br/>
7、相比dubbo原始框架，代码结构清晰，有助于阅读及参考<br/>
8、欢迎积极扩展!<br/>

使用非常简单：<br/>

发布服务：<br/>
//initialize a protocol<br/>
DubboProtocol protocol = new DubboProtocol();<br/>
<br/>
//export a service<br/>
protocol.export(new SimpleImpl(), Simple.class, 2880);<br/>

消费服务：<br/>
//initialize a protocol<br/>
DubboProtocol protocol = new DubboProtocol();<br/>
<br/>
Simple invoker = protocol.refer(Simple.class, "127.0.0.1", 2880, 3600);<br/>

Spring集成：<br/>
从此Spring集成将会变的非常简单<br/>
<br/>
在Spring配置文件中加入如下内容:<br/>
<br/>
&lt;dubbo:annotation-driven/&gt;<br/>
<br/>

消费服务：加上@Consumer注解<br/>
@Consumer<br/>
private SimpleProvider simpleProvider;<br/>
<br/>
发布服务：加上@Provider注解<br/>
@Provider<br/>
public class SimpleProviderImpl implements SimpleProvider <br/>