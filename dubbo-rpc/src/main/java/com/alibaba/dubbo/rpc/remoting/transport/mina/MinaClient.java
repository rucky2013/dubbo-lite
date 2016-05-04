/*
 * Optimize from Alibaba Dubbo Framework
 *
 * DUBBO Mina Client Minimum Edition (better for Expand)
 *
 * by xiaoyuepeng
 *
 */
package com.alibaba.dubbo.rpc.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
//import com.alibaba.dubbo.common.store.SimpleDataStore;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.Version;
import com.alibaba.dubbo.rpc.codec.ProtostuffDubboDecoder;
import com.alibaba.dubbo.rpc.codec.ProtostuffDubboEncoder;
import com.alibaba.dubbo.rpc.remoting.*;
import com.alibaba.dubbo.rpc.remoting.transport.dispatcher.ChannelEventRunnable;
import org.apache.log4j.Logger;
import org.apache.mina.common.*;
import org.apache.mina.filter.codec.*;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MinaClient  implements ChannelHandler, ExchangeClient {

    private static final Logger logger = Logger.getLogger(MinaClient.class);

    protected static final String CLIENT_THREAD_POOL_NAME  ="DubboClientHandler";

    private final Lock connectLock = new ReentrantLock();

    private volatile boolean     closed;

    private int                   timeout;

    private int                   connectTimeout;

    private final boolean send_reconnect ;

    private final long shutdown_timeout ;

    private volatile  ScheduledFuture<?> reconnectExecutorFuture = null;

    private final AtomicInteger reconnect_count = new AtomicInteger(0);

    //重连的error日志是否已经被调用过.
    private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false) ;

    //重连warning的间隔.(waring多少次之后，warning一次) //for test
    private final int reconnect_warning_period ;

    private String connectorKey;

    //mina socket connector
    private SocketConnector connector;

    //mina session
    private volatile IoSession session;

    private ExecutorService executor;


    //the last successed connected time
    private long lastConnectedTime = System.currentTimeMillis();

    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        private final String mPrefix = "DubboClientReconnectTimer-thread-";

        private final boolean mDaemo = true;

        private final ThreadGroup mGroup =
                ( System.getSecurityManager() == null ) ? Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup();

        public Thread newThread(Runnable runnable)
        {
            String name = mPrefix + mThreadNum.getAndIncrement();
            Thread ret = new Thread(mGroup, runnable, name, 0);
            ret.setDaemon(mDaemo);
            return ret;
        }

        public ThreadGroup getThreadGroup()
        {
            return mGroup;
        }
    });


    private static final Map<String, SocketConnector> connectors = new ConcurrentHashMap<String, SocketConnector>();

    private final ChannelHandler srcHandler;

    private ChannelHandler delegate;

    private volatile URL         url;

    private ExchangeChannel channel;


    public MinaClient(URL srcUrl, final ChannelHandler handler) throws RemotingException {

        //check
        if (srcUrl == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }

        this.channel = new HeaderExchangeChannel(this);

        srcUrl = ExecutorUtil.setThreadName(srcUrl, CLIENT_THREAD_POOL_NAME);
        final URL newUrl = srcUrl.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);

        this.timeout = srcUrl.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = srcUrl.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);

        send_reconnect = srcUrl.getParameter(Constants.SEND_RECONNECT_KEY, false);

        shutdown_timeout = srcUrl.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);

        reconnect_warning_period = srcUrl.getParameter("reconnect.waring.period", 1800);

        this.srcHandler = handler;
        this.url = newUrl;

        //executor
        final String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
        int threads = url.getParameter(Constants.THREADS_KEY, Integer.MAX_VALUE);
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        int alive = url.getParameter(Constants.ALIVE_KEY, Constants.DEFAULT_ALIVE);
        executor = new ThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                //thread factory
                new ThreadFactory() {
                    private final AtomicInteger mThreadNum = new AtomicInteger(1);

                    private final String mPrefix = name + "-thread-";

                    private final boolean mDaemo = true;

                    private final ThreadGroup mGroup =
                            ( System.getSecurityManager() == null ) ? Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup();

                    public Thread newThread(Runnable runnable)
                    {
                        String name = mPrefix + mThreadNum.getAndIncrement();
                        Thread ret = new Thread(mGroup, runnable, name, 0);
                        ret.setDaemon(mDaemo);
                        return ret;
                    }

                    public ThreadGroup getThreadGroup()
                    {
                        return mGroup;
                    }
                },

                //abort policy
                new ThreadPoolExecutor.AbortPolicy(){

                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        String msg = String.format("Thread pool is EXHAUSTED!" +
                                        " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: %d)," +
                                        " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s), in %s://%s:%d!" ,
                                name, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(), e.getLargestPoolSize(),
                                e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(),
                                url.getProtocol(), url.getIp(), url.getPort());
                        logger.warn(msg);
                        throw new RejectedExecutionException(msg);
                    }
                }
        );

        //store
        //SimpleDataStore.getDataStore().put(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()), executor);

        //create delegate
        delegate = new ChannelHandler() {
            public void connected(Channel channel) throws RemotingException {
                executor.execute(new ChannelEventRunnable(channel, handler , ChannelEventRunnable.ChannelState.CONNECTED));
            }

            public void disconnected(Channel channel) throws RemotingException {
                executor.execute(new ChannelEventRunnable(channel, handler , ChannelEventRunnable.ChannelState.DISCONNECTED));
            }

            public void sent(Channel channel, Object message) throws RemotingException {
                handler.sent(channel, message);
            }

            public void received(Channel channel, Object message) throws RemotingException {
                executor.execute(new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.RECEIVED, message));
            }

            public void caught(Channel channel, Throwable exception) throws RemotingException {
                executor.execute(new ChannelEventRunnable(channel, handler , ChannelEventRunnable.ChannelState.CAUGHT, exception));
            }
        };


        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        try {
            // connect.
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close();
                throw t;
            } else {
                logger.error("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                        + " connect to the server " + getRemoteAddress() + " (check == false, ignore and retry later!), cause: " + t.getMessage(), t);
            }
        } catch (Throwable t){
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }


    }


    /**
     * @param url
     * @return 0-false
     */
    private static int getReconnectParam(URL url){
        int reconnect ;
        String param = url.getParameter(Constants.RECONNECT_KEY);
        if (param == null || param.length()==0 || "true".equalsIgnoreCase(param)){
            reconnect = Constants.DEFAULT_RECONNECT_PERIOD;
        }else if ("false".equalsIgnoreCase(param)){
            reconnect = 0;
        } else {
            try{
                reconnect = Integer.parseInt(param);
            }catch (Exception e) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:"+param);
            }
            if(reconnect < 0){
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:"+param);
            }
        }
        return reconnect;
    }

    private synchronized void destroyConnectStatusCheckCommand(){
        try {
            if (reconnectExecutorFuture != null && ! reconnectExecutorFuture.isDone()){
                reconnectExecutorFuture.cancel(true);
                reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * init reconnect thread
     */
    private synchronized void initConnectStatusCheckCommand(){
        //reconnect=false to close reconnect
        int reconnect = getReconnectParam(getUrl());
        if(reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())){
            Runnable connectStatusCheckCommand =  new Runnable() {
                public void run() {
                    try {
                        if (! isConnected()) {
                            connect();
                        } else {
                            lastConnectedTime = System.currentTimeMillis();
                        }
                    } catch (Throwable t) {
                        String errorMsg = "client reconnect to "+getUrl().getAddress()+" find error . url: "+ getUrl();
                        // wait registry sync provider list
                        if (System.currentTimeMillis() - lastConnectedTime > shutdown_timeout){
                            if (!reconnect_error_log_flag.get()){
                                reconnect_error_log_flag.set(true);
                                logger.error(errorMsg, t);
                                return ;
                            }
                        }
                        if ( reconnect_count.getAndIncrement() % reconnect_warning_period == 0){
                            logger.warn(errorMsg, t);
                        }
                    }
                }
            };
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }

    protected void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected()) {
                return;
            }
            initConnectStatusCheckCommand();
            doConnect();
            if (! isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                        + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            } else {
                if (logger.isInfoEnabled()){
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }
            reconnect_count.set(0);
            reconnect_error_log_flag.set(false);
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                    + ", cause: " + e.getMessage(), e);
        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            destroyConnectStatusCheckCommand();
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }

    protected void doOpen() throws Throwable {
        connectorKey = getUrl().toFullString();
        SocketConnector c = connectors.get(connectorKey);
        if (c != null) {
            connector = c;
        } else {
            // set thread pool.
            connector = new SocketConnector(Constants.DEFAULT_IO_THREADS,
                    Executors.newCachedThreadPool(new ThreadFactory() {
                        private final AtomicInteger mThreadNum = new AtomicInteger(1);

                        private final String mPrefix = "MinaClientWorker-thread-";

                        private final boolean mDaemo = true;

                        private final ThreadGroup mGroup =
                                ( System.getSecurityManager() == null ) ? Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup();

                        public Thread newThread(Runnable runnable)
                        {
                            String name = mPrefix + mThreadNum.getAndIncrement();
                            Thread ret = new Thread(mGroup, runnable, name, 0);
                            ret.setDaemon(mDaemo);
                            return ret;
                        }

                        public ThreadGroup getThreadGroup()
                        {
                            return mGroup;
                        }
                    }));
            // config
            SocketConnectorConfig cfg = connector.getDefaultConfig();
            cfg.setThreadModel(ThreadModel.MANUAL);
            cfg.getSessionConfig().setTcpNoDelay(true);
            cfg.getSessionConfig().setKeepAlive(true);
            int timeout = getTimeout();
            cfg.setConnectTimeout(timeout < 1000 ? 1 : timeout / 1000);
            // set codec.
            connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory(){

                public ProtocolEncoder getEncoder() throws Exception {
                    return new ProtostuffDubboEncoder();
                }

                public ProtocolDecoder getDecoder() throws Exception {
                    return new ProtostuffDubboDecoder();
                }

            }));
            connectors.put(connectorKey, connector);
        }
    }

    protected void doConnect() throws Throwable {
        ConnectFuture future = connector.connect(getConnectAddress(), new IoHandlerAdapter() {

            public void sessionOpened(IoSession ioSession) throws Exception {
                MinaChannel channel = MinaChannel.getOrAddChannel(ioSession, url, delegate);
                try {
                    connected(channel);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(ioSession);
                }
            }

            public void sessionClosed(IoSession ioSession) throws Exception {
                MinaChannel channel = MinaChannel.getOrAddChannel(ioSession, url, delegate);
                try {
                    disconnected(channel);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(ioSession);
                }
            }


            public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
                MinaChannel channel = MinaChannel.getOrAddChannel(ioSession, url, delegate);
                try {
                    caught(channel, throwable);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(ioSession);
                }
            }

            public void messageReceived(IoSession ioSession, Object o) throws Exception {
                MinaChannel channel = MinaChannel.getOrAddChannel(ioSession, url, delegate);
                try {
                    received(channel, o);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(ioSession);
                }
            }

            public void messageSent(IoSession ioSession, Object o) throws Exception {
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    sent(channel, o);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }
        });
        long start = System.currentTimeMillis();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        final CountDownLatch finish = new CountDownLatch(1); // resolve future.awaitUninterruptibly() dead lock
        future.addListener(new IoFutureListener() {
            public void operationComplete(IoFuture future) {
                try {
                    if (future.isReady()) {
                        IoSession newSession = future.getSession();
                        try {
                            // 关闭旧的连接
                            IoSession oldSession = MinaClient.this.session; // copy reference
                            if (oldSession != null) {
                                try {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("Close old mina channel " + oldSession + " on create new mina channel " + newSession);
                                    }
                                    oldSession.close();
                                } finally {
                                    MinaChannel.removeChannelIfDisconnectd(oldSession);
                                }
                            }
                        } finally {
                            if (MinaClient.this.isClosed()) {
                                try {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("Close new mina channel " + newSession + ", because the client closed.");
                                    }
                                    newSession.close();
                                } finally {
                                    MinaClient.this.session = null;
                                    MinaChannel.removeChannelIfDisconnectd(newSession);
                                }
                            } else {
                                MinaClient.this.session = newSession;
                            }
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    finish.countDown();
                }
            }
        });
        try {
            finish.await(getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server " + getRemoteAddress() + " client-side timeout "
                    + getTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start)
                    + "ms) from netty client " + NetUtils.getLocalHost() + " using dubbo version "
                    + Version.getVersion() + ", cause: " + e.getMessage(), e);
        }
        Throwable e = exception.get();
        if (e != null) {
            throw e;
        }
    }

    protected void doDisConnect() throws Throwable {
        try {
            MinaChannel.removeChannelIfDisconnectd(session);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
    }

    protected void doClose() throws Throwable {
        //release mina resouces.
    }


    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }


    protected int getTimeout() {
        return timeout;
    }

    protected int getConnectTimeout() {
        return connectTimeout;
    }

    public void connected(Channel channel) throws RemotingException {
        if (closed) {
            return;
        }
        delegate.connected(channel);
    }

    public void disconnected(Channel channel) throws RemotingException {
        delegate.disconnected(channel);
    }

    public void sent(Channel channel, Object message) throws RemotingException {
        if (closed) {
            return;
        }
        delegate.sent(channel, message);
    }

    public void received(Channel channel, Object message) throws RemotingException {
        if (closed) {
            return;
        }
        delegate.received(channel, message);
    }

    public void caught(Channel channel, Throwable exception) throws RemotingException {
        delegate.caught(channel, exception);
    }

    public URL getUrl() {
        return url;
    }

    public ChannelHandler getChannelHandler() {
        return srcHandler;
    }


    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }

    public void send(Object message, boolean sent) throws RemotingException {
        if (send_reconnect && !isConnected()){
            connect();
        }
        Channel channel = getChannel();
        //TODO getChannel返回的状态是否包含null需要改进
        if (channel == null || ! channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    public void close() {
        try {
            if (executor != null) {
                ExecutorUtil.shutdownNow(executor, 100);
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        closed = true;
        try {
            disconnect();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public ResponseFuture request(Object request) throws RemotingException {
        return channel.request(request);
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return channel.request(request, timeout);
    }

    public ExchangeHandler getExchangeHandler() {
        return (ExchangeHandler)delegate;
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor ,timeout);
        close();
    }

    public boolean isClosed() {
        return false;
    }

    protected Channel getChannel() {
        IoSession s = session;
        if (s == null || ! s.isConnected())
            return null;
        return MinaChannel.getOrAddChannel(s, getUrl(), this);
    }

    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }

    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.isConnected();
    }

    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return null;
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.hasAttribute(key);
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }


    public void reconnect() throws RemotingException {
        disconnect();
        connect();
    }

}
