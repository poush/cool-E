package com.ipiyush.agrawal.followme;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.icu.util.Output;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;


public class HomeActivity extends AppCompatActivity {

    private SensorManager accSensorManager;
    private Sensor accSensor;
    private RegisterListener accRegisterListener;
    private SensorManager magSensorManager;
    private Sensor magSensor;
    private RegisterListener magRegisterListener;

    private SensorManager stepDetectorManager;
    private Sensor stepDetector;
    private RegisterListener stepDetectorListener;
    private SensorManager stepCounterManager;
    private Sensor stepCounter;
    private RegisterListener stepCounterListener;

    private float accelCurrent = 0;
    private float accel = 0;

    private float[] x_accel = new float[15];
    private float[] y_accel = new float[15];
    private float[] z_accel = new float[15];

    private int accel_count = 0;

    private float[] x_mag = new float[15];
    private float[] y_mag = new float[15];
    private float[] z_mag = new float[15];

    private int mag_count = 0;

    private MoveRobot r;

    String ip;
    int port;

    private class MoveRobot extends AsyncTask<String, Void, Void> {

        String ip;
        int port;
        String msg;

        MoveRobot(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        private void write(String msg){
            this.msg = msg;
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                Socket socket = new Socket (ip, port);
                Toast.makeText(HomeActivity.this, "Hello", Toast.LENGTH_SHORT).show();
                while (socket.isConnected()){
                    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                    if (!msg.equals("")){
                        OutputStream out = socket.getOutputStream();
                        out.write(msg.getBytes());
                        msg = "";
                    }
                }
            } catch (Exception e){
                Toast.makeText(HomeActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d("jatin", "Hello");
        accSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = accSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accRegisterListener = new RegisterListener(new RegisterInterface() {
            @Override
            public void run(SensorEvent sensorEvent) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                if (accel_count < 15){
                    x_accel[accel_count] = x;
                    y_accel[accel_count] = y;
                    z_accel[accel_count] = z;
                    accel_count++;
                } else {

                    float average_x = 0;
                    float average_y = 0;
                    float average_z = 0;
                    for (int i=0; i<15; i++){
                        average_x += x_accel[i];
                        average_y += y_accel[i];
                        average_z += z_accel[i];
                    }

                    ((TextView) findViewById(R.id.moving)).setText("" + (int)average_x/15 + " - " + (int)average_y/15 + " - " + (int)average_z/15);
                    accel_count = 0;
                }

                float accelLast;

                accelLast = accelCurrent;
                accelCurrent = (float)Math.sqrt(x*x + y*y + z*z);
                float delta = accelCurrent - accelLast;
                accel = accel*0.9f + delta;
                Log.e("Jatin", ""+accel);
                if (Math.abs(accel) > 3){
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            lib.executePost("http://" + ip, "action=w");
                        }
                    });
                    thread.run();
                }



            }
        });
        magSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magSensor = magSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magRegisterListener = new RegisterListener(new RegisterInterface() {
            @Override
            public void run(SensorEvent sensorEvent) {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                if (mag_count < 15){
                    x_mag[mag_count] = x;
                    y_mag[mag_count] = y;
                    z_mag[mag_count] = z;
                    mag_count++;
                } else {

                    float average_x = 0;
                    float average_y = 0;
                    float average_z = 0;
                    for (int i=0; i<15; i++){
                        average_x += x_mag[i];
                        average_y += y_mag[i];
                        average_z += z_mag[i];
                    }

                    ((TextView) findViewById(R.id.moving)).setText("" + (int)average_x/15 + " - " + (int)average_y/15 + " - " + (int)average_z/15);
                    accel_count = 0;
                }

            }
        });
        stepCounterManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounter = stepCounterManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepCounterListener = new RegisterListener(new RegisterInterface() {
            @Override
            public void run(SensorEvent sensorEvent) {
                Log.d("jatin", "" + sensorEvent.values[0]);
            }
        });
        stepDetectorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetector = stepDetectorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        stepDetectorListener = new RegisterListener(new RegisterInterface() {
            @Override
            public void run(SensorEvent sensorEvent) {

            }
        });


        Button set = (Button) findViewById(R.id.set_button);
        Button forward = (Button) findViewById(R.id.forward);
        Button right = (Button) findViewById(R.id.right);
        Button left = (Button) findViewById(R.id.left);


        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ip = ((EditText) findViewById(R.id.ip)).getText().toString();
//                port = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
//                try {
//                    ip = ((EditText) findViewById(R.id.ip)).getText().toString();
//                    port = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
//                    r = new MoveRobot(ip, port);
//                    r.execute();
//                } catch (Exception e){
//                    Toast.makeText(HomeActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
//                }
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lib.executePost("http://"+ip, "action=w");
                    }
                });
                thread.start();
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lib.executePost("http://"+ip, "action=d");
                    }
                });
                thread.start();
            }
        });
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lib.executePost("http://"+ip, "action=a");
                    }
                });
                thread.start();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accSensorManager.registerListener(accRegisterListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        magSensorManager.registerListener(magRegisterListener, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
        stepCounterManager.registerListener(stepCounterListener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        stepDetectorManager.registerListener(stepDetectorListener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
//        magSensorManager.registerListener(magRegisterListener, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        accSensorManager.unregisterListener(accRegisterListener);
        magSensorManager.unregisterListener(magRegisterListener);
        stepCounterManager.unregisterListener(stepCounterListener);
        stepDetectorManager.unregisterListener(stepDetectorListener);
//        magSensorManager.unregisterListener(magRegisterListener);
    }

    private class RegisterListener implements SensorEventListener{

        RegisterInterface r;

        RegisterListener(RegisterInterface r){
            this.r = r;
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            r.run(sensorEvent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private interface RegisterInterface {
        void run(SensorEvent sensorEvent);
    }

}
