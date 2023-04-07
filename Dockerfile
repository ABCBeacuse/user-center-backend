# docker 镜像依赖于哪一个基础镜像，SpringBoot 项目依赖于Maven 和 java ，
# 下面这个 maven:3.5-jdk-8-alpine as builder 镜像打包了 maven 3.5 和 java 8
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
# 指定镜像的工作目录，所有的项目代码尽量都放在工作目录中
WORKDIR /app
 # copy 将本地的代码复制到容器中，maven 根据 pom.xml 去构建项目，后面的 . 就是当前目录 /app
COPY pom.xml .
# 将本地的源码目录复制到 /app/src 目录下
COPY src ./src

# Build a release artifact. 执行 maven 的打包命令
RUN mvn package -DskipTests

# 通过上面的命令我们已经制作好镜像了，之后我们需要运行镜像时，需要执行的命令。
# Run the web service on container startup. 注意一下底下的 jar 包名
CMD ["java","-jar","/app/target/yupao-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]
# 这行命令是可以在运行容器的时候覆盖的，docker run 跟一些参数