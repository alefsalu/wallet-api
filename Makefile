DOCKER_MAVEN_IMAGE = maven:3.9-eclipse-temurin-21
DOCKER_OPENSSL_IMAGE = alpine/openssl:latest

DOCKER_MAVEN_CMD = docker run --rm \
	-v $(CURDIR):/workspace \
	-w /workspace \
	-v $(HOME)/.m2:/root/.m2 \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
	--add-host=host.docker.internal:host-gateway \
	$(DOCKER_MAVEN_IMAGE)

.PHONY: all up down test test-unit test-integration lint format keys ci export-openapi clean

all: ci

up:
	docker compose up -d

down:
	docker compose down

keys:
	docker run --rm -v $(CURDIR):/workspace -w /workspace $(DOCKER_OPENSSL_IMAGE) sh -c "mkdir -p keys && if [ ! -f keys/private.pem ]; then openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem; fi && if [ ! -f keys/public.pem ]; then openssl rsa -pubout -in keys/private.pem -out keys/public.pem; fi"

test-unit:
	$(DOCKER_MAVEN_CMD) mvn test -Dtest="*Test,!*IntegrationTest,!*EndToEndTest,!*ConcurrencyTest"

test-integration:
	$(DOCKER_MAVEN_CMD) mvn test -Dtest="*IntegrationTest,*EndToEndTest,*ConcurrencyTest"

test:
	$(DOCKER_MAVEN_CMD) mvn verify

lint:
	$(DOCKER_MAVEN_CMD) mvn spotless:check

format:
	$(DOCKER_MAVEN_CMD) mvn spotless:apply

ci: lint test
	bash scripts/guard.sh

export-openapi:
	bash scripts/export-openapi.sh

clean:
	$(DOCKER_MAVEN_CMD) mvn clean
