.DEFAULT_GOAL := help

ifeq ($(OS), Windows_NT)
  MVN_WRAPPER = .\mvnw.cmd
else
  MVN_WRAPPER = ./mvnw
endif

ifeq ($(MVN_DAEMON),true)
  MVN = mvnd
else
  MVN = $(MVN_WRAPPER)
endif

MVN_ARGS ?=
MODULE ?= engine-tests

help: ## List available targets
	@grep -hE '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/^([a-zA-Z_-]+):.*## /\1|/' | sort | awk -F'|' '{printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

clean: ## Remove Maven build outputs
	$(MVN) $(MVN_ARGS) clean

format: ## Apply Spotless formatting to all modules
	$(MVN) $(MVN_ARGS) spotless:apply

codeformat: format ## Alias for format

detekt: ## Run detekt static analysis
	${MVN} detekt:check

format-check: ## Check Spotless formatting for all modules
	$(MVN) $(MVN_ARGS) spotless:check

compile: ## Compile all modules
	$(MVN) $(MVN_ARGS) compile

test: ## Run tests for all modules
	$(MVN) $(MVN_ARGS) test

test-module: ## Run tests for one module and its dependencies, e.g. make test-module MODULE=engine-tests
	$(MVN) $(MVN_ARGS) -pl $(MODULE) -am test

package: ## Package all modules
	$(MVN) $(MVN_ARGS) package

verify: ## Run full Maven verify lifecycle for all modules without coverage (skips Kover)
	$(MVN) $(MVN_ARGS) verify -Dkover.skip=true

build: clean ## Run clean and the full verify lifecycle with kover
	$(MVN) $(MVN_ARGS) verify

dependency-tree: ## Print dependency tree for one module, e.g. make dependency-tree MODULE=engine-tests
	$(MVN) $(MVN_ARGS) -pl $(MODULE) dependency:tree

release: ## Tag a release to trigger the release workflow, e.g. make release VERSION=1.2.3
ifndef VERSION
	$(error VERSION is required — usage: make release VERSION=1.2.3)
endif
	@echo "$(VERSION)" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$$' || { echo "VERSION must be semver x.y.z (jgitver derives the release from the tag)"; exit 1; }
	@test -z "$$(git status --porcelain)" || { echo "working tree is dirty — commit or stash first"; exit 1; }
	@test "$$(git rev-parse --abbrev-ref HEAD)" = "main" || { echo "release from the main branch only"; exit 1; }
	@git fetch --quiet origin main && test "$$(git rev-parse HEAD)" = "$$(git rev-parse origin/main)" || { echo "main is not in sync with origin/main"; exit 1; }
	@git rev-parse "$(VERSION)" >/dev/null 2>&1 && { echo "tag $(VERSION) already exists"; exit 1; } || true
	git tag -a "$(VERSION)" -m "Release $(VERSION)"
	git push origin "$(VERSION)"

# demo-kotlin/demo-java collide with real directories; release is a command with no output file.
.PHONY: release demo-kotlin demo-java

demo-kotlin: ## Run the Kotlin demo (credit-application v1..v6) tests
	$(MVN) $(MVN_ARGS) -pl demo-kotlin -am test

demo-java: ## Run the Java demo (credit-application v1..v6) tests
	$(MVN) $(MVN_ARGS) -pl demo-java -am test

demos: demo-kotlin demo-java ## Run both demo modules' tests
