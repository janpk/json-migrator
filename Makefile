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

# Only these target names collide with real directories, so only they must be phony to always run.
.PHONY: demo-kotlin demo-java

demo-kotlin: ## Run the Kotlin demo (credit-application v1..v6) tests
	$(MVN) $(MVN_ARGS) -pl demo-kotlin -am test

demo-java: ## Run the Java demo (credit-application v1..v6) tests
	$(MVN) $(MVN_ARGS) -pl demo-java -am test

demos: demo-kotlin demo-java ## Run both demo modules' tests
