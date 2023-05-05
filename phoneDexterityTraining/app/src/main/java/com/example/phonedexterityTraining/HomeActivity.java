package com.example.phonedexterityTraining;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;

import static processing.core.PApplet.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.phonedexterityTraining.relaxing.relaxingActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private BarChart weekHistory;
    private static final int participantId = 0;
    private static final int practiceDay = 7;
    private long largestRecordDay = 0;

    HashMap<String, record> recordList = new HashMap<>();


    private recognizingBackground sensor;
    private static long prevTime = 0;
    @SuppressLint("HandlerLeak")
    private static Handler resultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            if(bundle.containsKey(relaxingActivity.MSG_KEY)) {
                String string = bundle.getString(relaxingActivity.MSG_KEY);
                String scoreString = bundle.getString(relaxingActivity.SCORE_KEY);
                String speedString = bundle.getString(relaxingActivity.SPEED_KEY);
                long time = System.currentTimeMillis();
                if ((time - prevTime) >= relaxingActivity.GESTURE_GAP_TIME) {
                    Log.d(TAG, "" + Integer.parseInt(string) + ", " + Float.parseFloat(scoreString) + ", " + Float.parseFloat(speedString));
                    Log.d(TAG, string + (time - prevTime));
                    prevTime = time;
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        final Context context = this;

        //This starts the recognizingBackground thread
        // sensor
        sensor = new recognizingBackground(getApplicationContext(), resultHandler, true);
        sensor.start();

        //ArrayList<String> list = new ArrayList<String>();
        //ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
        //leaderBoardList = (ListView) findViewById(R.id.leader_board);
        //leaderBoard();

        weekHistory = findViewById(R.id.history_bar);
        //weekHistory.setOnChartValueSelectedListener(this);

        weekHistory.getDescription().setEnabled(false);

        // if more than 60 entries are displayed in the chart, no values will be
        // drawn
        weekHistory.setMaxVisibleValueCount(40);

        // scaling can now only be done on x- and y-axis separately
        weekHistory.setPinchZoom(false);

        weekHistory.setDrawGridBackground(false);
        weekHistory.setDrawBarShadow(false);

        weekHistory.setDrawValueAboveBar(false);
        weekHistory.setHighlightFullBarEnabled(false);

        // change the position of the y-labels
        YAxis leftAxis = weekHistory.getAxisLeft();
        //leftAxis.setValueFormatter(new MyAxisValueFormatter());
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        leftAxis.setAxisMaximum(20.0f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize((float)12.0);
        weekHistory.getAxisRight().setEnabled(false);

        XAxis xLabels = weekHistory.getXAxis();
        xLabels.setPosition(XAxis.XAxisPosition.BOTTOM);
        xLabels.setTextColor(Color.WHITE);
        xLabels.setTextSize((float)12.0);

        // chart.setDrawXLabels(false);
        // chart.setDrawYLabels(false);

        Legend l = weekHistory.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setFormSize(12.0f);
        l.setFormToTextSpace(4f);
        l.setXEntrySpace(6f);
        l.setTextColor(Color.WHITE);
        l.setTextSize((float)12.0);

        loadData();

        final Button relaxButton = findViewById(R.id.relax);
        relaxButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Map.Entry<String,record> today = getToday();
                if(today != null) {
                    Intent intent = new Intent(context, relaxingActivity.class);
                    intent.putExtra("todayRecord", today.getValue());
                    intent.putExtra("todayKey", today.getKey());
                    startActivity(intent);
                }
            }
        });
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
    protected void onDestroy() {
        super.onDestroy();

        sensor.stopThread();
        try {
            sensor.join();
            Log.d(TAG, "sensor join");
        } catch (InterruptedException e) {
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

    private void loadData() {
        DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference();
        Query query = recordRef.child("records").orderByChild("uId").equalTo(String.valueOf(participantId));

        query.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean todayExist = false;

                if (dataSnapshot.exists()) {
                    LocalDate todayDate= now();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        /*System.out.println("The ID is: " + snapshot.child("uId").getValue());
                        System.out.println("The Date is: " + snapshot.child("date").getValue());
                        System.out.println("The Time is: " + snapshot.child("time").getValue());
                        System.out.println("The SpinRep is: " + snapshot.child("spinRep").getValue());
                        System.out.println("The SpinSpeed is: " + snapshot.child("spinSpeed").getValue());
                        System.out.println("The SpinScore is: " + snapshot.child("spinScore").getValue());
                        System.out.println("The RotateRep is: " + snapshot.child("rotateRep").getValue());
                        System.out.println("The RotateSpeed is: " + snapshot.child("rotateSpeed").getValue());
                        System.out.println("The RotateScore is: " + snapshot.child("rotateScore").getValue());
                        System.out.println("The FlipRep is: " + snapshot.child("flipRep").getValue());
                        System.out.println("The FlipSpeed is: " + snapshot.child("flipSpeed").getValue());
                        System.out.println("The FlipScore is: " + snapshot.child("flipScore").getValue());*/
                        record rec = new record( snapshot.child("uId").getValue().toString(),
                                snapshot.child("date").getValue().toString(),
                                ((Number) snapshot.child("time").getValue()).intValue(),
                                ((Number) snapshot.child("spinRep").getValue()).intValue(),
                                ((Number) snapshot.child("spinSpeed").getValue()).floatValue(),
                                ((Number) snapshot.child("spinScore").getValue()).floatValue(),
                                ((Number) snapshot.child("rotateRep").getValue()).intValue(),
                                ((Number) snapshot.child("rotateSpeed").getValue()).floatValue(),
                                ((Number) snapshot.child("rotateScore").getValue()).floatValue(),
                                ((Number) snapshot.child("flipRep").getValue()).intValue(),
                                ((Number) snapshot.child("flipSpeed").getValue()).floatValue(),
                                ((Number) snapshot.child("flipScore").getValue()).floatValue());

                        long daysBetween = DAYS.between(rec.getDate(), todayDate);
                        if(daysBetween > largestRecordDay && daysBetween < practiceDay){
                            largestRecordDay = daysBetween;
                        }
                        //System.out.println("Days between " + daysBetween);
                        if(daysBetween == 0){
                            todayExist = true;
                        }

                        recordList.put(snapshot.getKey(), rec);
                    }
                    largestRecordDay = min(largestRecordDay, practiceDay-1);
                }

                if(!todayExist){
                    // create a record in database
                    System.out.println("create Today");
                    String key = recordRef.child("records").push().getKey();
                    record rec = new record( String.valueOf(participantId),
                            now().toString(),
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0);

                    Map<String, Object> recordValues = rec.toMap();

                    Map<String, Object> recordUpdates = new HashMap<>();
                    recordUpdates.put("/records/" + key, recordValues);

                    recordRef.updateChildren(recordUpdates);
                    recordList.put(key, rec);
                }

                processHistory();
                processToday();
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

            }
        });
    }

    private void processHistory(){
        // process the record list and draw the bar chart
        ArrayList<BarEntry> values = new ArrayList<>();

        for (Map.Entry<String,record> entry : recordList.entrySet()){
            record rec = entry.getValue();
            long daysBetween = DAYS.between(rec.getDate(), now());

            if(largestRecordDay - daysBetween < 0)  continue;

            if(rec.getAverageScore() == 0 || rec.getAverageScore() == 0){
                values.add(new BarEntry(
                        largestRecordDay - daysBetween,
                        new float[]{0, 0}));
            }
            else{
                float score = map(Math.max(Math.min((float)0.08, rec.getAverageScore()), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0);
                float speed = map(Math.max(Math.min((float)4, rec.getAverageSpeed()), (float)1), (float)1, (float)4, (float)9, (float)0);

                values.add(new BarEntry(
                        largestRecordDay - daysBetween,
                        new float[]{speed, score}));
            }
        }

        for (int i = (int) largestRecordDay + 1; i < practiceDay; i++){
            values.add(new BarEntry(
                    i,
                    new float[]{0, 0}));
        }

        BarDataSet set1;

        if (weekHistory.getData() != null &&
                weekHistory.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) weekHistory.getData().getDataSetByIndex(0);
            set1.setValues(values);
            weekHistory.getData().notifyDataChanged();
            weekHistory.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(values, "Week history");
            set1.setDrawIcons(false);
            set1.setColors(getColors());
            set1.setStackLabels(new String[]{"Average Speed", "Average Score"});

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            //data.setValueFormatter(new MyValueFormatter());
            data.setValueTextColor(Color.WHITE);
            data.setValueTextSize((float)12.0);

            weekHistory.setData(data);
        }

        weekHistory.setFitBars(true);
        weekHistory.invalidate();
    }

    private void processToday(){
        TextView timeText, spinRepText, spinSpeedText, spinScoreText, rotateRepText, rotateSpeedText, rotateScoreText, flipRepText, flipSpeedText, flipScoreText;
        ImageView spinGesture, rotateGesture, flipGesture;

        timeText = (TextView) findViewById(R.id.time_today);
        spinRepText = (TextView) findViewById(R.id.spin_rep);
        spinSpeedText = (TextView) findViewById(R.id.spin_speed);
        spinScoreText = (TextView) findViewById(R.id.spin_score);
        rotateRepText = (TextView) findViewById(R.id.rotate_rep);
        rotateSpeedText = (TextView) findViewById(R.id.rotate_speed);
        rotateScoreText = (TextView) findViewById(R.id.rotate_score);
        flipRepText = (TextView) findViewById(R.id.flip_rep);
        flipSpeedText = (TextView) findViewById(R.id.flip_speed);
        flipScoreText = (TextView) findViewById(R.id.flip_score);
        spinGesture = (ImageView) findViewById(R.id.spin_today);
        rotateGesture = (ImageView) findViewById(R.id.rotate_today);
        flipGesture = (ImageView) findViewById(R.id.flip_today);

        Map.Entry<String,record> today = getToday();

        if(today != null) {
            record rec = today.getValue();
            // set time string
            String timeString = "Today: " + (int)ceil((double)rec.getTime() / 60.0) + " minutes";
            Spannable sb = new SpannableString(timeString);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 7, timeString.length() - 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); //bold
            timeText.setText(sb, TextView.BufferType.SPANNABLE);

            // normalize speed and score and set gesture
            if(rec.getSpinRep() == 0){
                updateSingleFigure(rec.getSpinRep(), "-", "-", 2, spinGesture, spinSpeedText, spinScoreText, spinRepText, false);
            }
            else{
                int spinScore = Math.round(map(Math.max(Math.min((float)0.08, rec.getSpinScore()), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));
                int spinSpeed = Math.round(map(Math.max(Math.min((float)4, rec.getSpinSpeed()), (float)1), (float)1, (float)4, (float)9, (float)0));
                updateSingleFigure(rec.getSpinRep(), String.valueOf(spinSpeed), String.valueOf(spinScore), 2, spinGesture, spinSpeedText, spinScoreText, spinRepText, (spinSpeed >= 6 && spinScore >= 6));
            }

            if(rec.getRotateRep() == 0){
                updateSingleFigure(rec.getRotateRep(), "-", "-", 1, rotateGesture, rotateSpeedText, rotateScoreText, rotateRepText, false);
            }
            else{
                int rotateScore = Math.round(map(Math.max(Math.min((float)0.08, rec.getRotateScore()), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));
                int rotateSpeed = Math.round(map(Math.max(Math.min((float)4, rec.getRotateSpeed()), (float)1), (float)1, (float)4, (float)9, (float)0));
                updateSingleFigure(rec.getRotateRep(), String.valueOf(rotateSpeed), String.valueOf(rotateScore), 1, rotateGesture, rotateSpeedText, rotateScoreText, rotateRepText, (rotateSpeed >= 6 && rotateScore >= 6));
            }


            if(rec.getFlipRep() == 0) {
                updateSingleFigure(rec.getFlipRep(), "-", "-", 0, flipGesture, flipSpeedText, flipScoreText, flipRepText, false);
            }
            else {
                int flipScore = Math.round(map(Math.max(Math.min((float)0.08, rec.getFlipScore()), (float)0.02), (float)0.02, (float)0.08, (float)9, (float)0));
                int flipSpeed = Math.round(map(Math.max(Math.min((float) 4, rec.getFlipSpeed()), (float) 1), (float) 1, (float) 4, (float)9, (float)0));
                updateSingleFigure(rec.getFlipRep(), String.valueOf(flipSpeed), String.valueOf(flipScore), 0, flipGesture, flipSpeedText, flipScoreText, flipRepText, (flipSpeed >= 6 && flipScore >= 6));
            }
        }
    }

    private int[] getColors() {

        // have as many colors as stack-values per entry
        int[] colors = new int[2];

        System.arraycopy(ColorTemplate.MATERIAL_COLORS, 0, colors, 0, 2);

        return colors;
    }

    private Map.Entry<String,record> getToday(){
        for (Map.Entry<String,record> entry : recordList.entrySet()){
            record rec = entry.getValue();
            long daysBetween = DAYS.between(rec.getDate(), now());
            if(daysBetween == 0){
                return entry;
            }
        }
        return null;
    }

    private void updateSingleFigure(int rep, String speed, String score, int cat, ImageView gesture, TextView speedText, TextView scoreText, TextView repText, boolean usingStar){
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

        speedText.setText(speed);
        scoreText.setText(score);
        repText.setText(String.valueOf(rep));
    }
}
