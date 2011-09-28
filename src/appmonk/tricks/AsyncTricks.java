package appmonk.tricks;

/*
 * Copyright (C) 2009, 2010 Sebastian Delmont <sd@notso.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 * 
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.util.*;

public class AsyncTricks {
    public static final String TAG = "AsyncTricks";

    public static final int INTERACTIVE = 10;
    public static final int INTERMEDIATE = 0;
    public static final int BATCH = -10;

    protected static boolean verboseDebug = false;
    public static void enableVerboseDebug() {
        verboseDebug = true;
    }
    public static void disableVerboseDebug() {
        verboseDebug = false;
    }

    
    public abstract static class AsyncRequest {
        public static int sequenceCounter = 0;

        int priority;
        int sequence;

        abstract Handler handler();

        abstract String label();

        public abstract boolean before();
        
        public abstract void request();

        public abstract void interrupted();

        public abstract void after();

        protected AsyncRequest(int priority) {
            this.priority = priority;
            this.sequence = sequenceCounter++;
        }

        public static class Comparator implements java.util.Comparator<AsyncRequest>, Serializable {
            private static final long serialVersionUID = 1L;

            public int compare(AsyncRequest task1, AsyncRequest task2) {
                if (task1 == null) {
                    return 1;
                }
                else if (task2 == null) {
                    return -1;
                }
                else if (task1.priority == task2.priority) {
                    return (int) (task1.sequence - task2.sequence); // lower sequence numbers go ahead in the queue
                }
                else {
                    return task2.priority - task1.priority; // The higher the priority, the more important it is so it gets nearer the head
                }
            }
        }

        void runInterruptedInHandlerIfPossible() {
            if (handler() == null) {
                interrupted();
            }
            else {
                handler().post(new Runnable() {
                    public void run() {
                        interrupted();
                    }
                });
            }
        }

        void runAfterInHandlerIfPossible() {
            if (handler() == null) {
                after();
            }
            else {
                handler().post(new Runnable() {
                    public void run() {
                        after();
                    }
                });
            }
        }
    }

    public abstract static class SimpleAsyncRequest extends AsyncRequest {
        String label;
        Handler handler;
        Object[] arguments;

        public SimpleAsyncRequest(String label) {
            this(label, BATCH, null);
        }

        public SimpleAsyncRequest(String label, int priority) {
            this(label, priority, null);
        }

        public SimpleAsyncRequest(String label, Object... arguments) {
            this(label, BATCH, null);
            this.arguments = arguments;
        }

        public SimpleAsyncRequest(String label, Handler handler) {
            this(label, BATCH, handler);
        }

        public SimpleAsyncRequest(String label, Handler handler, Object... arguments) {
            this(label, BATCH, handler);
            this.arguments = arguments;
        }

        public SimpleAsyncRequest(String label, int priority, Handler handler) {
            super(priority);
            this.label = label;
            this.handler = handler;
            this.arguments = null;
        }

        public SimpleAsyncRequest(String label, int priority, Handler handler, Object... arguments) {
            this(label, priority, handler);
            this.arguments = arguments;
        }

        String label() {
            return label;
        }

        Handler handler() {
            return handler;
        }

        public boolean before() {
            return true;
        }
        
        public abstract void request();

        public void interrupted() {
        }

        public void after() {
        }

        protected Object argument(int n) {
            if (n < arguments.length)
                return arguments[n];
            else
                return null;
        }
    }

    private static PriorityBlockingQueue<AsyncRequest> requestQueue = new PriorityBlockingQueue<AsyncRequest>(10,
            new AsyncRequest.Comparator());
    private static Set<String> namedRequests = new HashSet<String>();

    private static class AsyncRequestRunner implements Runnable {
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            AsyncRequest request = null;
            while (true && !Thread.interrupted()) {
                try {
                    if (requestQueue.size() > 0) { // there seems to be a bug inside blockingqueue where in some cases an empty queue causes an exception
                        request = requestQueue.take();
                        if (request != null) {
                            synchronized (requestQueue) {
                                namedRequests.remove(request.label());
                            }

                            if (AsyncTricks.verboseDebug)
                                Log.d(TAG, "Processing async request '" + request.label() + "'");
                            if (!Thread.interrupted()) {
                                request.request();

                                if (!Thread.interrupted()) {
                                    if (AsyncTricks.verboseDebug)
                                        Log.d(TAG, "Async request '" + request.label()
                                                + "' performed, running 'after'");
                                    request.runAfterInHandlerIfPossible();
                                }
                                else {
                                    if (AsyncTricks.verboseDebug)
                                        Log.d(TAG, "Async request '" + request.label() + "' interrupted!");
                                    request.runInterruptedInHandlerIfPossible();
                                }
                            }
                            else {
                                if (AsyncTricks.verboseDebug)
                                    Log.d(TAG, "Async request '" + request.label() + "' interrupted!");
                                request.runInterruptedInHandlerIfPossible();
                            }
                        }
                    }
                    else {
                        if (AsyncTricks.verboseDebug) Log.d(TAG, "No more pending requests, releasing Wake Lock");
                        AsyncTricks.releaseWakeLock("Empty request queue");
                        break; // if we happen to be here with an empty queue, let's terminate the worker
                    }
                }
                catch (InterruptedException e) {
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "Async request thread interrupted!");
                    AsyncTricks.releaseWakeLock("Interrupted exception running request queue");
                    request.interrupted();
                }
            }
            if (AsyncTricks.verboseDebug) Log.d(TAG, "AsyncRequestRunner finished");
        }
    }

    protected static final int REQUEST_RUNNER_THREADS = 5;
    private static ArrayList<AsyncRequestRunner> runners = null;
    private static ArrayList<Thread> threads = null;

    public static void queueRequest(int priority, AsyncRequest request) {
        queueRequest(priority, request, false);
    }

    public static void replaceRequest(int priority, AsyncRequest request) {
        queueRequest(priority, request, true);
    }

    public static void queueRequest(int priority, AsyncRequest request, boolean replace) {
        if (AsyncTricks.verboseDebug) Log.d(TAG, "Queueing request " + request.label());

        synchronized (requestQueue) {
            if (!replace && namedRequests.contains(request.label())) {
                if (AsyncTricks.verboseDebug) Log.d(TAG, "Duplicate request, not queueing");
                // There is already a pending request with the same name
                request = null;
            }
            else {
                if (request.before()) {
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "Queueing a request, getting a Wake Lock just in case");
                    AsyncTricks.getWakeLock(request.label());
                    namedRequests.add(request.label());
                    requestQueue.add(request);
                }
                else {
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "Request's 'before' returned false.");
                    request = null;
                }
            }
        }

        if (request != null) {
            if (threads == null) {
                runners = new ArrayList<AsyncRequestRunner>();
                threads = new ArrayList<Thread>();
                for (int i = 0; i < REQUEST_RUNNER_THREADS; i++) {
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "Starting background runner #" + (i + 1));
                    AsyncRequestRunner runner = new AsyncRequestRunner();
                    runners.add(runner);
                    Thread thread = new Thread(null, runner, "Request Runner #" + (i + 1));
                    threads.add(thread);
                    thread.start();
                }
            }
            else {
                for (int i = 0; i < REQUEST_RUNNER_THREADS; i++) {
                    Thread thread = threads.get(i);
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "Checking thread " + thread.getName());
                    if (!thread.isAlive()) {
                        if (AsyncTricks.verboseDebug) Log.d(TAG, "Restarting thread " + thread.getName());
                        thread = new Thread(null, runners.get(i), "Request Runner #" + (i + 1));
                        thread.start();
                        threads.set(i, thread);
                    }
                }
            }
        }
    }

    public static void resetQueue(String queueName) {
        if (AsyncTricks.verboseDebug) Log.d(TAG, "Resetting " + queueName + " queue");

        try {
            // Cannot use queue.clear() because it seems to trigger a null
            // pointer bug inside the queue
            synchronized (requestQueue) {
                while (requestQueue.size() > 0)
                    requestQueue.take();
                namedRequests = new HashSet<String>();
            }
        }
        catch (Exception e) {
        }
    }
    
    protected static Context appContext = null;
    public static void setContext(Context context) {
        if (appContext == null)
            appContext = context;
    }
    
    private static PowerManager powerManager = null;
    private static PowerManager.WakeLock activeWakeLock = null;

    protected static synchronized void getWakeLock(String id) {
        if (appContext == null)
            return;
        
        if (powerManager == null) {
            powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        }
        if (activeWakeLock == null) {
            activeWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AsyncTricks");
            activeWakeLock.acquire();
            if (AsyncTricks.verboseDebug) Log.d(TAG, "++++ Acquired wakelock: " + id);
        }
    }
    
    protected static synchronized void releaseWakeLock(String id) {
        if (activeWakeLock != null) {
            try {
                if (activeWakeLock.isHeld()) {
                    activeWakeLock.release();
                    if (AsyncTricks.verboseDebug) Log.d(TAG, "---- Released wakelock: " + id);
                }
            }
            catch (Exception e) {
                // Sometimes we might "under-lock" and release one time too many
                if (AsyncTricks.verboseDebug) Log.e(TAG, "---- Release wakelock error: " + id, e);
            }
            activeWakeLock = null;
        }
    }
}
