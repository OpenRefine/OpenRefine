FROM maven:3.6.3-openjdk-8

COPY . /refine
RUN apt-get update -y \
    && apt-get install -y curl \
    && /refine/refine build
EXPOSE 3333
WORKDIR /refine
ENTRYPOINT ["/refine/refine", "-i", "0.0.0.0", "run"]
