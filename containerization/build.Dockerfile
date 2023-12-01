FROM clojure:temurin-21-tools-deps-1.11.1.1413-bullseye-slim AS build
RUN mkdir -p /usr/local/4-clojure
WORKDIR /usr/local/4-clojure
COPY . .
RUN clojure -M:test
RUN clojure -T:build uber

FROM eclipse-temurin:21.0.1_12-jre-ubi9-minimal AS runtime
# Note: Can't use environment variables with WORKDIR
# So if you change RUNTIME_CTR_DIR, you need to update this line
WORKDIR /opt/app
COPY --from=build /usr/local/4-clojure/target/${JAR_FILE} .
EXPOSE ${PORT}
CMD java -jar ${RUNTIME_CTR_DIR}/${JAR_FILE}
