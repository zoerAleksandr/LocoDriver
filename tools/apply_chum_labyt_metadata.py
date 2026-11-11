#!/usr/bin/env python3
"""Attach XML-derived signal/crossing metadata to an imported advisor section."""

import asyncio
import json
import sys
import uuid

from sqlalchemy import text

from src.db.session import async_session
from src.services.advisor_km import to_abs_km
from src.services.advisor_ref_import import parse_ref_section


async def main(path: str):
    payload = json.load(open(path, encoding="utf-8"))
    section_id = uuid.uuid5(
        uuid.UUID("6f1c0a52-3a7e-4d0b-9a55-5d1b7d3c2e11"),
        "ref:" + payload["id"],
    )
    profile = payload.get("prof", [])
    gaps = [
        (float(a[0]), float(b[0]))
        for a, b in zip(profile, profile[1:])
        if float(b[0]) - float(a[0]) > 1.0
    ]
    # The source has many normal coarse profile steps; only the largest gap is
    # an actual missing-data interval and must be rendered as noData.
    missing_start, missing_end = max(gaps, key=lambda x: x[1] - x[0])
    parsed = parse_ref_section(payload)
    elements = []
    for item in parsed["elements"]:
        if float(item["endKm"]) > missing_start and float(item["startKm"]) < missing_end:
            item = dict(item, noData=True, gradePermille=0.0)
        elements.append(item)
    async with async_session() as session:
        current = await session.execute(
            text("select profile_variants from advisor_section where id = :id"),
            {"id": section_id},
        )
        variants = current.scalar_one_or_none()
        if variants is None:
            raise SystemExit("section not found")
        variants = dict(variants or {})
        ref_variant = dict(variants.get("ref") or {})
        ref_variant["elements"] = elements
        variants["ref"] = ref_variant
        result = await session.execute(
            text("""
                update advisor_section
                set length_km = :length_km,
                    km_origin = :km_origin,
                    km_marks = cast(:km_marks as jsonb),
                    geometry = cast(:geometry as jsonb),
                    stations = cast(:stations as jsonb),
                    elements = cast(:elements as jsonb),
                    profile_variants = cast(:profile_variants as jsonb),
                    signals = cast(:signals as jsonb),
                    crossings = cast(:crossings as jsonb),
                    brake_tests = cast(:brake_tests as jsonb),
                    version = version + 1
                where id = :id
                returning id
            """),
            {
                "signals": json.dumps(payload.get("signals", []), ensure_ascii=False),
                "crossings": json.dumps(payload.get("crossings", []), ensure_ascii=False),
                "brake_tests": json.dumps(payload.get("brakeTests", []), ensure_ascii=False),
                "id": section_id,
                "length_km": parsed["length_km"],
                "km_origin": to_abs_km(parsed["km_marks"], 0.0) if parsed["km_marks"] else None,
                "km_marks": json.dumps(parsed["km_marks"], ensure_ascii=False),
                "geometry": json.dumps(parsed["geometry"], ensure_ascii=False),
                "stations": json.dumps(parsed["stations"], ensure_ascii=False),
                "elements": json.dumps(elements, ensure_ascii=False),
                "profile_variants": json.dumps(variants, ensure_ascii=False),
            },
        )
        if result.scalar_one_or_none() is None:
            raise SystemExit("section not found")
        await session.commit()
    print(section_id, f"noData={missing_start:.3f}..{missing_end:.3f} km")


if __name__ == "__main__":
    asyncio.run(main(sys.argv[1]))
