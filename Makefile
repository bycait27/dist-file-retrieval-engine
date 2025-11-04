# Variables
SERVER_JAR = server/target/server.jar
CLIENT_JAR = client/target/client.jar
BENCHMARK_JAR = benchmark/target/benchmark.jar

.PHONY: help build clean rebuild server client benchmark test

# Default target - show help
help:
	@echo "Distributed File Retrieval Engine - Build & Run Commands"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build          					- Compile and package all modules (creates JARs)"
	@echo "  make clean          					- Remove all build artifacts"
	@echo "  make rebuild        					- Clean and rebuild all modules"
	@echo ""
	@echo "Run Commands:"
	@echo "  make server ARGS='<port>'                  		- Start server"
	@echo "  make client                                   	- Start client"
	@echo "  make benchmark ARGS='<ip> <port> <n> <paths>' 	- Run benchmark"
	@echo ""
	@echo "Test Commands:"
	@echo "  make test           					- Run unit tests"
	@echo ""
	@echo "Examples:"
	@echo "  make server ARGS='8080'"
	@echo "  make benchmark ARGS='127.0.0.1 8080 4 datasets/client_1 datasets/client_2 datasets/client_3 datasets/client_4'"

# Build the project and create jar files
build:
	mvn clean package

# Clean build artifacts
clean:
	mvn clean

# Clean and rebuild
rebuild: 
	clean build

# Run the server with arguments
# Usage: make server ARGS='8080'
server:
	java -Xmx2g -jar $(SERVER_JAR) $(ARGS)

# Run the client
client:
	java -jar $(CLIENT_JAR)

 # Run benchmark with arguments
 # Usage: make benchmark ARGS='127.0.0.1 8080 4 path1 path2 path3 path4'
 benchmark:
	java -jar $(BENCHMARK_JAR) $(ARGS)

 # Run tests
 test:
	mvn test