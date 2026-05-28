# 🎓 FAMS (Freshman Academic Management System)

**FAMS** is a robust, role-based mobile academic management ecosystem designed to streamline university life for freshmen. Built with **Kotlin**, **Jetpack Compose**, and **Firebase**, it provides a real-time platform for coordination, learning, and communication.

## 🚀 Key Features
*   **Smart Scheduling:** Constraint-based engine that generates conflict-free timetables.
*   **Real-time Interaction:** Instant chat and push notifications via Firestore and FCM.
*   **Academic Lifecycle:** End-to-end management of attendance, assignments, and auto-graded quizzes.
*   **Resource Repository:** Centralized cloud storage for lecture notes and academic materials.
*   **Role-Based Dashboards:** Specialized interfaces for Coordinators, Teachers, Students, and Class Reps.

## 🏗️ Technical Architecture
*   **MVVM Pattern:** Strict separation of UI and business logic for maintainability.
*   **Why NoSQL (Firestore)?** We chose Firestore over traditional SQL to leverage live data synchronization and seamless offline persistence, ensuring students stay updated even with spotty connectivity.
*   **Firebase Storage:** Large binary files (PDFs, media) are stored in object storage to keep the database lightweight and high-performing.

## 🛠️ Tech Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Modern Declarative UI)
*   **Backend:** Firebase (Auth, Firestore, Storage, Messaging)
*   **Architecture:** MVVM + Clean Architecture Principles

## 👥 Contributors
1. Simon Ahmed
2. Sosina Amare
3. Robel Bahiru
4. Ephrem Fasil
5. Selman Ebrahim
6. Znabu Tafese