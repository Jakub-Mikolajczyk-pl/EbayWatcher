FROM openjdk:11

# Copy war file from my machine to dockerImage location
COPY ebayWatcher-0.0.1-SNAPSHOT.jar /users.war

# Run the app
CMD ["/usr/bin/java", "-jar", "/users.war"]
