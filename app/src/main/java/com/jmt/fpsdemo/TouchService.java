package com.jmt.fpsdemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.jmt.fps.JmtFP;

public class TouchService  extends Service {

    private static final String TAG = "JMT103";

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock = null;

    private static JmtFP mSensor;

    public void onCreate() {
        Log.e(TAG, "onCreate");

        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);

        super.onCreate();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.e(TAG, "ACTION_SCREEN_OFF");

                acquireWakeLock();
                Log.e(TAG, "Set Wait Touch Finger");
                WaitTouchFinger();

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.e(TAG, "ACTION_SCREEN_ON");
                releaseWakeLock();
                if (mSensor != null) {
                    Log.e(TAG, "CancelWaitTouch");
                    mSensor.CancelAction();
                    mSensor.CancelWaitTouch();
                }
            }
        }
    };

    public void WaitTouchFinger() {
        Log.e(TAG, "Begin WaitTouchFinger Thread!!");

        mSensor = new JmtFP();
        mSensor.CancelAction();

        Thread tid_waitTouch = new Thread(new WaitTouchThread());
        tid_waitTouch.setPriority(Thread.MAX_PRIORITY);
        tid_waitTouch.start();
     }

    private class WaitTouchThread implements Runnable {
        @Override
        public void run() {
            boolean keep_going = true;
            int Module_Result;

            while(keep_going) {
                Log.e(TAG, "go Wait Touch...");
                Module_Result = mSensor.SetWaitTouch();
                Log.e(TAG, "go Wait Touch ret ="+ Integer.toString(Module_Result));
                if (Module_Result == JmtFP.ERR_OK) {
                    Module_Result = mSensor.Verify();
                    Log.e(TAG, "mSensor.MatchFinger");
                    if (Module_Result == JmtFP.ERR_OK) {
                        Log.e(TAG, "FP Verify OK!!");
                        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
                        if (!mWakeLock.isHeld()) {
                            Log.e(TAG, "Acquiring wakelock");
                            mWakeLock.acquire();
                        }
                        keep_going = false;
                    } else {
                        Log.e(TAG, "Wait Touch Cancel!");
                        mSensor.CancelWaitTouch();
                    }
                }else{
                    keep_going = false;
                    Log.e(TAG, "Wait Touch Cancel or exceptions occur!");
                    mSensor.CancelWaitTouch();
                }
            }
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");

        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);

        return START_STICKY;
    }

    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        releaseWakeLock();

        unregisterReceiver(receiver);

        super.onDestroy();
    }

    private void acquireWakeLock() {
        if (null == mWakeLock) {
            Log.e(TAG, "Acquiring wakelock");
            mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, this.getClass().getCanonicalName());
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (null != mWakeLock && mWakeLock.isHeld()) {
            Log.e(TAG, "call releaseWakeLock");
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
