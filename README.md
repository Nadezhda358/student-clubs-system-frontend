# Student Clubs System Client

Frontend web client for the Student Clubs System. This repository contains the server-rendered UI built with Spring Boot, Thymeleaf, Spring Security, and OpenFeign. It talks to the companion backend API and presents the Bulgarian interface used by students, teachers, and administrators.

## Overview

The client is a browser application, not a SPA. It renders pages on the server, fetches data through Feign clients, and uses a small amount of JavaScript for shared UI behavior such as delete confirmations, pagination inputs, and page-specific interactions.

The application covers:

- Public browsing of clubs and events
- Student registration and login
- Teacher and admin management screens
- Membership application workflows
- Club main image and gallery media management
- Event and announcement management
- Admin reports and teacher invites

## Key Features

- Public home page with a custom hero section and branded landing layout
- Club directory with search, filters, pagination, and club detail pages
- Club detail tabs for announcements, events, and media
- Student self-service pages for event registrations and membership applications
- Teacher pages for assigned clubs, events, announcements, and participant views
- Admin pages for clubs, events, announcements, teacher invites, reports, and membership review
- Shared modal-based delete confirmation instead of browser `confirm()` dialogs
- Main club image replacement and club gallery media add/remove support
- Responsive layout and reusable CSS design tokens
- Bulgarian UI copy throughout the app

## Tech Stack

- Java 17
- Spring Boot 4.0.1
- Spring MVC / Web
- Thymeleaf
- Spring Security
- Spring Cloud OpenFeign
- Bean Validation
- Lombok
- Plain CSS and small browser-side JavaScript modules

## Repository Layout

- `src/main/java/.../clients` - OpenFeign clients for the backend API
- `src/main/java/.../controllers` - page controllers and route handlers
- `src/main/java/.../config` - security, app wiring, and shared configuration
- `src/main/java/.../dtos` - request and response models used by the client
- `src/main/java/.../enums` - client-side enums and display helpers
- `src/main/resources/templates` - Thymeleaf templates
- `src/main/resources/static/css` - page and component styles
- `src/main/resources/static/js` - shared browser-side behavior
- `src/main/resources/static/assets` - logo and landing page imagery

## Prerequisites

- Java 17 or newer
- A running companion backend API
- Maven Wrapper is included, so no separate Maven install is required

Optional but useful:

- MySQL for the backend
- SMTP server for teacher invitation emails
- AWS S3 credentials if you are testing image upload flows end to end

## Companion Backend

This repo depends on the backend service in the sibling repository `student-clubs-system-api`.

By default, the client expects the API at:

```text
http://localhost:8080
```

If your API runs elsewhere, set `APP_API_BASE_URL` before starting the client.

## Local Setup

### 1. Start the backend API

From the sibling backend repository:

```powershell
cd ..\student-clubs-system-api
.\mvnw.cmd spring-boot:run
```

If you want the backend in dev mode, follow that repository's README for its profile and infrastructure settings.

### 2. Start the client

From this repository:

```powershell
$env:APP_API_BASE_URL = "http://localhost:8080"
.\mvnw.cmd spring-boot:run
```

The client runs on port `8081` by default.

### 3. Open the app

```text
http://localhost:8081
```

## Configuration

Default runtime configuration lives in `src/main/resources/application.properties`.

| Key | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8081` | Client web port |
| `app.api.base-url` | `http://localhost:8080` | Backend API base URL |
| `spring.thymeleaf.cache` | `false` | Disable template caching during development |
| `spring.servlet.multipart.max-file-size` | `10MB` | Client-side upload ceiling |
| `spring.servlet.multipart.max-request-size` | `100MB` | Total multipart request limit |

### File Upload Note

The client allows fairly large uploads, but the backend currently enforces a smaller per-file limit. For club images and gallery media, keep individual files comfortably under `5MB` to avoid API-side rejections.

## Main Screens

### Public

- Home page
- Club directory
- Club details
- Events listing
- Login
- Student registration
- Teacher registration by invite

### Student

- My events
- My membership applications

### Teacher

- Managed clubs
- Managed events
- Managed announcements
- Event participants
- Club membership applications

### Admin

- Clubs management
- Events management
- Announcements management
- Teacher invites
- Membership applications
- Reports

## Security and Access

Route access is protected through Spring Security. Public pages are open, while teacher and admin pages are restricted to the matching role. The client also redirects unauthenticated users to the login page when they try to open protected screens.

## Media and UI Notes

- The home page uses `src/main/resources/static/assets/mainPageImage.png` as the main visual on the landing hero.
- Delete actions use a shared modal for consistent confirmation messaging.
- Club editing screens support both a main image and additional media images.
- The styling is built to stay readable on desktop and mobile, with a strong header surface so the navbar does not disappear into the hero image.

## Useful Commands

Build:

```powershell
.\mvnw.cmd clean package
```

Run tests:

```powershell
.\mvnw.cmd test
```

Run the app:

```powershell
.\mvnw.cmd spring-boot:run
```

## Troubleshooting

- If pages fail to load, confirm the backend API is running and `APP_API_BASE_URL` points to it.
- If image uploads fail, check the backend file-size limit first.
- If you change templates and do not see updates, make sure the app is running with Thymeleaf cache disabled, which is the default in this repo.
- If login redirects behave unexpectedly, check whether the request is hitting a protected route without an authenticated session.

## Related Backend Documentation

The backend repository contains the API, persistence, and infrastructure documentation for the full system. Use that README for database, SMTP, S3, and endpoint-specific details.
