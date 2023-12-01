include .env

BUILD_IMG=clojure-build-image
RUNTIME_CTR=compiled-clojure-on-jvm

.PHONY : run-docker docker-build docker-build-image native-build test clean-build

run-docker : docker-build-image
	docker rm --force $(RUNTIME_CTR)
	docker run --rm --env-file .env      \
	           --publish $(PORT):$(PORT) \
	           --name $(RUNTIME_CTR)     \
	           $(BUILD_IMG)

docker-build : docker-build-image
	mkdir -p target
	docker create --rm --name $(RUNTIME_CTR) \
	              $(BUILD_IMG)
	docker cp $(RUNTIME_CTR):$(RUNTIME_CTR_DIR)/$(JAR_FILE) ./target/
	docker rm --force $(RUNTIME_CTR)

docker-build-image :
	docker build --file containerization/build.Dockerfile \
	             --tag $(BUILD_IMG) .

native-build : clean-build test
	clojure -T:build uber

test :
	clojure -M:test

clean-build :
	clojure -T:build clean
