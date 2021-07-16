FROM 172.25.10.100:5555/jdk/jdk:base
COPY openvidu-server/target/openvidu-server-2.11.0.jar /opt/sudi/openvidu/
WORKDIR /opt/sudi/openvidu/
EXPOSE 4444 9999 18719
ENV  TZ=Asia/Shanghai
#ENTRYPOINT ["java","-server","-XX:MaxDirectMemorySize=100m","-XX:+UnlockExperimentalVMOptions","-XX:+UseCGroupMemoryLimitForHeap","-XX:NewRatio=3","-XX:+HeapDumpOnOutOfMemoryError","-jar","openvidu-server-2.11.0.jar","--spring.profiles.active=locals"]
ENTRYPOINT ["java","-server","-XX:MaxDirectMemorySize=100m","-XX:+UnlockExperimentalVMOptions","-XX:+UseCGroupMemoryLimitForHeap","-XX:NewRatio=3","-XX:+HeapDumpOnOutOfMemoryError","-jar","openvidu-server-2.11.0.jar","--spring.profiles.active=locals"]
