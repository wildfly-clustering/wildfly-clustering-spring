[![Version](https://img.shields.io/maven-central/v/org.wildfly.clustering/wildfly-clustering-spring-session?style=for-the-badge&logo=redhat&logoColor=ee0000&label=latest)](https://search.maven.org/artifact/org.wildfly.clustering/wildfly-clustering-spring-session)
[![License](https://img.shields.io/github/license/wildfly-clustering/wildfly-clustering-spring-session?style=for-the-badge&color=darkgreen&logo=apache&logoColor=d22128)](https://www.apache.org/licenses/LICENSE-2.0)
[![Project Chat](https://img.shields.io/badge/zulip-chat-lightblue.svg?style=for-the-badge&logo=zulip&logoColor=ffffff)](https://wildfly.zulipchat.com/#narrow/stream/wildfly-clustering)

# wildfly-clustering-spring-session

wildfly-clustering-spring-session is a Spring Session module based on WildFly's distributed session management and Infinispan.
This brings the same clustering features to Spring Session that one can expect from WildFly's distributed session management, including:

* Servlet 5.0 specification compliance (excluding [limitations inherent to Spring Session](#notes)).
  * Including support for standard session event notifications
* Session attribute replication via an embedded cache or persitence to a remote Infinispan cluster.
* Configurable session replication/persistence strategies, i.e. per session vs per attribute.
* Similar semantics to that of an in-memory session repository, including a high level of consistency under concurrent request access, and support for mutable session attributes.
* Ability to limit the number of active sessions to retain in local memory
* Configurable session attribute marshalling.
  * Support for user-supplied marshalling optimizations.

## Building

1.	Clone this repository.

		$ git clone git@github.com:wildfly-clustering/wildfly-clustering-spring-session.git	
		$ cd wildfly-clustering-spring-session

1.	Build using Java 17 or higher and Apache Maven 3.8.x or higher.

		$ mvn clean install

## Installation

Spring Session is intended to operate within any servlet container.
The following describes how to install wildfly-clustering-spring-session support into a Tomcat distribution:

1.	Enter directory of the desired session manager implementation:

		$ cd hotrod

	or:

		$ cd infinispan

1.	Copy the maven artifact to Tomcat's `/lib` directory:

		$ mvn dependency:copy -DoutputDirectory=$CATALINA_HOME/lib

1.	Copy runtime dependencies to Tomcat's `/lib` directory:

		$ mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=$CATALINA_HOME/lib

## Configuration

Spring Session is traditionally enabled via XML or annotations.

### Annotation-based Configuration

:warning: The configuration instructions for wildfly-clustering-spring-session deviate from the documentation of other Spring Session modules.

#### How *not* to configure wildfly-clustering-spring-session

The Spring Session documentation directs users to provide an implementation of `org.springframework.web.WebApplicationInitializer`, which is supposed to auto-wire the requisite request filters and listeners.

e.g.

```java
@EnableJdbcHttpSession
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

However, this mechanism *cannot possibly work correctly* in a specification-compliant servlet container.
Spring Session's auto-wiring initiates from the [`AbstractHttpSessionApplicationInitializer.onStartup(ServletContext)`](https://github.com/spring-projects/spring-session/blob/2.4.0/spring-session-core/src/main/java/org/springframework/session/web/context/AbstractHttpSessionApplicationInitializer.java#L107) method, where it dynamically registers a `ServletContextListener`.
Unfortunately, &sect;4.4 of the servlet specification is very specific about how a container should treat ServletContext events for dynamically registered listeners:

> If the ServletContext passed to the ServletContextListener’s contextInitialized method where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the methods defined in ServletContext for programmatic configuration of servlets, filters and listeners.

Consequently, the only *feasible* way to configure Spring Session via annotations is to create the WebApplicationContext from an explicitly defined `ServletContextListener`, rather than from the `HttpSessionApplicationInitializer`.

#### How to properly configure wildfly-clustering-spring-session

1. Define a `/WEB-INF/applicationContext.xml`, e.g.

	```xml
	<?xml version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://www.springframework.org/schema/beans"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
			xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
		<context:annotation-config/>
	</beans>
	```

1. Define an application initializer, but *do not* construct it with any component classes.

	```java
	// Auto-registers the requisite servlet filter used by Spring Session to intercept the request chain
	public class MyHttpSessionApplicationInitializer extends AbstractHttpSessionApplicationInitializer { 
		public MyHttpSessionApplicationInitializer() {
			// Do not construct with any component classes
			// This skips dynamic registration of Spring Session's ServletContextListener
		}
	}
	```

1. Define the session repository configuration using one of 4 annotations enumerated below.

	```java
	// Spring Session repository configuration
	@EnableHotRodHttpSession(...)
	public class Config {
	}
	```

1. Since we have denied Spring the ability to register its `ServletContextListener` programmatically, we must instead define one explicitly.
	This listener should extend `org.wildfly.clustering.web.spring.context.ContextLoaderListener` and be constructed with the component classes that would normally be registered via the application initializer.

	```java
	@WebListener
	public class MyContextLoaderListener extends org.wildfly.clustering.web.spring.context.ContextLoaderListener { 
		public MyContextLoaderListener() {
			// Specify spring session repository component class to super implementation
			super(Config.class);
		}
	}
	```

Users will configure wildfly-clustering-spring-session using one of the four annotations defined below.
The annotation type determines the implementation of the underlying session repository, as well as whether or not the repository supports indexing.
Each annotation type is a composite of some subset of four component annotations.
See the corresponding linked section for the definition of a each component annotation.

* @EnableHotRodHttpSession(manager = [@SessionManager](#sessionmanager), config = [@HotRod](#hotrod))
  * Configures a SessionRepository that persists sessions to a remote Infinispan cluster via HotRod.

* @EnableHotRodIndexedHttpSession(manager = [@SessionManager](#sessionmanager), config = [@HotRod](#hotrod), indexing = [@Indexing](#indexing))
  * Configures a FindByIndexNameSessionRepository that persists sessions to a remote Infinispan cluster via HotRod.

* @EnableInfinispanHttpSession(manager = [@SessionManager](#sessionmanager), config = [@Infinispan](#infinispan))
  * Configures a SessionRepository that persists sessions to an embedded Infinispan cache.

* @EnableInfinispanIndexedHttpSession(manager = [@SessionManager](#sessionmanager), config = [@Infinispan](#infinispan), indexing = [@Indexing](#indexing))
  * Configures a FindByIndexNameSessionRepository that persists sessions to an embedded Infinispan cache.

##### @SessionManager

This configuration component defines the behavior of the session manager that are common to each top-level configuration annotation.
The `@SessionManager` annotation defines the following properties:

|Property|Description|
|:---|:---|
|granularity|Defines how a session is mapped to entries in the cache. Supported granularities are enumerated by the `org.wildfly.clustering.web.spring.SessionPersistenceGranularity` enum. `SESSION` will store all attributes of a session in a single cache entry, while `ATTRIBUTE` will store each session attribute in a separate cache entry.  Default is `SESSION`.|
|marshallerFactory|Specifies the marshaller used to serialize and deserialize session attributes. Supported marshallers are enumerated by the `org.wildfly.clustering.web.spring.SessionMarshallerFactory` enum and include: `JAVA`, i.e. Java serialization; `JBOSS`, i.e. JBoss Marshalling; `PROTOSTREAM`, i.e. protobuf. Default marshaller is `JBOSS`.|
|maxActiveSessions|Defines the maximum number of sessions to retain within the data container, for embedded Infinispan; or within the HotRod near-cache, for a remote Infinispan cluster.  By default, embedded Infinispan will use an unbounded data container, while HotRod will disable its near-cache.|

##### @HotRod

This configuration component defines the HotRod client configuration used by the HotRod-based session repository.
The `@HotRod` annotation defines the following properties:

|Property|Description|
|:---|:---|
|uri|Defines a HotRod URI, which includes a list of infinispan server instances and any authentication details. For details, see: https://infinispan.org/blog/2020/05/26/hotrod-uri/|
|template|Defines the server-side configuration template from which a deployment cache is created on the server. Default is `org.infinispan.DIST_SYNC`.|

##### @Infinispan

This configuration component defines the HotRod client configuration used by an embedded Infinispan-based session repository.
The `@Infinispan` annotation defines the following properties:

|Property|Description|
|:---|:---|
|resource|The location of an Infinispan XML configuration within the deployment. Default resource location is `/WEB-INF/infinispan.xml`|
|template|The name of the configuration template defined within the configuration XML from which a deployment cache will be created. If undefined, the configuration of the default cache configuration will be used.|

An example Infinispan XML can be found [here](https://github.com/wildfly-clustering/wildfly-clustering-spring-session/blob/master/infinispan/src/test/java/org/wildfly/clustering/web/spring/infinispan/infinispan.xml)
 
N.B. If the corresponding [@SessionManager](#sessionmanager) defines a value for maxActiveSessions, the Infinispan configuration should include a passivating or persistent cache store, e.g. `<file-store/>`.

##### @Indexing

This configuration component defines the indexing configuration used by an indexing session repository.
An indexing session repository allows the user to look up a given Spring Session by an indexed session attribute.
The primary use case of an indexing session repository is to support Spring Security concurrency control.
The `@Indexing` annotation defines the following properties:

|Property|Description|
|:---|:---|
|indexes|Defines an array of @Index definitions that defines the index identifiers and attribute names by which sessions should should be indexed. Default value is configured for use with Spring Security concurrency control, i.e. `{ @Index(id = "SPRING_SECURITY_CONTEXT", name = "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME") }`|
|resolverClass|Defines the class of the `IndexResolver` used to resolve all indexes for a given session.  Default value is configured for use with Spring Security concurrency control, i.e. `org.springframework.session.PrincipalNameIndexResolver.class`|

### XML-based Configuration

Alternatively, the Spring Session repository can be configured via XML rather than via annotation.
When configuring Spring Session via XML, you must also define the ServletContextListener declaratively, for the reasons described in the previous section.

'WEB-INF/web.xml':

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
		version="5.0">
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

Because Spring Session operates entirely within user space (i.e. external to the servlet container), its session management behavior will inherently deviate from the servlet specification in several areas.
In particular, applications using Spring Session should be aware of the following aberrant behavior:

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionListener` declared in web.xml or annotated with @WebListener) of newly created or destroyed sessions.
   Users must instead rely on Spring's own event mechanism.

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionAttributeListener` declared in web.xml or annotated with @WebListener) of new, replaced and removed session attributes.
   Spring has no mechanism for triggering these events.

1. Spring Session lacks any facility to notify standard listeners (instances of `HttpSessionIdListener` declared in web.xml or annotated with @WebListener) of session identifier changes resulting from `HttpServletRequest.changeSessionId()`.
   Users must instead rely on Spring's own event mechanism.

1. Applications using Spring Session will generally need to rely on Spring Security for authentication and authorization.
   Many authentication mechanisms store user identity in the `HttpSession` or will need to change the session ID following authentication - a common practice for preventing session fixation attacks.
   Since the servlet container has no access to sessions created by Spring, most container managed security mechanisms will not work.
