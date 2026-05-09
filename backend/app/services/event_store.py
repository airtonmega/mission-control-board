"""In-memory session event store for analytics.

Single-process only — data is lost on restart. Suitable for demo/research use.
"""
import time
from dataclasses import dataclass, field
from typing import Optional
from collections import defaultdict


@dataclass
class AnalysisEvent:
    event_type: str   # "text", "image", "audio", "interview_start", "interview_evaluate"
    session_id: str
    timestamp: float = field(default_factory=time.time)
    latency_ms: Optional[int] = None
    theme: Optional[str] = None
    score: Optional[float] = None
    mock: bool = True


_events: dict[str, list[AnalysisEvent]] = defaultdict(list)


def record_event(event: AnalysisEvent) -> None:
    _events[event.session_id].append(event)


def get_events(session_id: str) -> list[AnalysisEvent]:
    return list(_events.get(session_id, []))


def clear_events(session_id: str) -> None:
    _events.pop(session_id, None)


def clear_all() -> None:
    _events.clear()
