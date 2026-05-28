#!/bin/sh
SERVER_URL=http://host.containers.internal:18750 uv run pytest -v "$@"
