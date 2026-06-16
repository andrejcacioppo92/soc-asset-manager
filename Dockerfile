# ===== FASE 1: BUILD =====
# uso un'immagine con Maven e JDK 21 per compilare il progetto
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# copio prima solo il pom.xml e scarico le dipendenze
# così Docker mette in cache questo strato e non riscarica tutto ad ogni modifica del codice
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ora copio il codice sorgente e compilo il JAR
# salto i test perché in fase di build dell'immagine non ho il database a disposizione
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== FASE 2: RUNTIME =====
# parto da un'immagine leggera con solo il JRE 21, non mi serve Maven né il JDK completo
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# creo un utente non-root per eseguire l'applicazione
# far girare i container come root è un rischio di sicurezza, meglio un utente dedicato
RUN groupadd -r spring && useradd -r -g spring spring

# copio solo il JAR prodotto nella fase di build, niente codice sorgente né strumenti
COPY --from=build /app/target/assetmanager-0.0.1-SNAPSHOT.jar app.jar

# l'applicazione gira come utente spring, non come root
USER spring

# documento la porta su cui gira l'applicazione
EXPOSE 8080

# comando di avvio dell'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]