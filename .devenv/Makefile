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

# Gradle 7.6 and Checkstyle 10 both need a JDK newer than the image default of 8, and the
# build declares a Java 17 toolchain. `with-java17` is the toolbox's JDK switcher.
GRADLE_H2  := with-java17 ./gradlew --no-daemon
EXEC_BUILD := $(COMPOSE) exec -T builder
SERVER     := -p server/catgenome

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
	# TEST-BASELINE.md. with-java17 is still correct and needed: from Phase 2 both the server jar
	# and the CLI are built by a JDK 17 toolchain, and the script runs `java -jar` directly.
	$(EXEC_BUILD) with-java17 bash e2e/integration_tests.sh

# --------------------------------------------------------------------- run --

.PHONY: up
up: certs ## Run NGB on H2 (http://ngb.dev.local:8080/catgenome)
	$(COMPOSE) up -d builder ngb-h2
	@echo "==> http://ngb.dev.local:8080/catgenome  (AUTH_MODE from .env)"

.PHONY: up-saml
up-saml: certs ## Run NGB with Keycloak SAML SSO (https://ngb.dev.local:9443/catgenome)
	$(COMPOSE) --profile saml up -d idp
	$(COMPOSE) --profile saml run --rm idp-metadata
	AUTH_MODE=saml $(COMPOSE) up -d builder ngb-h2
	@echo ""
	@echo "==> NGB      https://ngb.dev.local:9443/catgenome  (self-signed cert)"
	@echo "==> Keycloak http://idp.dev.local:8081  (admin/admin)"
	@echo "==> sign in as ngbadmin@ngb.dev.local / admin  (NGB admin)"
	@echo "                ngbuser@ngb.dev.local  / user   (plain user)"

.PHONY: up-pg
up-pg: certs ## Run NGB on PostgreSQL (http://ngb-pg.dev.local:8090/catgenome)
	$(COMPOSE) --profile pg up -d postgres ngb-pg
	@echo "==> http://ngb-pg.dev.local:8090/catgenome"

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
	$(COMPOSE) --profile saml --profile pg logs -f

.PHONY: ps
ps: ## Show container status
	$(COMPOSE) --profile saml --profile pg --profile cli ps

.PHONY: smoke
smoke: ## Check that the H2 instance answers
	# The app exposes a single connector, so exactly one of these answers:
	# HTTP:8080 in AUTH_MODE=none, HTTPS:9443 in AUTH_MODE=saml.
	# curl still writes %{http_code} (000) when the connection fails, so discard its
	# output on a non-zero exit instead of letting both appear.
	@code=$$(curl -fsS -o /dev/null -w '%{http_code}' http://localhost:8080/catgenome/ 2>/dev/null) \
		&& echo "AUTH_MODE=none  -> http  $$code" || echo "AUTH_MODE=none  -> http  not listening"
	@code=$$(curl -fksS -o /dev/null -w '%{http_code}' https://localhost:9443/catgenome/ 2>/dev/null) \
		&& echo "AUTH_MODE=saml  -> https $$code" || echo "AUTH_MODE=saml  -> https not listening"
	@echo "version:        $$(curl -fsS http://localhost:8080/catgenome/restapi/version 2>/dev/null || curl -fksS https://localhost:9443/catgenome/restapi/version 2>/dev/null || echo '-')"

.PHONY: smoke-saml
smoke-saml: builder ## Full browser-less SSO login through Keycloak (needs `make up-saml`)
	$(EXEC_BUILD) python3 /workspace/.devenv/scripts/saml-login-test.py \
		$(or $(U),ngbadmin@ngb.dev.local) $(or $(P),admin)

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
	$(COMPOSE) --profile saml --profile pg --profile cli stop

.PHONY: down
down: ## Remove containers, keep volumes
	$(COMPOSE) --profile saml --profile pg --profile cli down

.PHONY: reset
reset: ## Remove containers AND all volumes (DB, caches, indexes)
	$(COMPOSE) --profile saml --profile pg --profile cli down -v

.PHONY: reset-pg
reset-pg: ## Wipe only the PostgreSQL volume (needed when changing PG_VERSION)
	$(COMPOSE) --profile pg rm -sf postgres
	docker volume rm ngb-dev_pg-data 2>/dev/null || true

.PHONY: reset-ngb-data
reset-ngb-data: ## Wipe NGB's H2 database and content/index volumes
	$(COMPOSE) rm -sf ngb-h2
	docker volume rm ngb-dev_ngb-h2-db ngb-dev_ngb-h2-contents 2>/dev/null || true
