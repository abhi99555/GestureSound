package com.example.gesturesound;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.polyak.iconswitch.IconSwitch;

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

    private ArrayList<MediaPlayer> instr1, instr2, instr3;
    private int instr1Index = 0, instr2Index = 0, instr3Index = 0;
    private MediaPlayer instrument1, instrument2, instrument3;
    private Button instrument3Button, instrument1Button, instrument2Button, interactiveSwitcher;
    private IconSwitch instrumentSwitch;
    private boolean interactive = false;

    private BarVisualizer soundVisualizer;
    private int audioSessionId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestAudioPermissions();


    }

    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            init();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    init();
                } else {
                    requestAudioPermissions();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void init() {
        currX = 0;

        soundVisualizer = findViewById(R.id.visualizer);
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioSessionId = audioManager.generateAudioSessionId();
        if (audioSessionId != AudioManager.ERROR) {
            soundVisualizer.setAudioSessionId(audioSessionId);
        }


        instr1 = new ArrayList<>();
        instr2 = new ArrayList<>();
        instr3 = new ArrayList<>();
        setInstruments();

        instrument3Button = findViewById(R.id.instrument3);
        instrument1Button = findViewById(R.id.instrument1);
        instrument2Button = findViewById(R.id.instrument2);
        interactiveSwitcher = findViewById(R.id.interactive_switcher);
        instrumentSwitch = findViewById(R.id.icon_switch);


        instrument1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInstrument1();
                playingInstrument1 = false;
            }
        });
        instrument2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInstrument2();
                playingInstrument2 = false;
            }
        });

        instrument3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInstrument3();
            }
        });

        interactiveSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (interactive) {
                    instrument1Button.setVisibility(View.GONE);
                    instrument2Button.setVisibility(View.GONE);
                    interactiveSwitcher.setText("Switch to Interactive Display");

                } else {
                    instrument1Button.setVisibility(View.VISIBLE);
                    instrument2Button.setVisibility(View.VISIBLE);
                    interactiveSwitcher.setText("Switch to Hand Gestures");
                }
                interactive = !interactive;
            }
        });
        instrumentSwitch.setCheckedChangeListener(new IconSwitch.CheckedChangeListener() {
            @Override
            public void onCheckChanged(IconSwitch.Checked current) {
                if (current == IconSwitch.Checked.LEFT) {
                    instrumentType = "GUITAR";
                    setInstruments();
                } else {
                    instrumentType = "DRUMS";
                    setInstruments();
                }
            }
        });

        startTime = newTime = prevTime = System.currentTimeMillis();
        lineGraphSeries_forZ = new LineGraphSeries<>();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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
                    if (playingInstrument1 && xAcceleration > -20) {
                        playingInstrument1 = false;
                    } else if (xAcceleration < -20) {
//                        Log.d("TAG: ","SNARE x: "+xAcceleration+" z: "+zAcceleration);
                        if (!interactive) playInstrument1();
                    }

                    if (playingInstrument2 && zAcceleration < 20) {
                        playingInstrument2 = false;
                    } else if (zAcceleration > 20) {
                        if (!interactive) playInstrument2();
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
        for (int i = 0; i < instr1.size(); i++) {
            instr1.get(i).release();
            instr2.get(i).release();
            instr3.get(i).release();
        }
        if (instrument1 != null) {
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
                instrument1 = new MediaPlayer();
                instrument1.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.snare);
                    instrument1.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument1.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instrument2 = new MediaPlayer();
                instrument2.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.tomtom);
                    instrument2.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument2.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instrument3 = new MediaPlayer();
                instrument3.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.cymbals);
                    instrument3.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument3.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instr1.add(instrument1);
                instr2.add(instrument2);
                instr3.add(instrument3);
            }
        } else if (instrumentType.equals("GUITAR")) {
            for (int i = 0; i < INSTRUMENT_BUFFER_SIZE; i++) {
                instrument1 = new MediaPlayer();
                instrument1.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.guitarcmajor);
                    instrument1.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument1.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instrument2 = new MediaPlayer();
                instrument2.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.guitargmajor);
                    instrument2.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument2.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instrument3 = new MediaPlayer();
                instrument3.setAudioSessionId(audioSessionId);
                try {
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.guitardmajor);
                    instrument3.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    instrument3.prepare();
                } catch (Exception e) {
                    Log.d("TAGTAG", e.getMessage());
                }

                instr1.add(instrument1);
                instr2.add(instrument2);
                instr3.add(instrument3);
            }

        }

        //initial playing, to prevent later loading issues
        for (int i = 0; i < INSTRUMENT_BUFFER_SIZE; i++) {
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
            Log.d("TAG: ", "PLAYING HANGING" + (++instr2Count) + "INDEX " + instr2Index);
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

        if (sensorManager != null)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null)
        sensorManager.unregisterListener(this);
    }
}

