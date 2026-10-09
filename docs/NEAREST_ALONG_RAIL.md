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
