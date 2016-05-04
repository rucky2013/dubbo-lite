package com.alibaba.dubbo.rpc.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.remoting.Channel;
import com.alibaba.dubbo.rpc.remoting.ChannelHandler;
import com.alibaba.dubbo.rpc.remoting.Endpoint;
import com.alibaba.dubbo.rpc.remoting.RemotingException;
import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import java.net.InetSocketAddress;

/**
 * Created by xyp on 16-4-28.
 */
public class MinaChannel implements Endpoint, ChannelHandler, Channel{

    private static final Logger logger = Logger.getLogger(MinaChannel.class);
    private static final String CHANNEL_KEY = MinaChannel.class.getName() + ".CHANNEL";
    private final IoSession session;
    private final ChannelHandler handler;
    private volatile boolean     closed;
    private volatile URL         url;


    private MinaChannel(IoSession session, URL url, ChannelHandler handler){
        //
        this.handler = handler;
        this.url = url;

        if (session == null) {
            throw new IllegalArgumentException("mina session == null");
        }
        this.session = session;
    }

    static MinaChannel getOrAddChannel(IoSession session, URL url, ChannelHandler handler) {
        if (session == null) {
            return null;
        }
        MinaChannel ret = (MinaChannel) session.getAttribute(CHANNEL_KEY);
        if (ret == null) {
            ret = new MinaChannel(session, url, handler);
            if (session.isConnected()) {
                MinaChannel old = (MinaChannel) session.setAttribute(CHANNEL_KEY, ret);
                if (old != null) {
                    session.setAttribute(CHANNEL_KEY, old);
                    ret = old;
                }
            }
        }
        return ret;
    }

    static void removeChannelIfDisconnectd(IoSession session) {
        if (session != null && ! session.isConnected()) {
            session.removeAttribute(CHANNEL_KEY);
        }
    }


    public void connected(Channel channel) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(channel);
    }

    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    public void sent(Channel channel, Object message) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(channel, message);
    }

    public void received(Channel channel, Object message) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(channel, message);
    }

    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    public URL getUrl() {
        return url;
    }

    public ChannelHandler getChannelHandler() {
        return handler;
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) session.getLocalAddress();
    }

    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }

    public void send(Object message, boolean sent) throws RemotingException {
        if (isClosed()) {
            throw new RemotingException(this, "Failed to send message "
                    + (message == null ? "" : message.getClass().getName()) + ":" + message
                    + ", cause: Channel closed. channel: " + getLocalAddress() + " -> " + getRemoteAddress());
        }
        boolean success = true;
        int timeout = 0;
        try {
            WriteFuture future = session.write(message);
            if (sent) {
                timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
                success = future.join(timeout);
            }
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }

        if(!success) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress()
                    + "in timeout(" + timeout + "ms) limit");
        }
    }

    public void close() {
        closed = true;
        try {
            removeChannelIfDisconnectd(session);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("CLose mina channel " + session);
            }
            session.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void close(int timeout) {
        close();
    }

    public boolean isClosed() {
        return closed;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) session.getRemoteAddress();
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public boolean hasAttribute(String key) {
        return session.containsAttribute(key);
    }

    public Object getAttribute(String key) {
        return session.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        session.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        session.removeAttribute(key);
    }

    @Override
    public String toString() {
        return "MinaChannel [session=" + session + "] " + getLocalAddress() + " -> " + getRemoteAddress();
    }
}
