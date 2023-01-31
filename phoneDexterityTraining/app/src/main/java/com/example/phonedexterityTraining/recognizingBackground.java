package com.example.phonedexterityTraining;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.commons.math3.complex.Quaternion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import processing.data.FloatList;

import static android.content.Context.SENSOR_SERVICE;

public class recognizingBackground extends Thread{
    private static final String TAG = "sensorBackground";

    boolean continuousLog = false;
    Handler uiHandler;
    private static final String MSG_KEY = "result";
    private static final String LOG_KEY = "log";
    private static final String SCORE_KEY = "score";
    private static final String SPEED_KEY = "speed";

    protected long frameRatePeriod = 1000000000L / 50L;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Sensors
    static float[] accel = new float[3];
    static int accelAccuracy = 0;
    static float[] rotation = new float[5];
    static int rotationAccuracy = 0;

    FloatList quaternionDiffX = new FloatList();
    FloatList quaternionDiffY = new FloatList();
    FloatList quaternionDiffZ = new FloatList();

    FloatList accelX = new FloatList();
    FloatList accelY = new FloatList();
    FloatList accelZ = new FloatList();

    // recog
    boolean recognizing = false;
    int recogGestureResult = -1;
    int verifyGesture = -1;
    int gestureVerifyTime = 0;
    int recognizingGap = 0;
    float recogGestureScore = -1;
    float verifyGestureScore = -1;
    float recogGestureSpeed = -1;
    float verifyGestureSpeed = -1;

    long startTime;
    int logGap = 0;

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // If the sensor data is unreliable return
            /*if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                return;*/
            //Log.d(TAG, "type: " + sensorEvent.sensor.getType());
            // Gets the value of the sensor that has been changed
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accel = sensorEvent.values.clone();
                    accelAccuracy = sensorEvent.accuracy;
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    rotation = sensorEvent.values.clone();
                    rotationAccuracy = sensorEvent.accuracy;
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private Quaternion quaternion_diff(Quaternion a, Quaternion b)
    {
        Quaternion inv = a.getInverse();
        return inv.multiply(b);
    }

    public recognizingBackground(Context context, Handler resultHandler, boolean continuousLogging) {
        super("Sensor Thread");

        continuousLog = continuousLogging;
        uiHandler = resultHandler;
        startTime = System.currentTimeMillis();

        SensorManager sm = (SensorManager) context.getSystemService(SENSOR_SERVICE);

        for (android.hardware.Sensor sensorItem : sm.getSensorList(android.hardware.Sensor.TYPE_ALL)) {
            //Log.d(TAG, String.valueOf(sensorItem.getType()));
            switch(sensorItem.getType()) {
                case android.hardware.Sensor.TYPE_ACCELEROMETER:
                    sm.registerListener(mSensorEventListener, sensorItem, SensorManager.SENSOR_DELAY_GAME);
                    break;
                case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                    sm.registerListener(mSensorEventListener, sensorItem, SensorManager.SENSOR_DELAY_GAME);
                    break;
                default:
                    break;
            }
        }
    }

    public void stopThread() {
        running.set(false);
    }

    @Override
    public void run() {  // not good to make this synchronized, locks things up
        long beforeTime = System.nanoTime();
        long overSleepTime = 0L;

        int noDelays = 0;
        // Number of frames with a delay of 0 ms before the
        // animation thread yields to other running threads.
        final int NO_DELAYS_PER_YIELD = 15;

        final int gestureMaxWindow = 200;
        final float breakThreshold = (float)0.005;
        Quaternion prevPosition = Quaternion.IDENTITY;
        Quaternion prevLogPosition = Quaternion.IDENTITY;

        running.set(true);
        while (running.get()) {
            if (Thread.currentThread().isInterrupted()) {
                Log.d(TAG, "interrupt");
                return;
            }
            //Log.d(TAG, String.valueOf(System.currentTimeMillis()));

            // The sensor thread work
            Quaternion position = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
            //Log.d(TAG, Arrays.toString(rotation));
            if(position.equals(Quaternion.ZERO)) {
                continue;
            }

            Quaternion rotationDiff = quaternion_diff(prevPosition, position);
            if(quaternionDiffX.size() == gestureMaxWindow){
                quaternionDiffX.remove(0);
                quaternionDiffY.remove(0);
                quaternionDiffZ.remove(0);
                accelX.remove(0);
                accelY.remove(0);
                accelZ.remove(0);
            }
            quaternionDiffX.append((float)rotationDiff.getQ1());
            quaternionDiffY.append((float)rotationDiff.getQ2());
            quaternionDiffZ.append((float)rotationDiff.getQ3());
            accelX.append(accel[0]);
            accelY.append(accel[1]);
            accelZ.append(accel[2]);

            if(continuousLog) {
                if(logGap == 5) {
                    Quaternion rotationLogDiff = quaternion_diff(prevLogPosition, position);
                    long time = System.currentTimeMillis() - startTime;
                    float[] diffArray = {(float) rotationLogDiff.getQ1(), (float) rotationLogDiff.getQ2(), (float) rotationLogDiff.getQ3(), (float) time / 1000};
                    Message log_msg = uiHandler.obtainMessage();
                    Bundle log_bundle = new Bundle();
                    log_bundle.putFloatArray(LOG_KEY, diffArray);
                    log_msg.setData(log_bundle);
                    uiHandler.sendMessage(log_msg);
                    logGap = 0;
                    prevLogPosition = position;
                }
                logGap++;
            }

            if(!recognizing && recognizingGap >= 10){
                // do recognition
                //print("recognition");
                recognizing = true;
                recognizingGap = 0;
                final FloatList inputX = quaternionDiffX.copy();
                final FloatList inputY = quaternionDiffY.copy();
                final FloatList inputZ = quaternionDiffZ.copy();
                new Thread(new Runnable() {
                    public void run() {
                        recognize(inputX, inputY, inputZ);
                    }
                }).start();
            }

            recognizingGap++;

            if(recogGestureResult != -1){
                // check the accelerometer data
                float maxAccelX = accelX.max();
                float maxAccelY = accelY.max();
                float maxAccelZ = accelZ.max();
                float minAccelX = accelX.min();
                float minAccelY = accelY.min();
                float minAccelZ = accelZ.min();

                // no zero crossing
                if(maxAccelX * minAccelX > 0 && maxAccelY * minAccelY > 0 && maxAccelZ * minAccelZ > 0) {
                    recogGestureResult = 0;
                }
                // no axis crossing
                if( (minAccelX >= maxAccelY && minAccelY >= maxAccelZ )
                        || (minAccelX >= maxAccelZ && minAccelZ >= maxAccelY )
                        || (minAccelY >= maxAccelX && minAccelX >= maxAccelZ )
                        || (minAccelY >= maxAccelZ && minAccelZ >= maxAccelX )
                        || (minAccelZ >= maxAccelY && minAccelY >= maxAccelX )
                        || (minAccelZ >= maxAccelX && minAccelX >= maxAccelY )) {
                    recogGestureResult = 0;
                }
                // no large movement
                float movementThreshold = 3;
                if( (Math.abs(maxAccelX - minAccelX) < movementThreshold)
                    && (Math.abs(maxAccelY - minAccelY) < movementThreshold)
                    && (Math.abs(maxAccelZ - minAccelZ) < movementThreshold)) {
                    recogGestureResult = 0;
                }
                if(recogGestureResult != 0) {
                    verifyGesture = recogGestureResult;
                    verifyGestureScore = recogGestureScore;
                    verifyGestureSpeed = recogGestureSpeed;
                }
                recogGestureResult = -1;
                recogGestureScore = -1;
                recogGestureSpeed = -1;
                recognizing = false;
            }

            // check break
            if(continuousLog){
                if(verifyGesture != -1 ) {
                    Message msg = uiHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(MSG_KEY, String.valueOf(verifyGesture));
                    bundle.putString(SCORE_KEY, String.valueOf(verifyGestureScore));
                    bundle.putString(SPEED_KEY, String.valueOf(verifyGestureSpeed));
                    msg.setData(bundle);
                    uiHandler.sendMessage(msg);

                    quaternionDiffX.clear();
                    quaternionDiffY.clear();
                    quaternionDiffZ.clear();
                    verifyGesture = -1;
                    verifyGestureScore = -1;
                    verifyGestureSpeed = -1;
                }
            }
            else {
                if (Math.abs((float) rotationDiff.getQ1()) < breakThreshold && Math.abs((float) rotationDiff.getQ2()) < breakThreshold && Math.abs((float) rotationDiff.getQ3()) < breakThreshold) {
                    gestureVerifyTime++;
                } else {
                    gestureVerifyTime = 0;
                    verifyGesture = -1;
                }

                if (verifyGesture != -1 && verifyGestureScore != -1 && (( verifyGesture % 2 == 1 && gestureVerifyTime == 10) || (verifyGesture % 2 == 0) )) {
                    Log.i(TAG, "False Positive: " + String.valueOf(verifyGesture));
                    Message msg = uiHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(MSG_KEY, String.valueOf(verifyGesture));
                    bundle.putString(SCORE_KEY, String.valueOf(verifyGestureScore));
                    bundle.putString(SPEED_KEY, String.valueOf(verifyGestureSpeed));
                    msg.setData(bundle);
                    uiHandler.sendMessage(msg);

                    quaternionDiffX.clear();
                    quaternionDiffY.clear();
                    quaternionDiffZ.clear();
                    verifyGesture = -1;
                    verifyGestureScore = -1;
                    verifyGestureSpeed = -1;
                    gestureVerifyTime = 0;
                }
            }

            prevPosition = position;

            long afterTime = System.nanoTime();
            long timeDiff = afterTime - beforeTime;
            //System.out.println("time diff is " + timeDiff);
            long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;

            if (sleepTime > 0) {  // some time left in this cycle
                try {
                    Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
                    noDelays = 0;  // Got some sleep, not delaying anymore
                } catch (InterruptedException ex) { }

                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;

            } else {    // sleepTime <= 0; the frame took longer than the period
                overSleepTime = 0L;
                noDelays++;

                if (noDelays > NO_DELAYS_PER_YIELD) {
                    Thread.yield();   // give another thread a chance to run
                    noDelays = 0;
                }
            }

            beforeTime = System.nanoTime();
        }
    }

    private void recognize(final FloatList rotationX, final FloatList rotationY, final FloatList rotationZ) {
        final float halfThreshold = (float)1.3;
        float fullSpinThreshold = (float)1.8;
        float fullRotateThreshold = (float)1.8;
        float fullFlipThreshold = (float)1.8;
        final float noneThreshold = (float)1.0;
        final float breakThreshold = (float)0.005;
        final int gestureMaxWindow = 200;
        final int gestureHalfWindow = 125;

        // check dexterity gesture
        float accumulateX = 0;
        float accumulateY = 0;
        float accumulateZ = 0;
        int breakTime = 0;

        FloatList diffX = new FloatList();
        FloatList diffY = new FloatList();
        FloatList diffZ = new FloatList();
        int main_axis = -1;

        recogGestureResult = 0;
        recogGestureScore = -1;
        recogGestureSpeed = -1;

        int oldestIdx = rotationX.size()-1;

        if(continuousLog){
            fullSpinThreshold = (float)2.8;
            fullRotateThreshold = (float)2.8;
            fullFlipThreshold = (float)2.8;
        }

        for(int i=rotationX.size()-1; i>=0; i--) {
            accumulateX += rotationX.get(i);
            accumulateY += rotationY.get(i);
            accumulateZ += rotationZ.get(i);
            diffX.append(rotationX.get(i));
            diffY.append(rotationY.get(i));
            diffZ.append(rotationZ.get(i));
            oldestIdx = i;
            if(!continuousLog) {
                // in relaxing app, no need to check half gesture
                if (rotationX.size() - i <= gestureHalfWindow) {
                    // check half gesture
                    if (accumulateX > halfThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                        // Flip/ Adduction/ semi
                        recogGestureResult = 13;
                    } else if (accumulateX < -halfThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                        // Flip/ Abduction/ semi
                        recogGestureResult = 11;
                    } else if (accumulateY > halfThreshold && Math.abs(accumulateX) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                        // Rotate/ Adduction/ semi
                        recogGestureResult = 9;
                    } else if (accumulateY < -halfThreshold && Math.abs(accumulateX) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                        // Rotate/ Abduction/ semi
                        recogGestureResult = 7;
                    } else if (accumulateZ > halfThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateX) < noneThreshold) {
                        // Rotate/ Adduction/ semi
                        recogGestureResult = 5;
                    } else if (accumulateZ < -halfThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateX) < noneThreshold) {
                        // Rotate/ Abduction/ semi
                        recogGestureResult = 3;
                    }
                }
            }
            // check full gesture
            if (accumulateX > fullFlipThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                // Flip/ Adduction/ Full
                recogGestureResult = 14;
                main_axis = 0;
            } else if (accumulateX < -fullFlipThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                // Flip/ Abduction/ Full
                recogGestureResult = 12;
                main_axis = 0;
            } else if (accumulateY > fullRotateThreshold && Math.abs(accumulateX) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                // Rotate/ Adduction/ Full
                recogGestureResult = 10;
                main_axis = 1;
            } else if (accumulateY < -fullRotateThreshold && Math.abs(accumulateX) < noneThreshold && Math.abs(accumulateZ) < noneThreshold) {
                // Rotate/ Abduction/ Full
                recogGestureResult = 8;
                main_axis = 1;
            } else if (accumulateZ > fullSpinThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateX) < noneThreshold) {
                // Spin/ Adduction/ Full
                recogGestureResult = 6;
                main_axis = 2;
            } else if (accumulateZ < -fullSpinThreshold && Math.abs(accumulateY) < noneThreshold && Math.abs(accumulateX) < noneThreshold) {
                // Spin/ Abduction/ Full
                recogGestureResult = 4;
                main_axis = 2;
            }

            if(!continuousLog) {
                // check break
                if (Math.abs(rotationX.get(i)) < breakThreshold && Math.abs(rotationY.get(i)) < breakThreshold && Math.abs(rotationZ.get(i)) < breakThreshold) {
                    breakTime++;
                } else {
                    breakTime = 0;
                }
                if (recogGestureResult != 0 && breakTime == 10) {
                    // detect break during the gesture, no need to finish calculate
                    //Log.d(TAG, "break");
                    break;
                }
            }
            else{
                if (recogGestureResult != 0) {
                    break;
                }
            }
        }
        //Log.d(TAG, String.valueOf(recogGestureResult));

        if(recogGestureResult != 0) {
            // calculate gesture score
            if(main_axis == 0){
                FloatList array = new FloatList();
                array.append(diffY);
                array.append(diffZ);
                recogGestureScore = calculateScore(diffX.array(), array.array());
            }
            else if(main_axis == 1){
                FloatList array = new FloatList();
                array.append(diffX);
                array.append(diffZ);
                recogGestureScore = calculateScore(diffY.array(), array.array());
            }
            else if(main_axis == 2){
                FloatList array = new FloatList();
                array.append(diffX);
                array.append(diffY);
                recogGestureScore = calculateScore(diffZ.array(), array.array());
            }

            // calculate gesture speed (framerate 50)
            recogGestureSpeed = (rotationX.size() - oldestIdx - 1) * 0.02f;
        }
    }

    private float calculateScore(float[] diffArray, float[] diffArraySec){
        Arrays.sort(diffArray);
        float median;
        if (diffArray.length % 2 == 0)
            median = ((float)diffArray[diffArray.length/2] + (float)diffArray[diffArray.length/2 - 1])/2;
        else
            median = (float) diffArray[diffArray.length/2];

        float totalDiff = 0;
        for(int i=0;i<diffArray.length;i++){
            totalDiff += Math.abs(median - diffArray[i]);
        }
        float averageDiff = totalDiff / diffArray.length;

        float totalDiffSec = 0;
        for(int i=0;i<diffArraySec.length;i++){
            totalDiffSec += Math.abs(diffArraySec[i]);
        }
        float averageDiffSec = totalDiffSec / diffArraySec.length;

        return averageDiff + averageDiffSec;
    }

}

