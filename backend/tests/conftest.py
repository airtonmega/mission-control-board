import pytest
import os
from httpx import AsyncClient, ASGITransport

os.environ.setdefault("USE_MOCK_AI", "true")
os.environ.setdefault("JWT_SECRET", "test-secret-key")

from app.main import app  # noqa: E402 — must import after env vars set


@pytest.fixture
async def client():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac
