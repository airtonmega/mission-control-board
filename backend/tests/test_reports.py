"""Tests for /reports endpoints — Phase 6."""
import pytest

from app.services.event_store import clear_events


@pytest.mark.anyio
async def test_session_report_empty_session(client):
    """GET /reports/session/{id} returns mock report with all required fields when no events."""
    response = await client.get("/reports/session/abc123")
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == "abc123"
    assert isinstance(data["total_queries"], int) and data["total_queries"] >= 0
    assert isinstance(data["topics_covered"], list)
    assert "input_modes" in data
    assert isinstance(data["highlights"], list) and len(data["highlights"]) > 0


@pytest.mark.anyio
async def test_session_report_short_id_returns_404(client):
    """GET /reports/session/{id} with id shorter than 5 chars returns 404."""
    response = await client.get("/reports/session/ab")
    assert response.status_code == 404


@pytest.mark.anyio
async def test_session_report_has_input_modes(client):
    """Response includes input_modes with text/image/audio/interview fields."""
    response = await client.get("/reports/session/xyz99")
    assert response.status_code == 200
    data = response.json()
    modes = data["input_modes"]
    assert "text" in modes
    assert "image" in modes
    assert "audio" in modes
    assert "interview" in modes


@pytest.mark.anyio
async def test_session_report_has_events_list(client):
    """Response includes an events field that is a list."""
    response = await client.get("/reports/session/xyz99")
    assert response.status_code == 200
    data = response.json()
    assert "events" in data
    assert isinstance(data["events"], list)


@pytest.mark.anyio
async def test_session_report_after_analyze(client):
    """After POST /analyze/text, GET /reports/session/{id} reflects total_queries=1."""
    session_id = "report-test-unique-42"
    # Ensure clean state for this session
    clear_events(session_id)

    analyze_response = await client.post(
        "/analyze/text",
        json={
            "session_id": session_id,
            "question": "O que são Kotlin Coroutines e como funcionam?",
            "language": "pt-BR",
        },
    )
    assert analyze_response.status_code == 200

    report_response = await client.get(f"/reports/session/{session_id}")
    assert report_response.status_code == 200
    data = report_response.json()
    assert data["session_id"] == session_id
    assert data["total_queries"] == 1
