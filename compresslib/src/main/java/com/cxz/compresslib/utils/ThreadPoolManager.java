package com.cxz.compresslib.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理类
 */
public class ThreadPoolManager {

    private int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private int MAXIMUM_POOL_SIZE = CORE_POOL_SIZE;
    private long KEEP_ALIVE_SECONDS = 5L;

    private BlockingQueue<Runnable> mPoolWorkQueue;
    private ThreadFactory mThreadFactory;
    private RejectedExecutionHandler mRejectedHandler;

    private ThreadPoolExecutor mCPUThreadPoolExecutor;
    private Handler mMainThreadHandler;

    private static ThreadPoolManager instance;

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    private ThreadPoolManager() {
        mPoolWorkQueue = new LinkedBlockingQueue<>();
        mThreadFactory = Executors.defaultThreadFactory();
        mRejectedHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Executors.newCachedThreadPool().execute(r);
            }
        };
        mCPUThreadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, mPoolWorkQueue, mThreadFactory, mRejectedHandler);
        mCPUThreadPoolExecutor.allowCoreThreadTimeOut(true);

        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public void runOnWorkThread(Runnable runnable) {
        mCPUThreadPoolExecutor.execute(runnable);
    }

    public void runOnUIThread(Runnable runnable) {
        mMainThreadHandler.post(runnable);
    }

    public boolean isInMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
