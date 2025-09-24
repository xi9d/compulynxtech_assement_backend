# Compulynx - Full Stack Application
<img width="2555" height="1289" alt="full house" src="https://github.com/user-attachments/assets/d2c950bc-8b56-4dd0-9d52-93d29f9e7894" />

A modern full-stack web application built with Spring Boot, Angular 17, and PostgreSQL.

## 🚀 Tech Stack

### Backend
- **Spring Boot 3.x** - Java framework for building REST APIs
- **Spring Data JPA** - Data persistence layer
- **PostgreSQL** - Primary database
- **Maven** - Dependency management

### Frontend
- **Angular 17** - Modern web framework with standalone components
- **TypeScript** - Type-safe JavaScript
- **Angular Material** - UI component library
- **Netlify** - Frontend deployment


## 📋 Prerequisites

Before running this project, make sure you have the following installed:

- **Java 17+**
- **Node.js 18+** and npm
- **PostgreSQL 12+**
- **Maven 3.6+**
- **Angular CLI 17+**
- **Git**

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/xi9d/compulynxtech_assement_backend.git
cd compulynx
```

### 2. Backend Setup (Spring Boot)

#### Database Configuration
1. Create a PostgreSQL database:
```sql
CREATE DATABASE compulynx_db;
CREATE USER compulynx_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE compulynx_db TO compulynx_user;
```

2. Update `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/database_name
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
server.port=8080
server.servlet.context-path=/api

# CORS Configuration
cors.allowed.origins=http://localhost:4200
```

#### Run the Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend server will start at `http://localhost:8080`




