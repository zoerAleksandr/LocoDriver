# stations/nearest alongRail

`POST /v1/service_phase/stations/nearest` (JWT). Аддитивно, старые клиенты не затронуты.

Запрос: `{latitude, longitude, radiusM?: 2000, alongRail?: false}`
- `alongRail=false` (по умолчанию) — как раньше: ближайшая по ПРЯМОЙ, radiusM 100..5000, ответ `{"station": {...}|null}`.
- `alongRail=true` — ближайшая станция/о.п. ПО РЕЛЬСАМ: точка привязывается к пути (до 60 м), Дейкстра по графу в обе
  стороны до radiusM метров по пути (radiusM зажимается в 100..3000). Станция на соседней ветке, до которой по графу
  далеко, не выбирается, даже если рядом по прямой.

Ответ при alongRail=true:
`{"station": {osmId,name,kind,esr,operator,latitude,longitude}|null, "alongRail": true, "distanceAlongM": int, "offsetM": int, "reason"?: str}`
- `offsetM` — расстояние точки до пути; `distanceAlongM` — расстояние до станции по пути.
- `station=null` + `reason`: `off_rails` (точка дальше 60 м от любого пути; distanceAlongM/offsetM нет), `not_found`
  (на пути в радиусе станций нет), `no_rail_data` (на сервере нет графа).
- 503 (`Retry-After: 30`) — сервис занят или поиск не уложился в 2 с (пул/семафор как у route/length).
- Тот же лимит запросов, что у прямого режима; координаты не логируются.

## candidates (станции справочника клиента)

Запрос (только при `alongRail=true`, иначе игнорируется): `"candidates": [{"key": str(1..64), "name": str(1..120), "latitude", "longitude"}]`,
максимум 40 штук (больше — 422; неверные координаты/длины — 422). Клиент шлёт станции своего справочника в радиусе ~2 км.

Алгоритм: точка привязывается к пути (60 м) -> Дейкстра по пути в radiusM; каждый кандидат привязывается к графу
(<= 80 м, как станции OSM; дальше — игнорируется) и получает расстояние по пути; кандидаты и OSM-станции сравниваются
по пути. Если кандидат не дальше лучшей OSM-станции более чем на 30 м (ничья) — выигрывает кандидат (названия пользователя).
Бюджет 2 с/пул/семафор — общие с обычным поиском (при превышении 503).

Ответ (аддитивно): `source: "candidate"|"osm"` (при найденной станции) и `candidateKey` (только если выбран кандидат).
Для кандидата `station = {osmId: null, name, kind: "station", esr: null, operator: null, latitude, longitude}` — координаты
и название те, что прислал клиент. `distanceAlongM`, `offsetM`, `alongRail`, `reason` (`off_rails`/`not_found`/`no_rail_data`) — как раньше.
Без `candidates` ответ прежний + `source: "osm"`.
