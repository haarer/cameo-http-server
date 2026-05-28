import os
import httpx
import pytest

SERVER_URL = os.environ.get("SERVER_URL", "http://localhost:18741")


@pytest.fixture(scope="session")
def client():
    with httpx.Client(base_url=SERVER_URL, timeout=10) as c:
        yield c


# -- Connectivity --

def test_server_reachable(client):
    r = client.get("/test")
    assert r.status_code == 200


def test_server_reachable_via_test_endpoint(client):
    r = client.get("/test")
    assert r.status_code == 200
    assert r.text == "OK"


# -- GET endpoints --

def test_logging_demo(client):
    r = client.get("/logging_demo")
    assert r.status_code == 200
    assert "Cameo notification window" in r.text


def test_list_elements_returns_json(client):
    r = client.get("/list_elements/1-OpsCon")
    assert r.status_code == 200
    assert r.headers["content-type"] == "application/json"
    data = r.json()
    assert isinstance(data, list)


# -- 404 handling --

def test_nonexistent_path_returns_404(client):
    r = client.get("/nonexistent")
    assert r.status_code == 404
    assert "No handler" in r.text


def test_nonexistent_path_with_suffix_returns_404(client):
    r = client.get("/test/extra")
    assert r.status_code == 404


def test_unknown_method_returns_404(client):
    r = client.put("/test")
    assert r.status_code == 404


# -- CORS (OPTIONS) --

def test_options_returns_204(client):
    r = client.options("/test")
    assert r.status_code == 204


def test_options_has_cors_headers(client):
    r = client.options("/test")
    assert r.headers.get("access-control-allow-origin") == "*"
    assert "POST" in r.headers.get("access-control-allow-methods", "")
    assert "OPTIONS" in r.headers.get("access-control-allow-methods", "")
    assert "Content-Type" in r.headers.get("access-control-allow-headers", "")


# -- Response headers --

def test_get_returns_content_type(client):
    r = client.get("/test")
    ct = r.headers.get("content-type", "")
    assert "text/plain" in ct or "application/octet-stream" in ct or ct == ""


def test_all_get_endpoints_return_200(client):
    for path in ["/test", "/logging_demo", "/list_elements/1-OpsCon"]:
        r = client.get(path)
        assert r.status_code == 200, f"{path} returned {r.status_code}"
