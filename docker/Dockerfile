FROM adoptopenjdk:11.0.9.1_1-jre-hotspot

# Copy war file
COPY ebayWatcher-0.0.1-SNAPSHOT.jar /users.war

# Run the app
CMD ["/usr/bin/java", "-jar", "/users.war"]
