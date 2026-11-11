import asyncio, json
from sqlalchemy import text
from src.db.session import async_session

async def main():
    async with async_session() as s:
        r=(await s.execute(text("""
            select name, from_station, to_station, length_km, km_origin,
                   profile_source, source, notes, geometry, stations
            from advisor_section
            where id = '9a0a6edc-ba64-52c5-907c-cd118e0ae8fe'
        """))).mappings().one()
    out=dict(r)
    print(json.dumps(out, ensure_ascii=False, default=str))

if __name__ == '__main__': asyncio.run(main())
