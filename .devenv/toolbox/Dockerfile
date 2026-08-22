# NGB dev toolbox: everything needed to build and run NGB.
#
# JDK 21 is the default and runs everything - Gradle 8.14.5, the server module's 21 toolchain,
# the jar. JDK 17 is here for one reason: `server/ngb-cli` declares
# `toolchain { languageVersion = 17 }`, so that the CLI keeps running on a 17 JRE, and
# `org.gradle.java.installations.auto-download=false` in the root gradle.properties means Gradle
# must *find* a 17 installation rather than fetch one. It discovers /opt/java/jdk17 as a "Common
# Linux Location", so no configuration is needed beyond installing it. `with-java17` /
# `with-java21` (or `use-java 17|21` in a shell) switch per command; nothing in the build needs
# any other JDK, so no other JDK is installed.
#
# The same image is used for the builder, the app runtime and the CLI, so a jar built
# here runs on exactly the JDK you point at it, with no second image to keep in sync.

FROM eclipse-temurin:17-jdk-jammy AS jdk17

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

# --- JDK 17 alongside the image's JDK 21, for ngb-cli's toolchain -----------
COPY --from=jdk17 /opt/java/openjdk /opt/java/jdk17
ENV JAVA_HOME_17=/opt/java/jdk17
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
# with-java21 is a no-op now that 21 is the default; it stays so that a command can say which
# JDK it means, and so that `with-java17 <cmd>` has a symmetrical partner.
RUN set -eux; \
    printf '#!/bin/sh\nexport JAVA_HOME="%s"\nexport PATH="$JAVA_HOME/bin:$PATH"\nexec "$@"\n' \
      "$JAVA_HOME_17" > /usr/local/bin/with-java17; \
    printf '#!/bin/sh\nexport JAVA_HOME="%s"\nexport PATH="$JAVA_HOME/bin:$PATH"\nexec "$@"\n' \
      "$JAVA_HOME_21" > /usr/local/bin/with-java21; \
    chmod +x /usr/local/bin/with-java17 /usr/local/bin/with-java21

# Interactive convenience: `use-java 17` inside a shell.
#
# The first two lines are not convenience but repair. Ubuntu's /etc/profile assigns PATH
# unconditionally ("/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" for root),
# throwing away what the image's ENV put there - so in a *login* shell (`make shell`, or any
# `bash -lc`) neither java nor node was on the PATH, and only `use-java 21` brought them back.
RUN printf '%s\n' \
  'export JAVA_HOME="${JAVA_HOME:-'"$JAVA_HOME_21"'}"' \
  'export PATH="$JAVA_HOME/bin:/opt/node/bin:$PATH"' \
  '' \
  'use-java() {' \
  '  case "$1" in' \
  '    17) export JAVA_HOME="$JAVA_HOME_17" ;;' \
  '    21) export JAVA_HOME="$JAVA_HOME_21" ;;' \
  '    *)  echo "usage: use-java 17|21" >&2; return 1 ;;' \
  '  esac' \
  '  export PATH="$JAVA_HOME/bin:/opt/node/bin:/usr/local/bin:/usr/bin:/bin"' \
  '  java -version' \
  '}' \
  > /etc/profile.d/ngb-java.sh

# JDK 21 is the default: it is what Gradle 8.14.5 runs on, what the server module's toolchain
# asks for and what the jar's class files need. Because it is the default, no Gradle invocation
# in the Makefile needs a `with-java*` prefix.
ENV JAVA_HOME=$JAVA_HOME_21
ENV PATH=$JAVA_HOME/bin:$PATH

ENV GRADLE_USER_HOME=/cache/gradle
ENV npm_config_cache=/cache/npm
ENV GRADLE_OPTS="-Xmx2g -Dorg.gradle.daemon=false"

WORKDIR /workspace
