# Performance Benchmark (Lightweight)

This benchmark measures the gateway flow using real HTTP requests and reports:

- total events sent
- total elapsed time
- average response time per request
- approximate throughput (events/second)
- alerts generated during each scenario

## Scenarios

- `POST /api/movement/events`
- `POST /api/temperature/events`
- `POST /api/access/events`

## Run

1. Start the microservices stack (for example with Docker Compose):

```bash
docker compose up --build
```

2. Run the benchmark runner:

```bash
python3 tools/benchmark_gateway.py --base-url http://localhost:8080 -n 100
```

Optional parameters:

- `--events` / `-n`: events per scenario
- `--timeout`: HTTP timeout (seconds)
- `--settle-seconds`: wait after each scenario for async alerts
- `--output-prefix`: output file prefix

## Output

The script prints a table in console and writes:

- `<prefix>.csv`
- `<prefix>.md`

Both files are suitable for memory/presentation evidence.

