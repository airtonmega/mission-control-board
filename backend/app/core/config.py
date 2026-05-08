from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "TeseAI Live API"
    app_version: str = "1.0.0"
    debug: bool = False

    use_mock_ai: bool = True
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    openai_timeout_seconds: float = 60.0
    openai_max_repair_attempts: int = 1

    jwt_secret: str = "change-me-in-production-secret"
    jwt_expire_minutes: int = 60 * 24

    cors_origins: list[str] = ["*"]


@lru_cache
def get_settings() -> Settings:
    return Settings()
