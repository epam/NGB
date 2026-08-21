# NGB containerised dev environment. Run everything from this directory.
#
#   make init           one-time setup (dirs + .env + /etc/hosts hint)
#   make image          build the toolbox image
#   make jar            build dist/catgenome-h2.jar (UI + docs + server, no tests)
#   make up             run NGB on H2
#   make test           run the server unit tests
#
# `make help` lists everything.

SHELL := /bin/bash
.DEFAULT_GOAL := help

# Prefer the compose plugin, fall back to the standalone binary.
COMPOSE ?= $(shell docker compose version >/dev/null 2>&1 && echo "docker compose" || echo "docker-compose")

# Gradle 8.14 and Checkstyle 11 both need a JDK newer than the image default of 8, and since
# migration Phase 3 the server declares a Java 21 toolchain. `with-java21` is the toolbox's JDK
# switcher. `server/ngb-cli` still declares a 17 toolchain; Gradle provisions that itself from
# the JDKs in the image, so one launcher JDK covers both modules.
GRADLE_H2  := with-java21 ./gradlew --no-daemon
EXEC_BUILD := $(COMPOSE) exec -T builder
SERVER     := -p server/catgenome

# SAML compares URLs byte-for-byte, so the SAML targets address NGB by its entity id and not by
# localhost. Matches NGB_BASE_URL in scripts/saml-login-test.py.
NGB_SAML_URL := https://ngb.dev.local:8443/catgenome

# MinIO stands in for S3 so that the s3:// and sws:// read paths can be verified on a machine
# with no AWS account (migration Phase 8). Exported because docker-compose.yml substitutes them.
# The root credentials are dev-only; override them in .env if you care.
export MINIO_USER     ?= ngbminio
export MINIO_PASSWORD ?= ngbminio-secret
export MINIO_BUCKET   ?= ngb-cloud

.PHONY: help
help: ## Show this help
	@grep -hE '^[a-zA-Z0-9_-]+:.*?## ' $(MAKEFILE_LIST) \
	 | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

# ------------------------------------------------------------------- setup --

.PHONY: init
init: ## One-time setup: create dirs, .env, print /etc/hosts entries
	@mkdir -p data/ngs secrets ../dist
	@touch data/ngs/.gitkeep
	@if [ ! -f .env ]; then cp .env.example .env; echo "created .env from .env.example"; else echo ".env already exists"; fi
	@echo ""
	@$(MAKE) --no-print-directory hosts

.PHONY: hosts
hosts: ## Print the /etc/hosts entries this environment needs
	@echo "Add these to /etc/hosts (needed because SAML matches URLs exactly):"
	@echo ""
	@echo "  127.0.0.1 ngb.dev.local ngb-pg.dev.local idp.dev.local"
	@echo ""
	@echo "  sudo sh -c 'echo \"127.0.0.1 ngb.dev.local ngb-pg.dev.local idp.dev.local\" >> /etc/hosts'"

.PHONY: image
image: ## Build the toolbox image (JDK 8 + 17 + 21 + Node 14 + mkdocs)
	# Plain `docker build` with the legacy builder: this host has no buildx plugin,
	# and compose's build path trips over multi-arch manifests without it.
	DOCKER_BUILDKIT=0 docker build -t ngb-dev-toolbox:latest \
		--build-arg NODE_VERSION=14.17.5 ./toolbox

.PHONY: builder
builder: ## Start the long-lived builder container
	$(COMPOSE) up -d builder

.PHONY: certs
certs: ## (Re)generate the JKS keystore and JWT keypair
	$(COMPOSE) run --rm certs

.PHONY: certs-force
certs-force: ## Throw away and regenerate all secrets
	$(COMPOSE) run --rm -e FORCE=1 certs

# ------------------------------------------------------------------- build --

.PHONY: jar
jar: builder ## Full H2 jar: UI + docs + server -> dist/catgenome-h2.jar
	$(EXEC_BUILD) $(GRADLE_H2) buildJar -PnoTest
	$(EXEC_BUILD) cp /workspace/dist/catgenome.jar /workspace/dist/catgenome-h2.jar
	@echo "==> dist/catgenome-h2.jar"

.PHONY: jar-pg
jar-pg: builder ## PostgreSQL jar -> dist/catgenome-psql.jar
	$(EXEC_BUILD) $(GRADLE_H2) buildJar -PnoTest -Pdatabase=postgres
	$(EXEC_BUILD) cp /workspace/dist/catgenome.jar /workspace/dist/catgenome-psql.jar
	@echo "==> dist/catgenome-psql.jar"

.PHONY: jar-fast
jar-fast: builder ## Server-only rebuild, skips UI and mkdocs (fast inner loop)
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) bootJar -Pprofile=jar -Pdatabase=h2 -x test
	$(EXEC_BUILD) cp /workspace/server/catgenome/build/libs/catgenome.jar /workspace/dist/catgenome-h2.jar
	@echo "==> dist/catgenome-h2.jar (server only - UI and docs unchanged)"

.PHONY: jar-pg-fast
jar-pg-fast: builder ## Server-only PostgreSQL rebuild (fast inner loop)
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) bootJar -Pprofile=jar -Pdatabase=postgres -x test
	$(EXEC_BUILD) cp /workspace/server/catgenome/build/libs/catgenome.jar /workspace/dist/catgenome-psql.jar
	@echo "==> dist/catgenome-psql.jar (server only - UI and docs unchanged)"

.PHONY: ui
ui: builder ## Rebuild only the AngularJS client into the server's static resources
	$(EXEC_BUILD) $(GRADLE_H2) ui

.PHONY: cli-build
cli-build: builder ## Build ngb-cli -> dist/ngb-cli.tar.gz
	$(EXEC_BUILD) $(GRADLE_H2) buildCli -PnoTest

.PHONY: clean-build
clean-build: builder ## Gradle clean of the server module
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) clean

# -------------------------------------------------------------------- test --

.PHONY: test
test: builder ## Server unit tests on H2 (129 test classes)
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) test -Pdatabase=h2

.PHONY: test-one
test-one: builder ## Single test class: make test-one T=VcfManagerTest
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) test -Pdatabase=h2 --tests "*$(T)*"

.PHONY: test-pg
test-pg: ## Server unit tests against PostgreSQL (runs in the DB's netns)
	$(COMPOSE) --profile pg up -d postgres
	$(COMPOSE) --profile pg run --rm test-pg

.PHONY: lint
lint: builder ## checkstyle + pmd on the server module
	$(EXEC_BUILD) $(GRADLE_H2) $(SERVER) checkstyleMain pmdMain

.PHONY: cli-test
cli-test: builder ## CLI/server integration tests (BROKEN: test-data host is gone)
	# e2e/integration_tests.sh wgets its fixtures from http://ngb.opensource.epam.com/, which no
	# longer resolves (NXDOMAIN as of migration Phase 2 - the parent domain does resolve, the ngb
	# host does not), so this target cannot pass until the data is hosted somewhere again. See
	# TEST-BASELINE.md. with-java21 is still correct and needed: from Phase 3 the server jar is
	# built by a JDK 21 toolchain and the script runs `java -jar` directly. (The CLI it drives is
	# still a 17 toolchain, but 17 bytecode runs on 21.)
	$(EXEC_BUILD) with-java21 bash e2e/integration_tests.sh

# --------------------------------------------------------------------- run --

.PHONY: up
up: certs ## Run NGB on H2 (http://ngb.dev.local:8080/catgenome)
	$(COMPOSE) up -d builder ngb-h2
	@echo "==> http://ngb.dev.local:8080/catgenome  (AUTH_MODE from .env)"

.PHONY: up-saml
up-saml: certs ## Run NGB with Keycloak SAML SSO (https://ngb.dev.local:8443/catgenome)
	$(COMPOSE) --profile saml up -d idp
	$(COMPOSE) --profile saml run --rm idp-metadata
	AUTH_MODE=saml $(COMPOSE) up -d builder ngb-h2
	@echo ""
	@echo "==> NGB      https://ngb.dev.local:8443/catgenome  (self-signed cert)"
	@echo "==> Keycloak http://idp.dev.local:8081  (admin/admin)"
	@echo "==> sign in as ngbadmin@ngb.dev.local / admin  (NGB admin)"
	@echo "                ngbuser@ngb.dev.local  / user   (plain user)"

.PHONY: up-pg
up-pg: certs ## Run NGB on PostgreSQL (http://ngb-pg.dev.local:8090/catgenome)
	$(COMPOSE) --profile pg up -d postgres ngb-pg
	@echo "==> http://ngb-pg.dev.local:8090/catgenome"

.PHONY: cloud
cloud: ## Start MinIO and upload the track fixtures the cloud checks read
	$(COMPOSE) --profile cloud up -d minio
	$(COMPOSE) --profile cloud run --rm minio-init
	@echo "==> MinIO API http://localhost:9000, console http://localhost:9001 ($(MINIO_USER))"
	@echo "==> bucket    $(MINIO_BUCKET)"

# Everything NGB needs in order to reach MinIO instead of AWS goes in on the command line:
#   swift.stack.*  Spring properties, read by Application into S3Client.configure. System
#                  properties outrank catgenome.properties, so no override.properties is involved.
#   aws.*          AWS SDK v2 settings. endpointUrlS3 is v2-only - v1 had no such switch, which is
#                  why this target could not have existed before migration Phase 8 - and
#                  sharedCredentialsFile points at the file written below, which carries both the
#                  [default] profile the s3:// client resolves through the default provider chain
#                  and the [sws] one S3Client asks for by name.
#
# MinIO is addressed by its own name. Between Phase 8 and Phase 9 it had to be addressed as
# s3.amazonaws.com instead, because EnhancedUrlHelper decided whether to tolerate a 403 on HEAD by
# matching the host against `.*s3.*\.amazonaws\.com` and a pre-signed URL is signed for GET only -
# so under the honest name the pre-signed read path read every object as empty. Phase 9 keyed that
# tolerance to the URL's signature instead, which is what makes this line the proof of the fix:
# `make verify-cloud` reads a track through a pre-signed http://<bucket>.minio:9000/... URL.
MINIO_ENDPOINT  := http://minio:9000
MINIO_JAVA_OPTS := -Dswift.stack.endpoint.url=$(MINIO_ENDPOINT) \
                   -Dswift.stack.region=us-east-1 \
                   -Dswift.stack.path.style.access=true \
                   -Daws.region=us-east-1 \
                   -Daws.endpointUrlS3=$(MINIO_ENDPOINT) \
                   -Daws.sharedCredentialsFile=/secrets/aws-credentials

.PHONY: up-cloud
up-cloud: certs cloud ## Run NGB with s3:// and sws:// pointed at MinIO
	@printf '# Written by .devenv/Makefile (make up-cloud). MinIO root credentials, dev only.\n[default]\naws_access_key_id = %s\naws_secret_access_key = %s\n\n[sws]\naws_access_key_id = %s\naws_secret_access_key = %s\n' \
		"$(MINIO_USER)" "$(MINIO_PASSWORD)" "$(MINIO_USER)" "$(MINIO_PASSWORD)" > secrets/aws-credentials
	@echo "wrote secrets/aws-credentials ([default] and [sws])"
	JAVA_EXTRA_OPTS="$(MINIO_JAVA_OPTS)" $(COMPOSE) up -d builder ngb-h2
	@echo "==> http://ngb.dev.local:8080/catgenome, s3:// and sws:// -> MinIO"

.PHONY: verify-cloud
verify-cloud: ## Read tracks over s3:// and sws:// from MinIO and compare with a local read
	bash scripts/verify-cloud.sh

.PHONY: cli
cli: ## Shell with ngb-cli configured against the running server
	$(COMPOSE) --profile cli up -d cli
	$(COMPOSE) --profile cli exec cli bash

.PHONY: shell
shell: builder ## Shell in the builder (JDK 8 default; `use-java 17|21` to switch)
	$(COMPOSE) exec builder bash -l

.PHONY: logs
logs: ## Follow NGB logs
	$(COMPOSE) logs -f ngb-h2

.PHONY: logs-all
logs-all: ## Follow all service logs
	$(COMPOSE) --profile saml --profile pg --profile cloud logs -f

.PHONY: ps
ps: ## Show container status
	$(COMPOSE) --profile saml --profile pg --profile cli --profile cloud ps

.PHONY: smoke
smoke: ## Check that the H2 instance answers
	# The app exposes a single connector, so exactly one of these answers:
	# HTTP:8080 in AUTH_MODE=none, HTTPS:8443 in AUTH_MODE=saml.
	# curl still writes %{http_code} (000) when the connection fails, so discard its
	# output on a non-zero exit instead of letting both appear.
	@code=$$(curl -fsS -o /dev/null -w '%{http_code}' http://localhost:8080/catgenome/ 2>/dev/null) \
		&& echo "AUTH_MODE=none  -> http  $$code" || echo "AUTH_MODE=none  -> http  not listening"
	@code=$$(curl -fksS -o /dev/null -w '%{http_code}' https://localhost:8443/catgenome/ 2>/dev/null) \
		&& echo "AUTH_MODE=saml  -> https $$code" || echo "AUTH_MODE=saml  -> https not listening"
	@echo "version:        $$(curl -fsS http://localhost:8080/catgenome/restapi/version 2>/dev/null || curl -fksS https://localhost:8443/catgenome/restapi/version 2>/dev/null || echo '-')"

.PHONY: smoke-saml
smoke-saml: builder ## Full browser-less SSO login through Keycloak (needs `make up-saml`)
	$(EXEC_BUILD) python3 /workspace/.devenv/scripts/saml-login-test.py \
		$(or $(U),ngbadmin@ngb.dev.local) $(or $(P),admin)

.PHONY: cli-token
cli-token: builder ## Print a JWT for the CLI: logs in over SAML and asks /restapi/user/token
	@$(EXEC_BUILD) python3 /workspace/.devenv/scripts/jwt-token.py \
		$(or $(U),ngbadmin@ngb.dev.local) $(or $(P),admin)

.PHONY: saml-verify-signing
saml-verify-signing: builder ## Check both signatures NGB produces: SP metadata, and the AuthnRequest
	# Neither is checked by `make smoke-saml` on its own: saml.client.signature is off in the
	# committed realm (the SP cert is generated per machine, so it cannot be inlined there) and
	# nothing at all validates the metadata signature. So: verify the metadata signature with the
	# JDK's XML-DSIG, then make Keycloak require the AuthnRequest signature and log in again.
	# `make up-saml` re-imports the realm, which puts the flag back to off.
	@echo "==> SP metadata signature"
	$(EXEC_BUILD) bash -lc 'curl -fksS $(NGB_SAML_URL)/saml/metadata -o /tmp/sp-metadata.xml \
		&& with-java21 java /workspace/.devenv/scripts/VerifyXmlSignature.java /tmp/sp-metadata.xml'
	@echo "==> AuthnRequest signature"
	$(EXEC_BUILD) python3 /workspace/.devenv/scripts/saml-client-signature.py on
	@$(MAKE) smoke-saml $(if $(U),U=$(U)) $(if $(P),P=$(P))

# ---------------------------------------------------------------- migration --

.PHONY: probe-java21
probe-java21: builder ## Show what breaks today under JDK 21 (expected to fail)
	@echo "=== JDK 21 present? ==="
	-$(EXEC_BUILD) with-java21 java -version
	@echo ""
	@echo "=== Gradle 3.3 on JDK 21 (expected: unsupported class file / init failure) ==="
	-$(EXEC_BUILD) with-java21 ./gradlew --no-daemon -v
	@echo ""
	@echo "=== lombok 1.16.16 under javac 21 (expected: ExceptionInInitializerError) ==="
	# Compiles a one-line @Getter class with the exact lombok the build resolves, first on
	# JDK 8 (works) then on JDK 21 (annotation processor dies on internal javac API).
	-$(EXEC_BUILD) bash /workspace/.devenv/scripts/probe-lombok.sh
	@echo ""
	@echo "Interpretation: Gradle must go to 8.x before JDK 21 can compile anything;"
	@echo "lombok must reach >= 1.18.30. See README 'Migration checkpoints'."

# ------------------------------------------------------------------- teardown --

.PHONY: stop
stop: ## Stop containers, keep data
	$(COMPOSE) --profile saml --profile pg --profile cli --profile cloud stop

.PHONY: down
down: ## Remove containers, keep volumes
	$(COMPOSE) --profile saml --profile pg --profile cli --profile cloud down

.PHONY: reset
reset: ## Remove containers AND all volumes (DB, caches, indexes)
	$(COMPOSE) --profile saml --profile pg --profile cli --profile cloud down -v

.PHONY: reset-pg
reset-pg: ## Wipe only the PostgreSQL volume (needed when changing PG_VERSION)
	$(COMPOSE) --profile pg rm -sf postgres
	docker volume rm ngb-dev_pg-data 2>/dev/null || true

.PHONY: reset-ngb-data
reset-ngb-data: ## Wipe NGB's H2 database and content/index volumes
	$(COMPOSE) rm -sf ngb-h2
	docker volume rm ngb-dev_ngb-h2-db ngb-dev_ngb-h2-contents 2>/dev/null || true
