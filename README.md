# EasyMove

## Overview 🏠
EasyMove is an Android app that connects customers who need a move with movers, supports move planning, and enables real-time chat and confirmations. It also offers a complete moving experience: search for movers, read reviews about them, then coordinate directly in the in-app chat, manage the move details, and add items with descriptions to track what has been packed and what is still open.

## Architecture 🏗️
The app follows an MVVM-style structure:
- View layer: `activities/` and `fragments/` under `app/src/main/java/com/example/easymove/view`.
- ViewModel layer: `app/src/main/java/com/example/easymove/viewmodel`, exposing LiveData for UI state.
- Data layer: repositories in `app/src/main/java/com/example/easymove/model/repository` for Firestore/Auth/Storage access.
- Models: data objects in `app/src/main/java/com/example/easymove/model`.
- Services: Firebase Messaging in `app/src/main/java/com/example/easymove/services`.

## Features ✅
- Authentication (email/password and Google Sign-In).
- Profile management for customers and movers.
- Move requests with status tracking and confirmation flow.
- Location-based mover search using GeoFire.
- Real-time chat and move confirmation in chat.
- Partner matching and notifications.
- Reviews, notifications, and inventory management screens.

## Project Structure 🗂️
```
EasyMove/
  app/
    src/main/
      AndroidManifest.xml
      java/com/example/easymove/
        adapters/
        model/
          repository/
        services/
        view/
          activities/
          fragments/
        viewmodel/
      res/
        layout/
        values/
        drawable/
        menu/
        xml/
  functions/
    index.js
    package.json
  gradle/
  build.gradle
  settings.gradle 
```

## Tech Stack 🛠️
- Android (Java, AndroidX, Material Components)
- Firebase: Auth, Firestore, Storage, Messaging, Analytics
- Google Maps + Places API
- GeoFire for geospatial queries

## Configuration ⚙️
- Firebase config: `app/google-services.json` is required.
- Maps API key: set `GOOGLE_MAPS_API_KEY` in `local.properties`.

Example `local.properties` entry:
```
GOOGLE_MAPS_API_KEY=YOUR_KEY_HERE
```

## How to Run 🚀
1. Ensure `app/google-services.json` is present.
2. Set `GOOGLE_MAPS_API_KEY` in `local.properties`.
3. Open the project in Android Studio and let Gradle sync.
4. Run the `app` configuration on an emulator or device.

## Firebase Functions ☁️
Cloud Functions are located in `functions/` and handle notifications for:
- New chat messages
- Partner requests and approvals
- Move confirmations and cancellations

## Team 👥
- Amit Bitton
- David Kitinberg
- Shira Ben Artzi
- Alaa Abu Hegly
