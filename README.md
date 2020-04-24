# wildfly-clustering-spring-session

A distributed session manager for Spring Session based on WildFly's distributed session management.


## Building

1.	Clone this repository and build using a standard maven build.

		$ git clone git@github.com:wildfly-clustering/wildfly-clustering-spring-session.git
		$ cd wildfly-clustering-spring-session
		$ mvn clean install

## Installation

Spring Session is intended to function in any servlet container.
The following describes how to install wildfly-clustering-spring-session support for Tomcat:

1.	Enter directory of session manager implementation:

		$ cd hotrod

1.	Copy the maven artifact to Tomcat's lib directory:

		$ mvn dependency:copy -DoutputDirectory=$CATALINA_HOME/lib

1.	Copy runtime dependencies to Tomcat's lib directory:

		$ mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=$CATALINA_HOME/lib

## Configuration

Spring Session is traditionally enabled either via XML or annotations.
While wildfly-clustering-spring-session includes an @EnableHotRodHttpSession annotation, which is supposed to auto-wire the requisite request filters and listeners.
However, due to one or more bugs in Spring Session core, this does not work without violating the servlet specificiation.
Thus we need to manually modify web.xml with the requsite filter for Spring to intercept and wrap the request, as well as a listener capture the servlet context:

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


Until the @EnableHotRotHttpSession annotation is functional, you must provide the configuration for the distributed session repository via Spring XML.
Here is a sample /WEB-INF/applicationContext.xml:

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
|maxActiveSessions|Defines the maximum number of sessions to retain in the near cache. Default is limitless. A value of 0 will disable the near cache.|

#### HotRod properties

The complete set of HotRod properties can be found here:

https://github.com/infinispan/infinispan/blob/9.4.x/client/hotrod-client/src/main/java/org/infinispan/client/hotrod/impl/ConfigurationProperties.java

