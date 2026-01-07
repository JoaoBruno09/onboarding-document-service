## 🧩 Document Service

The Document Service is a dedicated microservice responsible for managing documents associated with customers and accounts within the banking onboarding system. It encapsulates all business rules related to document storage and deletion, ensuring that required documentation is consistently enforced across the account opening process. Designed according to Domain-Driven Design (DDD) and event-driven microservices principles, this service operates independently with its own database and communicates asynchronously with other services to maintain system-wide consistency.

## 🔍 Key Features

- Upload and storage of customer and account documents
- Replacement of existing documents by type
- Deletion of documents
- Event-driven coordination with Customer service
- Independent document persistence using the Database per Service pattern

## 🔗 API Endpoints
- PUT /documents - Upload customers and accounts documents.
- DELETE /documents - Delete customers and accounts documents.
  
## 👨‍💻 Technologies

<div style="display: inline_block"><br>
<img align="center" alt="Java" height="40" width="40" src="https://github.com/devicons/devicon/blob/master/icons/java/java-original.svg">
<img align="center" alt="Spring" height="40" width="40" src="https://github.com/devicons/devicon/blob/master/icons/spring/spring-original.svg">
<img align="center" alt="Docker" height="40" width="40" src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/docker/docker-original.svg" />
<img align="center" alt="PostgreSQL" height="40" width="40" src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/postgresql/postgresql-original.svg" />
</div>

## 📂 Repository Structure

The repository is organized as follows:

- `boot`: Module that includes the application startup.
- `services/src/main/java/com/bank/onboarding/accountservice/services`: Contains services and their implementation.
- `web/src/main/java/com/bank/onboarding/accountservice/controllers`: Contains all the controllers of the application.

## 📋 Prerequisites

- Java 17+
- Maven
- Docker
- PostgreSQL database instance (local or containerized)

## 🌟 Additional Resources

- [Master's dissertation](http://hdl.handle.net/10400.22/26586)
