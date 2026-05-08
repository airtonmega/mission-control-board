import pytest


@pytest.mark.anyio
async def test_analyze_text_mock(client):
    response = await client.post(
        "/analyze/text",
        json={
            "session_id": "test-session-001",
            "question": "O que são Kotlin Coroutines e como funcionam?",
            "language": "pt-BR",
        },
    )
    assert response.status_code == 200
    data = response.json()

    assert "detected_theme" in data
    assert "quick_tip" in data
    assert "short_answer" in data
    assert "interview_answer" in data
    assert "complete_answer" in data
    assert isinstance(data["common_errors"], list)
    assert isinstance(data["study_suggestions"], list)
    assert 0.0 <= data["confidence_score"] <= 1.0
    assert data["mock"] is True
    assert data["processing_time_ms"] > 0


@pytest.mark.anyio
async def test_analyze_text_theme_detection(client):
    response = await client.post(
        "/analyze/text",
        json={
            "session_id": "test-session-002",
            "question": "Como usar Jetpack Compose com Material 3?",
            "language": "pt-BR",
        },
    )
    data = response.json()
    assert "compose" in data["detected_theme"].lower() or "android" in data["detected_theme"].lower()


@pytest.mark.anyio
async def test_analyze_text_validation_error(client):
    response = await client.post(
        "/analyze/text",
        json={"session_id": "x", "question": "ab"},
    )
    assert response.status_code == 422


@pytest.mark.anyio
async def test_analyze_image_mock(client):
    response = await client.post(
        "/analyze/image",
        json={"session_id": "test-session-003", "image_base64": "base64fakedata=="},
    )
    assert response.status_code == 200
    assert response.json()["mock"] is True


@pytest.mark.anyio
async def test_interview_start(client):
    response = await client.post(
        "/interview/start",
        json={"session_id": "test-session-004", "area": "Android", "level": "Pleno"},
    )
    assert response.status_code == 200
    data = response.json()
    assert "first_question" in data
    assert data["total_questions"] > 0


@pytest.mark.anyio
async def test_interview_evaluate(client):
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "test-session-005",
            "interview_id": "iv-001",
            "question": "O que são coroutines?",
            "answer": "São unidades de computação suspensas...",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["score"] <= 10.0
    assert "feedback" in data


@pytest.mark.anyio
async def test_session_report(client):
    response = await client.get("/reports/session/test-session-001")
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == "test-session-001"
    assert isinstance(data["topics_covered"], list)
