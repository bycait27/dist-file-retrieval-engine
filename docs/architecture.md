# Architecture

## System Overview

The Distributed File Retrieval Engine is a **multi-threaded client-server system** designed for concurrent document indexing and full-text search across distributed clients. The architecture follows a layered design with clear separation between communication, processing, and storage layers.

```
Clients (1..N)                    Server
   │                                │
   │─── TCP/IP Socket ──────────────│
   │                                │
   ├─ connect()              Dispatcher Thread
   ├─ index()                      │
   └─ search()                     ├─ accept connections
                                   └─ spawn workers
                                         │
                                   Worker Pool (max 50)
                                   ├─ REGISTER
                                   ├─ INDEX
                                   └─ SEARCH
                                         │
                                   IndexStore (Thread-Safe)
                                   ├─ DocumentMap<path_clientID, docNum>
                                   ├─ ReverseDocumentMap<docNum, path>
                                   └─ TermInvertedIndex<term, [(doc,freq)]>
```

---

## Modules

### Common
**Purpose:** Shared data transfer objects (DTOs) and protocol definitions

**Key Classes:**
- `MessageType.java` - Protocol message types enum (REGISTER, INDEX, SEARCH, QUIT)
- `IndexResult.java` - Indexing operation metadata (execution time, bytes read)
- `SearchResult.java` - Search operation results (execution time, ranked documents)
- `DocPathFreqPair.java` - Document path with word frequency pair

**Responsibilities:**
- Define the wire protocol message types
- Provide common data structures shared between client and server
- Ensure type safety across module boundaries

### Server
**Purpose:** Multi-threaded server with dispatcher-worker pattern and thread-safe inverted index

**Key Classes:**

| Class | Purpose | Threading |
|-------|---------|-----------|
| `FileRetrievalServer.java` | Main entry point, validates port | Main thread |
| `ServerProcessingEngine.java` | Orchestrates dispatcher and workers | Manages threads |
| `Dispatcher.java` | Accepts incoming connections | Single thread (Runnable) |
| `ServerWorker.java` | Handles individual client requests | Per-client thread (Runnable) |
| `IndexStore.java` | Thread-safe inverted index | Accessed by all workers |
| `ServerAppInterface.java` | CLI for server commands (list, quit) | Main thread |

**Responsibilities:**
- Accept and manage TCP connections from multiple clients
- Process REGISTER, INDEX, SEARCH, and QUIT requests
- Maintain thread-safe inverted index
- Provide server management CLI

### Client
**Purpose:** Client application for connecting to server, indexing documents, and performing searches

**Key Classes:**

| Class | Purpose |
|-------|---------|
| `FileRetrievalClient.java` | Main entry point |
| `ClientProcessingEngine.java` | Handles socket communication, file indexing, searching |
| `ClientAppInterface.java` | CLI for client commands (connect, index, search, quit) |

**Responsibilities:**
- Establish TCP connection to server
- Traverse directory trees and extract words from text files
- Send indexing requests with word frequencies
- Send search queries and display results
- Provide interactive CLI for user commands

### Benchmark
**Purpose:** Performance testing tool for concurrent client scenarios

**Key Classes:**
- `FileRetrievalBenchmark.java` - Creates multiple concurrent client threads
- `BenchmarkWorker` (inner class) - Individual client worker thread

**Responsibilities:**
- Simulate concurrent client connections
- Measure indexing throughput and latency
- Execute predefined search queries
- Generate performance metrics

---

## Thread Model

### Dispatcher-Worker Pattern

The server uses a **dispatcher-worker thread pool pattern** to handle multiple concurrent clients:

**Dispatcher Thread:**
- Runs in a loop listening on `ServerSocket`
- Blocks on `accept()` waiting for new connections
- Spawns a new `ServerWorker` thread for each accepted connection
- Graceful shutdown via `terminate` flag

**ServerWorker Threads:**
- One thread per connected client (max 50 concurrent workers)
- Handles socket I/O (BufferedReader/PrintWriter)
- Processes message protocol (REGISTER, INDEX, SEARCH, QUIT)
- Accesses `IndexStore` with proper locking

**Thread Safety Guarantees:**
- `IndexStore` uses two `ReentrantLock` instances:
  - `documentMapLock` - Protects document mappings
  - `termInvertedIndexLock` - Protects inverted index
- `ServerProcessingEngine` uses:
  - `clientSocketLock` - Protects client socket HashMap
  - `threadLock` - Protects worker thread list

---

## Data Structures

### IndexStore (Core Storage)

The `IndexStore` class implements a thread-safe inverted index with the following data structures:

**DocumentMap:** `HashMap<String, Long>`
- Maps "documentPath_clientID" to unique document number
- Protected by: `documentMapLock` (ReentrantLock)

**ReverseDocumentMap:** `HashMap<Long, String>`
- Reverse lookup from document number to path
- Protected by: `documentMapLock` (ReentrantLock)

**TermInvertedIndex:** `HashMap<String, ArrayList<DocFreqPair>>`
- Maps term to list of documents and frequencies
- Example: `"moon" → [(doc1, freq=5), (doc3, freq=2), ...]`
- Protected by: `termInvertedIndexLock` (ReentrantLock)

**Key Operations:**

| Operation | Lock Required | Complexity |
|-----------|---------------|------------|
| `putDocument(path, clientID)` | documentMapLock | O(1) |
| `updateIndex(docNum, wordFreqs)` | termInvertedIndexLock | O(k) where k = unique words |
| `lookupIndex(term)` | None (read-only) | O(1) |
| `getDocument(docNum)` | documentMapLock | O(1) |

**Design Rationale:**
- **Fine-grained locking:** Separate locks for documents and index minimize contention
- **Document numbering:** Sequential IDs provide efficient storage and lookup
- **Inverted index:** HashMap provides O(1) term lookup for fast searches
- **Per-term posting lists:** ArrayList allows efficient frequency aggregation

---

## Communication Flow

### Protocol Overview

**Transport:** TCP/IP sockets with blocking I/O   
**Format:** Line-delimited text (human-readable)    
**Pattern:** Synchronous request-response     

### Message Flows

#### 1. Registration Flow
**Client sends:**
```
REGISTER REQUEST
```

**Server responds:**
```
7320700042194535390
```

#### 2. Indexing Flow
**Client sends:**
```
INDEX REQUEST
7320700042194535390
folder1/Document10016.txt
3
moon=5
vortex=3
adaptation=2
```

**Server:** Updates IndexStore

#### 3. Search Flow
**Client sends:**
```
SEARCH REQUEST
2
moon
vortex
```

**Server responds:**
```
10
client 7320700042194535390:folder1/Document10016.txt=27
client 3852343861942436938:folder2/Document10200.txt=20
...
```

#### 4. Disconnection Flow
**Client sends:**
```
QUIT
```

**Server:** Connection closed

### Protocol Constraints

**Indexing:**
- Only words **longer than 3 characters** are indexed
- Words split on `[^a-zA-Z0-9_-]+` (preserves hyphens and underscores)
- Only `.txt` files are processed
- Nested directories are supported

**Searching:**
- Maximum **3 terms** per query
- Terms must be **longer than 3 characters**
- AND operator combines term frequencies
- Returns **top 10** results sorted by frequency descending

---

## Performance Characteristics

**Indexing:**
- Throughput: ~17 MB/s per client baseline
- Scaling: Near-linear speedup up to 4 clients (~3× faster)
- Peak: 53.5 MB/s with 4 concurrent clients
- Bottleneck: Disk I/O (file reading)

**Searching:**
- Latency: Sub-millisecond for most queries
- Throughput: O(1) term lookup via HashMap
- Independent of dataset size (in-memory operations)

**Memory:**
- In-memory storage (all indexed data in RAM)
- Peak usage: ~2 GB for 2 GB dataset
- No persistence (data lost on server restart)

---

*For detailed performance benchmarks, see [performance-evaluation.md](performance-evaluation.md)*
*For usage examples, see [../README.md](../README.md)*
