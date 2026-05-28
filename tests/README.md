# Cameo HTTP Server – Test Suite

Python-based integration tests for the Cameo HTTP Server plugin.

## Deployment Topology

```
Host machine
┌──────────────────────────────────────────┐
│  Cameo + HTTP plugin                     │
│  127.0.0.1:18741                         │
│                                          │
│  socat TCP:18750 ──► localhost:18741     │
└──────────────────────┬───────────────────┘
                       │ host.containers.internal:18750
                       ▼
           ┌─────────────────────┐
           │ OpenCode container  │
           │ uv run pytest       │
           │ SERVER_URL=         │
           │ host.containers     │
           │ .internal:18750     │
           └─────────────────────┘
```

Cameo and the HTTP plugin run on the host machine, binding to `127.0.0.1:18741`.
A `socat` bridge on the host forwards `TCP:18750` to `localhost:18741`.
Tests (and opencode) run from inside a Podman container and reach the server
via `host.containers.internal:18750`.

## Prerequisites

- [uv](https://docs.astral.sh/uv/) (Python package manager)
- A running Cameo instance with the HTTP Server plugin loaded
- `socat TCP-LISTEN:18750,fork,reuseaddr TCP:localhost:18741` on the host

## Running

```bash
cd tests
uv run pytest -v
```

Override the server URL:

```bash
SERVER_URL=http://host.containers.internal:18750 uv run pytest -v
```

Run a single test:

```bash
uv run pytest -v -k test_server_reachable
```

## Java code changes require restart

If the Java source (`.java` files) is modified:

```bash
gradle build deploy -PcameoHome=/path/to/Cameo
# then restart Cameo
```

Groovy script changes (`.groovy` files in `scripts/`) are hot-reloaded automatically.

## Test Layout

| Test | What it verifies |
|---|---|
| `test_server_reachable` | Server responds on the expected port |
| `test_server_reachable_via_test_endpoint` | `/test` returns `"OK"` |
| `test_logging_demo` | `/logging_demo` returns 200 with log confirmation |
| `test_list_opscon_elements` | `/list_opscon_elements` returns 200 + valid JSON |
| `test_nonexistent_path_returns_404` | Unknown paths return 404 with descriptive body |
| `test_nonexistent_path_with_suffix_returns_404` | Sub-paths of valid routes return 404 |
| `test_unknown_method_returns_404` | Wrong HTTP method returns 404 |
| `test_options_returns_204` | OPTIONS returns 204 No Content |
| `test_options_has_cors_headers` | CORS headers present in OPTIONS response |
| `test_get_returns_content_type` | GET responses include a content type |
| `test_all_get_endpoints_return_200` | All registered GET routes are healthy |
