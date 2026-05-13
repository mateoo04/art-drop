# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/artdropapp-frontend

COPY artdropapp-frontend/package*.json ./
RUN npm ci

ARG VITE_API_BASE_URL=
ARG VITE_CLOUDINARY_CLOUD_NAME=
ARG VITE_CLOUDINARY_UPLOAD_PRESET=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_CLOUDINARY_CLOUD_NAME=${VITE_CLOUDINARY_CLOUD_NAME}
ENV VITE_CLOUDINARY_UPLOAD_PRESET=${VITE_CLOUDINARY_UPLOAD_PRESET}

COPY artdropapp-frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /workspace/ArtDrop

COPY ArtDrop/.mvn .mvn
COPY ArtDrop/mvnw ArtDrop/pom.xml ./
RUN chmod +x ./mvnw

COPY ArtDrop/src ./src
COPY --from=frontend-build /workspace/artdropapp-frontend/dist ./src/main/resources/static

RUN ./mvnw -B -ntp -Dmaven.test.skip=true -Djacoco.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=backend-build /workspace/ArtDrop/target/*.jar /app/app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
