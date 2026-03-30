package com.example.sosapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView savedContactsText;
    private EditText contactInput;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "SOSApp";
    private static final String PREFS_NAME = "SOSPrefs";
    private static final String KEY_CONTACTS = "EmergencyContactsSet";

    private final Handler handler = new Handler();

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

        loadContacts();

        saveContactButton.setOnClickListener(v -> {
            String number = contactInput.getText().toString().trim();
            if (!number.isEmpty()) {
                addContact(number);
                contactInput.setText("");
                Toast.makeText(this, "Contact Added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }

            if (allGranted) {
                triggerSOS();
            } else {
                Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void triggerSOS() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());

        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please add at least one emergency contact!", Toast.LENGTH_LONG).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        StringBuilder messageBuilder = new StringBuilder("Emergency! I need help. ");
        if (location != null) {
            double lat = location.getLatitude();  //to get latitude
            double lon = location.getLongitude(); // to get longitude
            messageBuilder.append("My location: https://maps.google.com/?q=").append(lat).append(",").append(lon);
        } else {
            messageBuilder.append("Location not available.");
        }

        String message = messageBuilder.toString();
        ArrayList<String> contactList = new ArrayList<>(contacts);

        // 1. Send SMS to all
        for (String phone : contactList) {
            sendSMS(phone, message);
        }

        // 2. WhatsApp alerts
        // IMPORTANT: WhatsApp strictly forbids automated background sending (spam protection).
        // It WILL block your number if we bypass their security.
        // The safest way is to open WhatsApp for the first contact.
        if (!contactList.isEmpty()) {
            sendWhatsApp(contactList.get(0), message);
        }

        // 3. Make a Call to the first contact
        if (!contactList.isEmpty()) {
            makeCall(contactList.get(0));
        }

        // 4. Timer for 112 (after 60 seconds)
        statusText.setText("Alerts sent. Calling 112 in 60 seconds...");
        handler.postDelayed(() -> {
            makeCall("112");
        }, 60000);
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            Log.e(TAG, "SMS failed for " + phoneNumber, e);
        }
    }

    private void sendWhatsApp(String phoneNumber, String message) {
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // Using the modern 'wa.me' link format
            String url = "https://wa.me/" + cleanNumber + "?text=" + URLEncoder.encode(message, "UTF-8");
            intent.setPackage("com.whatsapp");
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp failed", e);
            Toast.makeText(this, "WhatsApp error", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeCall(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        }
    }
}
