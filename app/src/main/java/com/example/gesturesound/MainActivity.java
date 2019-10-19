package com.example.gesturesound;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static float SENSOR_SAMPLING_PERIOD = 10F;
    private LineGraphSeries<DataPoint> lineGraphSeries_forZ;
    private SensorManager sensorManager;
    private float currX = 0;
    private long newTime, startTime, prevTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = newTime = prevTime = System.currentTimeMillis();

        lineGraphSeries_forZ = new LineGraphSeries<>();
        final GraphView graphZ = (GraphView) findViewById(R.id.acclgraph);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        graphZ.getViewport().setYAxisBoundsManual(true);
        graphZ.getViewport().setMinY(-50);
        graphZ.getViewport().setMaxY(50);

        graphZ.getViewport().setXAxisBoundsManual(false);
        graphZ.getViewport().setMinX(1);


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
                if (Math.abs(event.values[2]) > 0.5) {
                    currX = event.values[2];
                } else {
                    currX = 0f;
                }
                lineGraphSeries_forZ.appendData(new DataPoint((newTime - startTime) / 10, currX), true, 10000, false);

            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}

