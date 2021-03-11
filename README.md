# wildfly-clustering-spring-session

A high-availability session repository implementation for Spring Session based on WildFly's distributed session management and Infinispan server.
This brings the same clustering features to Spring Session that one can expect from WildFly's distributed session management, including:

* Persists session data to either an embeeded Infinispan cache or a remote Infinispan cluster using either per session or per attribute granularity.
* Ability to configure the number of sessions to retain in local memory.
* Mutable session attribute semantics.
* Support for user supplied marshalling optimizations

## Building

1.	Clone this repository.

		$ git clone git@github.com:wildfly-clustering/wildfly-clustering-spring-session.git	
		$ cd wildfly-clustering-spring-session

1.	Build using Java 8 or higher and Apache Maven 3.2.5+.

		$ mvn clean install

## Installation

Spring Session is intended to operate within any servlet container.
The following describes how to install wildfly-clustering-spring-session support into a Tomcat distribution:

1.	Enter directory of the desired session manager implementation:

		$ cd hotrod
	or:
		$ cd infinispan

1.	Copy the maven artifact to Tomcat's lib directory:

		$ mvn dependency:copy -DoutputDirectory=$CATALINA_HOME/lib

1.	Copy runtime dependencies to Tomcat's lib directory:

		$ mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=$CATALINA_HOME/lib

## Configuration

Spring Session is traditionally enabled either via XML or annotations.

### Annotation-based Configuration

Most users will likely configure wildfly-clustering-spring-session using one of the four annotations defined below.
The annotation type determines the implementation of the underlying session repository, as well as whether or not the repository supports indexing.
The configuration of each annotation is encapsulated by a number of common component annotations.
See the corresponding subsequent section for the definition of a given component annotation.

* @EnableHotRodHttpSession(manager = [@SessionManager](#sessionmanager), config = [@HotRod](#hotrod))
  * Configures a FindByIndexNameSessionRepository for use with Spring Security concurrent control that persists sessions to a remote Infinispan cluster via HotRod.

* @EnableHotRodIndexedHttpSession(manager = [@SessionManager](#sessionmanager), config = [@HotRod](#hotrod), indexing = [@Indexing](#indexing))
  * Configures a FindByIndexNameSessionRepository for use with Spring Security concurrent control that persists sessions to a remote Infinispan cluster via HotRod.

* @EnableInfinispanHttpSession(manager = [@SessionManager](#sessionmanager), config = [@Infinispan](#infinispan))
  * Configures a SessionRepository that persists sessions to an embedded Infinispan cache.

* @EnableInfinispanIndexedHttpSession(manager = [@SessionManager](#sessionmanager), config = [@Infinispan](#infinispan), indexing = [@Indexing](#indexing))
  * Configures a FindByIndexNameSessionRepository for use with Spring Security concurrent control that persists sessions to an embedded Infinispan cache.


---


:warning: The configuration mechanics for wildfly-clustering-spring-session deviates from the documentation of other session repositories.

The Spring Session documentation directs users to provide an implementation of `org.springframework.web.WebApplicationInitializer`, which is supposed to auto-wire the requisite request filters and listeners.

e.g.

```java
@EnableHotRodHttpSession(...)
public class Config {
	// ...
}
```
```java
public class MyApplicationInitializer extends AbstractHttpSessionApplicationInitializer { 
	public MyApplicationInitializer() {
		// This doesn't work!!!
		super(Config.class); 
	}
}
```
:exclamation: However, this mechanism *cannot possibly work correctly* in a specification compliant servlet container.
Spring Session's auto-wiring initiates from the [`AbstractHttpSessionApplicationInitializer.onStartup(ServletContext)`](https://github.com/spring-projects/spring-session/blob/2.4.0/spring-session-core/src/main/java/org/springframework/session/web/context/AbstractHttpSessionApplicationInitializer.java#L107) method, where it dynamically registers a ServletContextListener.
Unfortunately, &sect;4.4 of the servlet specification is very specific about how a container should treat ServletContext events for dynamically registered listeners:

> If the ServletContext passed to the ServletContextListener’s contextInitialized method where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the methods defined in ServletContext for programmatic configuration of servlets, filters and listeners.


---


Consequently, the only *feasible* way to configure Spring Session via annotations is to create the WebApplicationContext from an explicitly defined ServletContextListener, rather than from the HttpSessionApplicationInitializer.

e.g.

`/WEB-INF/applicationContext.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
		xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
	<context:annotation-config/>
</beans>
```

:information_source: Define an application initializer, but *do not* construct it with any component classes.

```java
// Auto-registers the requisite servlet filter used by Spring Session to intercept the request chain
public class MyHttpSessionApplicationInitializer extends AbstractHttpSessionApplicationInitializer { 
	public MyHttpSessionApplicationInitializer() {
		// Do not construct with any component classes
		// This skips dynamic registration of Spring Session's ServletContextListener
	}
}
```

Define your session repository configuration. e.g.

```java
// Spring Session repository configuration
@EnableHotRodHttpSession(config = @HotRod(uri = "hotrod://127.0.0.1:11222?tcp_keep_alive=true"), manager = @SessionManager(granularity = SessionPersistenceGranularity.ATTRIBUTE))
public class Config {
}
```

:information_source: Since we cannot let Spring register a web listener programmatically, we should instead do this manually using the standard servlet annotation.
This listener should extend `org.wildfly.clustering.web.spring.context.ContextLoaderListener` and be constructed with your component classes.

```java
@WebListener
public class MyContextLoaderListener extends org.wildfly.clustering.web.spring.context.ContextLoaderListener { 
	public MyContextLoaderListener() {
		// Specify spring session repository component class to super implementation
		super(Config.class);
	}
}
```

#### @SessionManager

This configuration component defines the behavior of the session manager and common to all of the configuration annotations.
The `@SessionManager` annotation defines the following properties:

|Property|Description|
|:---|:---|
|granularity|Defines how a session is mapped to entries in the cache. Supported granularities are enumerated by the `org.wildfly.clustering.web.spring.SessionPersistenceGranularity` enum. `SESSION` will store all attributes of a session in a single cache entry, while `ATTRIBUTE` will store each session attribute in a separate cache entry.  Default is `SESSION`.|
|marshallerFactory|Specifies the marshaller used to serialize and deserialize session attributes. Supported marshallers are enumerated by the `org.wildfly.clustering.web.spring.SessionMarshallerFactory` enum and include: `JAVA`, `JBOSS`, `PROTOSTREAM`. Default marshaller is `JBOSS`.|
|maxActiveSessions|Defines the maximum number of sessions to retain in the heap of a server, i.e. either the data container, for embedded Infinispan; or the HotRod near-cache, for a remote Infinispan cluster.  By default, embedded Infinispan will use an unbounded data container, while HotRod will disable its near-cache.|

#### @HotRod

This configuration component defines the HotRod client configuration used by a HotRod-based session repository.
The `@HotRod` annotation defines the following properties:

|Property|Description|
|:---|:---|
|uri|Defines a HotRod URI, which includes a list of infinispan server instances and any authentication details. For details, see: https://infinispan.org/blog/2020/05/26/hotrod-uri/|
|template|Defines the server-side configuration template from which a deployment cache is created on the server. Default is `org.infinispan.DIST_SYNC`.|

#### @Infinispan

This configuration component defines the HotRod client configuration used by an embedded Infinispan-based session repository.
The `@Infinispan` annotation defines the following properties:

|Property|Description|
|:---|:---|
|resource|Defines the location of an Infinispan XML configuration within the deployment. Default resource location is `/WEB-INF/infinispan.xml`|
|template|Defines the name of the configuration template defined within the configuration XML from which a deployment cache will be created. If undefined, the configuration of the default cache configuration will be used.|

An example Infinispan XML can be found [here](https://github.com/wildfly-clustering/wildfly-clustering-spring-session/blob/master/infinispan/src/test/java/org/wildfly/clustering/web/spring/infinispan/infinispan.xml)

#### @Indexing

This configuration component defines the indexing configuration used indexing session repository.
An indexing session repository allows one to lookup a given Spring Session by an index session attribute.
The primary use case of an indexing session repository is to support Spring Security concurrency control.
The `@Indexing` annotation defines the following properties:

|Property|Description|
|:---|:---|
|indexes|Defines an array of @Index definitions that defines the index identifiers and attribute names by which sessions should should be indexed. Default value is configured for use with Spring Security, i.e. `{ @Index(id = "SPRING_SECURITY_CONTEXT", name = "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME") }`|
|resolverClass|Defines the class of the IndexResolver used to resolve all indexes for a given session.  Default value is configured for use with Spring Security, i.e. `org.springframework.session.PrincipalNameIndexResolver.class`|

### XML-based Configuration

Alternatively, the Spring Session repository can be configured via XML rather than via annotation.
When configuring Spring Session via XML, you must also define the ServletContextListener declaratively, for the reasons described in the previous section.

'WEB-INF/web.xml':

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3.1.xsd"
		version="3.1">
	<listener>
		<!-- We need to declare the ServletContextListener explicitly -->
		<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
	</listener>
</web-app>
```

`/WEB-INF/applicationContext.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
			xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
	<context:annotation-config/>

	<bean class="org.wildfly.clustering.web.spring.hotrod.annotation.HotRodHttpSessionConfiguration">
		<property name="uri">
			<value type="java.net.URI">hotrod://127.0.0.1:11222</value>
		</property>
		<property name="properties">
			<props>
				<prop key="infinispan.client.hotrod.tcp_keep_alive">true</prop>
			</props>
		</property>
		<property name="granularity">
			<value type="org.wildfly.clustering.web.spring.SessionPersistenceGranularity">SESSION</value>
		</property>
		<property name="marshallerFactory">
			<value type="org.wildfly.clustering.web.spring.SessionMarshallerFactory">PROTOSTREAM</value>
		</property>
		<property name="maxActiveSessions">1000</property>
	</bean>
</beans>
```

## Notes

Because Spring Session operates entirely within userspace (i.e. external to the servlet container), its session management behavior will inherently deviate from the servlet specification in several areas.
In particular, applications using Spring Session should be aware of the following aberrant behavior:

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionListener` declared in web.xml or annotated with @WebListener) of newly created or destroyed sessions.
   You must instead rely on Spring's own event mechanism.

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionAttributeListener` declared in web.xml or annotated with @WebListener) of new, replaced and removed session attributes.
   As far as I am aware, Spring has no mechanism for these events.

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionIdListener` declared in web.xml or annotated with @WebListener) of session identifier changes resulting from `HttpServletRequest.changeSessionId()`.
   You must instead rely on Spring's own event mechanism.

1. Applications using Spring Session will generally need to rely on Spring Security for authentication and authorization.
   Many authentication mechanisms store user identity in the HttpSession or will need to change the session ID following authentication - a common practice for preventing session fixation attacks.
   Since the servlet container has no access to sessions created by Spring, most container managed security mechanisms will not work.
