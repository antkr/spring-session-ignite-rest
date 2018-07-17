# Spring Session Ignite

[Spring Session](https://github.com/spring-projects/spring-session) Extension
--------------------------------

Spring Session Ignite is a [Spring Session](https://github.com/spring-projects/spring-session) extension that uses [Apache Ignite](https://ignite.apache.org/) for session storage and replication via REST.

## How to build
--------------------------------
* Clone this repository
* Run ``` ./gradlew clean install ``` in the project folder to build the project and install it to local Maven repo.

## Add `spring-session-ignite-rest` as a dependency to your project using:

##### Maven
```xml
<dependency>
    <groupId>com.apache.ignite</groupId>
    <artifactId>spring-session-ignite-rest</artifactId>
    <version>1.0</version>
</dependency>
```

##### Gradle
```groovy
compile group: 'org.apache.ignite', name: 'spring-session-ignite', version: '1.0'
```

Add `@EnableRestIgniteHttpSession` annotation to your Spring Boot application class and provide properties:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.apache.ignite.springsession.config.annotation.web.http.EnableRestIgniteHttpSession;

@EnableRestIgniteHttpSession(sessionCacheName = "session.cache.v2", igniteAddress = "localhost",
    ignitePort = "8080", maxInactiveIntervalInSeconds = 300)
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}