[![Version](https://img.shields.io/maven-central/v/org.wildfly.clustering/wildfly-clustering-spring?style=for-the-badge&logo=redhat&logoColor=ee0000&label=latest)](https://search.maven.org/artifact/org.wildfly.clustering/wildfly-clustering-spring)
[![License](https://img.shields.io/github/license/wildfly-clustering/wildfly-clustering-spring?style=for-the-badge&color=darkgreen&logo=apache&logoColor=d22128)](https://www.apache.org/licenses/LICENSE-2.0)
[![Project Chat](https://img.shields.io/badge/zulip-chat-lightblue.svg?style=for-the-badge&logo=zulip&logoColor=ffffff)](https://wildfly.zulipchat.com/#narrow/stream/wildfly-clustering)

# wildfly-clustering-spring

wildfly-clustering-spring is a set of Spring modules providing distributed session management for Spring Session and Spring Web (including Spring MVC and Spring Flux) using Infinispan.
This brings the same clustering features of WildFly's distributed session managers to the Spring ecosystem, including:

* Servlet 6.0 specification compliance (excluding [limitations inherent to Spring Session and Spring Web](#notes)).
  * Including support for standard session event notifications
* Session attribute replication via an embedded cache or persistence to a remote Infinispan cluster.
* Configurable session replication/persistence strategies, i.e. per session vs per attribute.
* Similar semantics to that of an in-memory session repository, including a high level of consistency under concurrent request access, and support for mutable session attributes.
* Ability to limit the number of active sessions to retain in local memory
* Configurable session attribute marshallers.

## Building

1.	Clone this repository.

		$ git clone git@github.com:wildfly-clustering/wildfly-clustering-spring.git
		$ cd wildfly-clustering-spring

1.	Build using Java 17 or higher and Apache Maven 3.8.x or higher.

		$ mvn clean install

## Modules

* [Spring Session](session/README.md)
* [Spring Web](web/README.md)

## Notes

Because Spring Session and Spring Web operate entirely within user/application space (i.e. external to a servlet container's native session manager), its session management behavior will inherently deviate from the Jakarta Servlet specification in several areas.
In particular, applications using Spring Session or Spring Web should be aware of the following aberrant behavior that affects *every* SessionRepository/WebSessionManager implementation:

1.	`ServletContext` methods affecting session behavior, e.g. [`ServletContext.setSessionTimeout(int)`](https://jakarta.ee/specifications/platform/10/apidocs/jakarta/servlet/servletcontext#setSessionTimeout(int)), do not propagate to the `SessionRepository` or `WebSessionManager` implementation and thus will not affect runtime behavior.

1.	Spring Session and Spring Web lack any facility to notify standard listeners (instances of `HttpSessionListener` declared in web.xml or annotated with @WebListener) of newly created or destroyed sessions.
	Users must instead rely on Spring's own event mechanism.

1.	Spring Session and Spring Web lack any facility to notify standard listeners (instances of `HttpSessionAttributeListener` declared in web.xml or annotated with @WebListener) of new, replaced and removed session attributes.
	Spring has no mechanism for triggering these events.

1.	Spring Session and Spring Web lack any facility to notify standard listeners (instances of `HttpSessionIdListener` declared in web.xml or annotated with @WebListener) of session identifier changes resulting from `HttpServletRequest.changeSessionId()`.
	Users must instead rely on Spring's own event mechanism.

1.	Applications using Spring Session or Spring Web will generally need to rely on Spring Security for authentication and authorization.
	Many authentication mechanisms store user identity in the `HttpSession` or will need to change the session ID following authentication - a common practice for preventing session fixation attacks.
	Since the servlet container has no access to sessions created by Spring, most container managed security mechanisms will not work.
