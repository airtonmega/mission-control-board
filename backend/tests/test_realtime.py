import time
from unittest.mock import patch, AsyncMock

import httpx
import pytest


@pytest.mark.anyio
async def test_realtime_session_mock_returns_expected_shape(client):
    response = await client.post(
        "/realtime/session",
        json={"session_id": "test-rt-001"},
    )
    assert response.status_code == 200
    data = response.json()

    assert data["session_id"] == "test-rt-001"
    assert "realtime_session_id" in data
    assert "client_secret" in data
    assert "value" in data["client_secret"]
    assert "expires_at" in data["client_secret"]
    assert "model" in data
    assert "expires_at" in data
    assert "voice" in data
    assert data["mock"] is True


@pytest.mark.anyio
async def test_realtime_session_mock_token_not_empty(client):
    response = await client.post(
        "/realtime/session",
        json={"session_id": "test-rt-002"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["client_secret"]["value"] != ""


@pytest.mark.anyio
async def test_realtime_session_mock_has_future_expires_at(client):
    response = await client.post(
        "/realtime/session",
        json={"session_id": "test-rt-003"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["expires_at"] > int(time.time())


def _make_mock_http_client(side_effect):
    """Cria um mock de httpx.AsyncClient compatível com `async with`."""
    from unittest.mock import MagicMock
    inner = AsyncMock()
    inner.post = AsyncMock(side_effect=side_effect)
    ctx = MagicMock()
    ctx.__aenter__ = AsyncMock(return_value=inner)
    ctx.__aexit__ = AsyncMock(return_value=False)
    return ctx


@pytest.mark.anyio
async def test_realtime_session_real_mode_unavailable_returns_503(client):
    with patch("app.routers.realtime._should_use_mock", return_value=False):
        with patch(
            "app.routers.realtime.httpx.AsyncClient",
            return_value=_make_mock_http_client(httpx.TimeoutException("timeout")),
        ):
            response = await client.post(
                "/realtime/session",
                json={"session_id": "test-rt-004"},
            )
    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_SERVICE_UNAVAILABLE"


@pytest.mark.anyio
async def test_realtime_session_real_mode_http_error_returns_503(client):
    mock_req = httpx.Request("POST", "https://api.openai.com/v1/realtime/sessions")
    mock_resp = httpx.Response(429, request=mock_req)
    err = httpx.HTTPStatusError("429 Too Many Requests", request=mock_req, response=mock_resp)

    with patch("app.routers.realtime._should_use_mock", return_value=False):
        with patch(
            "app.routers.realtime.httpx.AsyncClient",
            return_value=_make_mock_http_client(err),
        ):
            response = await client.post(
                "/realtime/session",
                json={"session_id": "test-rt-005"},
            )
    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_SERVICE_UNAVAILABLE"
