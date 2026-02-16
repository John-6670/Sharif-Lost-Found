#!/bin/bash
cd backend/spring-api/nexus
mvn clean package
java -jar target/nexus-0.0.1-SNAPSHOT.jar
