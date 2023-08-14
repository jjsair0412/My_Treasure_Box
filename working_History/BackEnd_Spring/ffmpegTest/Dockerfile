FROM ubuntu:20.04

RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get clean \
    && apt-get update -y \
    && apt-get install -y openjdk-17-jdk \
    && apt-get install -y wget \
    && apt-get clean


WORKDIR /app

COPY ./build/libs/ffmpegTest-0.0.1-SNAPSHOT.jar /app/

RUN cd /usr/local/bin && \
    mkdir ffmpeg && \
    cd ffmpeg && \
    wget https://www.johnvansickle.com/ffmpeg/old-releases/ffmpeg-3.4.2-64bit-static.tar.xz && \
    tar -xvf /usr/local/bin/ffmpeg/ffmpeg-3.4.2-64bit-static.tar.xz && \
    ln -sfn /usr/local/bin/ffmpeg/ffmpeg-3.4.2-64bit-static/ffmpeg /usr/bin/ffmpeg && \
    ln -sfn /usr/local/bin/ffmpeg/ffmpeg-3.4.2-64bit-static/ffprobe /usr/bin/ffprobe


CMD ["java", "-jar", "/app/ffmpegTest-0.0.1-SNAPSHOT.jar"]
