FROM openjdk:8-jre
MAINTAINER Karsten Thoms <karsten.thoms@itemis.de>

ENTRYPOINT ["/usr/share/p2-query-service/eclipse"]

ADD target/products/p2-query-service/linux/gtk/x86_64 /usr/share/p2-query-service
RUN ["chmod", "+x", "/usr/share/p2-query-service/eclipse"]

EXPOSE 8080
