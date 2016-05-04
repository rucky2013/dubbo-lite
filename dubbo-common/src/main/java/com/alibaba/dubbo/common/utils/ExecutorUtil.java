/*
 * Copy from Alibaba Dubbo Framework
 *
 */
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import org.apache.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorUtil {
    private static final Logger logger = Logger.getLogger(ExecutorUtil.class);
    private static final ThreadPoolExecutor shutdownExecutor = new ThreadPoolExecutor(0, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100),
            new ThreadFactory() {
                private final AtomicInteger mThreadNum = new AtomicInteger(1);

                private final String mPrefix = "Close-ExecutorService-Timer-thread-";

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

    public static boolean isShutdown(Executor executor) {
        if (executor instanceof ExecutorService) {
            if (((ExecutorService) executor).isShutdown()) {
                return true;
            }
        }
        return false;
    }
    public static void gracefulShutdown(Executor executor, int timeout) {
        if (!(executor instanceof ExecutorService) || isShutdown(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdown(); // Disable new tasks from being submitted
        } catch (SecurityException ex2) {
            return ;
        } catch (NullPointerException ex2) {
            return ;
        }
        try {
            if(! es.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (!isShutdown(es)){
            newThreadToCloseExecutor(es);
        }
    }
    public static void shutdownNow(Executor executor, final int timeout) {
        if (!(executor instanceof ExecutorService) || isShutdown(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (SecurityException ex2) {
            return ;
        } catch (NullPointerException ex2) {
            return ;
        }
        try {
            es.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (!isShutdown(es)){
            newThreadToCloseExecutor(es);
        }
    }

    private static void newThreadToCloseExecutor(final ExecutorService es) {
        if (!isShutdown(es)) {
            shutdownExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        for (int i=0;i<1000;i++){
                            es.shutdownNow();
                            if (es.awaitTermination(10, TimeUnit.MILLISECONDS)){
                                break;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    } 
                }
            });
        }
    }

    /**
     * append thread name with url address
     * @return new url with updated thread name
     */
    public static URL setThreadName(URL url, String defaultName) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, defaultName);
        name = new StringBuilder(32).append(name).append("-").append(url.getAddress()).toString();
        url = url.addParameter(Constants.THREAD_NAME_KEY, name);
        return url;
    }
}