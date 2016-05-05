/*
 * Optimize from Alibaba Dubbo Framework
 *
 * DUBBO Mina Server Minimum Edition (better for Expand)
 *
 * by xiaoyuepeng
 *
 */
package com.alibaba.dubbo.rpc.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.codec.ProtostuffDubboDecoder;
import com.alibaba.dubbo.rpc.codec.ProtostuffDubboEncoder;
import com.alibaba.dubbo.rpc.remoting.*;
import com.alibaba.dubbo.rpc.remoting.transport.dispatcher.ChannelEventRunnable;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.log4j.Logger;
import org.apache.mina.common.*;
import org.apache.mina.filter.codec.*;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xyp on 16-4-28.
 */
public class MinaServer implements ChannelHandler, Server{

    private static final Logger logger = Logger.getLogger(MinaServer.class);

    protected static final String SERVER_THREAD_POOL_NAME  ="DubboServerHandler";

    private InetSocketAddress localAddress;

    private InetSocketAddress bindAddress;

    private SocketAcceptor acceptor;

    private ExecutorService executor;

    private ChannelHandler srcHandler;

    private ChannelHandler delegate;

    private volatile URL         url;

    private volatile boolean     closed;

    private int                  accepts;

    private int                  idleTimeout = 600; //600 seconds


    public MinaServer(URL srcUrl, final ChannelHandler handler) throws RemotingException {

        //check
        if (srcUrl == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }

        final URL newUrl = ExecutorUtil.setThreadName(srcUrl, SERVER_THREAD_POOL_NAME);

        this.url = newUrl;
        this.srcHandler = handler;

        //address
        localAddress = url.toInetSocketAddress();
        String host = url.getParameter(Constants.ANYHOST_KEY, false)
                || NetUtils.isInvalidLocalHost(url.getHost())
                ? NetUtils.ANYHOST : url.getHost();
        bindAddress = new InetSocketAddress(host, url.getPort());

        //initialize parameters
        this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);
        this.idleTimeout = url.getParameter(Constants.IDLE_TIMEOUT_KEY, Constants.DEFAULT_IDLE_TIMEOUT);


        //initialize Executor
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
                                newUrl.getProtocol(), newUrl.getIp(), newUrl.getPort());
                        logger.warn(msg);
                        throw new RejectedExecutionException(msg);
                    }
                }
        );

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

        //start listen
        try {
            doOpen();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " bind " + bindAddress + ", export " + getLocalAddress());
            }
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName()
                    + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
        }

    }

    public URL getUrl() {
        return url;
    }

    protected void doOpen() throws Throwable {

        // set thread pool.
        acceptor = new SocketAcceptor(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                Executors.newCachedThreadPool(
                        new ThreadFactory() {

                            private final AtomicInteger mThreadNum = new AtomicInteger(1);

                            private final String mPrefix = "MinaServerWorker-thread-";

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
                        }
                ));

        // config
        SocketAcceptorConfig cfg = acceptor.getDefaultConfig();
        cfg.setThreadModel(ThreadModel.MANUAL);


        // set codec.
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory(){

            private ProtocolEncoder encoder = new ProtostuffDubboEncoder();
            private ProtocolDecoder decoder = new ProtostuffDubboDecoder();

            public ProtocolEncoder getEncoder() throws Exception {
                return encoder;
            }

            public ProtocolDecoder getDecoder() throws Exception {
                return decoder;
            }

        }));

        //bind
        acceptor.bind(bindAddress, new IoHandlerAdapter() {

            public void sessionOpened(IoSession session) throws Exception {
                logger.debug("Mina Server Session Opened: "+session);
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    connected(channel);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }

            public void sessionClosed(IoSession session) throws Exception {
                logger.debug("Mina Server Session Closed: "+session);
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    disconnected(channel);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }

            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                logger.debug("Mina Server Exception Caught: "+session+", Exception: "+cause);
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    caught(channel, cause);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }

            public void messageReceived(IoSession session, Object message) throws Exception {
                logger.debug("Mina Server Message Received: "+session+", Message: "+message);
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    received(channel, message);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }

            public void messageSent(IoSession session, Object message) throws Exception {
                logger.debug("Mina Server Message Sent: "+session+", Message: "+message);
                MinaChannel channel = MinaChannel.getOrAddChannel(session, url, delegate);
                try {
                    sent(channel, message);
                } finally {
                    MinaChannel.removeChannelIfDisconnectd(session);
                }
            }
        });


    }


    protected void doClose() throws Throwable {
        try {
            if (acceptor != null) {
                acceptor.unbind(bindAddress);
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }


    public Collection<Channel> getChannels() {
        Set<IoSession> sessions = acceptor.getManagedSessions(bindAddress);
        Collection<Channel> channels = new HashSet<Channel>();
        for (IoSession session : sessions) {
            if (session.isConnected()) {
                channels.add(MinaChannel.getOrAddChannel(session, getUrl(), this));
            }
        }
        return channels;
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        Set<IoSession> sessions = acceptor.getManagedSessions(bindAddress);
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(remoteAddress)) {
                return MinaChannel.getOrAddChannel(session, getUrl(), this);
            }
        }
        return null;
    }

    public boolean isBound() {
        return acceptor.isManaged(bindAddress);
    }


    //methods for endpoint
    public ChannelHandler getChannelHandler() {
        return srcHandler;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }

    public void send(Object message, boolean sent) throws RemotingException {
        Collection<Channel> channels = getChannels();
        for (Channel channel : channels) {
            if (channel.isConnected()) {
                channel.send(message, sent);
            }
        }
    }

    public void close() {
        if (logger.isInfoEnabled()) {
            logger.info("Close " + getClass().getSimpleName() + " bind " + bindAddress + ", export " + getLocalAddress());
        }
        ExecutorUtil.shutdownNow(executor ,100);
        closed = true;
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor ,timeout);
        close();
    }

    public boolean isClosed() {
        return closed;
    }



    //methods for channel handler
    public void connected(Channel channel) throws RemotingException {
        Collection<Channel> channels = getChannels();
        if (accepts > 0 && channels.size() > accepts) {
            logger.error("Close channel " + channel + ", cause: The server " + channel.getLocalAddress() + " connections greater than max config " + accepts);
            channel.close();
            return;
        }
        if (closed) {
            return;
        }
        delegate.connected(channel);
    }

    public void disconnected(Channel channel) throws RemotingException {
        Collection<Channel> channels = getChannels();
        if (channels.size() == 0){
            logger.warn("All clients has discontected from " + channel.getLocalAddress() + ". You can graceful shutdown now.");
        }
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

    public void reset(URL url) {
        if (url == null) {
            return;
        }
        try {
            if (url.hasParameter(Constants.ACCEPTS_KEY)) {
                int a = url.getParameter(Constants.ACCEPTS_KEY, 0);
                if (a > 0) {
                    this.accepts = a;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        try {
            if (url.hasParameter(Constants.IDLE_TIMEOUT_KEY)) {
                int t = url.getParameter(Constants.IDLE_TIMEOUT_KEY, 0);
                if (t > 0) {
                    this.idleTimeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        try {
            if (url.hasParameter(Constants.THREADS_KEY)
                    && executor instanceof ThreadPoolExecutor && !executor.isShutdown()) {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                int threads = url.getParameter(Constants.THREADS_KEY, 0);
                int max = threadPoolExecutor.getMaximumPoolSize();
                int core = threadPoolExecutor.getCorePoolSize();
                if (threads > 0 && (threads != max || threads != core)) {
                    if (threads < core) {
                        threadPoolExecutor.setCorePoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setMaximumPoolSize(threads);
                        }
                    } else {
                        threadPoolExecutor.setMaximumPoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setCorePoolSize(threads);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        getUrl().addParameters(url.getParameters());
    }


    public int getAccepts() {
        return accepts;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }



}
