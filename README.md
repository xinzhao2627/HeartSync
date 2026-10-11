# HeartSync ❤️

## About the App

HeartSync is an offline health tracking app built for students living with heart conditions like hypertension and arrhythmia. It connects the student with their healthcare provider on a single phone without requiring an internet connection or account login. The app gives students a simple way to track daily medication, complete healthy routines, save money for prescriptions, and write personal reflections.

## How It Works

HeartSync is designed around a single shared device setup that serves both the patient and the doctor.

1. Role Switching: Users can switch between Patient Mode and Doctor Mode from the top navigation bar or settings. The student manages daily habits and logs reflections, while the healthcare provider updates prescriptions, routines, and diet instructions.

2. Medication and Routine Management: Routines are split into morning, afternoon, and night tasks. Students check off tasks as they finish them. If a task was checked by mistake, they can edit or cancel the checkmark.

3. Dietary Directives: Doctors write simple lists of foods to eat and foods to avoid. Students can review these directives anytime to make safe meal choices at school.

4. Heart Reflection Journal: A built-in rich text editor lets students log daily resting pulse rates, struggles, and things they are thankful for. The editor supports lists, headers, quotes, and live text styling.

5. Medical Savings Tracker: Students can set up multiple savings funds for medicines and upcoming checkups. Each fund displays the target amount, current savings, deadline status, and quick deposit buttons.

6. Campus Weather Widget: The home screen displays local temperature and weather conditions to help students plan outdoor physical activity safely. It caches data so it still works when the internet drops.

7. Class Mode: When activated, alarms and sound effects switch to silent or vibration-only mode so notifications do not disturb lectures and study sessions.

## Database Schema

HeartSync stores all records locally using an SQLite database with twelve core tables.

1. patient_profile: Stores the student name, age, blood type, condition notes, and emergency contact details.

2. doctor_profile: Stores the assigned doctor name, medical specialty, clinic name, and contact phone number.

3. routine_items: Contains recurring habits with a title, time of day (morning, afternoon, night), target time, and active status.

4. routine_logs: Tracks daily routine completions by routine id, date key, and timestamp.

5. medications: Holds prescribed medications, dosage instructions, frequency, scheduled times, and doctor notes.

6. medication_logs: Records whether a specific medication dose was taken on a given date and time.

7. nutrition_guidelines: Stores food directives categorized as foods to eat or foods to avoid, along with doctor advice.

8. daily_journal_entries: Keeps daily reflections, mood scores (1 to 5), gratitude notes, struggles, rich text markdown content, and resting pulse numbers.

9. savings_goals: Manages individual savings funds with goal title, target amount, current amount, optional deadline date, and quick add preset values.

10. savings_transactions: Logs financial adjustments with goal id, amount, transaction type (ADD or SUBTRACT), note, and timestamp.

11. quotes_and_verses: Stores 365 daily encouraging quotes and Bible verses tied to the day of the year.

12. user_preferences: Saves local settings including the active role, class mode state, sound effect toggle, alarm toggles for routines and meds, and onboarding status.

## Compatibility

1. Minimum Operating System: Android 11 (API Level 30).
2. Target Operating System: Android 14 and Android 15 (API Level 37).
3. Network Requirement: 100% functional offline. Internet is only used optionally to refresh local weather forecasts.
4. Storage: Local device SQLite database. No external server, cloud subscription, or login account needed.

## Tools Used

1. Programming Language: Kotlin.
2. User Interface: Jetpack Compose with Material 3 components.
3. Architecture: Single Activity architecture using StateFlow and Kotlin Coroutines.
4. Database Engine: Native Android SQLiteOpenHelper with structured migrations and seed data.
5. Audio and Alarms: Android AlarmManager for exact reminders, NotificationManager for local alerts, and SoundPool for low latency button sounds.
6. Weather Integration: Open-Meteo REST API using Android HttpURLConnection and JSON parsing with local offline caching.

## Developer Section

Follow these steps to set up, build, and test HeartSync locally.

### Prerequisites

1. Android Studio Ladybug or newer.
2. Java Development Kit (JDK) 17.
3. Android SDK Platform 30 through 35.

### Project Setup

1. Open Android Studio and select File, then Open.
2. Choose the HeartSync project root folder.
3. Allow Gradle to download dependencies and sync the project files.

### Build Commands

Run these commands in PowerShell or your terminal from the project root.

1. Compile Kotlin source code:
```shell
.\gradlew.bat :app:compileDebugKotlin
```

2. Assemble the debug APK:
```shell
.\gradlew.bat :app:assembleDebug
```
The resulting package will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

3. Install on a connected Android phone or emulator:
```shell
.\gradlew.bat :app:installDebug
```

### Project Structure

1. app/src/main/java/com/stem/heartsync/data: Contains database helper classes, table contracts, models, and repository interfaces.
2. app/src/main/java/com/stem/heartsync/ui/screens: Houses Compose screen layouts for Home, Medication and Routines, Daily Journal, Savings, and Settings.
3. app/src/main/java/com/stem/heartsync/ui/components: Reusable UI widgets including the rich text editor, savings cards, and the weather card.
4. app/src/main/java/com/stem/heartsync/notification: Broadcast receivers and alarm management for time-based reminders.
5. app/src/main/java/com/stem/heartsync/audio: SoundPool sound player for app interactions.
6. app/src/main/java/com/stem/heartsync/weather: Weather fetcher, location checker, and offline caching layer.
