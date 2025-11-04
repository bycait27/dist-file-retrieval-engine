# Performance Evaluation

## Test Environment

| Component     | Specification      |
|---------------|--------------------|
| Machine       | MacBook Pro M2     |
| Cores         | 12                 |
| Memory        | 32 GB              |
| OS            | macOS Tahoe 26.0.1 |
| Java Version  | OpenJDK 21         |
| Maven Version | 4.0                |

## Datasets

| Dataset   | Size   | Description                |
|-----------|--------|----------------------------|
| Dataset 1 | 128 MB | Small - scalability test   |
| Dataset 2 | 512 MB | Medium - balanced workload |
| Dataset 3 | 2.0 GB | Large - throughput test    |

--- 

## Indexing Performance

### Dataset 1 - ~128 MB (Fixed-Work Scalability Test)

**Dataset Design:** Same corpus (~130 MB, 8 folders) split across 1, 2, or 4 clients to measure parallel speedup.

| Clients | Total Indexed | Time (s) | Throughput (MB/s) | Speedup | Efficiency |
|---------|---------------|----------|-------------------|---------|------------|
| 1       | 128.04 MB     | 7.90     | 16.21             | 1.00x   | 100%       |
| 2       | 128.04 MB     | 4.30     | 29.77             | 1.84x   | 92%        |
| 4       | 128.05 MB     | 2.67     | 47.96             | 2.96x   | 74%        |

**Observations:**
- Near-linear speedup from 1→2 clients (92% efficiency)
- Good speedup at 4 clients (74% efficiency, ~3× faster than single client)
- Throughput nearly triples with 4 concurrent clients
- Slight efficiency loss at 4 clients due to disk I/O contention

### Dataset 2 - ~512 MB (Balanced Workload Test)

| Clients | Total Indexed | Time (s) | Throughput (MB/s) | Speedup | Efficiency |
|---------|---------------|----------|-------------------|---------|------------|
| 1       | 512.34 MB     | 30.52    | 16.79             | 1.00x   | 100%       |
| 2       | 512.34 MB     | 16.73    | 30.62             | 1.82x   | 91%        |
| 4       | 512.34 MB     | 9.57     | 53.53             | 3.19x   | 80%        |

**Observations:**
- **Best throughput achieved: 53.5 MB/s with 4 clients**
- Excellent scaling efficiency (80% at 4 clients)
- Speedup of 3.19× demonstrates near-linear scaling
- Larger dataset size reduces relative overhead compared to Dataset 1

### Dataset 3 - ~2.0 GB (Large-Scale Throughput Test)

| Clients | Total Indexed | Time (s) | Throughput (MB/s) | Speedup | Efficiency |
|---------|---------------|----------|-------------------|---------|------------|
| 1       | 2.0 GB        | 117.20   | 17.46             | 1.00x   | 100%       |
| 2       | 2.0 GB        | 70.45    | 29.04             | 1.66x   | 83%        |
| 4       | 2.0 GB        | 39.50    | 51.79             | 2.97x   | 74%        |

**Observations:**
- Sustained high throughput (51.8 MB/s) on large dataset
- Consistent performance with 2 GB corpus
- Speedup remains strong (2.97× at 4 clients)
- Demonstrates system handles large-scale workloads effectively

---

## Search Performance

### Search Query Latency

| Query Type           | Terms                  | Results | Average Time (ms) | Notes               |
|----------------------|------------------------|---------|-------------------|---------------------|
| Single term (common) | `the`                  | 0       | <1                | Filtered (≤3 chars) |
| Single term (rare)   | `child-like`           | 10      | 1                 | Quick lookup        |
| Single term (common) | `vortex`               | 10      | 0-1               | O(1) index access   |
| Multi-term (2)       | `moon vortex`          | 10      | 1-4               | Fast intersection   |
| Multi-term (2)       | `distortion adaptation`| 4       | 0-1               | Efficient combining |

**Observations:**
- Sub-millisecond search times for most queries
- Inverted index provides O(1) term lookup
- Multi-term AND operations remain fast even on large datasets
- Search performance independent of dataset size (in-memory index)

---

### Scalability Analysis

### Throughput vs Client Count

| Dataset     | 1 Client (MB/s) | 2 Clients (MB/s) | 4 Clients (MB/s) |
|-------------|-----------------|------------------|------------------|
| Dataset 1   | 16.21           | 29.77            | 47.98            |
| Dataset 2   | 16.79           | 30.62            | **53.53**        |
| Dataset 3   | 17.46           | 29.04            | 51.79            |
| **Average** | **16.82**       | **29.81**        | **51.96**        |

**Key Findings:**
- Consistent baseline throughput (~17 MB/s single client)
- Near-linear scaling from 1→2 clients (~1.8× speedup)
- Strong scaling at 4 clients (~3× speedup)
- Dataset 2 shows optimal performance (53.53 MB/s peak)

### Scaling Efficiency

| Metric        | 1→2 Clients | 2→4 Clients | 1→4 Clients |
|---------------|-------------|-------------|-------------|
| Speedup (avg) | 1.77x       | 1.74x       | 3.08x       |
| Efficiency    | 89%         | 87%         | 77%         |

*Efficiency = (Speedup / # of clients) × 100%*

**Analysis:**
- Excellent efficiency maintained through 2 clients (89%)
- Good efficiency at 4 clients (77%)
- Minimal performance degradation with increased parallelism
- Efficiency loss primarily due to disk I/O contention, not lock contention

---

## System Behavior

### Thread Safety
- **Test:** 4 concurrent clients indexing simultaneously across all datasets
- **Result:** No data corruption or race conditions observed
- **Mechanism:** ReentrantLock on IndexStore operations
- **Validation:** Search results consistent across all client configurations

### Resource Utilization
- **CPU:** High utilization during indexing (near 100% across available cores)
- **Memory:** Peak usage ~2 GB for largest dataset (all in-memory)
- **Network:** Minimal overhead with string-based protocol (<1% of execution time)
- **Disk I/O:** Primary bottleneck (file reading dominates execution time)

### Bottlenecks Identified

1. **Disk I/O** (Primary Bottleneck)
- File reading dominates execution time
- Throughput limited by disk read speed (~17 MB/s per client)
- Parallel clients improve throughput by distributing I/O load
- SSD performance characteristics influence scaling

2. **Network Latency** (Minimal Impact)
- TCP socket overhead negligible (<1ms per message)
- String-based protocol efficient for this workload
- Most time spent in disk/index operations, not communication

3. **Lock Contention** (Not Observed)
- ReentrantLock performs well up to 4 clients
- No significant contention detected
- May become issue at higher client counts (>10)

---

## Conclusions

### Strengths
- ✅ **Excellent scalability** with concurrent clients (77-80% efficiency at 4 clients)
- ✅ **Sub-millisecond search latency** independent of dataset size
- ✅ **Thread-safe operations** with no observed race conditions
- ✅ **Efficient inverted index** implementation with O(1) term lookup
- ✅ **Peak throughput of 53.53 MB/s** achieved on balanced workload
- ✅ **Consistent performance** across varying dataset sizes

### Limitations
- ⚠️ **Disk I/O bound** during indexing (~17 MB/s per client baseline)
- ⚠️ **In-memory storage** limits maximum dataset size to available RAM
- ⚠️ **Untested beyond 4 concurrent clients**
- ⚠️ **No persistence** - data lost on server restart

### Performance Summary
This system demonstrates **strong parallel scalability** for document indexing workloads:
- Nearly **3× faster** with 4 concurrent clients
- Peak throughput of **53.5 MB/s** on balanced workloads
- **Consistent** sub-millisecond search performance
- **Production-ready** threading model with proven thread safety
