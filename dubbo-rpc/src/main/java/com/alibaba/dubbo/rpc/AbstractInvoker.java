/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.Version;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractInvoker<T> implements Invoker<T> {

    protected final Logger logger    = Logger.getLogger(getClass());

    private final Class<T>   type;

    private final URL url;

    private final Map<String, String> attachment;

    private volatile boolean available = true;

    private volatile boolean destroyed = false;
    
    public AbstractInvoker(Class<T> type, URL url){
        this(type, url, (Map<String, String>) null);
    }
    
    public AbstractInvoker(Class<T> type, URL url, String[] keys) {
        this(type, url, convertAttachment(url, keys));
    }

    public AbstractInvoker(Class<T> type, URL url, Map<String, String> attachment) {
        if (type == null)
            throw new IllegalArgumentException("service type == null");
        if (url == null)
            throw new IllegalArgumentException("service url == null");
        this.type = type;
        this.url = url;
        this.attachment = attachment == null ? null : Collections.unmodifiableMap(attachment);
    }
    
    private static Map<String, String> convertAttachment(URL url, String[] keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        Map<String, String> attachment = new HashMap<String, String>();
        for (String key : keys) {
            String value = url.getParameter(key);
            if (value != null && value.length() > 0) {
                attachment.put(key, value);
            }
        }
        return attachment;
    }

    public Class<T> getInterface() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return available;
    }
    
    protected void setAvailable(boolean available) {
        this.available = available;
    }

    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        destroyed = true;
        setAvailable(false);
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }

    public String toString() {
        return getInterface() + " -> " + (getUrl() == null ? "" : getUrl().toString());
    }

    public Result invoke(Invocation inv) throws RpcException {
        if(destroyed) {
            throw new RpcException("Rpc invoker for service " + this + " on consumer " + NetUtils.getLocalHost()
                                            + " use dubbo version " + Version.getVersion()
                                            + " is DESTROYED, can not be invoked any more!");
        }
        RpcInvocation invocation = (RpcInvocation) inv;
        invocation.setInvoker(this);
        if (attachment != null && attachment.size() > 0) {
        	invocation.addAttachmentsIfAbsent(attachment);
        }
        Map<String, String> context = RpcContext.getContext().getAttachments();
        if (context != null) {
        	invocation.addAttachmentsIfAbsent(context);
        }
        if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false)){
        	invocation.setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
        }
        RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
        
        
        try {
            return doInvoke(invocation);
        } catch (InvocationTargetException e) { // biz exception
            Throwable te = e.getTargetException();
            if (te == null) {
                return new RpcResult(e);
            } else {
                if (te instanceof RpcException) {
                    ((RpcException) te).setCode(RpcException.BIZ_EXCEPTION);
                }
                return new RpcResult(te);
            }
        } catch (RpcException e) {
            if (e.isBiz()) {
                return new RpcResult(e);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            return new RpcResult(e);
        }
    }

    protected abstract Result doInvoke(Invocation invocation) throws Throwable;

}