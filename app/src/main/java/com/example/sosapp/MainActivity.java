package com.example.sosapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView statusText;
    private TextView savedContactsText;
    private EditText contactInput;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "SOSApp";
    private static final String PREFS_NAME = "SOSPrefs";
    private static final String KEY_CONTACTS = "EmergencyContactsSet";
    private static final String KEY_PENDING_SOS = "PendingSOS";

    private final Handler handler = new Handler();
    
    // Shake detection variables
    private SensorManager sensorManager;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private static final int SHAKE_THRESHOLD = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sosButton = findViewById(R.id.sosButton);
        statusText = findViewById(R.id.statusText);
        savedContactsText = findViewById(R.id.savedContactsText);
        contactInput = findViewById(R.id.contactInput);
        Button saveContactButton = findViewById(R.id.saveContactButton);
        Button clearContactsButton = findViewById(R.id.clearContactsButton);

        // Initialize Shake Detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        acceleration = 10f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        loadContacts();

        // Check for pending alerts from when there was no signal
        checkPendingAlerts();

        // Handle Widget Click Trigger
        if (getIntent().getBooleanExtra("trigger_sos", false)) {
            triggerSOS();
        }

        saveContactButton.setOnClickListener(v -> {
            String number = contactInput.getText().toString().trim();
            if (!number.isEmpty()) {
                addContact(number);
                contactInput.setText("");
                Toast.makeText(this, "Contact Added!", Toast.LENGTH_SHORT).show();
            }
        });

        clearContactsButton.setOnClickListener(v -> {
            clearContacts();
            Toast.makeText(this, "All contacts cleared", Toast.LENGTH_SHORT).show();
        });

        sosButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                triggerSOS();
            } else {
                requestPermissions();
            }
        });
    }

    private void checkPendingAlerts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String pendingTime = prefs.getString(KEY_PENDING_SOS, null);
        if (pendingTime != null && isNetworkAvailable()) {
            String now = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String msg = "Delayed SOS! Original trigger: " + pendingTime + ", Current time: " + now;
            Toast.makeText(this, "Sending queued alert...", Toast.LENGTH_LONG).show();
            
            // Trigger SOS with fresh location but mention the delay
            triggerSOSWithComment(msg);
            prefs.edit().remove(KEY_PENDING_SOS).apply();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void addContact(String number) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> contacts = new HashSet<>(prefs.getStringSet(KEY_CONTACTS, new HashSet<>()));
        contacts.add(number);
        prefs.edit().putStringSet(KEY_CONTACTS, contacts).apply();
        updateContactsUI(contacts);
    }

    private void clearContacts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(KEY_CONTACTS).apply();
        savedContactsText.setText("No contacts saved");
    }

    private void loadContacts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
        updateContactsUI(contacts);
    }

    private void updateContactsUI(Set<String> contacts) {
        if (contacts.isEmpty()) {
            savedContactsText.setText("No contacts saved");
        } else {
            StringBuilder sb = new StringBuilder("Saved Contacts:\n");
            for (String c : contacts) {
                sb.append(c).append("\n");
            }
            savedContactsText.setText(sb.toString());
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        lastAcceleration = currentAcceleration;
        currentAcceleration = (float) Math.sqrt(x * x + y * y + z * z);
        float delta = currentAcceleration - lastAcceleration;
        acceleration = acceleration * 0.9f + delta;

        if (acceleration > SHAKE_THRESHOLD) {
            triggerSOS();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void triggerSOS() {
        triggerSOSWithComment("");
    }

    private void triggerSOSWithComment(String comment) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());

        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please add a contact first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        StringBuilder messageBuilder = new StringBuilder();
        if (!comment.isEmpty()) {
            messageBuilder.append(comment).append("\n");
        } else {
            messageBuilder.append("Emergency! I need help. Time: ").append(currentTime).append("\n");
        }

        if (location != null) {
            messageBuilder.append("My location: https://maps.google.com/?q=").append(location.getLatitude()).append(",").append(location.getLongitude());
        } else {
            messageBuilder.append("Location not available.");
        }

        String message = messageBuilder.toString();
        ArrayList<String> contactList = new ArrayList<>(contacts);

        // Check for Signal/Network
        if (!isNetworkAvailable()) {
            // Save as pending if no signal
            prefs.edit().putString(KEY_PENDING_SOS, currentTime).apply();
            Toast.makeText(this, "No Signal. SOS Queued.", Toast.LENGTH_LONG).show();
        }

        // 1. Send SMS to all immediately (SMS might work even if data is off)
        for (String phone : contactList) {
            sendSMS(phone, message);
        }

        // 2. WhatsApp and Phone Call for primary
        if (!contactList.isEmpty()) {
            if (isNetworkAvailable()) {
                sendWhatsApp(contactList.get(0), message);
            }
            makeCall(contactList.get(0));
        }

        // 3. Start Periodic Updates (every 5 mins, 3 times)
        startPeriodicUpdates(contactList, message);

        // 4. Timer for 112 (after 60 seconds)
        statusText.setText("SOS Active. Updates every 5 mins. 112 in 60s.");
        handler.postDelayed(() -> makeCall("112"), 60000);
    }

    private void startPeriodicUpdates(ArrayList<String> contactList, String originalMessage) {
        for (int i = 1; i <= 3; i++) {
            final int count = i;
            handler.postDelayed(() -> {
                String updateMsg = "SOS Update #" + count + "\n" + originalMessage;
                for (String phone : contactList) {
                    sendSMS(phone, updateMsg);
                }
                Log.d(TAG, "Sent periodic update #" + count);
            }, i * 5 * 60 * 1000); // i * 5 minutes
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            Log.e(TAG, "SMS failed", e);
        }
    }

    private void sendWhatsApp(String phoneNumber, String message) {
        try {
            String url = "https://wa.me/" + phoneNumber.replaceAll("[^0-9]", "") + "?text=" + URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp failed", e);
        }
    }

    private void makeCall(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        checkPendingAlerts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
