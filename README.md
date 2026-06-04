# Aviation Pipeline ETL

## Overview
An Extract, Transform, and Load (ETL) data pipeline built from the ground up to process aviation data. I chose to use core Java and raw JDBC for this project to develop a deep, mechanical understanding of data engineering and database architecture without relying on heavy abstractions.

## Current Status
🚧 **Phase 1: Database Foundation (Completed)**
* The initial database schema and SQL scripts have been drafted and are available in the repository.

🔜 **Phase 2: Containerization (Completed)**
* Setting up the Docker environment to containerize the database and ensure consistent, isolated development workflows.

⚙️ **Phase 3: Extraction and Ingestion (In Progress)**
* Building a core Java/Maven application that utilizes native `HttpClient` for aviation REST API retrieval and raw JDBC for MySQL ingestion, intentionally bypassing high-level frameworks to build mechanical mastery of database connection management.

## Tech Stack
* **Language:** Java (Core)
* **Data Access:** JDBC
* **Database:** SQL 
* **Infrastructure:** Docker (Pending)

## Project Roadmap
- [x] **Step 1:** Draft SQL scripts and establish initial data models.
- [x] **Step 2:** Configure Docker and `docker-compose.yml` for local database hosting.
- [x] **Step 3:** Establish JDBC connections and basic Java application structure.
- [ ] **Step 4:** Implement data extraction and parsing logic.
- [ ] **Step 5:** Develop transformation rules and load mechanisms.

## Getting Started

### Prerequisites
To run this pipeline locally, ensure you have the following installed:
- Docker & Docker Compose (for managing database containers)
- Java SDK (Version 17 or higher recommended)
- Maven (for dependency management and building the application)
- DBeaver (or your preferred database management tool to inspect schemas and data)

### Installation & Setup
1. Database Infrastructure
   The pipeline relies on local Docker containers for both the raw data ingestion stage (MySQL) and the cleaned data storage stage (PostgreSQL).

    To spin up the databases, navigate to the project root directory and run:

    ```bash
    docker compose up -d
    ```
    The initial schema creation is handled automatically on startup via initialization scripts. You can inspect the underlying structures by connecting to the containers using DBeaver or by reviewing the raw scripts in the /sql directory.


2. Application Setup
   (Pending Step 3 completion: Instructions for compiling and running the raw JDBC Java application will be added here).


3. Verifying the Connection in DBeaver
   Before starting Step 3, it is highly recommended to open DBeaver and verify that your local connections are active:

    MySQL: Connect to localhost:3306 to verify the landing area for raw data.
    PostgreSQL: Connect to localhost:5432 to verify the target warehouse area.

Once DBeaver confirms both databases are online and accessible, your environment is fully prepared for the raw JDBC connection logic.

