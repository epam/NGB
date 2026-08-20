# NGB dev toolbox: everything needed to build and run NGB, on both JDK 8 and JDK 21.
#
# Why both JDKs: the Gradle wrapper is pinned to Gradle 3.3, which only runs on JDK 8.
# The Java 21 migration therefore has to keep the JDK 8 baseline buildable while the
# new toolchain is introduced. `with-java8` / `with-java21` switch per command.
#
# The same image is used for the builder, the app runtime and the CLI, so a jar built
# here runs on exactly the JDK you point at it, with no second image to keep in sync.

FROM eclipse-temurin:8-jdk-jammy AS jdk8

FROM eclipse-temurin:21-jdk-jammy

ARG NODE_VERSION=14.17.5
ARG MKDOCS_VERSION=1.5.3
ARG MKDOCS_MATERIAL_VERSION=9.5.3

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      ca-certificates curl wget git unzip xz-utils gzip tar bzip2 \
      python3 python3-venv \
      openssl jq gettext-base \
      postgresql-client \
      procps less nano \
 && rm -rf /var/lib/apt/lists/*

# muscle is needed by the target-identification alignment feature (`muscle.path`).
# It lives in Ubuntu universe and may be missing for some architectures - not fatal.
RUN apt-get update \
 && (apt-get install -y --no-install-recommends muscle \
     || echo "WARNING: 'muscle' package unavailable for this architecture; sequence alignment features will be disabled") \
 && rm -rf /var/lib/apt/lists/*

# --- JDK 8 alongside the image's JDK 21 -------------------------------------
COPY --from=jdk8 /opt/java/openjdk /opt/java/jdk8
ENV JAVA_HOME_8=/opt/java/jdk8
ENV JAVA_HOME_21=/opt/java/openjdk

# --- Node.js pinned to the version client/build.gradle expects --------------
RUN set -eux; \
    arch="$(dpkg --print-architecture)"; \
    case "$arch" in \
      amd64) narch=x64 ;; \
      arm64) narch=arm64 ;; \
      *) echo "unsupported architecture: $arch" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/node.tar.xz \
      "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${narch}.tar.xz"; \
    mkdir -p /opt/node; \
    tar -xJf /tmp/node.tar.xz -C /opt/node --strip-components=1; \
    rm /tmp/node.tar.xz; \
    /opt/node/bin/node -v

ENV PATH=/opt/node/bin:$PATH

# --- mkdocs: the root `buildJar` task depends on `buildDocs` ----------------
RUN python3 -m venv /opt/mkdocs \
 && /opt/mkdocs/bin/pip install --no-cache-dir \
      "mkdocs==${MKDOCS_VERSION}" "mkdocs-material==${MKDOCS_MATERIAL_VERSION}" \
 && ln -s /opt/mkdocs/bin/mkdocs /usr/local/bin/mkdocs \
 && mkdocs --version

# --- JDK switching helpers -------------------------------------------------
RUN set -eux; \
    printf '#!/bin/sh\nexport JAVA_HOME="%s"\nexport PATH="$JAVA_HOME/bin:$PATH"\nexec "$@"\n' \
      "$JAVA_HOME_8" > /usr/local/bin/with-java8; \
    printf '#!/bin/sh\nexport JAVA_HOME="%s"\nexport PATH="$JAVA_HOME/bin:$PATH"\nexec "$@"\n' \
      "$JAVA_HOME_21" > /usr/local/bin/with-java21; \
    chmod +x /usr/local/bin/with-java8 /usr/local/bin/with-java21

# Interactive convenience: `use-java 21` inside a shell.
RUN printf '%s\n' \
  'use-java() {' \
  '  case "$1" in' \
  '    8)  export JAVA_HOME="$JAVA_HOME_8" ;;' \
  '    21) export JAVA_HOME="$JAVA_HOME_21" ;;' \
  '    *)  echo "usage: use-java 8|21" >&2; return 1 ;;' \
  '  esac' \
  '  export PATH="$JAVA_HOME/bin:/opt/node/bin:/usr/local/bin:/usr/bin:/bin"' \
  '  java -version' \
  '}' \
  > /etc/profile.d/ngb-java.sh

# Default to JDK 8 so the existing Gradle 3.3 build works out of the box.
ENV JAVA_HOME=/opt/java/jdk8
ENV PATH=$JAVA_HOME/bin:$PATH

ENV GRADLE_USER_HOME=/cache/gradle
ENV npm_config_cache=/cache/npm
ENV GRADLE_OPTS="-Xmx2g -Dorg.gradle.daemon=false"

WORKDIR /workspace
