# Imagen base con Java 17
FROM eclipse-temurin:17-jdk

# Crear directorio de la app
WORKDIR /app

# Copiar el JAR fat generado por Maven
COPY target/myapp-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app/myapp.jar

# Ejecutar el JAR
CMD ["java", "-jar", "myapp.jar"]