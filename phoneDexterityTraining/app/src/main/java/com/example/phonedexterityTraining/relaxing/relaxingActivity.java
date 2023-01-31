package com.example.phonedexterityTraining.relaxing;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.phonedexterityTraining.R;
import com.example.phonedexterityTraining.recognizingBackground;
import com.example.phonedexterityTraining.record;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.data.FloatList;
import processing.data.IntList;

import static processing.core.PApplet.map;

public class relaxingActivity extends AppCompatActivity {
    private static final String TAG = "relaxingActivity";
    private static final String MSG_KEY = "result";
    private static final String LOG_KEY = "log";
    private static final String SCORE_KEY = "score";
    private static final String SPEED_KEY = "speed";

    private TextView relaxText, currentRep, currentSpeed, currentScore;
    private TextView historyRep1, historySpeed1, historyScore1;
    private TextView historyRep2, historySpeed2, historyScore2;
    private TextView historySpeed3, historyScore3;
    private LineChart relaxChartX, relaxChartY, relaxChartZ;
    private ImageView currentGesture, historyGesture1, historyGesture2, historyGesture3;
    private MediaPlayer soundGreat, soundNormal;

    private recognizingBackground sensor;
    private boolean upsideDown = false;

    public static final long GESTURE_GAP_TIME = 500;
    long prevTime = 0;
    float[] accuDiff = {0, 0, 0};
    //int main_axis = -1;
    //FloatList diffX = new FloatList();
    //FloatList diffY = new FloatList();
    //FloatList diffZ = new FloatList();
    //FloatList diffTime = new FloatList();
    IntList gestureRepition = new IntList();
    IntList gestureScore = new IntList();
    IntList gestureSpeed = new IntList();
    IntList gestureCat = new IntList();

    // for log
    String todayRecordKey;
    record todayRecord;
    LocalDateTime sessionStartTime;
    int sessionSpinRep = 0;
    float sessionSpinSpeed = 0;
    float sessionSpinScore = 0;
    int sessionRotateRep = 0;
    float sessionRotateSpeed = 0;
    float sessionRotateScore = 0;
    int sessionFlipRep = 0;
    float sessionFlipSpeed = 0;
    float sessionFlipScore = 0;


    int prevGesture = 0;
    int repetition = 0;

    int displayWindow = 120;

    private final Handler resultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            if(bundle.containsKey(MSG_KEY)){
                String string = bundle.getString(MSG_KEY);
                String scoreString = bundle.getString(SCORE_KEY);
                String speedString = bundle.getString(SPEED_KEY);
                long time = System.currentTimeMillis();
                if ((time - prevTime) >= GESTURE_GAP_TIME) {
                    try {
                        showResult(Integer.parseInt(string), Float.parseFloat(scoreString), Float.parseFloat(speedString));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    changeRotation(Integer.parseInt(string));
                    //ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    //toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    accuDiff[0] = 0;
                    accuDiff[1] = 0;
                    accuDiff[2] = 0;
                    /*diffX.clear();
                    diffY.clear();
                    diffZ.clear();
                    diffTime.clear();*/
                    Log.d(TAG, string + (time - prevTime));
                    prevTime = time;
                }
            }
            else if(bundle.containsKey(LOG_KEY)){
                float[] data = bundle.getFloatArray(LOG_KEY);
                accuDiff[0] += data[0];
                accuDiff[1] += data[1];
                accuDiff[2] += data[2];
                /*diffX.append(data[0]);
                diffY.append(data[1]);
                diffZ.append(data[2]);
                diffTime.append(data[3]);*/
                LineData relaxDataX = relaxChartX.getData();

                if(relaxDataX != null) {
                    if (((LineDataSet) relaxDataX.getDataSetByIndex(0)).getEntryCount() == displayWindow) {
                        ((LineDataSet) relaxDataX.getDataSetByIndex(0)).removeEntry(0);
                    }
                    ((LineDataSet) relaxDataX.getDataSetByIndex(0)).addEntry(new Entry(data[3], accuDiff[0]));

                    relaxDataX.notifyDataChanged();

                    relaxChartX.notifyDataSetChanged();
                    relaxChartX.invalidate();
                }
                LineData relaxDataY = relaxChartY.getData();

                if(relaxDataY != null) {
                    if (((LineDataSet) relaxDataY.getDataSetByIndex(0)).getEntryCount() == displayWindow) {
                        ((LineDataSet) relaxDataY.getDataSetByIndex(0)).removeEntry(0);
                    }
                    ((LineDataSet) relaxDataY.getDataSetByIndex(0)).addEntry(new Entry(data[3], accuDiff[1]));

                    relaxDataY.notifyDataChanged();

                    relaxChartY.notifyDataSetChanged();
                    relaxChartY.invalidate();
                }
                LineData relaxDataZ = relaxChartZ.getData();

                if(relaxDataZ != null) {
                    if (((LineDataSet) relaxDataZ.getDataSetByIndex(0)).getEntryCount() == displayWindow) {
                        ((LineDataSet) relaxDataZ.getDataSetByIndex(0)).removeEntry(0);
                    }
                    ((LineDataSet) relaxDataZ.getDataSetByIndex(0)).addEntry(new Entry(data[3], accuDiff[2]));

                    relaxDataZ.notifyDataChanged();

                    relaxChartZ.notifyDataSetChanged();
                    relaxChartZ.invalidate();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relax_activity);


        setContentView(R.layout.relax_activity);
        relaxText = (TextView) findViewById(R.id.relaxText);
        currentRep = (TextView) findViewById(R.id.currentRep);
        currentSpeed = (TextView) findViewById(R.id.currentSpeed);
        currentScore = (TextView) findViewById(R.id.currentScore);
        historyRep1 = (TextView) findViewById(R.id.historyRep1);
        historySpeed1 = (TextView) findViewById(R.id.historySpeed1);
        historyScore1 = (TextView) findViewById(R.id.historyScore1);
        historyRep2 = (TextView) findViewById(R.id.historyRep2);
        historySpeed2 = (TextView) findViewById(R.id.historySpeed2);
        historyScore2 = (TextView) findViewById(R.id.historyScore2);
        historySpeed3 = (TextView) findViewById(R.id.historySpeed3);
        historyScore3 = (TextView) findViewById(R.id.historyScore3);
        currentGesture = (ImageView) findViewById(R.id.currentGesture);
        historyGesture1 = (ImageView) findViewById(R.id.historyGesture1);
        historyGesture2 = (ImageView) findViewById(R.id.historyGesture2);
        historyGesture3 = (ImageView) findViewById(R.id.historyGesture3);

        currentRep.setVisibility(View.INVISIBLE);
        currentSpeed.setVisibility(View.INVISIBLE);
        currentScore.setVisibility(View.INVISIBLE);
        historyRep1.setVisibility(View.INVISIBLE);
        historySpeed1.setVisibility(View.INVISIBLE);
        historyScore1.setVisibility(View.INVISIBLE);
        historyRep2.setVisibility(View.INVISIBLE);
        historySpeed2.setVisibility(View.INVISIBLE);
        historyScore2.setVisibility(View.INVISIBLE);
        historySpeed3.setVisibility(View.INVISIBLE);
        historyScore3.setVisibility(View.INVISIBLE);
        currentGesture.setVisibility(View.INVISIBLE);
        historyGesture1.setVisibility(View.INVISIBLE);
        historyGesture2.setVisibility(View.INVISIBLE);
        historyGesture3.setVisibility(View.INVISIBLE);

        {   // // Chart Style // //
            relaxChartX = (LineChart) findViewById(R.id.relaxLineX);
            relaxChartX.setBackgroundColor(Color.TRANSPARENT);
            relaxChartX.getDescription().setEnabled(false);
            relaxChartX.setDrawGridBackground(false);

            ArrayList<ILineDataSet> relaxDataSets = new ArrayList<>();

            List<Entry> value = new ArrayList<Entry>();

            LineDataSet dv = new LineDataSet(value, "value");
            dv.setLineWidth(1f);
            dv.setDrawCircles(false);
            dv.setDrawValues(false);

            dv.setColor(Color.WHITE);
            relaxDataSets.add(dv);

            LineData relaxData = new LineData(relaxDataSets);
            relaxChartX.setData(relaxData);
            relaxChartX.invalidate();
            relaxChartX.notifyDataSetChanged();
            relaxChartX.setTouchEnabled(false);
            relaxChartX.setDragEnabled(false);
            relaxChartX.setScaleEnabled(false);
            relaxChartX.setScaleXEnabled(false);
            relaxChartX.setScaleYEnabled(false);
            relaxChartX.setPinchZoom(false);
            relaxChartX.getLegend().setEnabled(false);
        }

        XAxis xAxis;
        {   // // X-Axis Style // //
            xAxis = relaxChartX.getXAxis();

            // vertical grid lines
            xAxis.setDrawAxisLine(false);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawLabels(false);
        }

        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = relaxChartX.getAxisLeft();

            // disable dual axis (only use LEFT axis)
            relaxChartX.getAxisRight().setEnabled(false);

            // axis range
            yAxis.setAxisMaximum(3.2f);
            yAxis.setAxisMinimum(-3.2f);
            yAxis.setDrawAxisLine(false);
            yAxis.setDrawGridLines(false);
            yAxis.setDrawLabels(false);
        }

        {   // // Chart Style // //
            relaxChartY = (LineChart) findViewById(R.id.relaxLineY);
            relaxChartY.setBackgroundColor(Color.TRANSPARENT);
            relaxChartY.getDescription().setEnabled(false);
            relaxChartY.setDrawGridBackground(false);

            ArrayList<ILineDataSet> relaxDataSets = new ArrayList<>();

            List<Entry> value = new ArrayList<Entry>();

            LineDataSet dv = new LineDataSet(value, "value");
            dv.setLineWidth(1f);
            dv.setDrawCircles(false);
            dv.setDrawValues(false);

            dv.setColor(Color.WHITE);
            relaxDataSets.add(dv);

            LineData relaxData = new LineData(relaxDataSets);
            relaxChartY.setData(relaxData);
            relaxChartY.invalidate();
            relaxChartY.notifyDataSetChanged();
            relaxChartY.setTouchEnabled(false);
            relaxChartY.setDragEnabled(false);
            relaxChartY.setScaleEnabled(false);
            relaxChartY.setScaleXEnabled(false);
            relaxChartY.setScaleYEnabled(false);
            relaxChartY.setPinchZoom(false);
            relaxChartY.getLegend().setEnabled(false);
        }

        {   // // X-Axis Style // //
            xAxis = relaxChartY.getXAxis();

            // vertical grid lines
            xAxis.setDrawAxisLine(false);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawLabels(false);
        }

        {   // // Y-Axis Style // //
            yAxis = relaxChartY.getAxisLeft();

            // disable dual axis (only use LEFT axis)
            relaxChartY.getAxisRight().setEnabled(false);

            // axis range
            yAxis.setAxisMaximum(3.2f);
            yAxis.setAxisMinimum(-3.2f);
            yAxis.setDrawAxisLine(false);
            yAxis.setDrawGridLines(false);
            yAxis.setDrawLabels(false);
        }

        {   // // Chart Style // //
            relaxChartZ = (LineChart) findViewById(R.id.relaxLineZ);
            relaxChartZ.setBackgroundColor(Color.TRANSPARENT);
            relaxChartZ.getDescription().setEnabled(false);
            relaxChartZ.setDrawGridBackground(false);

            ArrayList<ILineDataSet> relaxDataSets = new ArrayList<>();

            List<Entry> value = new ArrayList<Entry>();

            LineDataSet dv = new LineDataSet(value, "value");
            dv.setLineWidth(1f);
            dv.setDrawCircles(false);
            dv.setDrawValues(false);

            dv.setColor(Color.WHITE);
            relaxDataSets.add(dv);

            LineData relaxData = new LineData(relaxDataSets);
            relaxChartZ.setData(relaxData);
            relaxChartZ.invalidate();
            relaxChartZ.notifyDataSetChanged();
            relaxChartZ.setTouchEnabled(false);
            relaxChartZ.setDragEnabled(false);
            relaxChartZ.setScaleEnabled(false);
            relaxChartZ.setScaleXEnabled(false);
            relaxChartZ.setScaleYEnabled(false);
            relaxChartZ.setPinchZoom(false);
            relaxChartZ.getLegend().setEnabled(false);
        }

        {   // // X-Axis Style // //
            xAxis = relaxChartZ.getXAxis();

            // vertical grid lines
            xAxis.setDrawAxisLine(false);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawLabels(false);
        }

        {   // // Y-Axis Style // //
            yAxis = relaxChartZ.getAxisLeft();

            // disable dual axis (only use LEFT axis)
            relaxChartZ.getAxisRight().setEnabled(false);

            // axis range
            yAxis.setAxisMaximum(3.2f);
            yAxis.setAxisMinimum(-3.2f);
            yAxis.setDrawAxisLine(false);
            yAxis.setDrawGridLines(false);
            yAxis.setDrawLabels(false);
        }

        // sensor
        sensor = new recognizingBackground(getApplicationContext(), resultHandler, true);
        sensor.start();

        // session log
        Intent intent = getIntent();
        todayRecord = (record)intent.getSerializableExtra("todayRecord");
        todayRecordKey = intent.getStringExtra("todayKey");
        System.out.println("today record key: " + todayRecordKey);
        sessionStartTime = LocalDateTime.now();
        sessionSpinRep = 0;
        sessionSpinSpeed = 0;
        sessionSpinScore = 0;
        sessionRotateRep = 0;
        sessionRotateSpeed = 0;
        sessionRotateScore = 0;
        sessionFlipRep = 0;
        sessionFlipSpeed = 0;
        sessionFlipScore = 0;

        // initial icon
        for(int i=0;i<4;i++){
            gestureRepition.append(0);
            gestureScore.append(0);
            gestureSpeed.append(0);
            gestureCat.append(-1);
        }

        try {
            AssetFileDescriptor shortSound = getAssets().openFd("aliasing-chimes.mp3");
            AssetFileDescriptor longSound = getAssets().openFd("bliind__chimes-6.wav");

            soundGreat = new MediaPlayer();
            soundGreat.setDataSource(longSound.getFileDescriptor(), longSound.getStartOffset(), longSound.getLength());
            soundGreat.prepare();

            soundNormal = new MediaPlayer();
            soundNormal.setDataSource(shortSound.getFileDescriptor(), shortSound.getStartOffset(), shortSound.getLength());
            soundNormal.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // update record
        LocalDateTime sessionEndTime = LocalDateTime.now();
        Duration duration = Duration.between(sessionStartTime, sessionEndTime);
        todayRecord.updateRecord((int)duration.getSeconds(), sessionSpinRep, sessionSpinSpeed, sessionSpinScore,
                sessionRotateRep, sessionRotateSpeed, sessionRotateScore, sessionFlipRep, sessionFlipSpeed, sessionFlipScore);

        Map<String, Object> recordValues = todayRecord.toMap();

        Map<String, Object> recordUpdates = new HashMap<>();
        recordUpdates.put("/records/" + todayRecordKey, recordValues);

        FirebaseDatabase.getInstance().getReference().updateChildren(recordUpdates);

        sensor.stopThread();
        try {
            sensor.join();
            Log.d(TAG, "join");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
    }

    public void showResult(int gestureIndex, float recognizedScore, float recognizedSpeed) throws IOException {
        //print(index);
        int[][] Gesture = {
                {4},
                {0, 0, 2},
                {0, 1, 2},
                {1, 0, 0},
                {1, 0, 1},
                {1, 1, 0},
                {1, 1, 1},
                {2, 0, 0},
                {2, 0, 1},
                {2, 1, 0},
                {2, 1, 1},
                {3, 0, 0},
                {3, 0, 1},
                {3, 1, 0},
                {3, 1, 1}};

        final String[] gesturesName = {"Shift", "Spin", "Rotate", "Flip", "None"};
        final String[] directionName = {"Abduction", "Adduction"};
        final String[] directionShiftingName = {"Up", "Down"};
        final String[] directionSpinningName = {"Clockwise", "Counterclockwise"};
        final String[] directionRotatingName = {"Left", "Right"};
        final String[] directionFlippingName = {"Away", "Forward"};
        final String[] magName = {"Half", "Full", "None"};

        int main_axis = -1;

        if(prevGesture == gestureIndex){
            repetition ++;
        }
        else{
            repetition = 1;
            prevGesture = gestureIndex;
        }

        String finalString = "";
        // update text
        if (Gesture[gestureIndex][0] == 0) {
            finalString = gesturesName[Gesture[gestureIndex][0]] + " " + directionShiftingName[Gesture[gestureIndex][1]];
            main_axis = -1;
        } else if (Gesture[gestureIndex][0] == 1) {
            finalString= gesturesName[Gesture[gestureIndex][0]] + " " + directionSpinningName[Gesture[gestureIndex][1]];
            main_axis = 2;
            relaxText.setTextColor(Color.parseColor("#5CAD07"));
        } else if (Gesture[gestureIndex][0] == 2) {
            finalString = gesturesName[Gesture[gestureIndex][0]] + " " + directionRotatingName[Gesture[gestureIndex][1]];
            main_axis = 1;
            relaxText.setTextColor(Color.parseColor("#52BFEF"));
        } else if (Gesture[gestureIndex][0] == 3) {
            finalString = gesturesName[Gesture[gestureIndex][0]] + " " + directionFlippingName[Gesture[gestureIndex][1]];
            main_axis = 0;
            relaxText.setTextColor(Color.parseColor("#4D4D93"));
        }
        Spannable sb = new SpannableString( finalString );
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, gesturesName[Gesture[gestureIndex][0]].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); //bold
        relaxText.setText(sb, TextView.BufferType.SPANNABLE);

        // calculate score and speed
        //float gestureTime = diffTime.get(diffTime.size() - 1) - diffTime.get(0);
        int speed = Math.round(map(Math.max(Math.min((float)4, recognizedSpeed), (float)1), (float)1, (float)4, (float)9, (float)0));

        float calScore = 0;
        int score = 0;
        if(main_axis == 0){
            /*FloatList array = new FloatList();
            array.append(diffY);
            array.append(diffZ);
            calScore = calculateScore(diffX.array(), array.array());*/
            score = Math.round(map(Math.max(Math.min((float)0.08, recognizedScore), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));

            // update flip
            sessionFlipRep++;
            sessionFlipScore+=recognizedScore;
            sessionFlipSpeed+=recognizedSpeed;
        }
        else if(main_axis == 1){
            /*FloatList array = new FloatList();
            array.append(diffX);
            array.append(diffZ);
            calScore = calculateScore(diffY.array(), array.array());*/
            score = Math.round(map(Math.max(Math.min((float)0.08, recognizedScore), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));

            // update rotate
            sessionRotateRep++;
            sessionRotateScore+=recognizedScore;
            sessionRotateSpeed+=recognizedSpeed;
        }
        else if(main_axis == 2){
            /*FloatList array = new FloatList();
            array.append(diffX);
            array.append(diffY);
            calScore = calculateScore(diffZ.array(), array.array());*/
            score = Math.round(map(Math.max(Math.min((float)0.08, recognizedScore), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));

            // update spin
            sessionSpinRep++;
            sessionSpinScore+=recognizedScore;
            sessionSpinSpeed+=recognizedSpeed;
        }


        if(gestureRepition.size() == 4){
            gestureRepition.remove(0);
            gestureSpeed.remove(0);
            gestureScore.remove(0);
            gestureCat.remove(0);
        }
        gestureRepition.append(repetition);
        gestureSpeed.append(speed);
        gestureScore.append(score);
        gestureCat.append(main_axis);

        updateFigure(gestureRepition.array(), gestureSpeed.array(), gestureScore.array(), gestureCat.array());
        if(speed >= 6 && score >= 6){
            // long sound
            soundGreat.start();
        }
        else{
            soundNormal.start();
        }
    }

    public void changeRotation(int gesture) {
        if(gesture == 3 || gesture == 5){
            upsideDown = !upsideDown;
            if(upsideDown){
                relaxText.setRotation(180);
            }
            else{
                relaxText.setRotation(0);
            }
        }
    }

    private void updateSingleFigure(int rep, int speed, int score, int cat, ImageView gesture, TextView speedText, TextView scoreText, TextView repText){
        boolean usingStar = (speed >= 6 && score >= 6);

        if(repText == null){
            // history3
            if(usingStar) {
                if (cat == 0) {
                    gesture.setImageResource(R.drawable.relaxing_x_star_half);
                }
                else if(cat == 1){
                    gesture.setImageResource(R.drawable.relaxing_y_star_half);
                }
                else if(cat == 2){
                    gesture.setImageResource(R.drawable.relaxing_z_star_half);
                }
            }
            else{
                if (cat == 0) {
                    gesture.setImageResource(R.drawable.relaxing_x_circle_half);
                }
                else if(cat == 1){
                    gesture.setImageResource(R.drawable.relaxing_y_circle_half);
                }
                else if(cat == 2){
                    gesture.setImageResource(R.drawable.relaxing_z_circle_half);
                }
            }

            speedText.setText(String.valueOf(speed));
            scoreText.setText(String.valueOf(score));
        }
        else{
            if(usingStar) {
                if (cat == 0) {
                    gesture.setImageResource(R.drawable.relaxing_x_star);
                }
                else if(cat == 1){
                    gesture.setImageResource(R.drawable.relaxing_y_star);
                }
                else if(cat == 2){
                    gesture.setImageResource(R.drawable.relaxing_z_star);
                }
            }
            else{
                if (cat == 0) {
                    gesture.setImageResource(R.drawable.relaxing_x_circle);
                }
                else if(cat == 1){
                    gesture.setImageResource(R.drawable.relaxing_y_circle);
                }
                else if(cat == 2){
                    gesture.setImageResource(R.drawable.relaxing_z_circle);
                }
            }

            speedText.setText(String.valueOf(speed));
            scoreText.setText(String.valueOf(score));
            repText.setText(String.valueOf(rep));
        }
    }

    private void updateFigure(int[] repetition, int[] speed, int[] score, int[] category){
        // update the history icon
        if(category[0] == -1){
            // set invisible
            historyGesture3.setVisibility(View.INVISIBLE);
            historyScore3.setVisibility(View.INVISIBLE);
            historySpeed3.setVisibility(View.INVISIBLE);
        }
        else{
            historyGesture3.setVisibility(View.VISIBLE);
            historyScore3.setVisibility(View.VISIBLE);
            historySpeed3.setVisibility(View.VISIBLE);
            updateSingleFigure(repetition[0], speed[0], score[0], category[0], historyGesture3, historySpeed3, historyScore3, null);
        }

        if(category[1] == -1){
            // set invisible
            historyGesture2.setVisibility(View.INVISIBLE);
            historyScore2.setVisibility(View.INVISIBLE);
            historySpeed2.setVisibility(View.INVISIBLE);
            historyRep2.setVisibility(View.INVISIBLE);
        }
        else{
            historyGesture2.setVisibility(View.VISIBLE);
            historyScore2.setVisibility(View.VISIBLE);
            historySpeed2.setVisibility(View.VISIBLE);
            historyRep2.setVisibility(View.VISIBLE);
            updateSingleFigure(repetition[1], speed[1], score[1], category[1], historyGesture2, historySpeed2, historyScore2, historyRep2);
        }

        if(category[2] == -1){
            // set invisible
            historyGesture1.setVisibility(View.INVISIBLE);
            historyScore1.setVisibility(View.INVISIBLE);
            historySpeed1.setVisibility(View.INVISIBLE);
            historyRep1.setVisibility(View.INVISIBLE);
        }
        else{
            historyGesture1.setVisibility(View.VISIBLE);
            historyScore1.setVisibility(View.VISIBLE);
            historySpeed1.setVisibility(View.VISIBLE);
            historyRep1.setVisibility(View.VISIBLE);
            updateSingleFigure(repetition[2], speed[2], score[2], category[2], historyGesture1, historySpeed1, historyScore1, historyRep1);
        }

        if(category[3] == -1){
            // set invisible
            currentGesture.setVisibility(View.INVISIBLE);
            currentSpeed.setVisibility(View.INVISIBLE);
            currentScore.setVisibility(View.INVISIBLE);
            currentRep.setVisibility(View.INVISIBLE);
        }
        else{
            currentGesture.setVisibility(View.VISIBLE);
            currentSpeed.setVisibility(View.VISIBLE);
            currentScore.setVisibility(View.VISIBLE);
            currentRep.setVisibility(View.VISIBLE);
            updateSingleFigure(repetition[3], speed[3], score[3], category[3], currentGesture, currentSpeed, currentScore, currentRep);
        }


        LineData relaxData = null;
        if(category[3] == 0){
            relaxData = relaxChartX.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(5f);
            }
            relaxData.notifyDataChanged();
            relaxChartX.notifyDataSetChanged();
            relaxChartX.invalidate();
            relaxData = relaxChartY.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartY.notifyDataSetChanged();
            relaxChartY.invalidate();
            relaxData = relaxChartZ.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartZ.notifyDataSetChanged();
            relaxChartZ.invalidate();
        }
        else if(category[3] == 1){
            relaxData = relaxChartX.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartX.notifyDataSetChanged();
            relaxChartX.invalidate();
            relaxData = relaxChartY.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(5f);
            }
            relaxData.notifyDataChanged();
            relaxChartY.notifyDataSetChanged();
            relaxChartY.invalidate();
            relaxData = relaxChartZ.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartZ.notifyDataSetChanged();
            relaxChartZ.invalidate();
        }
        else if(category[3] == 2){
            relaxData = relaxChartX.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartX.notifyDataSetChanged();
            relaxChartX.invalidate();
            relaxData = relaxChartY.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(1f);
            }
            relaxData.notifyDataChanged();
            relaxChartY.notifyDataSetChanged();
            relaxChartY.invalidate();
            relaxData = relaxChartZ.getData();
            if(relaxData != null) {
                ((LineDataSet) relaxData.getDataSetByIndex(0)).setLineWidth(5f);
            }
            relaxData.notifyDataChanged();
            relaxChartZ.notifyDataSetChanged();
            relaxChartZ.invalidate();
        }
    }
}
