# Distributed File Retrieval Engine

> Multi-threaded client-server system for concurrent indexing and full-text search

## Table of Contents

## Features

- Concurrent document indexing across multiple clients
- Thread-safe inverted index with ReentrantLock
- Dispatcher-worker thread pool pattern
- Full-text search with ranked results
- TCP socket-based protocol

## Architecture 

This system uses a **layered, client-server architecture** with the following components:

### Modules
- **common** - Shared DTOs (IndexResult, SearchResult, DocPathFreqPair) and protocol definitions
- **server** - Multi-threaded server with dispatcher-worker pattern and thread-safe inverted index
- **client** - Client application for connecting, indexing, and searching
- **benchmark** - Performance testing tool for concurrent client scenarios

### Server Design

- **Dispatcher Thread**: Listens for incoming connections, spawns worker threads
- **Worker Threads**: Handle individual client requests (REGISTER, INDEX, SEARCH, QUIT)
- **IndexStore**: Thread-safe inverted index using ReentrantLock for concurrent access

### Communication

- TCP socket-based protocol
- String-based message format
- Client registers → receives unique ID → indexes documents → performs searches

*See [docs/architecture.md](docs/architecture.md) for detailed design.*

## Requirements

```bash
# Install Java 21 
brew install openjdk@21

# Install Maven 
brew install maven

# Install git 
brew install git
```

> [!Note]
> This repository does not include sample datasets due to size. 
> It is not intended for personal use, just for academic purposes.
<!-- 2. Or download sample datasets from [link if available] --> 

## Quick Start

### Clone the Repository

```
git clone git@github.com:bycait27/dist-file-retrieval-engine.git
```

### Build/Compile Program

```
$ cd dist-file-retrieval-engine
make build 
```

### Run Program

To run the server, use the following command:
```bash
make server ARGS=8080
# Available commands: list, quit
```
To run the client, use the following command:
```bash
make client
# Available commands: connect, get_info, index, search, quit 
```
To run the benchmark, use the following command:
```bash
make benchmark ARGS="127.0.0.1 8080 4 path1 path2 path3 path4"
```

**Example (2 clients)**

**Step 1:** start the server:
```
make server ARGS=8080
>
```

**Step 2:** start the clients and connect them to the server:

Client 1
```
make client
> connect 127.0.0.1 8080
Connection successful!
> get_info
client ID: 7320700042194535390
>
```

Client 2
```
make client
> connect 127.0.0.1 8080
Connection successful!
> get_info
client ID: 3852343861942436938
>
```

**Step 3:** list the connected clients on the server:

Server
```
> list
client ID:IP:PORT
client 7320700042194535390:127.0.0.1:58955
client 3852343861942436938:127.0.0.1:58959
>
```

**Step 4:** index files from the clients:

Client 1
```
> index datasets/dataset1_client_server/2_clients/client_1
Completed indexing 68383239 bytes of data 
Completed indexing in 4.328 seconds
>
```

Client 2
```
> index datasets/dataset1_client_server/2_clients/client_2
Completed indexing 65864138 bytes of data
Completed indexing in 3.897 seconds
>
```

**Step 5:** search files from the clients:

Client 1
```
> search the 
Search completed in 0.004 seconds
Search results (top 10):
> search child-like 
Search completed in 0.004 seconds
Search results (top 10):
* client 3852343861942436938:folder7/Document10926.txt:4
* client 7320700042194535390:folder3/Document10379.txt:3
* client 3852343861942436938:folder6/Document10866.txt:2
* client 7320700042194535390:folder2/Document10164.txt:1
* client 7320700042194535390:folder2/folderA/Document10374.txt:1
* client 7320700042194535390:folder2/folderA/Document10325.txt:1
* client 7320700042194535390:folder3/Document10387.txt:1
* client 7320700042194535390:folder4/Document10681.txt:1
* client 7320700042194535390:folder4/Document10669.txt:1
* client 7320700042194535390:folder1/Document10016.txt:1
> search distortion AND adaptation
Search completed in 0.001 seconds
Search results (top 10):
* client 3852343861942436938:folder7/folderC/Document10998.txt:6
* client 7320700042194535390:folder4/Document10516.txt:3
* client 3852343861942436938:folder8/Document11159.txt:2
* client 3852343861942436938:folder8/Document11157.txt:2
>
```

Client 2
```
> search vortex 
Search completed in 0.001 seconds
Search results (top 10):
* client 3852343861942436938:folder5/folderB/Document10706.txt:6
* client 3852343861942436938:folder5/folderB/Document10705.txt:4
* client 7320700042194535390:folder4/Document10681.txt:3
* client 3852343861942436938:folder7/Document1091.txt:3
* client 7320700042194535390:folder2/Document1033.txt:2
* client 7320700042194535390:folder3/folderA/Document10422.txt:2
* client 7320700042194535390:folder4/Document1051.txt:2
* client 3852343861942436938:folder6/Document1082.txt:2
* client 7320700042194535390:folder2/Document10201.txt:1
* client 7320700042194535390:folder2/Document1025.txt:1
> search moon AND vortex
Search completed in 0.002 seconds
Search results (top 10):
* client 3852343861942436938:folder5/folderB/Document10706.txt:27
* client 7320700042194535390:folder3/Document1043.txt:20
* client 7320700042194535390:folder4/Document10681.txt:19
* client 7320700042194535390:folder4/Document10600.txt:18
* client 3852343861942436938:folder8/Document11154.txt:15
* client 7320700042194535390:folder3/Document10379.txt:6
* client 7320700042194535390:folder3/folderA/Document10422.txt:6
* client 7320700042194535390:folder3/folderA/Document10421.txt:6
* client 3852343861942436938:folder5/folderB/Document10705.txt:5
* client 7320700042194535390:folder2/Document1033.txt:5
>
```

> [!Note]
> - Terms must be **longer than 3 characters**
> - Maximum **3 terms** per query
> - Use `AND` operator to combine terms

**Step 6:** close and disconnect the clients:

Client 1
```
> quit
```

Client 2
```
> quit
```

**Step 7:** close the server:

Server
```
> quit
```

**Example (benchmark with 2 clients)**

**Step 1:** start the server

Server
```
make server ARGS=8080
>
```

**Step 2:** start the benchmark:

Benchmark
```
make benchmark ARGS="127.0.0.1 8080 2 datasets/dataset1_client_server/2_clients/client_1 datasets/dataset1_client_server/2_clients/client_2"
Connection successful!
Connection successful!
Completed indexing 134247377 bytes of data
Completed indexing in 4.454 seconds
Searching for the
Search completed in 0.001 seconds
Search results (top 10):
Searching for child-like
Search completed in 0.000 seconds
Search results (top 10):
* client 5885206125837895897:folder7/Document10926.txt:4
* client 1270556743278085992:folder3/Document10379.txt:3
* client 5885206125837895897:folder6/Document10866.txt:2
* client 1270556743278085992:folder2/Document10164.txt:1
* client 1270556743278085992:folder2/folderA/Document10374.txt:1
* client 1270556743278085992:folder2/folderA/Document10325.txt:1
* client 1270556743278085992:folder3/Document10387.txt:1
* client 5885206125837895897:folder8/Document1108.txt:1
* client 5885206125837895897:folder6/Document1082.txt:1
* client 5885206125837895897:folder6/Document10848.txt:1
Searching for vortex
Search completed in 0.000 seconds
Search results (top 10):
* client 5885206125837895897:folder5/folderB/Document10706.txt:6
* client 5885206125837895897:folder5/folderB/Document10705.txt:4
* client 1270556743278085992:folder4/Document10681.txt:3
* client 5885206125837895897:folder7/Document1091.txt:3
* client 1270556743278085992:folder2/Document1033.txt:2
* client 1270556743278085992:folder3/folderA/Document10422.txt:2
* client 5885206125837895897:folder6/Document1082.txt:2
* client 1270556743278085992:folder4/Document1051.txt:2
* client 5885206125837895897:folder5/folderB/Document10703.txt:1
* client 1270556743278085992:folder2/Document10201.txt:1
Searching for moon vortex
Search completed in 0.001 seconds
Search results (top 10):
* client 5885206125837895897:folder5/folderB/Document10706.txt:27
* client 1270556743278085992:folder3/Document1043.txt:20
* client 1270556743278085992:folder4/Document10681.txt:19
* client 1270556743278085992:folder4/Document10600.txt:18
* client 5885206125837895897:folder8/Document11154.txt:15
* client 1270556743278085992:folder3/Document10379.txt:6
* client 1270556743278085992:folder3/folderA/Document10422.txt:6
* client 1270556743278085992:folder3/folderA/Document10421.txt:6
* client 5885206125837895897:folder5/folderB/Document10703.txt:5
* client 1270556743278085992:folder2/Document1033.txt:5
Searching for distortion adaptation
Search completed in 0.000 seconds
Search results (top 10):
* client 5885206125837895897:folder7/folderC/Document10998.txt:6
* client 1270556743278085992:folder4/Document10516.txt:3
* client 5885206125837895897:folder8/Document11157.txt:2
* client 5885206125837895897:folder8/Document11159.txt:2
```

**Step 3:** close the server:

Server
```
> quit
```

## Command Reference

### Client Commands
| Command               | Description                         | Example                  |
|-----------------------|-------------------------------------|--------------------------|
| `connect <ip> <port>` | Connect to server                   | `connect 127.0.0.1 8080` |
| `get_info`            | Display client ID                   | `get_info`               |
| `index <path>`        | Index directory                     | `index ~/Documents`      |
| `search <terms>`      | Search (max 3 terms, >3 chars each) | `search moon AND vortex` |
| `quit`                | Disconnect and exit                 | `quit`                   |

### Server Commands
| Command | Description            |
|---------|------------------------|
| `list`  | Show connected clients |
| `quit`  | Shutdown server        |

## Indexing Details

### What Gets Indexed

- **File Types**: Plain text files (`.txt`)
- **Word Length**: Only words **longer than 3 characters**
- **Directory Structure**: Supports nested directories
- **Processing**: Words split on non-alphanumeric characters (except `-` and `_`)

### Search Constraints

- Terms must be >3 characters
- Maximum 3 terms per query
- AND operator combines term frequencies
- Returns top 10 ranked results

## Performance

- **Peak Throughput:** 53.5 MB/s with 4 concurrent clients (512 MB dataset)
- **Scaling:** Near-linear speedup up to 4 clients (~3× faster)
- **Search Latency:** Sub-millisecond for most queries
- **Tested Datasets:** 128 MB to 2 GB

*See [docs/performance-evaluation.md](docs/performance-evaluation.md) for detailed analysis.*

### Benchmark Results Summary

| Clients | Avg Throughput | Speedup | Efficiency |
|---------|----------------|---------|------------|
| 1       | 16.82 MB/s     | 1.0×    | 100%       |
| 2       | 29.81 MB/s     | 1.8×    | 89%        |
| 4       | 51.96 MB/s     | 3.1×    | 77%        |

## Project Structure

```
dist-file-retrieval-engine/
  ├── client/          # Client application
  ├── server/          # Multi-threaded server
  ├── benchmark/       # Performance testing
  ├── common/          # Shared DTOs and protocol
  ├── docs/            # Documentation
  ├── datasets/        # Datasets for testing
  ├── examples/        # Sample queries and output
  ├── Makefile         # Build automation
  └── pom.xml          # Root Maven configuration
```

## Technologies

- Java 21
- Maven (multi-module)
- TCP Sockets
- Multi-threading (ReentrantLock, Thread pools)

## Future Enhancements

- [ ] JSON-based protocol for type-safe communication
- [ ] Persistent storage (currently in-memory only)
- [ ] Unit tests and integration tests
- [ ] Docker containerization
- [ ] Web-based UI
- [ ] Make available for any type of folder of documents (not just specific dataset folders or .txt files)

## TroubleShooting

**Build fails:**
```bash
mvn clean package  # Rebuild from scratch

Port already in use:
make server ARGS='9090'  # Use different port

Connection refused:
- Ensure server is running
- Check IP/port in connect command
- Verify firewall settings
```

## 📜 License 

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)  

see LICENSE file for more details

## 📫 Contact

**GitHub:** [@bycait27](https://github.com/bycait27)   
**Portfolio Website:** [caitlinash.io](https://caitlinash.io/)   
**LinkedIn:** [Caitlin Ash](https://www.linkedin.com/in/caitlin-ash/)