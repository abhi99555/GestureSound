package com.example.gesturesound;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static float SENSOR_SAMPLING_PERIOD = 10F;
    int snareCt = 0;
    int hangCt = 0;
    private LineGraphSeries<DataPoint> lineGraphSeries_forZ;
    private SensorManager sensorManager;
    private float currX;
    private long newTime, startTime, prevTime;
    private boolean playingSnare = false, playingHanging = false, playingCymbals = false;
    private TextView xAcc, zAcc;
    private MediaPlayer snare, tomtom, cym;
    private Button cymbals;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currX = 0;
        snare = MediaPlayer.create(this, R.raw.snare);
        tomtom = MediaPlayer.create(this, R.raw.tomtom);
        cym = MediaPlayer.create(this, R.raw.cymbals);

        cymbals = findViewById(R.id.cymbals);

        cymbals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCymbals();
            }
        });

        xAcc = findViewById(R.id.textView1);
        zAcc = findViewById(R.id.textView2);

        startTime = newTime = prevTime = System.currentTimeMillis();

        lineGraphSeries_forZ = new LineGraphSeries<>();
        final GraphView graphZ = (GraphView) findViewById(R.id.acclgraph);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        graphZ.getViewport().setYAxisBoundsManual(true);
        graphZ.getViewport().setMinY(-50);
        graphZ.getViewport().setMaxY(50);

        graphZ.getViewport().setXAxisBoundsManual(true);


        // enable scaling and scrolling
        graphZ.getViewport().setScalable(true);
        graphZ.getViewport().setScalableY(true);

        graphZ.addSeries(lineGraphSeries_forZ);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        newTime = System.currentTimeMillis();
        if (newTime - prevTime >= SENSOR_SAMPLING_PERIOD) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float xAcceleration = Math.round(event.values[0]);
                float zAcceleration = Math.round(event.values[2]);
                Log.d("TAGGING", xAcceleration+"x "+zAcceleration);
                if(Math.abs((xAcceleration))>1) {
                    currX = xAcceleration;
                    xAcc.setText("x = " + xAcceleration);
                    zAcc.setText("z = " + zAcceleration);
                    if (playingSnare && xAcceleration > -20) {
                        playingSnare = false;
                    }
                    else if (xAcceleration < -20) {
//                        Log.d("TAG: ","SNARE x: "+xAcceleration+" z: "+zAcceleration);
                        playSnare();
                    }

                    if (playingHanging && zAcceleration < 20){
                        playingHanging = false;
                    }
                    else if (zAcceleration >20){
                        playHanging();
                    }

                } else {
                    currX = 0f;
                }
                lineGraphSeries_forZ.appendData(new DataPoint((newTime - startTime) / 10, currX), true, 10000, false);


//
//                if (Math.abs(xAcceleration) > 1) {
//                    currX = xAcceleration;
//                    xAcc.setText("x = " + xAcceleration);
//                    zAcc.setText("z = " + zAcceleration);
//
//                    if (playingSnare && xAcceleration > -10 && zAcceleration < -5) {
//                        playingSnare = !playingSnare;
//                    } else if (playingHanging && zAcceleration > -5 && xAcceleration < -10) {
//                        playingHanging = !playingHanging;
//                    } else if (xAcceleration > 0 || zAcceleration > 0) {
//                    }
////                    else if(xAcceleration < -12 && zAcceleration < -7){
////                        Log.d("TAG: ","CYMBALS x: "+xAcceleration+" z: "+zAcceleration);
////                    }
//                    else if (xAcceleration < -10 && zAcceleration > -5) {
////                        Log.d("TAG: ","SNARE x: "+xAcceleration+" z: "+zAcceleration);
//                        playSnare();
//                    } else if (zAcceleration < -5 && xAcceleration > -10) {
////                        Log.d("TAG: ","HANGING x: "+xAcceleration+" z: "+zAcceleration);
//                        playHanging();
//                    }
//
//                } else {
//                    currX = 0f;
//                }
//                lineGraphSeries_forZ.appendData(new DataPoint((newTime - startTime) / 10, currX), true, 10000, false);

            }
        }

    }

    private void playSnare() {
        Log.d("TAG:  ", "SNARE TRIGGERED");
        if (!playingSnare) {
            playingSnare = true;
            snare.start();
            Log.d("TAG: ", "PLAYING SNARE" + (++snareCt));
        }
    }


    private void playHanging() {
        Log.d("TAG:  ", "HIHAT TRIGGERED");
        if (!playingHanging) {
            playingHanging = true;
            tomtom.start();
            Log.d("TAG: ", "PLAYING HANGING" + (++hangCt));
        }
    }

    private void playCymbals() {
        cym = MediaPlayer.create(this, R.raw.cymbals);
        cym.start();
        cym.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                cym.release();
            }
        });
        Log.d("TAG: ", "PLAYING HANGING" + (++hangCt));
    }

//    private void playBassDrum(){}


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}

