# Multi Cloud Guardian - Frontend Documentation

## 🗂️ Frontend Folder Structure

This project uses **React Native with Expo Router**, and follows a modular structure that separates screens, components, context, and services for better scalability and maintainability.

### 📁 `app/` Expo Router Navigation Structure

Contains all **screens** of the application, organized using **Expo Router**. This folder follows a file-based routing system.

- **`_layout.tsx`** – Defines the common layout for nested routes.
- **`index.tsx`** – The root screen (e.g., Home or Welcome).

#### 📁 `(auth)/` - Modal Screens

Holds **modal screens**, such as:

- `sign-in.tsx` – Screen for user authentication (login).
- `sign-up.tsx` - Screen for user registration (account creation).

Holds **modal screens**, such as:

- `create-file.tsx` – Modal for creating a new file.
- `create-folder.tsx` - Modal for creating a new folder.
- `create-invite.tsx`- Modal for creating a new invite to a shared folder.
- `preferences.tsx` - Modal for view user preferences.
- `received-invites.tsx` - Modal for viewing and managing received invitations to shared folders.
- `sent-invites.tsx` - Modal for viewing and managing sent invitations to shared folders.
- `signup-success.tsx` – Modal shown after successful signup.
- `storage-details.tsx` – Modal for displaying detailed storage information, such as usage summaries or breakdowns by category.

#### 📁 `(tabs)/` - Tab Navigation Screens

Contains the main **tab-based navigation screens**, such as:

- `home.tsx` - Dashboard or home screen.
- `profile.tsx` - User profile screen.
- `files.tsx` - File browsing screen.
- `folders.tsx` – Folder browsing screen.
- `plus.tsx` – Quick actions (create file and create folder).

Each file represents a screen displayed as a tab.

#### 📁 `files/` and 📁 `folders/` - Deep linking

Dedicated to **handling file and folder navigation** (deep linking). Each screen here enables browsing of files and folders using Expo Router paths.

- `[id].tsx` – Dynamic screen for viewing details of a specific file or folder.

#### 📁 `search/` - Search Feature

Screen(s) related to the **search feature**, where users can search files or folders.

- `[query].tsx` – Displays search results.

---

### 📁 `assets/` - Static Resources

Static resources used by the app.

- **fonts/** – Custom fonts used in the UI.
- **icons/** – App-specific or reusable SVG/PNG icons.
- **images/** – App logos, backgrounds, or splash images.

---

### 📁 `components/` – Reusable UI Components

Reusable **UI components** such as buttons, cards, inputs, headers, etc. Written in `.tsx`.

---

### 📁 `constants/` - Shared Constants

Centralized location for constants used throughout the app.

- `icons.ts` – Icon definitions or references.
- `images.ts` – Image paths or mappings.
- `index.ts` – Exports all constants.
- `months.ts` – List of months (for date handling).

---

### 📁 `context/` - React Contexts

Contains **React Contexts** for global state management.

- `AuthProvider.tsx` – Manages authentication state, login status, user information and keyMaster.

Used to provide access to login state and current user details throughout the app.

---

### 📁 `domain/` – Core Domain Models

This folder can be used to define **core domain models** or types/interfaces (if any are separated from API or services).

- `credentials` – types and interfaces related to authentication and access tokens
- `preferences` – user preferences and settings models
- `storage` – models for files, folders, and storage items
- `user` – user account and profile types
- `utils` – shared utility types and helpers for domain logic

---

### 📁 `hook/` – Custom React Hooks

Contains **custom React hooks** to encapsulate and reuse logic.

---

### 📁 `services/` – Core Application Logic

Implements the core **application logic** that communicates with the backend or handles data manipulation.

- `media/` – Media-related API logic (e.g., Problem).
- `notifications/` - Managing notifications, implementing the SSE manager (Server Sent Events) for real-time updates.
- `security/` – Authentication, encryption, session handling.
- `storage/` – Storage management (e.g., cloud provider interaction, secure storage).
- `users/` – User-related services (e.g., profile, preferences).
- `utils/` – Utility functions.

Files like `HttpService.ts` and `Function.ts` support HTTP communication and helper logic.

---

### 📄 Root Files

- `README.md` – Project documentation.
- `app.json`, `expo-env.d.ts`, `metro.config.js`, etc. – Expo and build configurations.
- `package.json` – Node dependencies.
- `tsconfig.json` – TypeScript configuration.
- `tailwind.config.js` – Tailwind CSS setup for styling.
