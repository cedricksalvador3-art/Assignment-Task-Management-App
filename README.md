# DueWise – Assignment & Task Management Application

DueWise is a modern, high-performance, and feature-complete Android application designed for students and professionals to manage assignments, track deadlines, monitor overall completion progress, and stay organized. The interface strictly adheres to the provided DueWise visual design system with a clean dark navy blue and indigo color scheme, light gray cards, rounded input fields, clear visual status indicators, and persistent bottom navigation.

---

## 📱 Screenshots & Visual Features

DueWise implements all 10 core screens matching the visual design guide:

1. **Welcome / Login Screen**: Dark navy header, custom task illustration graphic, rounded Email and Password inputs with eye toggle, "Forgot Password?" link, and "Don't have an account? Sign Up." link.
2. **Account Lockout Dialog**: Modal alert popup ("Account Temporarily Locked in DueWise - Wait 5 minutes before trying again.") triggered after 3 consecutive failed login attempts.
3. **Dashboard Screen**: Personalized greeting ("Good Morning, Jane!"), dynamic current date ("Monday, Oct 26"), header icons (Add, Notifications, Settings), 4 summary cards (Total Tasks: 28, Pending: 15, Completed: 11, Overdue: 2), "My Progress" circular progress ring (65%), "Upcoming Deadlines" task list, "See all" link, and Floating Action Button (+).
4. **Create New Task / Edit Task Screen**: Clean form with Title, Description, Subject autocomplete dropdown, Date/Time pickers, Priority segmented buttons (Low, Medium, High), Status radio group, deadline reminder toggle, and Save/Cancel buttons.
5. **My Assignments Screen**: Search bar, filter pill tabs (All, Pending, In Progress, Completed), sorting selector (Due Date, Priority, Title), assignment cards with right arrows, empty state handling, and FAB (+).
6. **Task Details Screen**: Detailed task summary view with Title, Description, Subject, Due Date, Priority badge (HIGH, MEDIUM, LOW), Status badge (PENDING, IN_PROGRESS, COMPLETED, OVERDUE), Edit Task button, Update Status dialog, Mark as Completed button, and Delete Task button.
7. **Delete Confirmation Dialog**: Modal popup dialog ("Delete Task? Are you sure you want to delete 'History Essay'?") with Cancel and red Delete buttons.
8. **Progress Monitoring Screen**: Large circular progress chart (65%), "Tasks by Status" section (Completed: 11, Pending: 15, Overdue: 2 with horizontal progress bars), and "Tasks by Priority" section (High: 8, Medium: 12, Low: 8 with horizontal progress bars).
9. **Notifications & Reminders Screen**: "My Reminders" section listing upcoming deadline notifications ("Upcoming: History Essay due tomorrow") and overdue alerts ("Task Overdue: Physics Lab Report"). Tapping any notification opens the corresponding Task Details screen.
10. **Profile & Account Settings Screen**: User profile card ("DueWise", "jane.doe@email.com"), Account Settings section with interactive toggle switches for Notification Preferences, Reminder Settings, and Alert Settings, red Logout button, and Logout Confirmation Dialog ("Logout? Are you sure you want to log out of DueWise?").

---

## 🛠️ Features & Technical Requirements

- **Architecture**: Modular MVVM pattern with separation between UI (Activities, Fragments, Adapters), Data Repositories, DAOs, and SQLite Database.
- **Persistent Local Database**: Built using SQLite with foreign keys, index optimization, and background coroutine dispatching. Automatic pre-population of 28 sample tasks for the default user (`jane.doe@email.com` / `Password123`) on initial launch.
- **Dynamic Overdue Classification**: Unfinished tasks with past due dates (`dueDate < currentTime`) are automatically classified and highlighted as Overdue in red.
- **Authentication & Security**: Account login/registration, password hashing, session management via SharedPreferences, inline error validation, password mismatch warning, and automatic 3-attempt account lockout modal dialog.
- **Scheduled Local Notifications & Alarms**: AlarmManager & NotificationManager integration to send exact deadline reminder notifications.
- **Interactive UI Components**: Material3 components, BottomNavigationView with active tab highlighting, ChipGroup filter tabs, segmented RadioButtons, Circular and Horizontal ProgressBars, MaterialAlertButtons, and custom shape drawables.

---

## 🚀 Getting Started & Installation Instructions

### Prerequisites
- **Android Studio**: Jellyfish / Koala or newer
- **Android SDK**: API Level 24 (Android 7.0) minimum, API Level 34/37 compiled
- **JDK**: Java 11 or higher
- **Gradle**: 8.x (bundled with project wrapper)

### Build & Run
1. Clone the project repository or download the source code.
2. Open Android Studio and choose **File > Open**, then select the project root folder.
3. Allow Gradle to sync dependencies.
4. Connect an Android emulator or physical device (Android 7.0+).
5. Click **Run > Run 'app'** or execute `./gradlew assembleDebug` in the terminal.

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```
Or run instrumented UI tests:
```bash
./gradlew connectedAndroidTest
```

---

## 👤 Sample Demo Account Credentials

- **Email**: `jane.doe@email.com`
- **Username**: `janedoe`
- **Password**: `Password123`

---

## 📄 License & Version Info

- **Version**: 1.0.0
- **App Name**: DueWise – Assignment/Task Management Application
