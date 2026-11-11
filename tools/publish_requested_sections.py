#!/usr/bin/env python3
"""Publish the exact sections approved in the user request."""

import asyncio
from uuid import UUID

from sqlalchemy import text

from src.db.session import async_session


IDS = [
    UUID("4549905d-eac0-557c-b5b6-d7ed5202538b"),  # Чум — Лабытнанги
    UUID("a1d006f3-f213-549e-81d1-ac0a9f785d55"),  # Бамовская — Тында
    UUID("9a0a6edc-ba64-52c5-907c-cd118e0ae8fe"),  # Сергач — Канаш
    UUID("993ac3c6-cf90-55cc-b930-52e0ae569f1f"),  # Вековка — Разъезд 408 км
    UUID("717a56b6-6949-5d39-8e44-e8c566ae9c3e"),  # Нижний Новгород — Сергач
    UUID("a5615ae0-bcd3-5417-b663-d29877118f2f"),  # Окская — Зелецино
    UUID("b981af45-236a-54a7-8e0f-e4a6ba01b50e"),  # Нижний Новгород — Арзамас
]


async def main():
    async with async_session() as session:
        rows = await session.execute(
            text("""
                update advisor_section
                set is_published = true, version = version + 1, updated_at = now()
                where id = any(:ids)
                returning id, name
            """),
            {"ids": IDS},
        )
        published = rows.fetchall()
        await session.commit()
    for row in published:
        print(row.id, row.name)
    if len(published) != len(IDS):
        raise SystemExit(f"expected {len(IDS)} sections, updated {len(published)}")


if __name__ == "__main__":
    asyncio.run(main())
