/*
 * Optimize from Alibaba Dubbo Framework
 *
 * DUBBO Protocol Minimum Edition (better for Expand)
 *
 * by xiaoyuepeng
 *
 */
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.*;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;
import com.alibaba.dubbo.rpc.remoting.*;
import com.alibaba.dubbo.rpc.remoting.transport.mina.MinaClient;
import com.alibaba.dubbo.rpc.remoting.transport.mina.MinaServer;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * dubbo protocol support.
 */
public class DubboProtocol implements Protocol {

    //logger
    private static final Logger logger = Logger.getLogger(DubboProtocol.class);

    //server map
    private final Map<String, Server> serverMap = new ConcurrentHashMap<String, Server>(); // <host:port,Exchanger>

    //exporter map
    private final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    //invokers
    private final Set<Invoker<?>> invokers = new ConcurrentHashSet<Invoker<?>>();


    private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";


    private ExchangeHandler channelHandler = new ExchangeHandler() {

        public Object reply(Channel channel, Object message) throws RemotingException {
            //invocation
            if (message instanceof Invocation) {

                Invocation inv = (Invocation) message;

                Invoker<?> invoker = getInvoker(channel, inv);

                RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());

                return invoker.invoke(inv);
            }
            throw new RemotingException(channel, "Unsupported request: " +( message == null ? null : (message.getClass().getName() + ": " + message) )+ ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
        }


        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }


        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(Constants.STUB_EVENT_KEY, false)){
                invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }


        public void connected(Channel channel) throws RemotingException {
            invoke(channel, Constants.ON_CONNECT_KEY);
        }

        public void disconnected(Channel channel) throws RemotingException {
            if(logger.isInfoEnabled()){
                logger.info("disconected from "+ channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, Constants.ON_DISCONNECT_KEY);
        }

        public void sent(Channel channel, Object message) throws RemotingException {
        }


        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                reply(channel, message);
            }
        }

        public void caught(Channel channel, Throwable exception) throws RemotingException {
            //
        }
    };


    private ExchangeHandler delegateChannelHandler = new ExchangeHandlerDelegate() {

        public Object reply(Channel channel, Object request) throws RemotingException {
            return channelHandler.reply(channel, request);
        }

        void handlerEvent(Channel channel, Request req) throws RemotingException {
            if (req.getData() != null && req.getData().equals(Request.READONLY_EVENT)) {
                channel.setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);
            }
        }

        Response handleRequest(Channel channel, Request req) throws RemotingException {
            Response res = new Response(req.getId(), req.getVersion());
            if (req.isBroken()) {
                Object data = req.getData();

                String msg;
                if (data == null) msg = null;
                else if (data instanceof Throwable) msg = StringUtils.toString((Throwable) data);
                else msg = data.toString();
                res.setErrorMessage("Fail to decode request due to: " + msg);
                res.setStatus(Response.BAD_REQUEST);

                return res;
            }
            // find handler by message class.
            Object msg = req.getData();
            try {
                // handle data.
                Object result = channelHandler.reply(channel, msg);
                res.setStatus(Response.OK);
                res.setResult(result);
            } catch (Throwable e) {
                res.setStatus(Response.SERVICE_ERROR);
                res.setErrorMessage(StringUtils.toString(e));
            }
            return res;
        }

        void handleResponse(Channel channel, Response response) throws RemotingException {
            if (response != null && !response.isHeartbeat()) {
                DefaultFuture.received(channel, response);
            }
        }

        public void connected(Channel channel) throws RemotingException {
            HeaderExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                channelHandler.connected(exchangeChannel);
            } finally {
                HeaderExchangeChannel.removeChannelIfDisconnected(exchangeChannel);
            }
        }

        public void disconnected(Channel channel) throws RemotingException {
            HeaderExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                channelHandler.disconnected(exchangeChannel);
            } finally {
                HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        }

        public void sent(Channel channel, Object message) throws RemotingException {
            Throwable exception = null;
            try {
                Channel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
                try {
                    channelHandler.sent(exchangeChannel, message);
                } finally {
                    HeaderExchangeChannel.removeChannelIfDisconnected(channel);
                }
            } catch (Throwable t) {
                exception = t;
            }
            if (message instanceof Request) {
                Request request = (Request) message;
                DefaultFuture.sent(channel, request);
            }
            if (exception != null) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                } else if (exception instanceof RemotingException) {
                    throw (RemotingException) exception;
                } else {
                    throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(),
                            exception.getMessage(), exception);
                }
            }
        }

        private boolean isClientSide(Channel channel) {
            InetSocketAddress address = channel.getRemoteAddress();
            URL url = channel.getUrl();
            return url.getPort() == address.getPort() &&
                    NetUtils.filterLocalHost(url.getIp())
                            .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
        }

        private void decode(Object message) {
            if (message != null && message instanceof Decodeable) {
                try {
                    ((Decodeable)message).decode();
                    if (logger.isDebugEnabled()) {
                        logger.debug(new StringBuilder(32).append("Decode decodeable message ")
                                .append(message.getClass().getName()).toString());
                    }
                } catch (Throwable e) {
                    logger.warn(
                            new StringBuilder(32)
                                    .append("Call Decodeable.decode failed: ")
                                    .append(e.getMessage()).toString(),
                            e);
                }
            }
        }


        public void received(Channel channel, Object message) throws RemotingException {

            if (message instanceof Decodeable) {
                decode(message);
            }

            if (message instanceof Request) {
                decode(((Request)message).getData());
            }

            if (message instanceof Response) {
                decode( ((Response)message).getResult());
            }

            HeaderExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                if (message instanceof Request) {
                    // handle request.
                    Request request = (Request) message;
                    if (request.isEvent()) {
                        handlerEvent(channel, request);
                    } else {
                        if (request.isTwoWay()) {
                            logger.debug("Request ["+request+"] Is Two Way Message.");
                            Response response = handleRequest(exchangeChannel, request);
                            channel.send(response);
                        } else {
                            logger.debug("Request ["+request+"] Is One Way Message.");
                            channelHandler.received(exchangeChannel, request.getData());
                        }
                    }
                } else if (message instanceof Response) {
                    handleResponse(channel, (Response) message);
                }else {
                    channelHandler.received(exchangeChannel, message);
                }
            } finally {
                HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        }

        public void caught(Channel channel, Throwable exception) throws RemotingException {
            if (exception instanceof ExecutionException) {
                ExecutionException e = (ExecutionException) exception;
                Object msg = e.getRequest();
                if (msg instanceof Request) {
                    Request req = (Request) msg;
                    if (req.isTwoWay() && ! req.isHeartbeat()) {
                        Response res = new Response(req.getId(), req.getVersion());
                        res.setStatus(Response.SERVER_ERROR);
                        res.setErrorMessage(StringUtils.toString(e));
                        channel.send(res);
                        return;
                    }
                }
            }
            HeaderExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                channelHandler.caught(exchangeChannel, exception);
            } finally {
                HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        }

        public ChannelHandler getHandler() {
            if (channelHandler instanceof ExchangeHandlerDelegate) {
                return ((ExchangeHandlerDelegate) channelHandler).getHandler();
            } else {
                return channelHandler;
            }
        }
    };


    public int getDefaultPort() {
        return 20880;
    }

    public <T> Exporter<T> export(T proxy, Class<T> type) throws RpcException {
        return export(proxy, type, getDefaultPort());
    }


    public <T> Exporter<T> export(final T proxy, final Class<T> type, int port) throws RpcException {
        //build a javassist wrapper
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);

        //build a url
        final URL url = new URL(
                "dubbo",
                NetUtils.getLocalHost(),
                port
        ).setPath(type.getName());

        //export
        return export(new Invoker<T>() {
            public Class<T> getInterface() {
                return type;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                try {
                    return new RpcResult(doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments()));
                } catch (InvocationTargetException e) {
                    return new RpcResult(e.getTargetException());
                } catch (Throwable e) {
                    throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: " + e.getMessage(), e);
                }
            }

            Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable{
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }

            public URL getUrl() {
                return url;
            }

            public boolean isAvailable() {
                return true;
            }

            public void destroy() {
                //
            }

        });
    }

    /**
     * create Service
     *
     * @param invoker 服务的执行体
     * @param <T> Instance
     * @return
     * @throws RpcException
     */
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {

        //the exporter
        Exporter<T> exporter;

        //simple validate
        if (invoker == null)
            throw new IllegalStateException("service invoker == null");
        if (invoker.getInterface() == null)
            throw new IllegalStateException("service type == null");
        if (invoker.getUrl() == null)
            throw new IllegalStateException("service url == null");

        //get url
        URL url = invoker.getUrl();

        //service key
        final String key = ProtocolUtils.serviceKey(url);

        logger.info("Service ["+key+"] Start Export......");

        //simple exporter map
        exporterMap.put(key, exporter = new Exporter<T>() {
            private boolean unexported = false;

            public Invoker<T> getInvoker() {
                return invoker;
            }

            public void unexport() {
                if (unexported) {
                    return ;
                }
                unexported = true;
                invoker.destroy();
                exporterMap.remove(key);
            }
        });

        //start mina server
        try {
            openServer(url);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }

        return exporter;
    }

    private void openServer(URL url) throws RemotingException {
        // find server.
        String key = url.getAddress();
        //client 也可以暴露一个只有server可以调用的服务。
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
        if (isServer) {
            Server server = serverMap.get(key);
            if (server == null) {
                serverMap.put(key, new MinaServer(url, delegateChannelHandler));
            } else {
                //server支持reset,配合override功能使用
                server.reset(url);
            }
        }
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(channel.getUrl().getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }


    private Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException{

        int port = channel.getLocalAddress().getPort();

        String path = inv.getAttachments().get(Constants.PATH_KEY);

        String serviceKey = ProtocolUtils.serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));

        logger.debug("Service ["+serviceKey+"] Call By : "+channel);

        Exporter<?> exporter = exporterMap.get(serviceKey);

        if (exporter == null)
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);

        return exporter.getInvoker();
    }


    public <T> T refer(Class<T> type, String ip, int port, int timeout) throws RpcException{
        return getProxy(refer(type,
                new URL("dubbo", ip, port, type.getName())
                .setServiceInterface(type.getName())
                .addParameter(Constants.VERSION_KEY, Version.getVersion())
                .addParameter(Constants.TIMEOUT_KEY, timeout)
        ), new Class<?>[]{type});
    }


    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(type, url, getClients(url), invokers);
        invokers.add(invoker);
        return invoker;
    }


    @SuppressWarnings({"unchecked"})
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }


    private ExchangeClient[] getClients(URL url){
        ExchangeClient[] clients = new ExchangeClient[1];
        clients[0] = initClient(url);
        return clients;
    }


    /**
     * 创建新连接.
     */
    private ExchangeClient initClient(URL url) {

        ExchangeClient client ;
        try {
            //设置连接应该是lazy的
            client = new MinaClient(url, delegateChannelHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url
                    + "): " + e.getMessage(), e);
        }
        return client;
    }


    public void destroy() {
        for (Invoker<?> invoker : invokers){
            if (invoker != null) {
                invokers.remove(invoker);
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        for (String key : new ArrayList<String>(exporterMap.keySet())) {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }
}