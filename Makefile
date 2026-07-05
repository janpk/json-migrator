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

clean: ## Remove Maven build outputs
	$(MVN) $(MVN_ARGS) clean

format: ## Apply Spotless formatting to all modules
	$(MVN) $(MVN_ARGS) spotless:apply

codeformat: format ## Alias for format

# Code quality checks
detekt:
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

verify: ## Run full Maven verify lifecycle for all modules
	$(MVN) $(MVN_ARGS) verify

dependency-tree: ## Print dependency tree for one module, e.g. make dependency-tree MODULE=engine-tests
	$(MVN) $(MVN_ARGS) -pl $(MODULE) dependency:tree
