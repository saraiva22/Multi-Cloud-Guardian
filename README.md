# <image src ="docs/images/icon_cloud.png" width=50> Multi-Cloud Guardian

<a name="readme-top"></a>

---

<div align="center">
  <a href="docs/images/splash-screen.png">
    <img src="docs/images/Logo-MultiCloudGuardian-clean.png" alt="Multi-Cloud Guardian Splash" width="400">
  </a>
  <h1 align="center">Multi-Cloud Guardian</h1>
  <h2 align="center">Secure Multi-Cloud Storage for Sensitive Data</h2>

  <p align="center">
    Secure and manage your files with client-side, privacy-first encryption
    <br />
    <br />
    <br />

  </p>
</div>

---

## 📑 Table of Contents

- [About the Project](#about-the-project)
  - [Main Features](#main-features)
  - [Built With](#built-with)
  - [Backend Documentation](#-backend-documentation)
  - [Frontend Documentation](#-frontend-documentation)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Usage](#usage)
- [Architecture](#architecture)
- [License](#license)
- [Contact](#contact)

---

## 🔍 About The Project

**Multi-Cloud Guardian** is a privacy-focused application that enables secure and seamless file storage across multiple cloud providers. It gives users full control over their data by integrating robust client-side encryption with intelligent, dynamic storage orchestration, effectively abstracting provider-specific complexities while maintaining strong security and privacy guarantees.

### 🔐 Main Features

- Client-side encryption using AES with authenticated encryption (Encrypt-then-MAC)
- Seamless integration with multiple cloud providers: AWS S3, Google Cloud Storage, Azure Blob Storage, and Backblaze B2
- Cloud vendor abstraction through the use of `jclouds`, enabling provider-agnostic operations
- Core file operations: upload, download, deletion, and direct preview
- Customizable storage policies based on user preferences for location, performance, and cost
- Cross-platform frontend built with React Native and Expo
- Modular and scalable backend implemented in Kotlin using Spring Boot and Gradle

---

### 📚 Backend Documentation

This section provides links and context for exploring the backend codebase and its responsibilities.

- [**Backend Source Code**](./code/backend/multicloud-guardian/)  
  Contains all the backend code.

- [**Backend Architecture Overview**](./code/backend/docs/backend-impl.md)  
  Overview of the backend structure and module responsibilities.

---

### 📚 Frontend Documentation

This section provides links and context for exploring the frontend codebase and its responsibilities.

- [**Frontend Source Code**](./code/frontend/)  
  Contains all the frontend code.



---

## 🛠️ Built With

| Technology                                            | Description                                  |
| ----------------------------------------------------- | -------------------------------------------- |
| [Kotlin](https://kotlinlang.org/)                     | Programming language used in the backend     |
| [Spring Boot](https://spring.io/projects/spring-boot) | Framework for building the backend API       |
| [Gradle](https://gradle.org/)                         | Build automation and dependency management   |
| [jclouds](https://jclouds.apache.org/)                | Library for cloud provider abstraction       |
| [React Native](https://reactnative.dev/)              | Cross-platform framework for the frontend    |
| [Expo](https://expo.dev/)                             | Toolchain for developing React Native apps   |
| [TypeScript](https://www.typescriptlang.org/)         | Strongly typed language for the frontend     |
| [AES + HMAC](https://www.npmjs.com/package/crypto-js) | Authenticated client-side file encryption    |
| [Docker](https://www.docker.com/)                     | Containerization for development and testing |

---

## 🚀 Getting Started

Follow these instructions to set up the project locally.

## 📋 Prerequisites

1. You need to have node to run the application.

   [![NodeJS](https://img.shields.io/badge/node.js-6DA55F?style=for-the-badge&logo=node.js&logoColor=white)](https://nodejs.org/en)

2. Install npm globally on your machine:

```sh
npm install
```

3. Install Expo Go on your smart phone:

   [![App Store](https://img.shields.io/badge/App_Store-0D96F6?style=for-the-badge&logo=app-store&logoColor=white)](https://apps.apple.com/us/app/expo-go/id982107779)
   [![Play Store](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=host.exp.exponent&hl=en)

### 🧰 Installation

#### Backend

```bash
git clone https://github.com/saraiva22/Multi-Cloud-Guardian.git
cd Multi-Cloud-Guardian/code/backend/multicloud-guardian
docker-compose up -d
```

#### Frontend

```bash
cd Multi-Cloud-Guardian/code/frontend
npx expo start
```

📱 Scan the QR Code provided with your smart phone to run the app

# 📬 `Contact Us`

📥 Francisco Saraiva (Developer) - [a49462@alunos.isel.pt](mailto:a49462@alunos.isel.pt)

📥 José Simão(Supervisor) - [jose.simao@isel.pt](mailto:jose.simao@isel.pt)

<p align="right">(<a href="#readme-top">back to top</a>)</p>
