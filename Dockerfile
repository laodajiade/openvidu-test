FROM 172.25.10.100:5555/jdk/jdk:base
COPY openvidu-server/target/openvidu-server-2.11.0.jar /opt/sudi/openvidu/
WORKDIR /opt/sudi/openvidu/
EXPOSE 4444 9999 18719
ENV  TZ=Asia/Shanghai
ENTRYPOINT ["java","-server","-XX:MaxDirectMemorySize=100m","-Xms500m","-Xmx500m","-XX:NewRatio=3","-XX:+HeapDumpOnOutOfMemoryError","-jar","openvidu-server-2.11.0.jar","--spring.profiles.active=locals"]
