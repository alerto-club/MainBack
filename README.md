<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0D1117&height=200&section=header&text=Telegram%20File%20Manager&fontSize=50&animation=fadeIn&fontColor=ffffff" alt="Header" />

  <p align="center">
    <strong>Автономный файловый сервер в Telegram с поддержкой приватных хранилищ и мгновенной синхронизацией.</strong>
  </p>

  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/Telegram_API-7.x-24A1DE?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram API">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</div>

---

### 🎯 Обзор проекта
**Telegram File Manager** — это специализированный бэкенд, предоставляющий удаленный доступ к файловой системе сервера через интерфейс Telegram-бота. Проект решает проблему безопасного и удобного обмена файлами без использования FTP-клиентов.

Система поддерживает **изоляцию пользователей** (Private Mode), автоматически синхронизирует состояние диска с базой данных и предотвращает доступ к системным файлам.

---

### 🏗 Архитектура системы
В проекте реализована логика **Direct Disk Sync** для обеспечения актуальности данных:

```mermaid
graph TD
    User((User)) -->|Long Polling| Bot[Telegram Service]
    Bot -->|Auth & Config| DB[(MySQL Database)]
    Bot <-->|Scan & Sync| Disk[Local Storage]
    
    subgraph "Storage Modes"
    Disk -->|SHARED| Public[/"/test_storage/"/]
    Disk -->|PRIVATE| Private[/"/test_storage/{username}/"/]
    end
```
---

### 🔥 Ключевые возможности

* **Two Operation Modes:**
* `SHARED` — Общее файловое пространство (как публичный FTP).
* `PRIVATE` — Автоматическое создание изолированной папки для каждого пользователя (`/alerto_club`, `/guest` и т.д.).


* **Lazy Synchronization:**
* **Ghost Buster:** Автоматическое удаление записей из БД, если файл был удален с диска вручную.
* **Auto Discovery:** Мгновенное обнаружение новых файлов, добавленных в папку сервера.


* **Security & Navigation:**
* **Jailbreak Protection:** Пользователь не может выйти выше своей корневой директории.
* **HTML Formatting:** Красивое отображение путей (`📂 alerto_club → folder → file.txt`).


* **Modern Stack:** Полный переход на **TelegramBots 7.x** (Spring Starter + OkHttp Client).

---

### 🛠 Технический стек

* **Language:** Java 21
* **Framework:** Spring Boot 3.2.5
* **Bot Lib:** TelegramBots Spring Boot Starter 7.2.1
* **Database:** MySQL (Hibernate / Spring Data JPA)
* **Config:** `spring-dotenv` (поддержка .env файлов)

---

### 🚀 Быстрый запуск

#### 1. Предварительная настройка

Создайте файл `.env` в корне проекта (рядом с `build.gradle`):

```env
# --- Database Configuration ---
DB_URL=jdbc:mysql://localhost:3306/files_bot_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
DB_USER=root
DB_PASS=root

# --- Telegram Bot Settings ---
BOT_NAME=MyFileManagerBot
BOT_TOKEN=123456:AAHE... (ваш токен)

# --- Application Settings ---
# Путь к папке хранилища
TARGET_ROOT_PATH=./test_storage
# Режим: PRIVATE (личные папки) или SHARED (общая)
APP_FILES_MODE=PRIVATE

```

#### 2. Запуск приложения

Сборка и запуск через Gradle Wrapper (установка Gradle не требуется):

```bash
# Windows
./gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun

```

#### 3. Использование

1. Откройте бота в Telegram.
2. Нажмите `/start`.
3. Если включен режим `PRIVATE`, бот создаст вашу личную папку и покажет её содержимое.

---
<div align="center">
<sub>Built with ❤️ by Alerto</sub>
</div>
