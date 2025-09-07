# TrackIt - Expense Tracker App

A simple and beautiful Android app built with modern tools to help users track their daily expenses. This project focuses on a clean UI, a reactive architecture, and a great user experience.

## 📱 screenshorts
![Screenshot_20250907_102806.png](../../Desktop/Screenshot_20250907_102806.png)
![Screenshot_20250907_102825.png](../../Desktop/Screenshot_20250907_102825.png)
![Screenshot_20250907_102844.png](../../Desktop/Screenshot_20250907_102844.png)
![Screenshot_20250907_102914.png](../../Desktop/Screenshot_20250907_102914.png)
![Screenshot_20250907_103009.png](../../Desktop/Screenshot_20250907_103009.png)

## ✨ Features
- **Expense Tracking**: Add, delete, and view expenses.
- **Budget Management**: Set and update a monthly budget.
- **Data Persistence**: Expenses and budget are saved on the device and are not lost when the app is closed.
- **Dynamic UI**:
    - A splash screen on startup.
    - Tab-based navigation for Home and All Expenses.
    - A custom progress bar to visualize the budget.
    - UI's colour will change based on the theme applied on phone
- **Search and Filter**:
    - Search for expenses by keyword.
    - Search results are automatically grouped by month.
    - View all expenses and filter them by month.
- **Custom Design**:
    - Custom font and a permanent dark theme.
    - Custom icons and animated UI elements.

## 🛠️ Technologies Used
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow
- **Navigation**: Navigation-Compose
- **Data Persistence**: SharedPreferences with Gson
- **Asynchronous Programming**: Kotlin Coroutines