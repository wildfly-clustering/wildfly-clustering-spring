# wildfly-clustering-spring-session

A session repository implementation for Spring Session based on WildFly's distributed session management.
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
wildfly-clustering-spring-session includes an `@EnableHotRodHttpSession` annotation for annotation-based configuration, but this configuration mechanism is currently non-functional for reasons explained below.

The Spring Session documentation directs users to provide an implementation of `org.springframework.web.WebApplicationInitializer`, which is supposed to auto-wire the requisite request filters and listeners.

e.g.

	@EnableHotRodHttpSession(...)
	public class Config {
		// ...
	}

	public class MyApplicationInitializer extends AbstractHttpSessionApplicationInitializer { 
		public MyApplicationInitializer() {
			super(Config.class); 
		}
	}

However, this mechanism cannot possibly work correctly in a specification compliant servlet container.

Spring Session's auto-wiring initiates from the [`AbstractHttpSessionApplicationInitializer.onStartup(ServletContext)`](https://github.com/spring-projects/spring-session/blob/2.3.0.RELEASE/spring-session-core/src/main/java/org/springframework/session/web/context/AbstractHttpSessionApplicationInitializer.java#L107) method, where it dynamically registers a ServletContextListener.
Unfortunately, &sect;4.4 of the servlet specification is very specific about how a container should treat ServletContext events for dynamically registered listeners:

> If the ServletContext passed to the ServletContextListener’s contextInitialized method where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the methods defined in ServletContext for programmatic configuration of servlets, filters and listeners.

Consequently, the only *feasible* way to configure Spring Session requires manually specifying the requisite listeners (to capture the `ServletContext`) and filters (intercept and wrap the request) within web.xml (or fragment).

	<?xml version="1.0" encoding="UTF-8"?>
	<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3.1.xsd"
			version="3.1">

		<listener>
			<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
		</listener>
		<filter>
			<filter-name>springSessionRepositoryFilter</filter-name>
			<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
		</filter>
		<filter-mapping>
			<filter-name>springSessionRepositoryFilter</filter-name>
			<url-pattern>/*</url-pattern>
			<dispatcher>REQUEST</dispatcher>
			<dispatcher>ERROR</dispatcher>
		</filter-mapping>
	</web-app>


Until the `@EnableHotRodHttpSession` annotation is functional, you must provide the configuration of the distributed session repository via Spring XML.

The following is a sample `/WEB-INF/applicationContext.xml`:

	<?xml version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://www.springframework.org/schema/beans"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
			xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

		<context:annotation-config/>

		<bean class="org.wildfly.clustering.web.spring.hotrod.annotation.HotRodHttpSessionConfiguration">
		    <property name="properties">
		        <props>
		            <prop key="infinispan.client.hotrod.server_list">127.0.0.1:11222</prop>
		        </props>
		    </property>
		    <property name="persistenceStrategy">
		        <value type="org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy">FINE</value>
		    </property>
		    <property name="maxActiveSessions">1000</property>
		</bean>
	</beans>

### Configuration Properties

#### Implementation specific properties

|Property|Description|
|:---|:---|
|configurationName|Defines the server-side configuration template from which a deployment cache is created on the server.  If undefined, the configuration of the server's default cache will be used.|
|persistenceStrategy|Defines how a session is mapped to entries in the cache. "COARSE" will store all attributes of a session in a single cache entry.  "FINE" will store each session attribute in a separate cache entry.  Default is "COARSE".|
|maxActiveSessions|Defines the maximum number of sessions to retain in the near cache. Default is boundless. A value of 0 will disable the near cache.|

#### HotRod properties

The complete set of HotRod properties can be found here:

https://github.com/infinispan/infinispan/blob/10.1.x/client/hotrod-client/src/main/java/org/infinispan/client/hotrod/impl/ConfigurationProperties.java

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
   Many authentication mechanisms store user identity in the HttpSession or will need to change the session ID following authetication - a common practice for preventing session fixation attacks.
   Since the servlet container has no access to sessions created by Spring, most container managed security mechanisms will not work.
