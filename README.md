# wildfly-clustering-spring-session

A high-availability session repository implementation for Spring Session based on WildFly's distributed session management and Infinispan server.
This brings the same clustering features to Spring Session that one can expect from WildFly's distributed session management, including:

* Persists session data to a remote Infinispan cluster using either per session or per attribute granularity.
* Ability to configure the number of sessions to retain in local memory.
* Mutable session attribute semantics.
* Support for user supplied marshalling optimizations

## Building

1.	Clone this repository.

		$ git clone git@github.com:wildfly-clustering/wildfly-clustering-spring-session.git	
		$ cd wildfly-clustering-spring-session

1.	Build using Java 8 and Apache Maven 3.2.5+.

		$ mvn clean install

## Installation

Spring Session is intended to operate within any servlet container.
The following describes how to install wildfly-clustering-spring-session support into a Tomcat distribution:

1.	Enter directory of session manager implementation:

		$ cd hotrod

1.	Copy the maven artifact to Tomcat's lib directory:

		$ mvn dependency:copy -DoutputDirectory=$CATALINA_HOME/lib

1.	Copy runtime dependencies to Tomcat's lib directory:

		$ mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=$CATALINA_HOME/lib

## Configuration

Spring Session is traditionally enabled either via XML or annotations.
wildfly-clustering-spring-session includes an `@EnableHotRodHttpSession` annotation for annotation-based configuration, but this configuration mechanism currently deviates from the documentation of other session repositories.

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

However, this mechanism *cannot possibly work correctly* in a specification compliant servlet container.

Spring Session's auto-wiring initiates from the [`AbstractHttpSessionApplicationInitializer.onStartup(ServletContext)`](https://github.com/spring-projects/spring-session/blob/2.4.0/spring-session-core/src/main/java/org/springframework/session/web/context/AbstractHttpSessionApplicationInitializer.java#L107) method, where it dynamically registers a ServletContextListener.
Unfortunately, &sect;4.4 of the servlet specification is very specific about how a container should treat ServletContext events for dynamically registered listeners:

> If the ServletContext passed to the ServletContextListener’s contextInitialized method where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the methods defined in ServletContext for programmatic configuration of servlets, filters and listeners.

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

```java
// Auto-registers the requisite servlet filter used by Spring Session to intercept the request chain
public class MyHttpSessionApplicationInitializer extends AbstractHttpSessionApplicationInitializer { 
	public MyHttpSessionApplicationInitializer() {
		// Do not construct with any component classes
		// This skips dynamic registration of Spring Session's ServletContextListener
	}
}
```

```java
// Spring Session repository configuration
@EnableHotRodHttpSession(config = @HotRod(uri = "hotrod://127.0.0.1:11222?tcp_keep_alive=true"), manager = @SessionManager(granularity = SessionPersistenceGranularity.ATTRIBUTE))
public class Config {
	// ...
}
```

```java
@WebListener
public class MyContextLoaderListener extends org.wildfly.clustering.web.spring.context.ContextLoaderListener { 
	public MyContextLoaderListener() {
		// Specify spring session repository component class to super implementation
		super(Config.class);
	}
}
```

Alternatively, the Spring Session repository can be configured via XML rather than the `@EnableHotRodHttpSession` annotation.

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

### Configuration Properties

#### Implementation specific properties

|Property|Description|
|:---|:---|
|uri|Defines a HotRod URI, which includes a list of infinispan server instances and any authentication details. For details, see: https://infinispan.org/blog/2020/05/26/hotrod-uri/|
|templateName|Defines the server-side configuration template from which a deployment cache is created on the server. Default is `org.infinispan.DIST_SYNC`.|
|granularity|Defines how a session is mapped to entries in the cache. Supported granularities are enumerated by the `org.wildfly.clustering.web.spring.SessionPersistenceGranularity` enum. `SESSION` will store all attributes of a session in a single cache entry, while `ATTRIBUTE` will store each session attribute in a separate cache entry.  Default is `SESSION`.|
|maxActiveSessions|Defines the maximum number of sessions to retain in the near cache. Default is limitless. A value of 0 will disable the near cache.|
|marshallerFactory|Specifies the marshaller used to serialize and deserialize session attributes. Supported marshallers are enumerated by the `org.wildfly.clustering.web.spring.SessionMarshallerFactory` enum and include: `JAVA`, `JBOSS`, `PROTOSTREAM`. Default marshaller is `JBOSS`.|
|properties|Specifies additional configuration properties. The complete set of HotRod properties can be found here: https://github.com/infinispan/infinispan/blob/11.0.x/client/hotrod-client/src/main/java/org/infinispan/client/hotrod/impl/ConfigurationProperties.java|

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
