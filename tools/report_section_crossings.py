#!/usr/bin/env python3
"""Read-only report of advisor crossings and their coordinates."""

import asyncio
import json
import math
from sqlalchemy import text
from src.db.session import async_session

IDS = [
    "9a0a6edc-ba64-52c5-907c-cd118e0ae8fe",  # Сергач — Канаш
    "993ac3c6-cf90-55cc-b930-52e0ae569f1f",  # Вековка — Разъезд 408 км
    "717a56b6-6949-5d39-8e44-e8c566ae9c3e",  # Нижний Новгород — Сергач
    "a5615ae0-bcd3-5417-b663-d29877118f2f",  # Окская — Зелецино
    "b981af45-236a-54a7-8e0f-e4a6ba01b50e",  # Нижний Новгород — Арзамас
]


def distance_m(a, b):
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp, dl = p2 - p1, math.radians(b[1] - a[1])
    q = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 6_371_000 * 2 * math.asin(math.sqrt(q))


def point_at(geometry, km):
    points = sorted(geometry or [], key=lambda p: float(p["km"]))
    if not points:
        return None
    for a, b in zip(points, points[1:]):
        if float(a["km"]) <= km <= float(b["km"]):
            q = (km - float(a["km"])) / max(1e-9, float(b["km"]) - float(a["km"]))
            return {"lat": float(a["lat"]) + q * (float(b["lat"]) - float(a["lat"])),
                    "lon": float(a["lon"]) + q * (float(b["lon"]) - float(a["lon"]))}
    p = points[0] if km < float(points[0]["km"]) else points[-1]
    return {"lat": float(p["lat"]), "lon": float(p["lon"])}


async def main():
    async with async_session() as session:
        rows = (await session.execute(text("""
            select id::text, name, crossings, geometry
            from advisor_section where id = any(cast(:ids as uuid[]))
            order by name
        """), {"ids": IDS})).mappings().all()
    out = []
    for row in rows:
        out.append({"id": row["id"], "name": row["name"], "geometry": row["geometry"] or [], "crossings": [
            dict(c, point=point_at(row["geometry"], float(c["km"])))
            for c in (row["crossings"] or [])
        ]})
    print(json.dumps(out, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    asyncio.run(main())
