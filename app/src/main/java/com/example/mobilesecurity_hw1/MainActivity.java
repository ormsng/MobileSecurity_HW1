package com.example.mobilesecurity_hw1;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText passwordEditText;
    private Button loginButton;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean hasAccelerometerData = false;
    private boolean hasMagnetometerData = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConditionsAndLogin();
            }
        });
    }

    private boolean isDeviceFacingNorth() {
        float azimuth = 0;
        if (hasAccelerometerData && hasMagnetometerData) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);

            azimuth = (float) Math.toDegrees(orientation[0]);
            if (azimuth < 0) {
                azimuth += 360;
            }
        }
            // You can adjust the tolerance (e.g., 10 degrees) to make it more or less strict
            float tolerance = 10;
            return (azimuth >= 360 - tolerance || azimuth <= 0 + tolerance);
    }


    private void checkConditionsAndLogin() {

        if (!isBatteryPercentageCorrect()) {
            showToast("Incorrect password.");
            return;
        }

        if (!isDeviceFacingNorth()) {
            showToast("Device must face north for login.");
            return;
        }

        if (!isBrightnessUnder50()) {
            showToast("Device brightness must be under 50%.");
            return;
        }

        // All conditions are met, proceed with login
        showToast("Login successful.");
    }

    private boolean isBatteryPercentageCorrect() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level * 100 / (float) scale;

        String password = passwordEditText.getText().toString();
        return password.equals(String.valueOf(Math.round(batteryPercentage)));
    }


    private boolean isBrightnessUnder50() {
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            return brightness < 128; // Assuming brightness range is 0-255, 128 is 50% of the total range
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkConditionsAndLogin();
            } else {
                showToast("Location permission required.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
                hasAccelerometerData = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
                hasMagnetometerData = true;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    };
}