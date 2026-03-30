# SOSApp - Emergency Response System 🚨
A robust, real-time Android emergency application designed to provide immediate assistance during critical situations. This app automates the process of notifying emergency contacts with location data via SMS and WhatsApp.

## 🌟 Key Features

- **One-Tap SOS:** Large, high-visibility button for instant activation.
- <img width="300" height="450" alt="image" src="https://github.com/user-attachments/assets/21ba6a8d-087b-43e2-8cdc-b67a5222334f" />

- **Shake Detection:** Built-in accelerometer support to trigger an SOS by shaking the phone—no screen interaction required.

-  <img width="300" height="450" alt="image" src="https://github.com/user-attachments/assets/2dd38198-e096-461a-af34-a030337d7abb" />
  <img width="300" height="600" alt="image" src="https://github.com/user-attachments/assets/939afc90-1d11-4c61-a20c-5f8f8f57207b" />

- **Multi-Channel Alerts:** Automatically sends emergency messages via **SMS** and **WhatsApp** simultaneously.
- **Live Location Tracking:** Integrates Google Maps API to send precise GPS coordinates to your saved contacts.
- **Smart Queueing:** If the user has no signal, the app caches the SOS request and sends it automatically once a network connection is restored.

- <img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/ab289992-e707-49d6-8b95-14fee2003cdc" />

- **Periodic Updates:** Automatically sends follow-up location updates every 5 minutes for 15 minutes.
- **Emergency Auto-Dial:** Initiates a phone call to primary contacts and a delayed auto-dial to emergency services (112).

- <img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/7c99d10c-8adf-4aaf-8e34-3d6422a8659c" />

- **Home Screen Widget:** A dedicated widget for triggering life-saving alerts without opening the app.

## 📸 Screen Specifications 

| Screenshot Type | Description |
|---|---|
| **Main Dashboard** | Show the large Red SOS Button and the Saved Contacts list. |
| **Permissions** | Show the system dialogs for Location, SMS, and Call permissions. |
| **Alert Sent** | Show the "SOS Active" status text at the bottom. |
| **Home Screen** | Show the SOS Widget active on your phone's launcher. |

## 🛠 Tech Stack & Permissions

- **Language:** Java
- **Architecture:** Android Activity Lifecycle, Sensor API, Location Services (GPS/Network).
- **Storage:** SharedPreferences for persistent contact management.
- **Permissions Required:**
  - `SEND_SMS`: To notify contacts without data.
  - `ACCESS_FINE_LOCATION`: For precise GPS coordinates.
  - `CALL_PHONE`: To initiate immediate voice calls.

## 🚀 How to Install & Run

Follow these steps to get a local copy of the project up and running:

### 1. Prerequisites
* **Android Studio Iguana** (or newer)
* **Android SDK 34** (Project targets API 34)
* **Gradle 8.0+**

### 2. Clone the Repository
Open your terminal or command prompt and run:
       "bash git clone https://github.com/sandeepnayak7880-creator/SOSApp.git"
### 3. Import into Android Studio
1. Open **Android Studio**.
2. Select **File > Open** and navigate to the cloned `SOSApp` folder.
3. Wait for the **Gradle Sync** to finish (this may take a few minutes as it downloads dependencies like `Material 1.11.0` and `Kotlin 2.2.10`).

### 4. Set Up Permissions
Since this is an SOS app, it requires specific permissions to function. After launching the app on your device:
* Allow **Location Access** (to send coordinates).
* Allow **SMS/Phone Access** (to trigger alerts).

### 5. Run the App
1. Connect a physical Android device (with **USB Debugging** enabled) or start an Emulator.
2. Click the **Green Play Button** (Run) in the top toolbar.
3. **To test Shake Detection:** Shake your physical device firmly. (On Emulator: Use the "Extended Controls" -> "Virtual Sensors" to simulate a shake).

---
   
