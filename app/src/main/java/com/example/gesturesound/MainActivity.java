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
    int instr1Count = 0;
    int instr2Count = 0;

    private LineGraphSeries<DataPoint> lineGraphSeries_forZ;
    private SensorManager sensorManager;

    private String instrumentType = "DRUMS";

    private float currX;
    private long newTime, startTime, prevTime;
    private boolean playingInstrument1 = false, playingInstrument2 = false;

    private TextView xAcc, zAcc;
    private MediaPlayer instrument1, instrument2, instrument3;
    private Button instrument3Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currX = 0;

        setInstruments();

        instrument3Button = findViewById(R.id.cymbals);

        instrument3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInstrument3();
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
                    if (playingInstrument1 && xAcceleration > -20) {
                        playingInstrument1 = false;
                    }
                    else if (xAcceleration < -20) {
//                        Log.d("TAG: ","SNARE x: "+xAcceleration+" z: "+zAcceleration);
                        playInstrument1();
                    }

                    if (playingInstrument2 && zAcceleration < 20){
                        playingInstrument2 = false;
                    }
                    else if (zAcceleration >20){
                        playInstrument2();
                    }

                } else {
                    currX = 0f;
                }
                lineGraphSeries_forZ.appendData(new DataPoint((newTime - startTime) / 10, currX), true, 10000, false);
            }
        }

    }

    private void setInstruments() {
        if (instrumentType.equals("DRUMS")){

        }

    }

    private void playInstrument1() {
        Log.d("TAG:  ", "SNARE TRIGGERED");
        if (!playingInstrument1) {
            playingInstrument1 = true;
            instrument1.start();
            Log.d("TAG: ", "PLAYING SNARE" + (++instr1Count));
        }
    }


    private void playInstrument2() {
        Log.d("TAG:  ", "HIHAT TRIGGERED");
        if (!playingInstrument2) {
            playingInstrument2 = true;
            instrument2.start();
            Log.d("TAG: ", "PLAYING HANGING" + (++instr2Count));
        }
    }

    private void playInstrument3() {
        if (instrumentType.equals("DRUMS")){
            instrument3 = MediaPlayer.create(this, R.raw.cymbals);
        }
        else if (instrumentType.equals("GUITAR")) {

        }
        else{
            instrument3 = MediaPlayer.create(this, R.raw.cymbals);
        }
        instrument3.start();
        instrument3.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                instrument3.release();
            }
        });
        Log.d("TAG: ", "PLAYING HANGING" + (++instr2Count));
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

