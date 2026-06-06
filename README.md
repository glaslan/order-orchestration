# Order Orchestration (Farm2Fork)
 
The order orchestration service for Farm2Fork, an enterprise system that manages the journey of farm-fresh food from producers to buyers. Built as part of a multi-team enterprise software development course, where five teams of five each owned a separate module and integrated through REST APIs.
 
![Java](https://img.shields.io/badge/Java-Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![CI](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)
![Static Analysis](https://img.shields.io/badge/Static%20Analysis-PVS--Studio-orange)
 
## Overview
 
This service handles order orchestration, coordinating orders as they move through the Farm2Fork platform and exposing the data other modules depend on. It is one component of a larger enterprise system in which independently developed modules communicate over REST APIs, mirroring how real distributed teams build and integrate software.
 
My focus on this project was owning the CI/CD pipeline for the team: automating builds, tests, and deployments so that every change was validated and shippable, and integrating static analysis to keep code quality high across contributors.
 
## Key Features
 
* RESTful API for creating, tracking, and orchestrating orders across the wider Farm2Fork system.
* Relational data layer initialized from init.sql for reproducible database setup.
* Containerized deployment via Docker and Docker Compose for a one-command local environment.
* Automated CI/CD with GitHub Actions running builds and tests on every push.
* Static code analysis integrated with PVS-Studio to catch defects early.
## Tech Stack
 
| Layer | Technology |
|-------|-----------|
| Backend | Java, Spring Boot, Maven |
| Database | SQL (schema seeded via init.sql) |
| Frontend | SCSS, CSS, JavaScript, HTML |
| Infrastructure | Docker, Docker Compose |
| CI/CD and Quality | GitHub Actions, PVS-Studio |
 
## Architecture
 
Farm2Fork is composed of independent modules, each built by a separate team, that interact through REST APIs. This repository is the order orchestration module: it manages order data and serves it to the rest of the platform.
 
```
Buyers / Producers
        |
        v
+-------------------------+    REST APIs    +----------------------+
| Order Orchestration     | <-------------> | Other Farm2Fork      |
| (this service)          |                 | modules (inventory,  |
| Spring Boot + Local SQL |                 | payments, etc.)      |
+-------------------------+                 +----------------------+
        |
        v
  Main SQL Database
```
 
## Getting Started
 
### Prerequisites
 
* Docker and Docker Compose
* For local builds, JDK 17 or later. The project ships with the Maven wrapper, so a separate Maven install is not required.
### Run with Docker Compose
 
```bash
git clone https://github.com/glaslan/order-orchestration.git
cd order-orchestration
docker compose up --build
```
 
This builds the application image and starts the service alongside its test database, seeded from init.sql.
 
### Run locally with Maven
 
```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```
 
## Continuous Integration
 
GitHub Actions workflows (.github/workflows) automatically build and test the project on each push, and PVS-Studio static analysis runs against the codebase to surface potential defects before they reach main.
