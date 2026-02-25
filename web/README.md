# wildfly-clustering-spring-web

wildfly-clustering-spring-web is a Spring Web module providing a number of reactive `WebSessionManager` implementations based on WildFly's distributed session management and Infinispan for use with Spring Flux.

## Installation

Spring Flux is intended to operate within any servlet container.
The following describes how to install wildfly-clustering-spring-web support into a Tomcat distribution:

1.	Enter directory of the desired session manager implementation:

		$ cd web/infinispan/embedded

	or:

		$ cd web/infinispan/remote

1.	Copy the maven artifact to Tomcat's `/lib` directory:

		$ mvn dependency:copy -DoutputDirectory=$CATALINA_HOME/lib

1.	Copy runtime dependencies to Tomcat's `/lib` directory:

		$ mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=$CATALINA_HOME/lib

## Configuration

Traditionally, users wanting to use a reactive session manager will configure a custom `WebSessionStore` for the default 'WebSessionManager', typically provided by Spring Session and backed by a `ReactiveSessionRepository`, either via XML or annotations.
This module provides distinct 'WebSessionManager' implementations that workaround significant issues with both the default `WebSessionManager` implementation provided by Spring Web and the `SpringSessionWebSessionStore` implementation provided by Spring Session:

1.	`SpringSessionWebSessionStore` relies on a thread local snapshot of a session attributes per request.
	Consequently, modifications made to a given session by one request thread are not visible to other concurrent request threads for the same session.

1.	At time time of this writing, the `WebSessionStore` SPI appears to be "half-baked".
	This SPI requires the implementation of two methods (`removeSession(String sessionId)` and `updateLastAccessTime(WebSession webSession)` that are never invoked by the default `WebSessionManager` implementation.

1	The default `WebSessionManager` chains multiple `WebSessionStore` invocations on the same thread (rather than delegating these to a separate thread pool).
	This can otherwise cause deadlocks if the `WebSessionStore` chained method implementations are not reentrant.

### Annotation-based Configuration

:warning: The following configuration instructions deviate from the "WebSession Integration" section of the Spring Session documentation.

The Spring MVC/Flux documentation directs users to provide an implementation of `org.springframework.web.WebApplicationInitializer`, which is supposed to auto-wire the requisite request filters and listeners.
However, this mechanism *cannot possibly work correctly* in a specification-compliant Jakarta Servlet container.
Spring Web's auto-wiring initiates from the [`AbstractReactiveWebInitializer.onStartup(ServletContext)`](https://github.com/spring-projects/spring-framework/blob/v6.1.0/spring-web/src/main/java/org/springframework/web/server/adapter/AbstractReactiveWebInitializer.java#L58) method, where it dynamically registers a `ServletContextListener`.
Unfortunately for Spring, &sect;4.4 of the Jakarta Servlet specification is very specific about how a container should treat ServletContext events for dynamically registered listeners:

> If the ServletContext passed to the ServletContextListener’s contextInitialized method where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the methods defined in ServletContext for programmatic configuration of servlets, filters and listeners.

Consequently, the only *feasible* way to configure Spring Session via annotations for use in a specification-compliant Jakarta Servlet container is to create the WebApplicationContext from an explicitly defined `ServletContextListener`, rather than from the `HttpSessionApplicationInitializer`.

1.	Define the reactive session manager configuration using one of the 2 annotations described later in this section and any beans required by [Spring Flux](https://docs.spring.io/spring-framework/reference/web/webflux/reactive-spring.html).
	e.g.

	```java
	@EnableHotRodWebSession(...)
	@Configuration(proxyBeanMethods = false)
	public class Config {
		@Bean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME)
		public WebHandler webHandler() {
			return new DispatcherHandler();
		}

		// Beans required by DispatcherHandler, per https://docs.spring.io/spring-framework/reference/web/webflux/dispatcher-handler.html

		@Bean
		public HandlerMapping handlerMapping() {
			return ...;
		}

		@Bean
		public HandlerAdapter handlerAdapter() {
			return ...;
		}
	}
	```

1.	By omitting a `WebApplicationInitializer` implementation, we have denied Spring the ability to dynamically register its `ServletContextListener`.
	In lieu of this, we create a proper servlet context listener extending `org.wildfly.clustering.spring.web.context.ContextLoaderListener` constructed with the component classes that would otherwise have been registered by `AbstractReactiveWebInitializer`.

	```java
	@WebListener
	public class SpringFluxContextLoaderListener extends org.wildfly.clustering.spring.web.context.ContextLoaderListener {
		public SpringFluxContextLoaderListener() {
			// Specify spring session repository component class to super implementation
			super(Config.class);
		}
	}
	```

#### WebSessionManager meta annotations

Users will configure wildfly-clustering-spring-web using one of the four annotations defined below.
The annotation type determines the implementation of the underlying session manager.
Each annotation type is a composite of some subset of component annotations.
See the corresponding linked section for the definition of a each component annotation.

* @EnableHotRodWebSession(manager = [@SessionManager](#sessionmanager), config = [@HotRod](#hotrod))
  * Configures a SessionRepository that persists sessions to a remote Infinispan cluster via HotRod.

* @EnableInfinispanWebSession(manager = [@SessionManager](#sessionmanager), config = [@Infinispan](#infinispan))
  * Configures a SessionRepository that persists sessions to an embedded Infinispan cache.

##### @SessionManager

This configuration component defines the behavior of the session manager that are common to each top-level configuration annotation.
The `@SessionManager` annotation defines the following properties:

|Property|Description|
|:---|:---|
|granularity|Defines how a session is mapped to entries in the cache. Supported granularities are enumerated by the `org.wildfly.clustering.spring.context.SessionPersistenceGranularity` enum. `SESSION` will store all attributes of a session in a single cache entry, while `ATTRIBUTE` will store each session attribute in a separate cache entry.  Default is `SESSION`.|
|marshaller|Specifies the marshaller used to serialize and deserialize session attributes. Supported marshallers are enumerated by the `org.wildfly.clustering.spring.context.SessionAttributeMarshaller` enum and include: `JAVA`, i.e. Java serialization; `JBOSS`, i.e. JBoss Marshalling; `PROTOSTREAM`, i.e. protobuf. Default marshaller is `JBOSS`.|
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

### XML-based Configuration

Alternatively, the `WebSessionManager` used by Spring Flux can be configured via XML rather than via annotation.
When configuring Spring Flux via XML, we also omit an `AbstractHttpSessionApplicationInitializer` implementation to prevent dynamic `ServletContextListener` registration, therefore we must also define manually the filter and listener required by Spring Flux.

`WEB-INF/web.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
		version="6.0">
	<filter>
		<filter-name>springSessionRepositoryFilter</filter-name>
		<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
		<async-supported>true</async-supported>
	</filter>
	<filter-mapping>
		<filter-name>springSessionRepositoryFilter</filter-name>
		<url-pattern>/*</url-pattern>
		<dispatcher>REQUEST</dispatcher>
		<dispatcher>ERROR</dispatcher>
		<dispatcher>ASYNC</dispatcher>
	</filter-mapping>
	<listener>
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

	<bean class="org.wildfly.clustering.spring.web.infinispan.remote.config.HotRodWebSessionConfiguration">
		<property name="uri">
			<value type="java.net.URI">hotrod://127.0.0.1:11222</value>
		</property>
		<property name="properties">
			<props>
				<prop key="infinispan.client.hotrod.tcp_keep_alive">true</prop>
			</props>
		</property>
		<property name="granularity">
			<value type="org.wildfly.clustering.spring.session.SessionPersistenceGranularity">SESSION</value>
		</property>
		<property name="marshaller">
			<value type="org.wildfly.clustering.spring.session.SessionAttributeMarshaller">PROTOSTREAM</value>
		</property>
		<property name="maxActiveSessions">1000</property>
	</bean>

	<bean name="webHandler" class="org.springframework.web.reactive.DispatcherHandler"/>
	<bean name="handlerAdapter" class="..."><!-- ... --></bean>
	<bean name="handlerMapping" class="..."><!-- ... --></bean>

</beans>
```
