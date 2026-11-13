# Campusly

**Your Campus. Your Community. Your Voice.**

Campusly is a social app for NMIMS Indore students. You can share campus news, events, sports, study help and anonymous gossip. Only students with a college email address can join. The design is black with neon lime, red and green.

---

## Features

- **College-only sign up.** Accounts need an `@nmims.in` email address.
- **Feed with categories:** Events, Sports, Education, Gossip and Others.
- **Posts:** like, comment, share, and delete your own.
- **Anonymous gossip.** Gossip posts, and the comments on them, show "Anonymous". Only admins can see who wrote them.
- **Explore.** Trending posts, plus search by text, name or category.
- **Real-time notifications.** You're notified when someone likes or comments on your post. New ones fly across the screen as a shooting star.
- **Profiles.** Name, School (STME, SOC, SPTM, SBM, SOL), Year (1–5) and a bio. All of these can be edited.
- **Offline support.** The feed is cached so you can read it offline. Unsent posts are saved as drafts.
- **Campus weather.** A live weather card for Indore, from Open-Meteo.
- **Moderation.** Students can report posts. Admins get a dashboard with stats and can dismiss reports or remove posts.
- **Server-side security.** Firestore security rules check every read and write, not just the app.

---

## Tech stack

| Part | Technology |
|---|---|
| Language | Java |
| UI | XML layouts, ViewBinding, Material Components |
| Login | Firebase Authentication (email and password) |
| Database | Cloud Firestore |
| Offline storage | Room (SQLite) |
| Networking | Retrofit and Gson |
| Weather API | [Open-Meteo](https://open-meteo.com) (no key needed) |
| Min / Target SDK | 24 / 34 |

---

## Project structure

```
app/src/main/java/com/example/campusly/
├── CampuslyApp.java        App start-up (switches on notification popups)
├── activities/             Every screen (Login, Main, Explore, CreatePost, Profile, Admin…)
├── adapters/               RecyclerView adapters (posts, comments, notifications, reports)
├── api/                    Retrofit weather API
├── database/               Room database, DAOs and entities
├── models/                 Data classes (Post, Comment, Report, AppNotification…)
├── repositories/           Firestore, Room and weather data access
├── utils/                  Helpers (AppConfig, Categories, popups, network…)
└── views/                  Custom ShootingStarView

app/src/main/res/           Layouts, drawables, colours, strings, styles
firestore.rules             Firestore security rules
firestore.indexes.json      Firestore indexes
```

---

## Setup

### 1. Clone and open
```bash
git clone https://github.com/Khushi-Patel15/Campusly.git
```
Open the folder in **Android Studio**, then wait for the Gradle sync to finish.

### 2. Connect Firebase
1. Create a project in the [Firebase Console](https://console.firebase.google.com).
2. Add an Android app with the package name `com.example.campusly`.
3. Download `google-services.json` and put it in the `app/` folder.
4. Go to **Authentication → Sign-in method** and turn on **Email/Password**.
5. Create a **Firestore Database**.

### 3. Security rules and index
1. In **Firestore → Rules**, paste the contents of `firestore.rules`.
2. Make sure the email line reads `matches('.*@nmims[.]in')`. It must match `AppConfig.COLLEGE_EMAIL_DOMAIN`.
3. Click **Publish**.
4. In **Firestore → Indexes**, add a composite index on the `posts` collection: `category` Ascending and `createdAt` Descending. If you skip this, the app shows a link in Logcat that creates the index for you.

### 4. Run
Connect a phone with USB debugging turned on, or start an emulator, then press **Run ▶**.

---

## Making someone an admin

1. In Firestore, create a top-level collection called `admins`.
2. Add a document whose **ID is the user's UID**. You can find the UID under **Authentication → Users**.
3. Give it a field such as `addedBy`. Its value doesn't matter.

That user now sees the **Admin Dashboard** button on their profile. Nobody can add admins from inside the app.

---

## Known limitations

- Some college mail servers block Firebase's emails, so **password reset emails may not arrive**. Email verification is turned off for the same reason.
- Notifications only appear while the app is open. There are no push notifications when the app is closed.

---

## Author

~ Khushi Patel

---


