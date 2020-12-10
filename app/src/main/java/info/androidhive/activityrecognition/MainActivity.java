package info.androidhive.activityrecognition;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;


import static com.google.android.gms.location.DetectedActivity.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    TextView tv_steps;
    Button button;

    private TextView txtActivity, txtConfidence ;
    private ImageView imgActivity;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static String TEXT = "text";

    SensorManager sensorManager;
    boolean running = false;
    private int sensorEvent;
    int x = 100;
    private int stepsInSensor = 0;
    private int stepsAtReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, //Full Ekran yapmak için kullandığımız kod satırı 50 ve 51.satır
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        tv_steps =  findViewById(R.id.tv_steps);
        txtActivity = findViewById(R.id.txt_activity);
        txtConfidence = findViewById(R.id.txt_confidence);
        imgActivity = findViewById(R.id.img_activity);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        button = findViewById(R.id.button);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);

        final SharedPreferences prefs = this.getPreferences(Context.MODE_PRIVATE);  //presf adında bir shared preferences parametresi oluşturduk

        stepsAtReset = prefs.getInt("stepsAtReset", 0);    //stepsArReset'e 0 değerini atadık.

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stepsAtReset = stepsInSensor;     //stepsAtReset ve stepsInSensor ü birbirine eşitledik.

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("stepsAtReset", stepsAtReset);
                editor.commit();

                // you can now display 0:
                tv_steps.setText(String.valueOf(0));    //değeri sıfırladıktan sonra ekrana 0 yazdırdık.


            }
        });

    }


    private void handleUserActivity(int type, int confidence){
        String label = getString(R.string.activity_unknown);
        int icon = R.drawable.ic_still;

        switch (type) {
            case IN_VEHICLE: {

                label = getString(R.string.activity_in_vehicle);
                icon = R.drawable.ic_driving;
                sensorManager.unregisterListener(this);
                break;
            }
            case ON_BICYCLE: {

                label = getString(R.string.activity_on_bicycle);
                icon = R.drawable.ic_on_bicycle;
                sensorManager.unregisterListener(this);
                break;
            }
            case ON_FOOT: {
                label = getString(R.string.activity_on_foot);
                icon = R.drawable.ic_walking;
                Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (countSensor != null){
                    sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);

                }
                break;
            }
            case RUNNING: {
                label = getString(R.string.activity_running);
                icon = R.drawable.ic_running;
                Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (countSensor != null){
                    sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);

                }
                break;
            }
            case STILL: {

                label = getString(R.string.activity_still);
                icon = R.drawable.ic_still;
                sensorManager.unregisterListener(this);
                break;
            }
            case TILTING: {

                label = getString(R.string.activity_tilting);
                icon = R.drawable.ic_tilting;
                sensorManager.unregisterListener(this);
                break;
            }
            case WALKING: {
                label = getString(R.string.activity_walking);
                icon = R.drawable.ic_walking;
                Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (countSensor != null){
                    sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);

                }
                break;
                }
            case UNKNOWN: {

                label = getString(R.string.activity_unknown);
                break;
            }

        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            txtActivity.setText(label);
            txtConfidence.setText("Güvenilirlik: " + confidence);
            imgActivity.setImageResource(icon);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this,"Adım Sayar Devam Ediyor",Toast.LENGTH_LONG).show();

//        saveData();
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);

        }
        else{
            Toast.makeText(this,"Sensör bulunamadı.",Toast.LENGTH_LONG).show();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));


    }

    @Override
    protected void onPause() {

        running = false;
        super.onPause();


        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        sensorManager.unregisterListener(this);


    }




    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        if (running) {
            stepsInSensor = Integer.valueOf((int) sensorEvent.values[0]);
            int stepsSinceReset = stepsInSensor - stepsAtReset;    //ilk değerden son değeri çıkardık ve StepsSinceReset'e atadık
            tv_steps.setText(String.valueOf(stepsSinceReset));    //ekrana atanan değeri yazdırdık
        } else {
            sensorEvent.values[0] = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}
