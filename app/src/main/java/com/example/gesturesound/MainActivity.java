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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static float SENSOR_SAMPLING_PERIOD = 10F;
    public static final int INSTRUMENT_BUFFER_SIZE = 8;

    int instr1Count = 0;
    int instr2Count = 0;

    private LineGraphSeries<DataPoint> lineGraphSeries_forZ;
    private SensorManager sensorManager;

    private String instrumentType = "DRUMS";

    private float currX;
    private long newTime, startTime, prevTime;
    private boolean playingInstrument1 = false, playingInstrument2 = false;

    private TextView xAcc, zAcc;
    private ArrayList<MediaPlayer> instr1, instr2, instr3;
    private int instr1Index = 0, instr2Index = 0, instr3Index = 0;
    private MediaPlayer instrument1, instrument2, instrument3;
    private Button instrument3Button, drumsButton, guitarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currX = 0;

        instr1 = new ArrayList<>();
        instr2 = new ArrayList<>();
        instr3 = new ArrayList<>();
        setInstruments();

        instrument3Button = findViewById(R.id.cymbals);
        drumsButton = findViewById(R.id.drums);
        guitarButton = findViewById(R.id.guitar);

        instrument3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInstrument3();
            }
        });
        drumsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instrumentType = "DRUMS";
                setInstruments();
            }
        });
        guitarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instrumentType = "GUITAR";
                setInstruments();
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
                //Log.d("TAGGING", xAcceleration + "x " + zAcceleration);
                if (Math.abs((xAcceleration)) > 1) {
                    currX = xAcceleration;
                    xAcc.setText("x = " + xAcceleration);
                    zAcc.setText("z = " + zAcceleration);
                    if (playingInstrument1 && xAcceleration > -20) {
                        playingInstrument1 = false;
                    } else if (xAcceleration < -20) {
//                        Log.d("TAG: ","SNARE x: "+xAcceleration+" z: "+zAcceleration);
                        playInstrument1();
                    }

                    if (playingInstrument2 && zAcceleration < 20) {
                        playingInstrument2 = false;
                    } else if (zAcceleration > 20) {
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

        //releasing any occupied resources
        for (int i =0; i< instr1.size(); i++) {
            instr1.get(i).release();
            instr2.get(i).release();
            instr3.get(i).release();
        }
        if (instrument1!= null) {
            instrument1.release();
            instrument2.release();
            instrument3.release();
        }
        instr1.clear();
        instr2.clear();
        instr3.clear();

        //initializing
        if (instrumentType.equals("DRUMS")) {
            for (int i = 0; i < INSTRUMENT_BUFFER_SIZE; i++) {
                instrument1 = MediaPlayer.create(this, R.raw.snare);
                instrument2 = MediaPlayer.create(this, R.raw.tomtom);
                instrument3 = MediaPlayer.create(this, R.raw.cymbals);

                instr1.add(instrument1);
                instr2.add(instrument2);
                instr3.add(instrument3);
            }
        } else if (instrumentType.equals("GUITAR")) {
            for (int i = 0; i < INSTRUMENT_BUFFER_SIZE; i++) {
                instrument1 = MediaPlayer.create(this, R.raw.guitarcmajor);
                instrument2 = MediaPlayer.create(this, R.raw.guitargmajor);
                instrument3 = MediaPlayer.create(this, R.raw.guitarfmajor);

                instr1.add(instrument1);
                instr2.add(instrument2);
                instr3.add(instrument3);
            }

        }

        //initial playing, to prevent later loading issues
        for (int i =0; i< INSTRUMENT_BUFFER_SIZE; i++) {
            instr1.get(i).start();
            instr2.get(i).start();
            instr3.get(i).start();
        }

    }

    private void playInstrument1() {
        Log.d("TAG:  ", "SNARE TRIGGERED");
        if (!playingInstrument1) {
            playingInstrument1 = true;
            instr1.get(instr1Index).start();
            instr1Index = (instr1Index + 1) % INSTRUMENT_BUFFER_SIZE;
            Log.d("TAG: ", "PLAYING SNARE" + (++instr1Count));
        }
    }


    private void playInstrument2() {
        Log.d("TAG:  ", "HIHAT TRIGGERED");
        if (!playingInstrument2) {
            playingInstrument2 = true;
            instr2.get(instr2Index).start();
            instr2Index = (instr2Index + 1) % INSTRUMENT_BUFFER_SIZE;
            Log.d("TAG: ", "PLAYING HANGING" + (++instr2Count)+"INDEX "+instr2Index);
        }
    }

    private void playInstrument3() {
        instr3.get(instr3Index).start();
        instr3Index = (instr3Index + 1) % INSTRUMENT_BUFFER_SIZE;
        Log.d("TAG: ", "PLAYING HANGING" + (++instr2Count));
    }

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

