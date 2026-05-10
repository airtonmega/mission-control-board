import time
from unittest.mock import patch, AsyncMock, MagicMock

import httpx
import pytest


def _make_mock_http_client(side_effect):
    """Cria um mock de httpx.AsyncClient compatível com `async with`."""
    inner = AsyncMock()
    inner.post = AsyncMock(side_effect=side_effect)
    ctx = MagicMock()
    ctx.__aenter__ = AsyncMock(return_value=inner)
    ctx.__aexit__ = AsyncMock(return_value=False)
    return ctx


@pytest.mark.anyio
async def test_realtime_session_success(client):
    """Token criado com sucesso quando OpenAI responde corretamente."""
    fake_response = {
        "id": "sess_test123",
        "model": "gpt-4o-realtime-preview-2024-12-17",
        "voice": "alloy",
        "client_secret": {
            "value": "eph_test_token_abc123",
            "expires_at": int(time.time()) + 60,
        },
    }
    mock_resp_obj = AsyncMock()
    mock_resp_obj.json = MagicMock(return_value=fake_response)
    mock_resp_obj.raise_for_status = MagicMock()

    mock_inner = AsyncMock()
    mock_inner.post = AsyncMock(return_value=mock_resp_obj)
    ctx = MagicMock()
    ctx.__aenter__ = AsyncMock(return_value=mock_inner)
    ctx.__aexit__ = AsyncMock(return_value=False)

    with patch("app.routers.realtime.httpx.AsyncClient", return_value=ctx):
        response = await client.post(
            "/realtime/session",
            json={"session_id": "test-rt-001"},
        )
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == "test-rt-001"
    assert data["realtime_session_id"] == "sess_test123"
    assert data["client_secret"]["value"] == "eph_test_token_abc123"
    assert data["client_secret"]["expires_at"] > int(time.time())
    assert data["model"] == "gpt-4o-realtime-preview-2024-12-17"
    assert data["voice"] == "alloy"


@pytest.mark.anyio
async def test_realtime_session_token_not_empty(client):
    """client_secret.value nunca é vazio na resposta de sucesso."""
    fake_response = {
        "id": "sess_xyz",
        "model": "gpt-4o-realtime-preview-2024-12-17",
        "voice": "alloy",
        "client_secret": {"value": "eph_not_empty", "expires_at": int(time.time()) + 60},
    }
    mock_resp_obj = AsyncMock()
    mock_resp_obj.json = MagicMock(return_value=fake_response)
    mock_resp_obj.raise_for_status = MagicMock()
    mock_inner = AsyncMock()
    mock_inner.post = AsyncMock(return_value=mock_resp_obj)
    ctx = MagicMock()
    ctx.__aenter__ = AsyncMock(return_value=mock_inner)
    ctx.__aexit__ = AsyncMock(return_value=False)
    with patch("app.routers.realtime.httpx.AsyncClient", return_value=ctx):
        response = await client.post("/realtime/session", json={"session_id": "test-rt-002"})
    assert response.json()["client_secret"]["value"] != ""


@pytest.mark.anyio
async def test_realtime_session_has_future_expires_at(client):
    """expires_at deve ser um timestamp no futuro."""
    future = int(time.time()) + 60
    fake_response = {
        "id": "sess_abc",
        "model": "gpt-4o-realtime-preview-2024-12-17",
        "voice": "alloy",
        "client_secret": {"value": "eph_future", "expires_at": future},
    }
    mock_resp_obj = AsyncMock()
    mock_resp_obj.json = MagicMock(return_value=fake_response)
    mock_resp_obj.raise_for_status = MagicMock()
    mock_inner = AsyncMock()
    mock_inner.post = AsyncMock(return_value=mock_resp_obj)
    ctx = MagicMock()
    ctx.__aenter__ = AsyncMock(return_value=mock_inner)
    ctx.__aexit__ = AsyncMock(return_value=False)
    with patch("app.routers.realtime.httpx.AsyncClient", return_value=ctx):
        response = await client.post("/realtime/session", json={"session_id": "test-rt-003"})
    assert response.json()["expires_at"] > int(time.time())


@pytest.mark.anyio
async def test_realtime_session_timeout_returns_503(client):
    with patch(
        "app.routers.realtime.httpx.AsyncClient",
        return_value=_make_mock_http_client(httpx.TimeoutException("timeout")),
    ):
        response = await client.post(
            "/realtime/session",
            json={"session_id": "test-rt-004"},
        )
    assert response.status_code == 503
    assert response.json()["detail"]["error_code"] == "AI_SERVICE_UNAVAILABLE"


@pytest.mark.anyio
async def test_realtime_session_http_error_returns_503(client):
    mock_req = httpx.Request("POST", "https://api.openai.com/v1/realtime/sessions")
    mock_resp = httpx.Response(429, request=mock_req)
    err = httpx.HTTPStatusError("429", request=mock_req, response=mock_resp)

    with patch(
        "app.routers.realtime.httpx.AsyncClient",
        return_value=_make_mock_http_client(err),
    ):
        response = await client.post(
            "/realtime/session",
            json={"session_id": "test-rt-005"},
        )
    assert response.status_code == 503
    assert response.json()["detail"]["error_code"] == "AI_SERVICE_UNAVAILABLE"
