# 3D Lottery Android App

Native Android app for 3D lottery management with SQLite Room database.

## Features
- ✅ Native Android app (Kotlin)
- ✅ SQLite Room database for data persistence
- ✅ Exact HTML lottery parsing logic
- ✅ Voucher management with history
- ✅ Profit calculation with commission
- ✅ Material Design UI

## Build Instructions

### Option 1: GitHub Actions (Recommended)
1. Push this repository to GitHub
2. GitHub Actions will automatically build the APK
3. Download APK from Actions → Artifacts

### Option 2: Local Build
```bash
./gradlew :app:assembleDebug
```

## App Features
- **Batch Input Parsing**: Supports formats like `123.231=100*100`, `123=100r50`
- **SQLite Database**: Room database for voucher storage
- **Real-time Calculations**: Commission, profit/loss calculations
- **History Tracking**: View all vouchers with timestamps

## Project Structure
```
app/src/main/java/com/example/lotteryapp/
├── MainActivity.kt              # Main activity with UI logic
├── data/
│   ├── LotteryDatabase.kt       # Room database setup
│   ├── Voucher.kt              # Voucher entity
│   ├── LotteryItem.kt          # Lottery item entity
│   ├── VoucherDao.kt           # Database operations
│   └── LotteryItemDao.kt       # Item operations
```

## Requirements
- Android SDK 34+
- Java 17
- Kotlin 1.9.20

## License
MIT License