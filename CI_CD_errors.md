docker-build
failed 2 minutes ago in 50s

1s
1s
2s
43s
Run ./mvnw -B test
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.facturx:backend >-------------------------
[INFO] Building  0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- dependency:3.10.0:properties (default) @ backend ---
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ backend ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- compiler:3.15.0:compile (default-compile) @ backend ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 85 source files with javac [debug parameters release 21] to target/classes
[INFO] /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java: /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java uses or overrides a deprecated API.
[INFO] /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ backend ---
[INFO] Copying 7 resources from src/test/resources to target/test-classes
[INFO] 
[INFO] --- compiler:3.15.0:testCompile (default-testCompile) @ backend ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 16 source files with javac [debug parameters release 21] to target/test-classes
[INFO] 
[INFO] --- surefire:3.5.6:test (default-test) @ backend ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.facturx.app.BackendApplicationTests
15:58:35.650 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.BackendApplicationTests]: BackendApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:35.715 [main] INFO org.testcontainers.images.PullPolicy -- Image pull policy will be performed by: DefaultPullPolicy()
15:58:35.717 [main] INFO org.testcontainers.utility.ImageNameSubstitutor -- Image name substitution will be performed by: DefaultImageNameSubstitutor (composite of 'ConfigurationFileImageNameSubstitutor' and 'PrefixingImageNameSubstitutor')
15:58:35.744 [main] INFO org.testcontainers.DockerClientFactory -- Testcontainers version: 2.0.5
15:58:36.814 [main] INFO org.testcontainers.dockerclient.DockerClientProviderStrategy -- Found Docker environment with local Unix socket (unix:///var/run/docker.sock)
15:58:36.823 [main] INFO org.testcontainers.DockerClientFactory -- Docker host IP address is localhost
15:58:36.840 [main] INFO org.testcontainers.DockerClientFactory -- Connected to docker: 
  Server Version: 28.0.4
  API Version: 1.48
  Operating System: Ubuntu 24.04.5 LTS
  Total Memory: 15989 MB
15:58:36.873 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling docker image: testcontainers/ryuk:0.14.0. Please be patient; this may take some time but only needs to be done once.
15:58:37.159 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Starting to pull image
15:58:37.177 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  0 downloaded,  0 extracted, (0 bytes/0 bytes)
15:58:37.264 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  1 pending,  1 downloaded,  0 extracted, (242 KB/? MB)
15:58:37.271 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  1 pending,  1 downloaded,  1 extracted, (242 KB/? MB)
15:58:37.286 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  2 downloaded,  1 extracted, (2 MB/2 MB)
15:58:37.619 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  2 downloaded,  2 extracted, (2 MB/2 MB)
15:58:37.627 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Image testcontainers/ryuk:0.14.0 pull took PT0.753832279S
15:58:37.645 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Creating container for image: testcontainers/ryuk:0.14.0
15:58:37.704 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Container testcontainers/ryuk:0.14.0 is starting: ba11df8102a7136dbd3aa8deb0bf319fe4f2a228238d2ecdba72773acb1c028e
15:58:38.081 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Container testcontainers/ryuk:0.14.0 started in PT0.435929809S
15:58:38.091 [main] INFO org.testcontainers.utility.RyukResourceReaper -- Ryuk started - will monitor and terminate Testcontainers containers on JVM exit
15:58:38.091 [main] INFO org.testcontainers.DockerClientFactory -- Checking the system...
15:58:38.092 [main] INFO org.testcontainers.DockerClientFactory -- ✔︎ Docker server version should be at least 1.6.0
15:58:38.095 [main] INFO tc.postgres:17 -- Pulling docker image: postgres:17. Please be patient; this may take some time but only needs to be done once.
15:58:38.292 [docker-java-stream-765662645] INFO tc.postgres:17 -- Starting to pull image
15:58:38.293 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending,  0 downloaded,  0 extracted, (0 bytes/0 bytes)
15:58:38.387 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 13 pending,  1 downloaded,  0 extracted, (64 KB/? MB)
15:58:38.416 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 12 pending,  2 downloaded,  0 extracted, (383 KB/? MB)
15:58:38.422 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 11 pending,  3 downloaded,  0 extracted, (383 KB/? MB)
15:58:38.464 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 10 pending,  4 downloaded,  0 extracted, (494 KB/? MB)
15:58:38.476 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  9 pending,  5 downloaded,  0 extracted, (494 KB/? MB)
15:58:38.494 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  8 pending,  6 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.495 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  7 pending,  7 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.505 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  6 pending,  8 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.523 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  5 pending,  9 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.533 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  4 pending, 10 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.544 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  3 pending, 11 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.570 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  2 pending, 12 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.572 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  1 pending, 13 downloaded,  0 extracted, (28 MB/? MB)
15:58:38.926 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  0 extracted, (109 MB/153 MB)
15:58:39.583 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  1 extracted, (109 MB/153 MB)
15:58:39.874 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  2 extracted, (109 MB/153 MB)
15:58:40.050 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  3 extracted, (115 MB/153 MB)
15:58:40.098 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  4 extracted, (117 MB/153 MB)
15:58:40.409 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  5 extracted, (124 MB/153 MB)
15:58:40.480 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  6 extracted, (126 MB/153 MB)
15:58:40.490 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  7 extracted, (126 MB/153 MB)
15:58:40.499 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  8 extracted, (126 MB/153 MB)
15:58:43.562 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  9 extracted, (153 MB/153 MB)
15:58:43.576 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 10 extracted, (153 MB/153 MB)
15:58:43.585 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 11 extracted, (153 MB/153 MB)
15:58:43.595 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 12 extracted, (153 MB/153 MB)
15:58:43.605 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 13 extracted, (153 MB/153 MB)
15:58:43.617 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 14 extracted, (153 MB/153 MB)
15:58:43.623 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pull complete. 14 layers, pulled in 5s (downloaded 153 MB at 30 MB/s)
15:58:43.623 [main] INFO tc.postgres:17 -- Image postgres:17 pull took PT5.528542828S
15:58:43.626 [main] INFO tc.postgres:17 -- Creating container for image: postgres:17
15:58:43.640 [main] INFO tc.postgres:17 -- Container postgres:17 is starting: 3b3bb61575e155a3e7c1bd69f7496204f5f7d2f509521f627111a1706bc37e46
15:58:44.725 [main] INFO tc.postgres:17 -- Container postgres:17 started in PT1.098858496S
15:58:44.726 [main] INFO tc.postgres:17 -- Container is started (JDBC URL: jdbc:postgresql://localhost:32769/test?loggerLevel=OFF)
15:58:44.816 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.BackendApplicationTests
15:58:44.895 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.BackendApplicationTests]: BackendApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:44.896 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:44.900 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.BackendApplicationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:58:45.278Z  INFO 2380 --- [           main] c.facturx.app.BackendApplicationTests    : Starting BackendApplicationTests using Java 21.0.12.1 with PID 2380 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:58:45.279Z  INFO 2380 --- [           main] c.facturx.app.BackendApplicationTests    : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:58:45.985Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:58:46.055Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 61 ms. Found 8 JPA repository interfaces.
2026-09-28T15:58:46.750Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:58:46.776Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:58:46.777Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:58:46.824Z  INFO 2380 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 1525 ms
2026-09-28T15:58:47.073Z  INFO 2380 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:58:47.141Z  INFO 2380 --- [           main] org.hibernate.orm.core                   : HHH000001: Hibernate ORM core version 7.4.1.Final
2026-09-28T15:58:47.501Z  INFO 2380 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:58:47.529Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-28T15:58:47.739Z  INFO 2380 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@65013d71
2026-09-28T15:58:47.741Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-28T15:58:47.798Z  INFO 2380 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:58:48.823Z  INFO 2380 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:58:48.876Z  INFO 2380 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:58:48.950Z  INFO 2380 --- [           main] o.s.d.j.r.query.QueryEnhancerFactories   : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-09-28T15:58:49.339Z  WARN 2380 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:58:49.457Z  INFO 2380 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:58:50.541Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 38657 (http) with context path '/'
2026-09-28T15:58:50.569Z  INFO 2380 --- [           main] c.facturx.app.BackendApplicationTests    : Started BackendApplicationTests in 5.615 seconds (process running for 16.185)
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.25 s -- in com.facturx.app.BackendApplicationTests
[INFO] Running com.facturx.app.auth.AuthFlowTest
2026-09-28T15:58:50.712Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.auth.AuthFlowTest]: AuthFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:50.736Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.auth.AuthFlowTest
2026-09-28T15:58:50.741Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.auth.AuthFlowTest]: AuthFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:50.742Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:50.747Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.auth.AuthFlowTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:58:50.793Z  INFO 2380 --- [           main] com.facturx.app.auth.AuthFlowTest        : Starting AuthFlowTest using Java 21.0.12.1 with PID 2380 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:58:50.793Z  INFO 2380 --- [           main] com.facturx.app.auth.AuthFlowTest        : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:58:50.989Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:58:51.005Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 13 ms. Found 8 JPA repository interfaces.
2026-09-28T15:58:51.110Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:58:51.111Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:58:51.111Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:58:51.133Z  INFO 2380 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 337 ms
2026-09-28T15:58:51.212Z  INFO 2380 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:58:51.239Z  INFO 2380 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:58:51.241Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Starting...
2026-09-28T15:58:51.250Z  INFO 2380 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-2 - Added connection org.postgresql.jdbc.PgConnection@242b48ef
2026-09-28T15:58:51.251Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Start completed.
2026-09-28T15:58:51.257Z  INFO 2380 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:58:51.348Z  INFO 2380 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:58:51.384Z  INFO 2380 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:58:51.502Z  WARN 2380 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:58:51.537Z  INFO 2380 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:58:51.696Z  INFO 2380 --- [           main] o.a.c.c.C.[Tomcat-1].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:58:51.696Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:58:51.698Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 2 ms
2026-09-28T15:58:51.730Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 35613 (http) with context path '/'
2026-09-28T15:58:51.735Z  INFO 2380 --- [           main] com.facturx.app.auth.AuthFlowTest        : Started AuthFlowTest in 0.982 seconds (process running for 17.351)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.023 s -- in com.facturx.app.auth.AuthFlowTest
[INFO] Running com.facturx.app.permission.PermissionServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.211 s -- in com.facturx.app.permission.PermissionServiceTest
[INFO] Running com.facturx.app.permission.PermissionIntegrationTest
2026-09-28T15:58:54.955Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.permission.PermissionIntegrationTest]: PermissionIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:54.971Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.permission.PermissionIntegrationTest
2026-09-28T15:58:54.972Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.permission.PermissionIntegrationTest]: PermissionIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:54.972Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:54.976Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.permission.PermissionIntegrationTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:58:55.011Z  INFO 2380 --- [           main] c.f.a.p.PermissionIntegrationTest        : Starting PermissionIntegrationTest using Java 21.0.12.1 with PID 2380 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:58:55.011Z  INFO 2380 --- [           main] c.f.a.p.PermissionIntegrationTest        : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:58:55.165Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:58:55.179Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 12 ms. Found 8 JPA repository interfaces.
2026-09-28T15:58:55.445Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:58:55.447Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:58:55.447Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:58:55.475Z  INFO 2380 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 462 ms
2026-09-28T15:58:55.585Z  INFO 2380 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:58:55.620Z  INFO 2380 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:58:55.621Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-3 - Starting...
2026-09-28T15:58:55.634Z  INFO 2380 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-3 - Added connection org.postgresql.jdbc.PgConnection@323b4ac5
2026-09-28T15:58:55.635Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-3 - Start completed.
2026-09-28T15:58:55.645Z  INFO 2380 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:58:55.755Z  INFO 2380 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:58:55.802Z  INFO 2380 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:58:55.971Z  WARN 2380 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:58:56.021Z  INFO 2380 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:58:56.247Z  INFO 2380 --- [           main] o.a.c.c.C.[Tomcat-2].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:58:56.247Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:58:56.248Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-28T15:58:56.283Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 39853 (http) with context path '/'
2026-09-28T15:58:56.288Z  INFO 2380 --- [           main] c.f.a.p.PermissionIntegrationTest        : Started PermissionIntegrationTest in 1.308 seconds (process running for 21.904)
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.874 s -- in com.facturx.app.permission.PermissionIntegrationTest
[INFO] Running com.facturx.app.validation.FacturXValidationServiceTest
2026-09-28T15:58:59.831Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.FacturXValidationServiceTest]: FacturXValidationServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.846Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.FacturXValidationServiceTest
2026-09-28T15:58:59.847Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.FacturXValidationServiceTest]: FacturXValidationServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.847Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.851Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.FacturXValidationServiceTest
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.042 s <<< FAILURE! -- in com.facturx.app.validation.FacturXValidationServiceTest
[ERROR] com.facturx.app.validation.FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere -- Time elapsed: 0.009 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.FacturXValidationServiceTest.readSample(FacturXValidationServiceTest.java:67)
	at com.facturx.app.validation.FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere(FacturXValidationServiceTest.java:49)

[ERROR] com.facturx.app.validation.FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.FacturXValidationServiceTest.readSample(FacturXValidationServiceTest.java:67)
	at com.facturx.app.validation.FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices(FacturXValidationServiceTest.java:24)

[INFO] Running com.facturx.app.validation.ValidationControllerTest
2026-09-28T15:58:59.874Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationControllerTest]: ValidationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.879Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationControllerTest
2026-09-28T15:58:59.880Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationControllerTest]: ValidationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.880Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:59.887Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationControllerTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[H-k_abhZvnuCGHPtMWFZTOQOytt-ZedPqbK6wIHf5n_jzkXfeohbUIxv3xive0GIVExtddJr57lLXNFiy9eDpbfrhR6Hr33p]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"134"]
             Body = {"email":"validate-98fec8d1-3545-4fa2-b2eb-8445aa46e244@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MzZlNDlhNzEtZWM1ZC00YjIxLWE1MWItZDlhNzJmZWE0ZTA5; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":20,"email":"validate-98fec8d1-3545-4fa2-b2eb-8445aa46e244@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@3fdef83 name = 'EFACTURE_SESSION', value = 'MzZlNDlhNzEtZWM1ZC00YjIxLWE1MWItZDlhNzJmZWE0ZTA5', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[EVIkHHDamf_sn6FAzi2BoQ3Nq0iExTlSfX-VIkRCtvKG666qczQTK0fjqZ3B-ZgiqgC1kj71hnG2_F1_GEmnG3MnhMrlicjL]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"134"]
             Body = {"email":"validate-db81db63-ebfb-43dc-a18d-6b7a499fef20@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MTMzZDdiZWItZjhlOS00ZGRlLTg5ZGQtNWJiN2QwOGY0ODdm; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":21,"email":"validate-db81db63-ebfb-43dc-a18d-6b7a499fef20@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@15050e17 name = 'EFACTURE_SESSION', value = 'MTMzZDdiZWItZjhlOS00ZGRlLTg5ZGQtNWJiN2QwOGY0ODdm', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 4, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.595 s <<< FAILURE! -- in com.facturx.app.validation.ValidationControllerTest
[ERROR] com.facturx.app.validation.ValidationControllerTest.validSampleReturnsValidTrue -- Time elapsed: 0.192 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.validSampleReturnsValidTrue(ValidationControllerTest.java:47)

[ERROR] com.facturx.app.validation.ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors -- Time elapsed: 0.161 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors(ValidationControllerTest.java:59)

[ERROR] com.facturx.app.validation.ValidationControllerTest.unauthenticatedRequestIsRejected -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.unauthenticatedRequestIsRejected(ValidationControllerTest.java:29)

[INFO] Running com.facturx.app.validation.MustangValidationClientSmokeTest
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.006 s <<< FAILURE! -- in com.facturx.app.validation.MustangValidationClientSmokeTest
[ERROR] com.facturx.app.validation.MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport -- Time elapsed: 0.002 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.MustangValidationClientSmokeTest.readSample(MustangValidationClientSmokeTest.java:45)
	at com.facturx.app.validation.MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport(MustangValidationClientSmokeTest.java:18)

[ERROR] com.facturx.app.validation.MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport -- Time elapsed: 0.001 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.MustangValidationClientSmokeTest.readSample(MustangValidationClientSmokeTest.java:45)
	at com.facturx.app.validation.MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport(MustangValidationClientSmokeTest.java:32)

[INFO] Running com.facturx.app.validation.ValidationReportControllerTest
2026-09-28T15:59:00.480Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportControllerTest]: ValidationReportControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.485Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportControllerTest
2026-09-28T15:59:00.487Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportControllerTest]: ValidationReportControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.487Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.491Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportControllerTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[F1ElmEEZXlykLRP-Z0MTBRBSxsCHNXUmth5mxfBCfGHIp8vuJ2VDqCIpPGuJSCDJV24nNSU06_iyAhcLj31QoZYmGgCrnvPc]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"132"]
             Body = {"email":"report-e20d194d-2a51-415c-87bc-c3cb8ed1272d@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=YjZkZTY2MWMtNWUwMS00MGYzLTk1YjQtZjkxMmFjOWY1Yzdj; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":23,"email":"report-e20d194d-2a51-415c-87bc-c3cb8ed1272d@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@24d4a409 name = 'EFACTURE_SESSION', value = 'YjZkZTY2MWMtNWUwMS00MGYzLTk1YjQtZjkxMmFjOWY1Yzdj', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.359 s <<< FAILURE! -- in com.facturx.app.validation.ValidationReportControllerTest
[ERROR] com.facturx.app.validation.ValidationReportControllerTest.reportIsReachableRightAfterValidating -- Time elapsed: 0.162 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportControllerTest.sampleFile(ValidationReportControllerTest.java:87)
	at com.facturx.app.validation.ValidationReportControllerTest.reportIsReachableRightAfterValidating(ValidationReportControllerTest.java:49)

[INFO] Running com.facturx.app.validation.ValidationReportServiceTest
2026-09-28T15:59:00.840Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportServiceTest]: ValidationReportServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.844Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportServiceTest
2026-09-28T15:59:00.845Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportServiceTest]: ValidationReportServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.845Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.848Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportServiceTest
[ERROR] Tests run: 4, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.030 s <<< FAILURE! -- in com.facturx.app.validation.ValidationReportServiceTest
[ERROR] com.facturx.app.validation.ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure(ValidationReportServiceTest.java:55)

[ERROR] com.facturx.app.validation.ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice(ValidationReportServiceTest.java:38)

[ERROR] com.facturx.app.validation.ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase -- Time elapsed: 0.002 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase(ValidationReportServiceTest.java:26)

[INFO] Running com.facturx.app.validation.RuleCatalogCoverageTest
2026-09-28T15:59:00.871Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.RuleCatalogCoverageTest]: RuleCatalogCoverageTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.875Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.RuleCatalogCoverageTest
2026-09-28T15:59:00.876Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.RuleCatalogCoverageTest]: RuleCatalogCoverageTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.876Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.879Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.RuleCatalogCoverageTest
[ERROR] Tests run: 10, Failures: 0, Errors: 10, Skipped: 0, Time elapsed: 0.093 s <<< FAILURE! -- in com.facturx.app.validation.RuleCatalogCoverageTest
[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-type-code.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:67)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-invoice-number.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:57)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[1] -- Time elapsed: 0.011 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[2] -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[3] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: plain-pdf-no-xml.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[4] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-invoice-number.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[5] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-type-code.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[6] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: wrong-total-amount.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes -- Time elapsed: 0.002 s <<< ERROR!
java.io.IOException: Sample not found on classpath: plain-pdf-no-xml.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:87)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: wrong-total-amount.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:77)

[INFO] Running com.facturx.app.organization.OrganizationFlowTest
2026-09-28T15:59:00.966Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.OrganizationFlowTest]: OrganizationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.979Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.OrganizationFlowTest
2026-09-28T15:59:00.980Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.OrganizationFlowTest]: OrganizationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.980Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:00.985Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.OrganizationFlowTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.822 s -- in com.facturx.app.organization.OrganizationFlowTest
[INFO] Running com.facturx.app.organization.InvitationFlowTest
2026-09-28T15:59:02.789Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.InvitationFlowTest]: InvitationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.794Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.InvitationFlowTest
2026-09-28T15:59:02.794Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.InvitationFlowTest]: InvitationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.794Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.797Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.InvitationFlowTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.155 s -- in com.facturx.app.organization.InvitationFlowTest
[INFO] Running com.facturx.app.document.DocumentFlowTest
2026-09-28T15:59:04.946Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentFlowTest]: DocumentFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:04.956Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentFlowTest
2026-09-28T15:59:04.957Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentFlowTest]: DocumentFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:04.958Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:04.961Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentFlowTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:59:04.993Z  INFO 2380 --- [           main] c.facturx.app.document.DocumentFlowTest  : Starting DocumentFlowTest using Java 21.0.12.1 with PID 2380 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:59:04.993Z  INFO 2380 --- [           main] c.facturx.app.document.DocumentFlowTest  : The following 1 profile is active: "test"
2026-09-28T15:59:05.154Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:59:05.167Z  INFO 2380 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 11 ms. Found 8 JPA repository interfaces.
2026-09-28T15:59:05.254Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:59:05.255Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:59:05.255Z  INFO 2380 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:59:05.276Z  INFO 2380 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 281 ms
2026-09-28T15:59:05.333Z  INFO 2380 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:59:05.355Z  INFO 2380 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:59:05.356Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-4 - Starting...
2026-09-28T15:59:05.365Z  INFO 2380 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-4 - Added connection org.postgresql.jdbc.PgConnection@26ccc142
2026-09-28T15:59:05.366Z  INFO 2380 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-4 - Start completed.
2026-09-28T15:59:05.371Z  INFO 2380 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:59:05.472Z  INFO 2380 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:59:05.511Z  INFO 2380 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:59:05.604Z  WARN 2380 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:59:05.629Z  INFO 2380 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:59:05.736Z  INFO 2380 --- [           main] o.a.c.c.C.[Tomcat-3].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:59:05.736Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:59:05.741Z  INFO 2380 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 5 ms
2026-09-28T15:59:05.770Z  INFO 2380 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 45305 (http) with context path '/'
2026-09-28T15:59:05.773Z  INFO 2380 --- [           main] c.facturx.app.document.DocumentFlowTest  : Started DocumentFlowTest in 0.808 seconds (process running for 31.389)
2026-09-28T15:59:06.858Z  WARN 2380 --- [           main] ration$PageModule$WarningLoggingModifier : Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
	For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))
	or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.

[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.296 s -- in com.facturx.app.document.DocumentFlowTest
[INFO] Running com.facturx.app.document.DocumentValidationPermissionTest
2026-09-28T15:59:07.242Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationPermissionTest]: DocumentValidationPermissionTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.248Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationPermissionTest
2026-09-28T15:59:07.249Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationPermissionTest]: DocumentValidationPermissionTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.249Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.253Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationPermissionTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[IumZ_ApprwkRaV-RuzzhjF2Xth4O5qfH7KX_WbfX7bBzRQ3MF9v7yDxbmWs8Wmf3jhHV7W30myY33pPqjpSdbo_n24RAI2_4]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"149"]
             Body = {"email":"doc-validate-perm-admin-3bb5ce0f-77d8-45e6-8007-da4adb3c7570@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MjExOGYwMTgtMDI5ZC00MzY5LWI2YjYtYzk4MzliODRkYjll; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":8,"email":"doc-validate-perm-admin-3bb5ce0f-77d8-45e6-8007-da4adb3c7570@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@4f66b446 name = 'EFACTURE_SESSION', value = 'MjExOGYwMTgtMDI5ZC00MzY5LWI2YjYtYzk4MzliODRkYjll', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[J4weYlC9vESq9os5fF3z0SuYAwZTpAJut5B04dMNHWC59CxAQrV6VmCMjHOHwu4KHnDH6U79LmRhwjZD1fMSgrI8LAaJzBty]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"150"]
             Body = {"email":"doc-validate-perm-client-668bdeec-7cbc-4562-8c34-155d2ce005f9@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=ZDZkYWU4MzgtNzA0OS00OTM2LWI4N2EtZTAwYWQ0NmI3OWQy; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":9,"email":"doc-validate-perm-client-668bdeec-7cbc-4562-8c34-155d2ce005f9@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@2ac140be name = 'EFACTURE_SESSION', value = 'ZDZkYWU4MzgtNzA0OS00OTM2LWI4N2EtZTAwYWQ0NmI3OWQy', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[ApXt9akFiSdzIjSzVVQ7UiUEergyC6FkJpmQJybljY_rMIcAM6KLw51g7xVeFlKDZXkPZEBnV9lRPZhJH_igEEXRvbyNAbRh]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"149"]
             Body = {"email":"doc-validate-perm-owner-967546d9-efde-4ca4-a6d9-672a51eeda89@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=OWI3NzdlOTMtNjA5MC00MTIwLWFlMTQtZTBhOTdiODJlNGM0; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":10,"email":"doc-validate-perm-owner-967546d9-efde-4ca4-a6d9-672a51eeda89@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@70cf87c name = 'EFACTURE_SESSION', value = 'OWI3NzdlOTMtNjA5MC00MTIwLWFlMTQtZTBhOTdiODJlNGM0', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[uNODWzjXgfrgLjgcUnNtXFeVO4Rfj-vWhJbRXLSm9cDFerCBiuOxaFq1sc7NGFstN15ZametFuVnvNz7tffoONfCl6X2TIXg]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"152"]
             Body = {"email":"doc-validate-perm-outsider-ab6d003c-0f33-456d-9f4b-c305629c7381@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=ODU3ZDAxZWEtN2ZjNC00ODQ2LWJlMDYtOGI5NmY3N2Y0MmYx; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":11,"email":"doc-validate-perm-outsider-ab6d003c-0f33-456d-9f4b-c305629c7381@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@10ab6440 name = 'EFACTURE_SESSION', value = 'ODU3ZDAxZWEtN2ZjNC00ODQ2LWJlMDYtOGI5NmY3N2Y0MmYx', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.673 s <<< FAILURE! -- in com.facturx.app.document.DocumentValidationPermissionTest
[ERROR] com.facturx.app.document.DocumentValidationPermissionTest.clientCannotValidateADocument -- Time elapsed: 0.333 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationPermissionTest.readSample(DocumentValidationPermissionTest.java:116)
	at com.facturx.app.document.DocumentValidationPermissionTest.uploadSample(DocumentValidationPermissionTest.java:97)
	at com.facturx.app.document.DocumentValidationPermissionTest.clientCannotValidateADocument(DocumentValidationPermissionTest.java:131)

[ERROR] com.facturx.app.document.DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument -- Time elapsed: 0.324 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationPermissionTest.readSample(DocumentValidationPermissionTest.java:116)
	at com.facturx.app.document.DocumentValidationPermissionTest.uploadSample(DocumentValidationPermissionTest.java:97)
	at com.facturx.app.document.DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument(DocumentValidationPermissionTest.java:155)

[INFO] Running com.facturx.app.document.DocumentValidationFlowTest
2026-09-28T15:59:07.917Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationFlowTest]: DocumentValidationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.921Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationFlowTest
2026-09-28T15:59:07.922Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationFlowTest]: DocumentValidationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.922Z  INFO 2380 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.925Z  INFO 2380 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationFlowTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[yMDJ-IpfYIkVR_J6ETT3WITN7TCgkfkHfRukX_lOkwZ6gv25qaL-zOw9Urg4f8UbIhnDYeL-wFGX9ckqSXqcb8l-pD5I45uA]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-validate-ok-170b3b3f-d469-4bf6-ab2f-beb2f9f0b368@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MzkzYzllMjgtMjc3ZS00OTAzLTljMzItN2ZiODVkYTYyYmNh; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":12,"email":"doc-validate-ok-170b3b3f-d469-4bf6-ab2f-beb2f9f0b368@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@3075e022 name = 'EFACTURE_SESSION', value = 'MzkzYzllMjgtMjc3ZS00OTAzLTljMzItN2ZiODVkYTYyYmNh', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[g0qkQXVp0hOYHrJy_aH1n_khrdphf908uGtnAyuDlJYBzagV4CjBJ0cL5SG1eIIUy4zB_sgRgOMCSOQRjlMGMBu2paQy-cwt]}
          Headers = [Cookie:"EFACTURE_SESSION=MzkzYzllMjgtMjc3ZS00OTAzLTljMzItN2ZiODVkYTYyYmNh"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":11,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:08.086948899"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[8w4Vl5EZr9xchmq7RpAzPrZescVqTAdfqH41eOCqiKDNSCmSxTx0ofItn-1x51neJL0HXNA6nPxSeWRyyR0BTdiZ7MX1Kxyq]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-report-none-d2061757-7fe3-421d-b510-facb76dc8c0a@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=YmE0ZTllZWYtMmFhNy00NzM1LTg4Y2QtZjU5NGRmN2U2MTA0; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":13,"email":"doc-report-none-d2061757-7fe3-421d-b510-facb76dc8c0a@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@79ed2670 name = 'EFACTURE_SESSION', value = 'YmE0ZTllZWYtMmFhNy00NzM1LTg4Y2QtZjU5NGRmN2U2MTA0', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[gz8aNeD0qOTuuV1h6KS8ET3kGyuOmnmv_Zz4XsW3r48WNU4ctAp7UNHDkNbD2mtS24mIclvQNkrrrEyCxf2ZZ_KPnbYmVy96]}
          Headers = [Cookie:"EFACTURE_SESSION=YmE0ZTllZWYtMmFhNy00NzM1LTg4Y2QtZjU5NGRmN2U2MTA0"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":12,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:08.255703644"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[hEs1othkJtI9PZvOeshlX0fyHHLcH6H7M8BpcMGmgI8d5JV7ty0Nle5cFLcQXq_7TOVRaX7FMRC_KZjWC6ZeRveT4boqhqRJ]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-validate-ko-3a0f8c22-c654-4b59-9334-4ed11f0687d2@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MjNjZTcyMjAtOGI1Mi00NjQ3LTgyZTAtYTNmMjE0YjQzYTQ4; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":14,"email":"doc-validate-ko-3a0f8c22-c654-4b59-9334-4ed11f0687d2@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@7b165203 name = 'EFACTURE_SESSION', value = 'MjNjZTcyMjAtOGI1Mi00NjQ3LTgyZTAtYTNmMjE0YjQzYTQ4', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[eSWKmzwIEwS4rkFDiQw393HhqUTvwJ0cKdFoB0ESVmaqoSfASRe6rgkxJWWVynR2sCEDxhSHhCbc9v4xGeFdM3UqZFCcwkHw]}
          Headers = [Cookie:"EFACTURE_SESSION=MjNjZTcyMjAtOGI1Mi00NjQ3LTgyZTAtYTNmMjE0YjQzYTQ4"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":13,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:08.422671485"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []
[ERROR] Tests run: 3, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.513 s <<< FAILURE! -- in com.facturx.app.document.DocumentValidationFlowTest
[ERROR] com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport -- Time elapsed: 0.166 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport(DocumentValidationFlowTest.java:106)

[ERROR] com.facturx.app.document.DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404 -- Time elapsed: 0.167 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404(DocumentValidationFlowTest.java:156)

[ERROR] com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant -- Time elapsed: 0.165 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant(DocumentValidationFlowTest.java:133)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404:156->uploadSample:75->readSample:96 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant:133->uploadSample:75->readSample:96 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport:106->uploadSample:75->readSample:96 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationPermissionTest.clientCannotValidateADocument:131->uploadSample:97->readSample:116 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument:155->uploadSample:97->readSample:116 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere:49->readSample:67 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices:24->readSample:67 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport:32->readSample:45 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport:18->readSample:45 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: plain-pdf-no-xml.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: missing-invoice-number.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: missing-type-code.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: wrong-total-amount.pdf
[ERROR]   RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes:57->readSample:97 IO Sample not found on classpath: missing-invoice-number.pdf
[ERROR]   RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes:67->readSample:97 IO Sample not found on classpath: missing-type-code.pdf
[ERROR]   RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes:87->readSample:97 IO Sample not found on classpath: plain-pdf-no-xml.pdf
[ERROR]   RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes:77->readSample:97 IO Sample not found on classpath: wrong-total-amount.pdf
[ERROR]   ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors:59->sampleFile:86 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   ValidationControllerTest.unauthenticatedRequestIsRejected:29->sampleFile:86 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationControllerTest.validSampleReturnsValidTrue:47->sampleFile:86 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportControllerTest.reportIsReachableRightAfterValidating:49->sampleFile:87 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure:55->readSample:102 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase:26->readSample:102 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice:38->readSample:102 IO Sample not found on classpath: EN16931_Einfach.pdf
[INFO] 
[ERROR] Tests run: 78, Failures: 0, Errors: 26, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  41.176 s
[INFO] Finished at: 2026-09-28T15:59:08Z
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project backend: 
[ERROR] 
[ERROR] See /home/runner/work/FacturX/FacturX/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Error: Process completed with exit code 1.



-----------------------
docker-build
failed 3 minutes ago in 1m 1s

1s
1s
5s
48s
Run ./mvnw -B test
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.facturx:backend >-------------------------
[INFO] Building  0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- dependency:3.10.0:properties (default) @ backend ---
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ backend ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- compiler:3.15.0:compile (default-compile) @ backend ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 85 source files with javac [debug parameters release 21] to target/classes
[INFO] /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java: /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java uses or overrides a deprecated API.
[INFO] /home/runner/work/FacturX/FacturX/backend/src/main/java/com/facturx/app/document/DocumentExceptionHandler.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ backend ---
[INFO] Copying 7 resources from src/test/resources to target/test-classes
[INFO] 
[INFO] --- compiler:3.15.0:testCompile (default-testCompile) @ backend ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 16 source files with javac [debug parameters release 21] to target/test-classes
[INFO] 
[INFO] --- surefire:3.5.6:test (default-test) @ backend ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.facturx.app.BackendApplicationTests
15:58:40.471 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.BackendApplicationTests]: BackendApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:40.544 [main] INFO org.testcontainers.images.PullPolicy -- Image pull policy will be performed by: DefaultPullPolicy()
15:58:40.547 [main] INFO org.testcontainers.utility.ImageNameSubstitutor -- Image name substitution will be performed by: DefaultImageNameSubstitutor (composite of 'ConfigurationFileImageNameSubstitutor' and 'PrefixingImageNameSubstitutor')
15:58:40.577 [main] INFO org.testcontainers.DockerClientFactory -- Testcontainers version: 2.0.5
15:58:41.744 [main] INFO org.testcontainers.dockerclient.DockerClientProviderStrategy -- Found Docker environment with local Unix socket (unix:///var/run/docker.sock)
15:58:41.753 [main] INFO org.testcontainers.DockerClientFactory -- Docker host IP address is localhost
15:58:41.770 [main] INFO org.testcontainers.DockerClientFactory -- Connected to docker: 
  Server Version: 28.0.4
  API Version: 1.48
  Operating System: Ubuntu 24.04.5 LTS
  Total Memory: 15989 MB
15:58:41.803 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling docker image: testcontainers/ryuk:0.14.0. Please be patient; this may take some time but only needs to be done once.
15:58:42.732 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Starting to pull image
15:58:42.750 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  0 downloaded,  0 extracted, (0 bytes/0 bytes)
15:58:43.237 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  1 pending,  1 downloaded,  0 extracted, (242 KB/? MB)
15:58:43.245 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  1 pending,  1 downloaded,  1 extracted, (242 KB/? MB)
15:58:43.250 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  2 downloaded,  1 extracted, (2 MB/2 MB)
15:58:43.273 [docker-java-stream-379213951] INFO tc.testcontainers/ryuk:0.14.0 -- Pulling image layers:  0 pending,  2 downloaded,  2 extracted, (2 MB/2 MB)
15:58:43.286 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Image testcontainers/ryuk:0.14.0 pull took PT1.482956293S
15:58:43.315 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Creating container for image: testcontainers/ryuk:0.14.0
15:58:43.596 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Container testcontainers/ryuk:0.14.0 is starting: 7fa3a2d8ee74e206309cb993153d2387d30fcee2fcef675a13baab9c0fd46135
15:58:44.093 [main] INFO tc.testcontainers/ryuk:0.14.0 -- Container testcontainers/ryuk:0.14.0 started in PT0.777915551S
15:58:44.103 [main] INFO org.testcontainers.utility.RyukResourceReaper -- Ryuk started - will monitor and terminate Testcontainers containers on JVM exit
15:58:44.104 [main] INFO org.testcontainers.DockerClientFactory -- Checking the system...
15:58:44.104 [main] INFO org.testcontainers.DockerClientFactory -- ✔︎ Docker server version should be at least 1.6.0
15:58:44.107 [main] INFO tc.postgres:17 -- Pulling docker image: postgres:17. Please be patient; this may take some time but only needs to be done once.
15:58:44.974 [docker-java-stream-765662645] INFO tc.postgres:17 -- Starting to pull image
15:58:44.975 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending,  0 downloaded,  0 extracted, (0 bytes/0 bytes)
15:58:45.446 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 13 pending,  1 downloaded,  0 extracted, (366 KB/? MB)
15:58:45.458 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 12 pending,  2 downloaded,  0 extracted, (366 KB/? MB)
15:58:45.526 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 11 pending,  3 downloaded,  0 extracted, (366 KB/? MB)
15:58:45.678 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers: 10 pending,  4 downloaded,  0 extracted, (6 MB/? MB)
15:58:45.713 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  9 pending,  5 downloaded,  0 extracted, (6 MB/? MB)
15:58:45.761 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  8 pending,  6 downloaded,  0 extracted, (6 MB/? MB)
15:58:45.897 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  7 pending,  7 downloaded,  0 extracted, (14 MB/? MB)
15:58:45.921 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  6 pending,  8 downloaded,  0 extracted, (14 MB/? MB)
15:58:46.135 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  5 pending,  9 downloaded,  0 extracted, (52 MB/? MB)
15:58:46.147 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  4 pending, 10 downloaded,  0 extracted, (52 MB/? MB)
15:58:46.335 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  3 pending, 11 downloaded,  0 extracted, (116 MB/? MB)
15:58:46.362 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  2 pending, 12 downloaded,  0 extracted, (118 MB/? MB)
15:58:46.363 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  1 pending, 13 downloaded,  0 extracted, (118 MB/? MB)
15:58:46.554 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  0 extracted, (119 MB/153 MB)
15:58:46.645 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  1 extracted, (119 MB/153 MB)
15:58:46.920 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  2 extracted, (119 MB/153 MB)
15:58:47.096 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  3 extracted, (125 MB/153 MB)
15:58:47.137 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  4 extracted, (127 MB/153 MB)
15:58:47.439 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  5 extracted, (134 MB/153 MB)
15:58:47.509 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  6 extracted, (136 MB/153 MB)
15:58:47.517 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  7 extracted, (136 MB/153 MB)
15:58:47.527 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  8 extracted, (136 MB/153 MB)
15:58:50.588 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded,  9 extracted, (153 MB/153 MB)
15:58:50.601 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 10 extracted, (153 MB/153 MB)
15:58:50.610 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 11 extracted, (153 MB/153 MB)
15:58:50.619 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 12 extracted, (153 MB/153 MB)
15:58:50.630 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 13 extracted, (153 MB/153 MB)
15:58:50.639 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pulling image layers:  0 pending, 14 downloaded, 14 extracted, (153 MB/153 MB)
15:58:50.645 [main] INFO tc.postgres:17 -- Image postgres:17 pull took PT6.538081967S
15:58:50.645 [docker-java-stream-765662645] INFO tc.postgres:17 -- Pull complete. 14 layers, pulled in 5s (downloaded 153 MB at 30 MB/s)
15:58:50.648 [main] INFO tc.postgres:17 -- Creating container for image: postgres:17
15:58:50.663 [main] INFO tc.postgres:17 -- Container postgres:17 is starting: 570d6fdd3648fda130a906056c2f74d7f8d0f0821fd3a983a53f2d7e20a970ff
15:58:51.784 [main] INFO tc.postgres:17 -- Container postgres:17 started in PT1.135476942S
15:58:51.785 [main] INFO tc.postgres:17 -- Container is started (JDBC URL: jdbc:postgresql://localhost:32769/test?loggerLevel=OFF)
15:58:51.884 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.BackendApplicationTests
15:58:51.970 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.BackendApplicationTests]: BackendApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:51.971 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:58:51.975 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.BackendApplicationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:58:52.401Z  INFO 2392 --- [           main] c.facturx.app.BackendApplicationTests    : Starting BackendApplicationTests using Java 21.0.12.1 with PID 2392 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:58:52.403Z  INFO 2392 --- [           main] c.facturx.app.BackendApplicationTests    : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:58:53.162Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:58:53.237Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 65 ms. Found 8 JPA repository interfaces.
2026-09-28T15:58:53.928Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:58:53.957Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:58:53.958Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:58:54.012Z  INFO 2392 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 1585 ms
2026-09-28T15:58:54.308Z  INFO 2392 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:58:54.366Z  INFO 2392 --- [           main] org.hibernate.orm.core                   : HHH000001: Hibernate ORM core version 7.4.1.Final
2026-09-28T15:58:54.794Z  INFO 2392 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:58:54.821Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-28T15:58:55.061Z  INFO 2392 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@36a7586f
2026-09-28T15:58:55.063Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-09-28T15:58:55.121Z  INFO 2392 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:58:56.167Z  INFO 2392 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:58:56.221Z  INFO 2392 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:58:56.302Z  INFO 2392 --- [           main] o.s.d.j.r.query.QueryEnhancerFactories   : Hibernate is in classpath; If applicable, HQL parser will be used.
2026-09-28T15:58:56.734Z  WARN 2392 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:58:56.861Z  INFO 2392 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:58:58.108Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 45219 (http) with context path '/'
2026-09-28T15:58:58.133Z  INFO 2392 --- [           main] c.facturx.app.BackendApplicationTests    : Started BackendApplicationTests in 6.098 seconds (process running for 19.037)
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 18.00 s -- in com.facturx.app.BackendApplicationTests
[INFO] Running com.facturx.app.auth.AuthFlowTest
2026-09-28T15:58:58.287Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.auth.AuthFlowTest]: AuthFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:58.307Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.auth.AuthFlowTest
2026-09-28T15:58:58.309Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.auth.AuthFlowTest]: AuthFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:58.309Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:58:58.314Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.auth.AuthFlowTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:58:58.348Z  INFO 2392 --- [           main] com.facturx.app.auth.AuthFlowTest        : Starting AuthFlowTest using Java 21.0.12.1 with PID 2392 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:58:58.348Z  INFO 2392 --- [           main] com.facturx.app.auth.AuthFlowTest        : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:58:58.579Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:58:58.608Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 26 ms. Found 8 JPA repository interfaces.
2026-09-28T15:58:58.714Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:58:58.715Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:58:58.716Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:58:58.736Z  INFO 2392 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 386 ms
2026-09-28T15:58:58.816Z  INFO 2392 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:58:58.841Z  INFO 2392 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:58:58.842Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Starting...
2026-09-28T15:58:58.855Z  INFO 2392 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-2 - Added connection org.postgresql.jdbc.PgConnection@48b745af
2026-09-28T15:58:58.855Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-2 - Start completed.
2026-09-28T15:58:58.862Z  INFO 2392 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:58:58.972Z  INFO 2392 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:58:59.012Z  INFO 2392 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:58:59.137Z  WARN 2392 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:58:59.172Z  INFO 2392 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:58:59.322Z  INFO 2392 --- [           main] o.a.c.c.C.[Tomcat-1].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:58:59.323Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:58:59.324Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-28T15:58:59.357Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 45361 (http) with context path '/'
2026-09-28T15:58:59.363Z  INFO 2392 --- [           main] com.facturx.app.auth.AuthFlowTest        : Started AuthFlowTest in 1.043 seconds (process running for 20.268)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.112 s -- in com.facturx.app.auth.AuthFlowTest
[INFO] Running com.facturx.app.permission.PermissionServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.233 s -- in com.facturx.app.permission.PermissionServiceTest
[INFO] Running com.facturx.app.permission.PermissionIntegrationTest
2026-09-28T15:59:02.645Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.permission.PermissionIntegrationTest]: PermissionIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.663Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.permission.PermissionIntegrationTest
2026-09-28T15:59:02.664Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.permission.PermissionIntegrationTest]: PermissionIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.664Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:02.668Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.permission.PermissionIntegrationTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:59:02.697Z  INFO 2392 --- [           main] c.f.a.p.PermissionIntegrationTest        : Starting PermissionIntegrationTest using Java 21.0.12.1 with PID 2392 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:59:02.697Z  INFO 2392 --- [           main] c.f.a.p.PermissionIntegrationTest        : No active profile set, falling back to 1 default profile: "default"
2026-09-28T15:59:02.864Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:59:02.879Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 13 ms. Found 8 JPA repository interfaces.
2026-09-28T15:59:03.152Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:59:03.153Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:59:03.154Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:59:03.185Z  INFO 2392 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 486 ms
2026-09-28T15:59:03.306Z  INFO 2392 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:59:03.340Z  INFO 2392 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:59:03.342Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-3 - Starting...
2026-09-28T15:59:03.358Z  INFO 2392 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-3 - Added connection org.postgresql.jdbc.PgConnection@18de437d
2026-09-28T15:59:03.358Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-3 - Start completed.
2026-09-28T15:59:03.366Z  INFO 2392 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:59:03.489Z  INFO 2392 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:59:03.538Z  INFO 2392 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:59:03.742Z  WARN 2392 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:59:03.808Z  INFO 2392 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:59:04.107Z  INFO 2392 --- [           main] o.a.c.c.C.[Tomcat-2].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:59:04.108Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:59:04.109Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-09-28T15:59:04.155Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 37795 (http) with context path '/'
2026-09-28T15:59:04.160Z  INFO 2392 --- [           main] c.f.a.p.PermissionIntegrationTest        : Started PermissionIntegrationTest in 1.489 seconds (process running for 25.064)
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.267 s -- in com.facturx.app.permission.PermissionIntegrationTest
[INFO] Running com.facturx.app.validation.FacturXValidationServiceTest
2026-09-28T15:59:07.915Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.FacturXValidationServiceTest]: FacturXValidationServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.940Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.FacturXValidationServiceTest
2026-09-28T15:59:07.943Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.FacturXValidationServiceTest]: FacturXValidationServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.944Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.948Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.FacturXValidationServiceTest
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.061 s <<< FAILURE! -- in com.facturx.app.validation.FacturXValidationServiceTest
[ERROR] com.facturx.app.validation.FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere -- Time elapsed: 0.010 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.FacturXValidationServiceTest.readSample(FacturXValidationServiceTest.java:67)
	at com.facturx.app.validation.FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere(FacturXValidationServiceTest.java:49)

[ERROR] com.facturx.app.validation.FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.FacturXValidationServiceTest.readSample(FacturXValidationServiceTest.java:67)
	at com.facturx.app.validation.FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices(FacturXValidationServiceTest.java:24)

[INFO] Running com.facturx.app.validation.ValidationControllerTest
2026-09-28T15:59:07.977Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationControllerTest]: ValidationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.983Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationControllerTest
2026-09-28T15:59:07.983Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationControllerTest]: ValidationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.984Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:07.990Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationControllerTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[8oW3PUZSLPMG6JsPCkWGua6F2OWFYIs9ybfDkOO5X1feFhiHlLGPXHVrGpYrjv4-bmiygcq99YSxA-gQrIf6pYWOOWboIimz]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"134"]
             Body = {"email":"validate-7e941dd6-dc79-4b2f-b120-33c6188d614f@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=NTc0YTc4YjEtNzM0My00YzIwLTgyZGMtYzE5NmZlNWJlZWNj; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":20,"email":"validate-7e941dd6-dc79-4b2f-b120-33c6188d614f@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@58ef7429 name = 'EFACTURE_SESSION', value = 'NTc0YTc4YjEtNzM0My00YzIwLTgyZGMtYzE5NmZlNWJlZWNj', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[4IFSK9Z5vdaJE4AtVM5eBfmhaGtvEMq4KxI1WDzEQzYhxJ2k2Oc2T7QchOKkKuUbMuNqM53FRVIOIquVTyYEPl2lcwQX8arG]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"134"]
             Body = {"email":"validate-dbd7f1a0-0059-4143-9630-3b475f120523@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=ZGFmYzgwN2MtMGUyOC00OGRhLThlODUtNTI0OTBiYjkyN2Qz; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":21,"email":"validate-dbd7f1a0-0059-4143-9630-3b475f120523@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@41df8b13 name = 'EFACTURE_SESSION', value = 'ZGFmYzgwN2MtMGUyOC00OGRhLThlODUtNTI0OTBiYjkyN2Qz', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 4, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.590 s <<< FAILURE! -- in com.facturx.app.validation.ValidationControllerTest
[ERROR] com.facturx.app.validation.ValidationControllerTest.validSampleReturnsValidTrue -- Time elapsed: 0.188 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.validSampleReturnsValidTrue(ValidationControllerTest.java:47)

[ERROR] com.facturx.app.validation.ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors -- Time elapsed: 0.169 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors(ValidationControllerTest.java:59)

[ERROR] com.facturx.app.validation.ValidationControllerTest.unauthenticatedRequestIsRejected -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationControllerTest.sampleFile(ValidationControllerTest.java:86)
	at com.facturx.app.validation.ValidationControllerTest.unauthenticatedRequestIsRejected(ValidationControllerTest.java:29)

[INFO] Running com.facturx.app.validation.MustangValidationClientSmokeTest
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.005 s <<< FAILURE! -- in com.facturx.app.validation.MustangValidationClientSmokeTest
[ERROR] com.facturx.app.validation.MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport -- Time elapsed: 0.002 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.MustangValidationClientSmokeTest.readSample(MustangValidationClientSmokeTest.java:45)
	at com.facturx.app.validation.MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport(MustangValidationClientSmokeTest.java:18)

[ERROR] com.facturx.app.validation.MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport -- Time elapsed: 0.001 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.MustangValidationClientSmokeTest.readSample(MustangValidationClientSmokeTest.java:45)
	at com.facturx.app.validation.MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport(MustangValidationClientSmokeTest.java:32)

[INFO] Running com.facturx.app.validation.ValidationReportControllerTest
2026-09-28T15:59:08.576Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportControllerTest]: ValidationReportControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.581Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportControllerTest
2026-09-28T15:59:08.582Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportControllerTest]: ValidationReportControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.582Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.587Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportControllerTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[wk_TpJt_l8hf12a4gjhPzKAxL1mSKcvzv-M4BFwUCNMyz7n08X2yxqJM8apytlOP5hV7_MYJAmGmGq7ejIIIZmxxbrEC9omV]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"132"]
             Body = {"email":"report-faaaf908-ab50-437a-9556-e74dc0362a43@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=NTU0NDk0NTQtMWU5MS00YTQxLTg1NWQtMDNhNjc2NjA5ZDA5; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":23,"email":"report-faaaf908-ab50-437a-9556-e74dc0362a43@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@7bed9e3 name = 'EFACTURE_SESSION', value = 'NTU0NDk0NTQtMWU5MS00YTQxLTg1NWQtMDNhNjc2NjA5ZDA5', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.371 s <<< FAILURE! -- in com.facturx.app.validation.ValidationReportControllerTest
[ERROR] com.facturx.app.validation.ValidationReportControllerTest.reportIsReachableRightAfterValidating -- Time elapsed: 0.166 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportControllerTest.sampleFile(ValidationReportControllerTest.java:87)
	at com.facturx.app.validation.ValidationReportControllerTest.reportIsReachableRightAfterValidating(ValidationReportControllerTest.java:49)

[INFO] Running com.facturx.app.validation.ValidationReportServiceTest
2026-09-28T15:59:08.947Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportServiceTest]: ValidationReportServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.954Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportServiceTest
2026-09-28T15:59:08.955Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.ValidationReportServiceTest]: ValidationReportServiceTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.956Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.959Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.ValidationReportServiceTest
[ERROR] Tests run: 4, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.041 s <<< FAILURE! -- in com.facturx.app.validation.ValidationReportServiceTest
[ERROR] com.facturx.app.validation.ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure -- Time elapsed: 0.005 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure(ValidationReportServiceTest.java:55)

[ERROR] com.facturx.app.validation.ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice(ValidationReportServiceTest.java:38)

[ERROR] com.facturx.app.validation.ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.ValidationReportServiceTest.readSample(ValidationReportServiceTest.java:102)
	at com.facturx.app.validation.ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase(ValidationReportServiceTest.java:26)

[INFO] Running com.facturx.app.validation.RuleCatalogCoverageTest
2026-09-28T15:59:08.989Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.RuleCatalogCoverageTest]: RuleCatalogCoverageTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.994Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.RuleCatalogCoverageTest
2026-09-28T15:59:08.995Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.validation.RuleCatalogCoverageTest]: RuleCatalogCoverageTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.995Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:08.999Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.validation.RuleCatalogCoverageTest
[ERROR] Tests run: 10, Failures: 0, Errors: 10, Skipped: 0, Time elapsed: 0.105 s <<< FAILURE! -- in com.facturx.app.validation.RuleCatalogCoverageTest
[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-type-code.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:67)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-invoice-number.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:57)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[1] -- Time elapsed: 0.012 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[2] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[3] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: plain-pdf-no-xml.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[4] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-invoice-number.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[5] -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: missing-type-code.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(String)[6] -- Time elapsed: 0.004 s <<< ERROR!
java.io.IOException: Sample not found on classpath: wrong-total-amount.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation(RuleCatalogCoverageTest.java:43)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes -- Time elapsed: 0.003 s <<< ERROR!
java.io.IOException: Sample not found on classpath: plain-pdf-no-xml.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:87)

[ERROR] com.facturx.app.validation.RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes -- Time elapsed: 0.002 s <<< ERROR!
java.io.IOException: Sample not found on classpath: wrong-total-amount.pdf
	at com.facturx.app.validation.RuleCatalogCoverageTest.readSample(RuleCatalogCoverageTest.java:97)
	at com.facturx.app.validation.RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes(RuleCatalogCoverageTest.java:77)

[INFO] Running com.facturx.app.organization.OrganizationFlowTest
2026-09-28T15:59:09.096Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.OrganizationFlowTest]: OrganizationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:09.107Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.OrganizationFlowTest
2026-09-28T15:59:09.108Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.OrganizationFlowTest]: OrganizationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:09.109Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:09.112Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.OrganizationFlowTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.850 s -- in com.facturx.app.organization.OrganizationFlowTest
[INFO] Running com.facturx.app.organization.InvitationFlowTest
2026-09-28T15:59:10.948Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.InvitationFlowTest]: InvitationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:10.952Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.InvitationFlowTest
2026-09-28T15:59:10.953Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.organization.InvitationFlowTest]: InvitationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:10.954Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:10.957Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.organization.InvitationFlowTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.223 s -- in com.facturx.app.organization.InvitationFlowTest
[INFO] Running com.facturx.app.document.DocumentFlowTest
2026-09-28T15:59:13.171Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentFlowTest]: DocumentFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:13.183Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentFlowTest
2026-09-28T15:59:13.184Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentFlowTest]: DocumentFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:13.185Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:13.189Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentFlowTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.1.0)

2026-09-28T15:59:13.225Z  INFO 2392 --- [           main] c.facturx.app.document.DocumentFlowTest  : Starting DocumentFlowTest using Java 21.0.12.1 with PID 2392 (started by runner in /home/runner/work/FacturX/FacturX/backend)
2026-09-28T15:59:13.225Z  INFO 2392 --- [           main] c.facturx.app.document.DocumentFlowTest  : The following 1 profile is active: "test"
2026-09-28T15:59:13.382Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-09-28T15:59:13.393Z  INFO 2392 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 9 ms. Found 8 JPA repository interfaces.
2026-09-28T15:59:13.468Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat initialized with port 0 (http)
2026-09-28T15:59:13.469Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-09-28T15:59:13.469Z  INFO 2392 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/11.0.22]
2026-09-28T15:59:13.489Z  INFO 2392 --- [           main] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 262 ms
2026-09-28T15:59:13.543Z  INFO 2392 --- [           main] org.hibernate.orm.jpa                    : HHH008540: Processing PersistenceUnitInfo [name: default]
2026-09-28T15:59:13.561Z  INFO 2392 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-09-28T15:59:13.562Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-4 - Starting...
2026-09-28T15:59:13.571Z  INFO 2392 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-4 - Added connection org.postgresql.jdbc.PgConnection@1f829ef1
2026-09-28T15:59:13.571Z  INFO 2392 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-4 - Start completed.
2026-09-28T15:59:13.576Z  INFO 2392 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [jdbc:postgresql://localhost:32769/test?loggerLevel=OFF]
	Database driver: PostgreSQL JDBC Driver
	Database dialect: PostgreSQLDialect
	Database version: 17.11
	Default catalog/schema: test/public
	Autocommit mode: undefined/unknown
	Isolation level: READ_COMMITTED [default READ_COMMITTED]
	JDBC fetch size: none
	Pool: DataSourceConnectionProvider
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-09-28T15:59:13.659Z  INFO 2392 --- [           main] org.hibernate.orm.core                   : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-09-28T15:59:13.703Z  INFO 2392 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-09-28T15:59:13.814Z  WARN 2392 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-09-28T15:59:13.836Z  INFO 2392 --- [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
2026-09-28T15:59:13.938Z  INFO 2392 --- [           main] o.a.c.c.C.[Tomcat-3].[localhost].[/]     : Initializing Spring TestDispatcherServlet ''
2026-09-28T15:59:13.938Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-09-28T15:59:13.941Z  INFO 2392 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 3 ms
2026-09-28T15:59:13.968Z  INFO 2392 --- [           main] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 42769 (http) with context path '/'
2026-09-28T15:59:13.971Z  INFO 2392 --- [           main] c.facturx.app.document.DocumentFlowTest  : Started DocumentFlowTest in 0.778 seconds (process running for 34.876)
2026-09-28T15:59:15.054Z  WARN 2392 --- [           main] ration$PageModule$WarningLoggingModifier : Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
	For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))
	or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.

[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.280 s -- in com.facturx.app.document.DocumentFlowTest
[INFO] Running com.facturx.app.document.DocumentValidationPermissionTest
2026-09-28T15:59:15.452Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationPermissionTest]: DocumentValidationPermissionTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:15.457Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationPermissionTest
2026-09-28T15:59:15.458Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationPermissionTest]: DocumentValidationPermissionTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:15.458Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:15.462Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationPermissionTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[M1EcKCgVRgqINwtds5vIY3FE-Zb1cQJfofDpl2sA72CPVRBkADR-GR0ncz6lUj5p17b8Wkh11PTNSGdykcXRrg03jFLuNyRT]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"149"]
             Body = {"email":"doc-validate-perm-admin-9c008185-dbb9-4a9a-a2e6-bc70857a4ac7@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=NWIwZTk0OWItOWE2ZS00M2NmLWJhOWItYTIzZjQ3MTJkMWYw; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":8,"email":"doc-validate-perm-admin-9c008185-dbb9-4a9a-a2e6-bc70857a4ac7@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@6c8c59b3 name = 'EFACTURE_SESSION', value = 'NWIwZTk0OWItOWE2ZS00M2NmLWJhOWItYTIzZjQ3MTJkMWYw', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[ueW6dtJ8XWGgAshbNWJmd1St9sv_wA-bu1BnNpRcw2cCg197iNPZQOROaQSNO65jBU9STzDJ2_Oa-Ti232JVAvJuol5jumtC]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"150"]
             Body = {"email":"doc-validate-perm-client-54d705a6-1594-4047-8ef5-f43619edbadd@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=MTRiM2Q2ZmItNmU1Yi00ZmFlLTg2ZWYtZGI3NGFiNDQxMzNl; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":9,"email":"doc-validate-perm-client-54d705a6-1594-4047-8ef5-f43619edbadd@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@749df7fa name = 'EFACTURE_SESSION', value = 'MTRiM2Q2ZmItNmU1Yi00ZmFlLTg2ZWYtZGI3NGFiNDQxMzNl', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[8G6OwUwoQReOSaOzrI_mefsVhhtvs9zfWl8X1A6scJsXuxmrwlq6pXgceS6jfJTXnKLSQZknq3pe1-TybW5y5zbJFPkm3yyf]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"149"]
             Body = {"email":"doc-validate-perm-owner-75b7722f-f7b5-4566-8057-813dc8675dc0@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=YmY0MTdmZTQtYjQ3NS00NjZjLTkxODMtMDcyZGNlYjJjY2My; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":10,"email":"doc-validate-perm-owner-75b7722f-f7b5-4566-8057-813dc8675dc0@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@72416b53 name = 'EFACTURE_SESSION', value = 'YmY0MTdmZTQtYjQ3NS00NjZjLTkxODMtMDcyZGNlYjJjY2My', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[bIn5Iv_Ak8062PiZBKU2y0mXJxuZ8h1w1sn3sbhOol9pGtwdCrHLR56m8PUX4Zv8ZogC-yqhCiOolyxd5qvP0ol5lTtQLr1-]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"152"]
             Body = {"email":"doc-validate-perm-outsider-e8a1d615-d37a-4009-9fc0-97446bfa4c04@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=OTAxMGJmMTctYzk0MS00Mzg1LWJhNjMtMjlkZTYxNmIyMmY3; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":11,"email":"doc-validate-perm-outsider-e8a1d615-d37a-4009-9fc0-97446bfa4c04@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@3aa2c4f4 name = 'EFACTURE_SESSION', value = 'OTAxMGJmMTctYzk0MS00Mzg1LWJhNjMtMjlkZTYxNmIyMmY3', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.666 s <<< FAILURE! -- in com.facturx.app.document.DocumentValidationPermissionTest
[ERROR] com.facturx.app.document.DocumentValidationPermissionTest.clientCannotValidateADocument -- Time elapsed: 0.329 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationPermissionTest.readSample(DocumentValidationPermissionTest.java:116)
	at com.facturx.app.document.DocumentValidationPermissionTest.uploadSample(DocumentValidationPermissionTest.java:97)
	at com.facturx.app.document.DocumentValidationPermissionTest.clientCannotValidateADocument(DocumentValidationPermissionTest.java:131)

[ERROR] com.facturx.app.document.DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument -- Time elapsed: 0.321 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationPermissionTest.readSample(DocumentValidationPermissionTest.java:116)
	at com.facturx.app.document.DocumentValidationPermissionTest.uploadSample(DocumentValidationPermissionTest.java:97)
	at com.facturx.app.document.DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument(DocumentValidationPermissionTest.java:155)

[INFO] Running com.facturx.app.document.DocumentValidationFlowTest
2026-09-28T15:59:16.119Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationFlowTest]: DocumentValidationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:16.123Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationFlowTest
2026-09-28T15:59:16.124Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.document.DocumentValidationFlowTest]: DocumentValidationFlowTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:16.124Z  INFO 2392 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.facturx.app.AbstractIntegrationTest]: AbstractIntegrationTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-09-28T15:59:16.128Z  INFO 2392 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.facturx.app.BackendApplication for test class com.facturx.app.document.DocumentValidationFlowTest

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[bOLUnJzl25xilOqqs2zEQzHd-j6cmu2AqvxSIQwMOcGob-AuWtC3pf3dva9PoY6e10Hwewfk1wb4rN-tz8pnEDxqDKeRCYJP]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-validate-ok-19a6e04b-9557-40b8-9982-9886c98c53ca@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=YjVhODg1NzQtYzJkMS00MzQ0LWEyNzktMTcwMTI3Mzg1NDA5; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":12,"email":"doc-validate-ok-19a6e04b-9557-40b8-9982-9886c98c53ca@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@4acca518 name = 'EFACTURE_SESSION', value = 'YjVhODg1NzQtYzJkMS00MzQ0LWEyNzktMTcwMTI3Mzg1NDA5', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[2xiMXvPgq6ypE0aDOcWpDdcM52bOHL9WqmNmtFIxKgnhMfLC63q7ZsvUnpSEIye2Wuida-Q5yl_5Ldt7zFMCgGRTHGvVAZSj]}
          Headers = [Cookie:"EFACTURE_SESSION=YjVhODg1NzQtYzJkMS00MzQ0LWEyNzktMTcwMTI3Mzg1NDA5"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":11,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:16.290385097"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[JexKDUSDTr6PmlYaymzDpAnWgZE2QZkVvsY85Tgms1ZvZcb_HNQoOSfiKo2irTd4-UH3lTvvrPMEJa0426JZ1wwfhjJeXfDO]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-report-none-0d59069d-898e-4a2d-b377-1613b81ef256@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=NWMyYzVkM2EtODA2ZC00Y2ZlLTgxYjAtZDcyM2Q2MTRmOGIw; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":13,"email":"doc-report-none-0d59069d-898e-4a2d-b377-1613b81ef256@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@6742e276 name = 'EFACTURE_SESSION', value = 'NWMyYzVkM2EtODA2ZC00Y2ZlLTgxYjAtZDcyM2Q2MTRmOGIw', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[ERrIuR-dl1jbhzbPWKURjaPWUpCsvX1di2Zlgamqcb9g6wCcIS7_iX2lom324VP7aogl78Xmf_Gd2Utw7QRQ4pybQI4F32Gk]}
          Headers = [Cookie:"EFACTURE_SESSION=NWMyYzVkM2EtODA2ZC00Y2ZlLTgxYjAtZDcyM2Q2MTRmOGIw"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":12,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:16.460726069"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/auth/register
       Parameters = {_csrf=[9A7svkhdKBAlE0UF2bzpZaeZjbLlhXLNPuhFubp45soMyX3UkDmI2CxlHCIIcSBk4ZHdUZ6toIrRvEvgD4l3jIMd3v5q8Um3]}
          Headers = [Content-Type:"application/json;charset=UTF-8", Content-Length:"141"]
             Body = {"email":"doc-validate-ko-54ec7c7d-d766-4ef4-a420-d16ebfbf8aa8@x.fr","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}
    Session Attrs = {}

Handler:
             Type = com.facturx.app.auth.AuthController
           Method = com.facturx.app.auth.AuthController#register(RegisterRequest, HttpServletRequest, HttpServletResponse)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 201
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY", Set-Cookie:"EFACTURE_SESSION=NTFiMTA0ZTUtMzgzNS00NWFlLWJmM2ItNjJmNTZlMGI3YjYy; Path=/; Secure; HttpOnly; SameSite=Lax"]
     Content type = application/json
             Body = {"id":14,"email":"doc-validate-ko-54ec7c7d-d766-4ef4-a420-d16ebfbf8aa8@x.fr","firstName":"Jean","lastName":"Dupont"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = [[MockCookie@60f66861 name = 'EFACTURE_SESSION', value = 'NTFiMTA0ZTUtMzgzNS00NWFlLWJmM2ItNjJmNTZlMGI3YjYy', comment = [null], domain = [null], maxAge = -1, path = '/', secure = true, version = 0, httpOnly = true]]

MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/organizations
       Parameters = {name=[Cabinet Test], _csrf=[DW8gGCoLLR0dJGtU5rBj05ZW3OOGDR0HTs5kNjx5r4P9tNT_OF1Fexg9HXgwElNk0J1XtqZi8YHlOi8qKv4GAghMnebK0LHJ]}
          Headers = [Cookie:"EFACTURE_SESSION=NTFiMTA0ZTUtMzgzNS00NWFlLWJmM2ItNjJmNTZlMGI3YjYy"]
             Body = null
    Session Attrs = {}

Handler:
             Type = com.facturx.app.organization.OrganizationController
           Method = com.facturx.app.organization.OrganizationController#create(String, Authentication)

Async:
    Async started = false
     Async result = null

Resolved Exception:
             Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", Content-Type:"application/json", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
     Content type = application/json
             Body = {"id":13,"name":"Cabinet Test","createdAt":"2026-09-28T15:59:16.629884812"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []
[ERROR] Tests run: 3, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.527 s <<< FAILURE! -- in com.facturx.app.document.DocumentValidationFlowTest
[ERROR] com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport -- Time elapsed: 0.167 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport(DocumentValidationFlowTest.java:106)

[ERROR] com.facturx.app.document.DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404 -- Time elapsed: 0.170 s <<< ERROR!
java.io.IOException: Sample not found on classpath: EN16931_Einfach.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404(DocumentValidationFlowTest.java:156)

[ERROR] com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant -- Time elapsed: 0.174 s <<< ERROR!
java.io.IOException: Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
	at com.facturx.app.document.DocumentValidationFlowTest.readSample(DocumentValidationFlowTest.java:96)
	at com.facturx.app.document.DocumentValidationFlowTest.uploadSample(DocumentValidationFlowTest.java:75)
	at com.facturx.app.document.DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant(DocumentValidationFlowTest.java:133)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   DocumentValidationFlowTest.reportForADocumentNeverValidatedReturns404:156->uploadSample:75->readSample:96 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant:133->uploadSample:75->readSample:96 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   DocumentValidationFlowTest.validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport:106->uploadSample:75->readSample:96 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationPermissionTest.clientCannotValidateADocument:131->uploadSample:97->readSample:116 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   DocumentValidationPermissionTest.userFromAnotherOrganizationCannotValidateADocument:155->uploadSample:97->readSample:116 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   FacturXValidationServiceTest.invalidSampleFailsAtPdfA3LayerAndStopsThere:49->readSample:67 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   FacturXValidationServiceTest.validSampleIsValidAndPersistsItsNotices:24->readSample:67 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   MustangValidationClientSmokeTest.validatesKnownInvalidSampleAndPrintsRawReport:32->readSample:45 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   MustangValidationClientSmokeTest.validatesRealSampleAndPrintsRawReport:18->readSample:45 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: plain-pdf-no-xml.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: missing-invoice-number.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: missing-type-code.pdf
[ERROR]   RuleCatalogCoverageTest.everyShownErrorHasAFrenchExplanation:43->readSample:97 IO Sample not found on classpath: wrong-total-amount.pdf
[ERROR]   RuleCatalogCoverageTest.missingInvoiceNumberRaisesTheExpectedCodes:57->readSample:97 IO Sample not found on classpath: missing-invoice-number.pdf
[ERROR]   RuleCatalogCoverageTest.missingTypeCodeRaisesTheExpectedCodes:67->readSample:97 IO Sample not found on classpath: missing-type-code.pdf
[ERROR]   RuleCatalogCoverageTest.plainPdfWithNoXmlRaisesTheExpectedCodes:87->readSample:97 IO Sample not found on classpath: plain-pdf-no-xml.pdf
[ERROR]   RuleCatalogCoverageTest.wrongTotalAmountRaisesTheExpectedCodes:77->readSample:97 IO Sample not found on classpath: wrong-total-amount.pdf
[ERROR]   ValidationControllerTest.invalidSampleReturnsValidFalseWithPdfA3Errors:59->sampleFile:86 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   ValidationControllerTest.unauthenticatedRequestIsRejected:29->sampleFile:86 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationControllerTest.validSampleReturnsValidTrue:47->sampleFile:86 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportControllerTest.reportIsReachableRightAfterValidating:49->sampleFile:87 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportServiceTest.invalidSampleReportExplainsThePdfA3Failure:55->readSample:102 IO Sample not found on classpath: veraPDFtestsuite6-7-11-t01-fail-a.pdf
[ERROR]   ValidationReportServiceTest.peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase:26->readSample:102 IO Sample not found on classpath: EN16931_Einfach.pdf
[ERROR]   ValidationReportServiceTest.validSampleReportHasNoErrorsAndHidesThePeppolNotice:38->readSample:102 IO Sample not found on classpath: EN16931_Einfach.pdf
[INFO] 
[ERROR] Tests run: 78, Failures: 0, Errors: 26, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  44.406 s
[INFO] Finished at: 2026-09-28T15:59:16Z
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.6:test (default-test) on project backend: 
[ERROR] 
[ERROR] See /home/runner/work/FacturX/FacturX/backend/target/surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Error: Process completed with exit code 1.
0s
0s
0s
0s
0s
1s
0s
0s
