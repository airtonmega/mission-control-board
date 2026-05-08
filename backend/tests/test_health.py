import pytest


@pytest.mark.anyio
async def test_health_ok(client):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data
    assert data["mock_mode"] is True


@pytest.mark.anyio
async def test_root(client):
    response = await client.get("/")
    assert response.status_code == 200
    assert "app" in response.json()


@pytest.mark.anyio
async def test_auth_anonymous(client):
    response = await client.post("/auth/anonymous")
    assert response.status_code == 200
    data = response.json()
    assert "session_id" in data
    assert "token" in data


@pytest.mark.anyio
async def test_consent_accept(client):
    auth_resp = await client.post("/auth/anonymous")
    session_id = auth_resp.json()["session_id"]

    response = await client.post(
        "/consent/accept",
        json={"session_id": session_id, "accepted": True, "timestamp": "2026-01-01T00:00:00Z"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["consent_recorded"] is True
