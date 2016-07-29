package com.jmt.fpsdemo;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;

import com.jmt.fps.JmtFP;

import java.io.File;
import java.nio.ByteBuffer;

public class MainActivity extends Activity implements SurfaceHolder.Callback, JmtFP.jmtcallback {

    private static final String TAG = "JMTDemoAPP";

    private static String Ver;

    private JmtFP mSensor;

    private boolean user_cancel;

    private int iFpImageWidth;
    private int iFpImageHeight;
    private int iFpSize;
    private int iFpChipID;

    private byte[] mRGBFinger;

    private Button btnImage;
    private Button btnEnroll;
    private Button btnVerify;
    private Button btnCancel;
    private Button btnErase;
    private Button btnStress;
    private Button btnExit;
    private Button btnNavigation;
    private Button btnWaitClick;

    private TextView tvSDK;
    private TextView tvChipID;
    private TextView tvPercentage;
    private TextView tvMessage;
    private TextView tvScroll;

    private ScrollView svScroll;
    private HorizontalScrollView hsvScroll;

    private SurfaceView mSurfaceView;
    private static SurfaceHolder mHolder;

    private static Toast mToast;

    private static boolean bmpReady;
    private static BitmapPool mBitmap_Finger;
    private static Paint mPaint;
    private static Canvas mCanvas;
    private static Matrix mMatrix;

    private Thread tid_draw;
    private HandlerThread tid_image;
    private Thread tid_enroll;
    private Thread tid_match;
    private Thread tid_stress;
    private Thread tid_navigation;
    private Thread tid_waitclick;

    private Handler hid_image;

    private Runnable rid_image;

    private Handler uiUpdateHandler;

    private SoundPool soundPool;
    private int currStreamId1;
    private int currStreamId2;

    private boolean onStress;

    private boolean onGetFinger;
    private boolean onEnroll;
    private boolean onVerify;
    //private boolean onNavigation;
    //private boolean onWaitClick;

    private ImageView ivImage1;

    private Vibrator mVibrator;

    static final int BITMAP_POOL_SIZE                   = 5;

    static final int MSG_NATIVE_CALLBACK                = 1;
    static final int MSG_THREAD_TOGGLE_BTN              = 2;
    static final int MSG_THREAD_GET_FIG                 = 3;
    static final int MSG_THREAD_ENROLL_FIG              = 4;
    static final int MSG_THREAD_ENROLL_OK               = 5;
    static final int MSG_THREAD_VERIFY_FIG              = 6;
    static final int MSG_THREAD_VERIFY_Fail             = 7;
    static final int MSG_THREAD_OUTOFMEMORY             = 10;
    static final int MSG_NO_SENSOR                      = 11;
    static final int MSG_THREAD_ERASE_FULL              = 12;
    static final int MSG_POOR_IMG                       = 13;
    static final int MSG_THREAD_GET_FIG_FAIL            = 14;
    static final int MSG_NOTIFY_LEAVE_FINGER            = 15;
    static final int MSG_ADJUST                         = 16;
    static final int MSG_WAIT_FINGER                    = 17;
    static final int MSG_CHANGE_FINGER                  = 18;

    File database = new File(Environment.getDataDirectory() + "/data/com.jmt.fpsdemo/fdbs");

    private int wait_finger = 0;

    private final static String driver_path = "/dev/jmt101";

    public int onClick(int event) {

        switch (event){
            case 1:
                Log.e(TAG, "Double click.....");
                break;
            case 2:
                Log.e(TAG, "Long Click.....");
                break;
            case 0:
                Log.e(TAG, "One click.....");
                break;

            default:
                break;
        }
        //uiUpdateHandler.sendMessage(uiMessage);
        return 0;
    }

    public int onNavigation( int result, int ver, int hor) {
        //Log.e(TAG, " onNavigation, ver = " + ver + "hor =" + hor);

        hsvScroll.smoothScrollBy(ver, 0);
        svScroll.smoothScrollBy(0, hor);

        return 0;
    }

    public int onEnrollProgress(String finger_id, int percentage) {
        Log.e(TAG, " onEnrollProgress finger_id = " + finger_id + "complete percent =" + Integer.toString(percentage));
        Message uiMessage;

        uiMessage = new Message();
        uiMessage.what = MSG_THREAD_ENROLL_FIG;
        uiMessage.arg1 = percentage;
        uiUpdateHandler.sendMessage(uiMessage);

        return 0;
    }

    public int onEnrollProgress2(int gid, int fid, int percentage) {
        Log.e(TAG, " onEnrollProgress gid = "+ Integer.toString(gid)  +" fid = "+Integer.toString(fid) + "complete percent =" + Integer.toString(percentage));
        Message uiMessage;

        uiMessage = new Message();
        uiMessage.what = MSG_THREAD_ENROLL_FIG;
        uiMessage.arg1 = percentage;
        uiUpdateHandler.sendMessage(uiMessage);

        return 0;
    }

    public int onAdvice(int event) {
        Message uiMessage;
        switch (event){
            case JmtFP.NOTIFY_LEAVE_FINGER:
                Log.e(TAG, "Leave your finger.....");
                uiMessage = new Message();
                uiMessage.what = MSG_NOTIFY_LEAVE_FINGER;
                uiUpdateHandler.sendMessage(uiMessage);
                break;
            case JmtFP.TOO_WET :
                /*
                uiMessage = new Message();
                uiMessage.what = JmtFP.TOO_WET;
                Log.e(TAG, "Please adjust your finger");
                tvMessage.setText("Your finger is too wet !!");
                */
                break;
            case JmtFP.TOO_SMALL:
                /*
                uiMessage = new Message();
                uiMessage.what = JmtFP.TOO_SMALL;
                tvMessage.setText("Pressed area is too small !!");
                */
                break;
            case JmtFP.ADJUST:
                Log.e(TAG, "Please adjust your finger");
                wait_finger = 0;
                uiMessage = new Message();
                uiMessage.what = MSG_ADJUST;
                uiUpdateHandler.sendMessage(uiMessage);
                break;
            case JmtFP.WAIT_FINGER:
            //    Log.e(TAG, "Wait Finger on.....");
                wait_finger++;
                uiMessage = new Message();
                uiMessage.what = MSG_WAIT_FINGER;
                uiMessage.arg1 = wait_finger;
                uiUpdateHandler.sendMessage(uiMessage);
                break;
            case JmtFP.CHANGE_FINGER:
                Log.e(TAG, "[105] Change Finger....");
                wait_finger = 0;
                uiMessage = new Message();
                uiMessage.what = MSG_CHANGE_FINGER;
                uiUpdateHandler.sendMessage(uiMessage);
                break;
            default:
                break;
        }
        //uiUpdateHandler.sendMessage(uiMessage);
        return 0;
    }

    public int onShowImage(byte[] pfpimage, int ifpwidth, int ifpheight) {
        int RawIdx;
        int RgbIdx;
        int Pixel;
        byte[] pRGBFinger;

        iFpImageWidth = ifpwidth;
        iFpImageHeight = ifpheight;

        pRGBFinger = mRGBFinger;
        RgbIdx = 0;

        for (RawIdx = 0; RawIdx < ifpwidth * ifpheight; RawIdx++) {
            Pixel = (pfpimage[RawIdx] & 0xFF);
            // 8 bits to RGB8888
            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) 0xFF;
        }

        if (uiUpdateHandler != null) {
            Message m = uiUpdateHandler.obtainMessage(MSG_NATIVE_CALLBACK, 0, 0);
            uiUpdateHandler.sendMessage(m);
        }

        return 0;
    }

    public int onShowData(byte[] pfpimage, int ifpwidth, int ifpheight)
    {
        return 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        currStreamId1 = soundPool.load(this, R.raw.success, 1);
        currStreamId2 = soundPool.load(this, R.raw.fail, 1);

        PackageManager m = getPackageManager();
        String app_path = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(app_path, 0);
            app_path = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error Package name not found");
        }

        Log.e(TAG, "Package name = " + app_path);

        mSensor = JmtFP.getInstance();
        mSensor.SetJmtSingleton(this, app_path, driver_path);

        //* Check finger's database */
        int i;
        int[] finger_db = null;

        finger_db = mSensor.CheckFingerDatabase();
        Log.e(TAG, "finger num = " + finger_db.length);
        if(finger_db.length > 0) {
            Log.i(TAG, "finger database info:");
            for(i = 0; i < finger_db.length; i++)
                Log.i(TAG, ""+ finger_db[i]);
        }
        else
            Log.e(TAG, "NO Finger's Data....");

        int Module_Result;
        int width[] = new int[1];
        int height[] = new int[1];
        int chipid[] = new int[1];

        int type = mSensor.GetSensorType();
        Log.e(TAG, "SensorType = " + Integer.toString(type));

        if(type == JmtFP._10X_)
            mSensor.SetEnrollCounts(6);
        else if(type == JmtFP._30X_)
            mSensor.SetEnrollCounts(6);
        else if(type == JmtFP._303_)
            mSensor.SetEnrollCounts(10);
        else if(type == JmtFP._305_)
            mSensor.SetEnrollCounts(6);
        else mSensor.SetEnrollCounts(10);

        mSensor.GetFingerAttr(type, width, height);

        iFpImageHeight  = height[0];
        iFpImageWidth   = width[0];
        iFpSize = iFpImageHeight * iFpImageWidth;

        Ver = mSensor.GetSDKVersion();
        user_cancel = false;

        mRGBFinger = new byte[iFpSize * 4];
        Log.e(TAG, "FINGER_SIZE = " + Integer.toString(iFpSize));

        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceView1);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mMatrix = new Matrix();
        mMatrix.setScale(6, 6);

        tvSDK = (TextView) findViewById(R.id.tvSDK);
        tvSDK.setText("SDK Version : " + Ver);


        Module_Result = mSensor.ReadChipID(chipid);
        iFpChipID = chipid[0];
        tvChipID = (TextView) findViewById(R.id.tv_chipid);
        if (Module_Result != JmtFP.ERR_FAILED) {
            tvChipID.setText("Chip ID : " + Integer.toHexString(iFpChipID));
        } else {
            tvChipID.setText("Chip ID : ERROR ID !!");
        }

        tvPercentage = (TextView) findViewById(R.id.tvPercentage);
        tvPercentage.setText("");

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        tvMessage.setText("");

        /*
        tvScroll = (TextView) findViewById(R.id.tvScroll);
        tvScroll.setText("");

        int i=0;
        for(i=0; i<1000; i++) {
            //tvScroll.setText("Testing: The number is !!!");
            tvScroll.setText("Testing: The number is"+tvScroll.getText().toString() + "\n" + i);
        }
*/
        ivImage1 = (ImageView) findViewById(R.id.ivImage1);
        ivImage1.setImageResource(R.drawable.jmt);

        svScroll = (ScrollView) findViewById(R.id.svScroll);
        hsvScroll = (HorizontalScrollView) findViewById(R.id.hsvScroll);

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleBtn);
        toggleButton.setOnCheckedChangeListener(toggleButtonTouch);

        onGetFinger = false;
        btnImage = (Button) findViewById(R.id.btn_image);
        btnImage.setOnClickListener(btnGetFingerOnClick);
        btnImage.setText("GetFinger");
        btnImage.setEnabled(true);

        onEnroll = false;
        btnEnroll = (Button) findViewById(R.id.btn_enroll);
        btnEnroll.setOnClickListener(btnEnrollFingerOnClick);
        btnEnroll.setText("EnrollFinger");
        btnEnroll.setEnabled(true);

        onVerify = false;
        btnVerify = (Button) findViewById(R.id.btn_verify);
        btnVerify.setOnClickListener(btnMatchFingerOnClick);
        btnVerify.setText("MatchFinger");
        btnVerify.setEnabled(true);

        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(btnUserCancelOnClick);
        btnCancel.setText("Cancel");
        btnCancel.setEnabled(true);

        btnErase = (Button) findViewById(R.id.btn_erase);
        btnErase.setOnClickListener(btnEraseRecordOnClick);
        btnErase.setText("EraseAll");
        btnErase.setEnabled(true);

        onStress = false;
        btnStress = (Button) findViewById(R.id.btn_stress);
        btnStress.setOnClickListener(btnGetFingerStressOnClick);
        btnStress.setText("RealTime");
        btnStress.setEnabled(true);

        //onNavigation = false;
        btnNavigation = (Button) findViewById(R.id.btn_navigation);
        btnNavigation.setOnClickListener(btnNavigationOnClick);
        btnNavigation.setText("Navigation");
        btnNavigation.setEnabled(true);

        //onWaitClick = false;
        btnWaitClick = (Button) findViewById(R.id.btn_waitclick);
        btnWaitClick.setOnClickListener(btnWaitClickOnClick);
        btnWaitClick.setText("WaitClick");
        btnWaitClick.setEnabled(true);

        btnExit = (Button) findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(btnExitOnClick);
        btnExit.setText("Exit");
        btnExit.setEnabled(true);

        bmpReady = false;
        mBitmap_Finger = new BitmapPool(BITMAP_POOL_SIZE);

        uiUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bitmap bitmap;

                switch (msg.what) {
                    case MSG_NO_SENSOR:
                        myToastShow(getApplicationContext(), "Error, No fingerprint reader !!", Toast.LENGTH_SHORT);
                        break;

                    case MSG_THREAD_TOGGLE_BTN:
                    /* clean up DrawThread */
                        user_cancel = true;
                        if(tid_draw != null){
                            if (tid_draw.isAlive()) {
                                tid_draw.interrupt();
                            }

                            try {
                                tid_draw.join(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        btnImage.setEnabled (true);
                        btnEnroll.setEnabled (true);
                        btnVerify.setEnabled (true);
                        btnErase.setEnabled (true);
                        btnStress.setEnabled (true);
                        btnExit.setEnabled (true);
                        btnNavigation.setEnabled(true);
                        btnWaitClick.setEnabled(true);
                        //btnWaitTouch.setEnabled(true);
                        user_cancel = false;
                        break;

                    case MSG_NATIVE_CALLBACK:
                    /* prepare picture data */
                        Log.e(TAG, ">>>>>>>>>>>>> MSG_NATIVE_CALLBACK..." + Integer.toString(msg.arg1));
                        if (iFpImageWidth > 0 && iFpImageHeight > 0 ) {
                            //Log.e(TAG, ">>>>> MSG >>>>>>>>>> width >0, height>0");
                            Log.e(TAG, "Width : " + iFpImageWidth);
                            Log.e(TAG, "Height : " + iFpImageHeight);
                            bitmap = mBitmap_Finger.getfirstBitmap();
                            if (bitmap == null) {
                                bitmap = Bitmap.createBitmap(iFpImageWidth, iFpImageHeight, Bitmap.Config.ARGB_8888);
                            }

                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mRGBFinger));
                            mBitmap_Finger.putlastBitmap(bitmap);
                            bmpReady = true;
                        }
                        //tvMessage.setText ("Sensor advice " + msg.arg1);
                        break;

                    case MSG_THREAD_GET_FIG:
                        tvMessage.setText("Get fingerprint image OK !!");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        soundPool.play(currStreamId1, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    case MSG_THREAD_ENROLL_FIG:
                        tvPercentage.setText("Enroll percent : " + msg.arg1 + " %");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        soundPool.play(currStreamId1, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    case MSG_THREAD_ENROLL_OK:
                        tvPercentage.setText("");
                        tvMessage.setText("Fingerprint enroll successfully !!");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        soundPool.play(currStreamId1, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    case MSG_THREAD_VERIFY_FIG:
                        tvMessage.setText("Fingerprint verify OK !!");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        soundPool.play(currStreamId1, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    case MSG_THREAD_VERIFY_Fail:
                        tvMessage.setText("Fingerprint verify fail, try again !!");
                        mVibrator.vibrate(new long[]{100, 300, 100, 300}, -1);
                        soundPool.play(currStreamId2, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    case MSG_THREAD_OUTOFMEMORY:
                        myToastShow(getApplicationContext(), "Error, Out of memory !!", Toast.LENGTH_SHORT);
                        break;

                    case MSG_THREAD_ERASE_FULL:
                        myToastShow(getApplicationContext(), "Attention, Fingerprint record is full !!", Toast.LENGTH_SHORT);
                        break;

                    case MSG_POOR_IMG:
                        //myToastShow(getApplicationContext(), " Please adjust position of your finger !!", Toast.LENGTH_SHORT);
                        //vibrator.vibrate(new long[]{100, 300, 100, 300}, -1);
                        //soundPool.play(currStreamId2, 1.0F, 1.0F, 0, 0, 1.0f);
                        break;

                    case MSG_NOTIFY_LEAVE_FINGER:
                        tvMessage.setText("Please leave your finger !!");
                        break;

                    case MSG_ADJUST:
                        tvMessage.setText("Get finger failed. Please adjust your finger !! ");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        break;

                    case MSG_WAIT_FINGER:
                        if (onGetFinger) {
                            tvMessage.setText("Press finger on sensor to get fingerprint !!");
                        } else if (onEnroll) {
                            tvMessage.setText("Please successive pressing \nfingers up to 100% !!");
                        } else if (onVerify && msg.arg1 == 5) {
                            tvMessage.setText("Place finger on sensor to match finger !!");
                        }
                        break;
                    case MSG_CHANGE_FINGER:
                        tvMessage.setText("Match fail, please adjust your finger !!");
                        mVibrator.vibrate(new long[]{100, 300}, -1);
                        break;

                    case JmtFP.TOO_SMALL:
                    case JmtFP.TOO_WET:
                        tvMessage.setText("Please adjust your finger !!");
                        mVibrator.vibrate(new long[]{100, 300, 100, 300}, -1);
                        soundPool.play(currStreamId2, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;

                    /*
                    case MSG_THREAD_GET_FIG_FAIL:
                        tvMessage.setText("Get Finger failed !!");
                        mVibrator.vibrate(new long[]{100, 300, 100, 300}, -1);
                        soundPool.play(currStreamId2, 1.0F, 1.0F, 0, 0, 1.0F);
                        break;
                    */
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected synchronized void onPause() {
        user_cancel = true;
        if (mSensor != null) {
            mSensor.CancelAction();
        }
        super.onPause();
    }

    @Override
    protected synchronized void onResume() {
        user_cancel = false;

        if (onStress) {

            btnImage.setEnabled (false);
            btnEnroll.setEnabled (false);
            btnVerify.setEnabled (false);
            btnErase.setEnabled (false);
            btnStress.setEnabled (false);
            btnExit.setEnabled (false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            bmpReady = false;
            tid_draw = new Thread(new DrawThread());
            tid_draw.setPriority (Thread.MAX_PRIORITY);
            tid_draw.start();

            tid_stress = new Thread(new GetFingerStressTask());
            tid_stress.setPriority (Thread.MAX_PRIORITY);
            tid_stress.start();

            //myToastShow(getApplicationContext(), " Please press finger !!", Toast.LENGTH_SHORT);
        }
        if(onGetFinger){
            btnImage.setEnabled (false);
            btnEnroll.setEnabled (false);
            btnVerify.setEnabled(false);
            btnErase.setEnabled (false);
            btnStress.setEnabled (false);
            btnExit.setEnabled (false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            bmpReady = false;
            tid_draw = new Thread(new DrawThread());
            tid_draw.setPriority(Thread.MAX_PRIORITY);
            tid_draw.start();

            tid_image = new HandlerThread("GetFinger");
            tid_image.setPriority (Thread.MAX_PRIORITY);
            tid_image.start();
            hid_image = new Handler(tid_image.getLooper());
            rid_image = new GetFingerTask();
            hid_image.post(rid_image);
        }
        super.onResume();
    }

    private ToggleButton.OnCheckedChangeListener toggleButtonTouch = new ToggleButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            Intent intent = new Intent(MainActivity.this, TouchService.class);
            if (isChecked) {
                startService(intent);
            } else {
                stopService(intent);
            }
        }
    };

    private Button.OnClickListener btnGetFingerOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);

            onGetFinger = true;
            user_cancel = false;

            tvPercentage.setText("");

            btnImage.setEnabled(false);
            btnEnroll.setEnabled(false);
            btnVerify.setEnabled(false);
            btnErase.setEnabled (false);
            btnStress.setEnabled (false);
            btnExit.setEnabled(false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            bmpReady = false;
            tid_draw = new Thread(new DrawThread());
            tid_draw.setPriority(Thread.MAX_PRIORITY);
            tid_draw.start();

            tid_image = new HandlerThread("GetFinger");
            tid_image.setPriority (Thread.MAX_PRIORITY);
            tid_image.start();
            hid_image = new Handler(tid_image.getLooper());
            rid_image = new GetFingerTask();
            hid_image.post(rid_image);
        }
    };

    private class GetFingerTask implements Runnable {
        @Override
        public void run() {

            boolean keep_going = true;
            int Module_Result;
            Message uiMessage;

            do {
                if (Thread.interrupted()) {
                    Log.i (TAG, "GetFingerTask interrupted!");
                    keep_going = false;
                    break;
                }

                if (user_cancel) {
                    Log.i (TAG, "GetFingerTask user cancel!");
                    keep_going = false;
                    //onGetFinger = false;
                    break;
                }

                Module_Result = mSensor.GetOneFinger();

                Log.e(TAG, "Call GetFinger Module_Result =" + Integer.toString( Module_Result));

                switch (Module_Result) {
                    case JmtFP.ERR_OK:
                        Log.e (TAG, "Get FP Image OK");

                        uiMessage = new Message();
                        uiMessage.what = MSG_THREAD_GET_FIG;
                        uiUpdateHandler.sendMessage(uiMessage);
                        keep_going = false;
                        onGetFinger = false;
                        break;

                    default:
                        Log.e (TAG, "Something wrong here");
                        uiMessage = new Message();
                        uiMessage.what = MSG_THREAD_GET_FIG_FAIL;
                        uiUpdateHandler.sendMessage(uiMessage);
                        keep_going = false;
                        break;
                }

            } while (false);

            if (keep_going) {
                hid_image.postDelayed (rid_image, 2);
            } else {
                uiMessage = new Message();
                uiMessage.what = MSG_THREAD_TOGGLE_BTN;
                uiUpdateHandler.sendMessage(uiMessage);
            }

            //onGetFinger = false;
        }
    }

    private Button.OnClickListener btnEnrollFingerOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);

            onEnroll = true;
            user_cancel = false;

            btnImage.setEnabled(false);
            btnEnroll.setEnabled(false);
            btnVerify.setEnabled(false);
            btnErase.setEnabled(false);
            btnStress.setEnabled(false);
            btnExit.setEnabled(false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            bmpReady = false;
            tid_draw = new Thread(new DrawThread());
            tid_draw.setPriority(Thread.MAX_PRIORITY);
            tid_draw.start();

            tid_enroll = new Thread(new EnrollPieceThread());
            tid_enroll.setPriority(Thread.MAX_PRIORITY);
            tid_enroll.start();
        }
    };

    private class EnrollPieceThread implements Runnable{
        @Override
        public void run() {

            int Module_Result;
            boolean keep_going = true;
            Message uiMessage;

            while (keep_going) {

                if (Thread.interrupted()) {
                    Log.e(TAG, "EnrollPieceThread interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i(TAG, "User cancel");
                    break;
                }

                Module_Result = mSensor.Enrollment();
                if (Module_Result == JmtFP.ERR_OK) {
                    uiMessage = new Message();
                    uiMessage.what = MSG_THREAD_ENROLL_OK;
                    uiUpdateHandler.sendMessage(uiMessage);

                    keep_going = false;
                } else {
                    Log.e(TAG, "Enroll Failed");

                    uiMessage = new Message();
                    uiMessage.what = MSG_POOR_IMG;
                    uiUpdateHandler.sendMessage(uiMessage);
                }
            }

            onEnroll = false;
            uiMessage = new Message();
            uiMessage.what = MSG_THREAD_TOGGLE_BTN;
            uiUpdateHandler.sendMessage(uiMessage);
	    }
    }

    private Button.OnClickListener btnMatchFingerOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            tvPercentage.setText("");

            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);

            if (database.list().length == 0) {
                tvMessage.setText("Not fingerprint, please enroll finger !!");
            } else {

                onVerify = true;
                user_cancel = false;

                btnImage.setEnabled(false);
                btnEnroll.setEnabled(false);
                btnVerify.setEnabled(false);
                btnErase.setEnabled(false);
                btnStress.setEnabled(false);
                btnExit.setEnabled(false);
                btnNavigation.setEnabled(false);
                btnWaitClick.setEnabled(false);

                bmpReady = false;
                tid_draw = new Thread(new DrawThread());
                tid_draw.setPriority(Thread.MAX_PRIORITY);
                tid_draw.start();

                tid_match = new Thread(new MatchFingerThread());
                tid_match.setPriority(Thread.MAX_PRIORITY);
                tid_match.start();
            }
        }
    };

    private class MatchFingerThread implements Runnable {
        @Override
        public void run() {

            boolean keep_going = true;
            int Module_Result;
            Message uiMessage;

            while (keep_going) {
                if (Thread.interrupted()) {
                    Log.i(TAG, "MatchFingerThread interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i(TAG, "MatchFingerThread user cancel!");
                    break;
                }

                Module_Result = mSensor.Verify();

                if(Module_Result > 0) {
                    Log.e(TAG, "FP Verify Successfully" + "finger ID is " + Module_Result);
                    uiMessage = new Message();
                    uiMessage.what = MSG_THREAD_VERIFY_FIG;
                    uiUpdateHandler.sendMessage(uiMessage);
                    keep_going = false;
                    break;
                }

                    switch (Module_Result) {
                        /*
                        case JmtFP.ERR_OK:
                            Log.e(TAG, "FP Verify Successfully");
                            uiMessage = new Message();
                            uiMessage.what = MSG_THREAD_VERIFY_FIG;
                            uiUpdateHandler.sendMessage(uiMessage);
                            keep_going = false;
                            break;
*/
                        case JmtFP.ERR_TIMEOUT:
                        /* notify user to swipe finger */
                            keep_going = false;
                            break;

                        case JmtFP.ERR_NOMATCH:
                            Log.e(TAG, "FP Verify Fail, Try again");
                            uiMessage = new Message();
                            uiMessage.what = MSG_THREAD_VERIFY_Fail;
                            uiUpdateHandler.sendMessage(uiMessage);

                            //keep_going = false;
                            break;

                        default:
                            Log.i (TAG, "Something wrong here");
                            keep_going = false;
                            break;
                    }
            }
            onVerify = false;
            uiMessage = new Message();
            uiMessage.what = MSG_THREAD_TOGGLE_BTN;
            uiUpdateHandler.sendMessage(uiMessage);
        }
    }

    private Button.OnClickListener btnUserCancelOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);
            user_cancel = true;
            onStress = false;
            onGetFinger = false;
            mSensor.CancelAction();
            tvPercentage.setText("");
            tvMessage.setText("Cancel action !!");
        }
    };

    private Button.OnClickListener btnNavigationOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            btnImage.setEnabled (false);
            btnEnroll.setEnabled(false);
            btnVerify.setEnabled (false);
            btnErase.setEnabled(false);
            btnStress.setEnabled (false);
            btnExit.setEnabled (false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            //onNavigation = true;
            user_cancel = false;

            svScroll.smoothScrollTo(0, 200);
            hsvScroll.smoothScrollTo(400, 0);

            tid_navigation = new Thread(new NavigationTask());
            tid_navigation.setPriority (Thread.MAX_PRIORITY);
            tid_navigation.start();
        }
    };

    private class NavigationTask implements Runnable {
        @Override
        public void run() {

            boolean keep_going = true;
            int Module_Result;
            Message uiMessage;
            Log.i(TAG, "NavigationTask START *****************************!");

            do {
                if (Thread.interrupted()) {
                    Log.i (TAG, "NavigationTask interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i (TAG, "NavigationTask user cancel!");
                    break;
                }

                Module_Result = mSensor.StartNavigation();

                switch (Module_Result) {
                    case JmtFP.ERR_OK:
                        Log.i (TAG, "NavigationTask OK");
                        keep_going = false;
                        break;

                    case JmtFP.ERR_ABORT:
                        Log.i (TAG, "NavigationTask User Cancel");
                        keep_going = false;
                        break;

                    default:
                        Log.i (TAG, "Something wrong here");
                        keep_going = false;
                        break;
                }
            } while (false);

            uiMessage = new Message();
            uiMessage.what = MSG_THREAD_TOGGLE_BTN;
            uiUpdateHandler.sendMessage(uiMessage);
        }
    }

    private Button.OnClickListener btnEraseRecordOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSensor.EraseFull(); // or you can decide finger_id
            tvMessage.setText("Erase all save successfully !!");
            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    };

    private Button.OnClickListener btnGetFingerStressOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);
            mHolder.unlockCanvasAndPost(mCanvas);

            tvPercentage.setText("");

            btnImage.setEnabled (false);
            btnEnroll.setEnabled(false);
            btnVerify.setEnabled (false);
            btnErase.setEnabled (false);
            btnStress.setEnabled (false);
            btnExit.setEnabled (false);
            btnNavigation.setEnabled(false);
            btnWaitClick.setEnabled(false);

            onStress = true;
            user_cancel = false;

            tvMessage.setText("Place finger on sensor !!");

            bmpReady = false;
            tid_draw = new Thread(new DrawThread());
            tid_draw.setPriority (Thread.MAX_PRIORITY);
            tid_draw.start();

            tid_stress = new Thread(new GetFingerStressTask());
            tid_stress.setPriority (Thread.MAX_PRIORITY);
            tid_stress.start();
        }
    };

    private class GetFingerStressTask implements Runnable {
        @Override
        public void run() {

            boolean keep_going = true;
            int Module_Result;
            Message uiMessage;
            int count =0;
            Log.i (TAG, "GetFingerStressTask START *****************************!");

            while (keep_going) {
                if (Thread.interrupted()) {
                    Log.i (TAG, "GetFingerStressTask interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i (TAG, "GetFingerStressTask user cancel!");
                    break;
                }

                count++;
                Module_Result = mSensor.GetFingerLoop();

                switch (Module_Result) {
                    case JmtFP.ERR_OK:
                        //Log.i (TAG, "Get FP Image OK");
                        break;

                    case JmtFP.ERR_ABORT:
                        Log.i (TAG, "GetFingerStressTask User Cancel");
                        keep_going = false;
                        break;

                    default:
                        Log.i (TAG, "Something wrong here");
                        keep_going = false;
                        break;
                }
            }
            uiMessage = new Message();
            uiMessage.what = MSG_THREAD_TOGGLE_BTN;
            uiUpdateHandler.sendMessage(uiMessage);
        }
    }


    private Button.OnClickListener btnWaitClickOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "WaitClickTask START *****************************!");

            tvPercentage.setText("");

            btnImage.setEnabled (false);
            btnEnroll.setEnabled(false);
            btnVerify.setEnabled(false);
            btnErase.setEnabled(false);
            btnStress.setEnabled (false);
            btnExit.setEnabled (false);
            btnWaitClick.setEnabled(false);
            btnNavigation.setEnabled(false);

            // onWaitClick = true;
            user_cancel = false;


            tid_waitclick = new Thread(new WaitClickTask());
            tid_waitclick.setPriority (Thread.MAX_PRIORITY);
            tid_waitclick.start();
        }
    };

    private class WaitClickTask implements Runnable {
        @Override
        public void run() {

            boolean keep_going = true;
            int Module_Result;
            Message uiMessage;
            int count =0;
            Log.i (TAG, "WaitClickTask Thread START *****************************!");

            while (keep_going) {
                if (Thread.interrupted()) {
                    Log.i (TAG, "WaitClickTask interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i (TAG, "WaitClickTask user cancel!");
                    break;
                }

                count++;
                Module_Result = mSensor.WaitClick();

                switch (Module_Result) {
                    case JmtFP.ERR_OK:
                        //Log.i (TAG, "Get FP Image OK");
                        break;

                    case JmtFP.ERR_ABORT:
                        Log.i (TAG, "WaitClickTask User Cancel");
                        keep_going = false;
                        break;

                    default:
                        Log.i (TAG, "Something wrong here");
                        keep_going = false;
                        break;
                }
            }
            uiMessage = new Message();
            uiMessage.what = MSG_THREAD_TOGGLE_BTN;
            uiUpdateHandler.sendMessage(uiMessage);
        }
    }

    private Button.OnClickListener btnExitOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            user_cancel = true;
            System.exit(0);
         }
    };

    private class DrawThread implements Runnable {
        @Override
        public void run() {
            Bitmap bitmap;
         
            while (true) {
                if (Thread.interrupted()) {
                    Log.i (TAG, "DrawThread interrupted!");
                    break;
                }

                if (user_cancel) {
                    Log.i (TAG, "DrawThread user cancel!");
                    break;
                }

                if (bmpReady) {
                    Log.i (TAG, "DrawThread one finger!");
            	    Rect mRect_Frame = new Rect(5, 10, iFpImageWidth * 3, iFpImageHeight * 3);

                    mCanvas = mHolder.lockCanvas(mRect_Frame);
                    bitmap = mBitmap_Finger.getlastBitmap();

                    mCanvas.drawColor(Color.WHITE);
                    //mCanvas.drawBitmap(bitmap, 64, 64, mPaint);
                    mCanvas.drawBitmap(bitmap, new Rect(0, 0, iFpImageWidth, iFpImageHeight - 1), mRect_Frame, mPaint);
                    mBitmap_Finger.putfirstBitmap(bitmap);

                    mHolder.unlockCanvasAndPost(mCanvas);

                    bmpReady = false;
                }
            }
        }
    }

    private void myToastShow(Context context, String text, int duration) {
        if (mToast != null) {
            mToast.cancel();
            mToast = Toast.makeText(context, text, duration);
        } else {
            mToast = Toast.makeText(context, text, duration);
         }

        LinearLayout toastLayout = new LinearLayout(context);
        toastLayout.setOrientation(LinearLayout.HORIZONTAL);
        toastLayout.setGravity(Gravity.CENTER_VERTICAL);
        toastLayout.setBackgroundResource(R.drawable.shape);

        ImageView toastImage = new ImageView(context);
        toastImage.setImageResource(R.drawable.images);
        toastLayout.addView(toastImage, 40, 40);

        TextView toastText = new TextView(context);
        toastText.setText(text);
        toastText.setTextSize(20);
        toastText.setTextColor(Color.BLACK);
        toastText.setGravity(Gravity.CENTER);
        toastLayout.addView(toastText);

        mToast.setView(toastLayout);
        mToast.show();
    }

    public void onBackPressed() {
        user_cancel = true;
        System.exit(0);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        user_cancel = true;

        if (mSensor != null) {
            mSensor.CancelAction();
        }

        if (tid_draw.isAlive()) {
            tid_draw.interrupt();
        }

        if (tid_image.isAlive()) {
            tid_image.interrupt();
        }

        if (tid_enroll.isAlive()) {
            tid_enroll.interrupt();
        }

        if (tid_match.isAlive()) {
            tid_match.interrupt();
        }

        if (tid_stress.isAlive()) {
            tid_stress.interrupt();
        }

        if (tid_waitclick.isAlive()){
            tid_waitclick.interrupt();
        }

        if (tid_navigation.isAlive()){
            tid_navigation.interrupt();
        }

        super.onDestroy();
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.WHITE);
        mHolder.unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawColor(Color.WHITE);
        mHolder.unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {}
}
