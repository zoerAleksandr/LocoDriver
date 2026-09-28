// Pass screen — добавление следования пассажиром (iOS + Android · light + dark)
//
// Поля из текущей версии (скриншот Android):
//   • № поезда
//   • Станция отправления + дата + время
//   • Станция прибытия + дата + время
//   • Примечания
//   • (расчётное) Время в пути — показываем как чип-сепаратор между «Откуда» и «Куда»
//
// UX-улучшения vs скриншот:
//   1. Поля сгруппированы: «Поезд», «Откуда» (станция+дата+время в одной карточке),
//      бейдж «В пути · 7 ч 40 мин», «Куда», «Примечания». Раньше — 5 одинаковых
//      инпутов вперемешку без визуальной иерархии.
//   2. У каждой группы — label (mono caps).
//   3. № поезда получил иконку IcRails слева — отличается от текстовых полей.
//   4. Дата и время — отдельные тапабельные пилюли с иконками 📅/🕐 и chevron,
//      чтобы было видно что это селекторы, а не текст.
//   5. Длительность поездки вынесена как чип-сепаратор между «Откуда» и «Куда»
//      (вместо большой цифры 07:40 без контекста сверху экрана).
//   6. Примечания — настоящий textarea, не одиночная строка.
//   7. iOS: «Готово» сверху справа (как trailingAccent); Android: large title + ⋮.

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

// Группа полей: верхний caps-mono лейбл + содержимое.
function PassGroup({ t, label, children, style }) {
  return (
    <div style={{ ...style }}>
      <div style={{
        ...M.t.group(t),
        padding: '4px 4px 8px',
      }}>{label}</div>
      {children}
    </div>
  );
}

// Тапабельная пилюля «иконка + значение + ›» — для даты и времени.
function PickerChip({ t, icon, value, flex = 1 }) {
  return (
    <button style={{
      flex, minWidth: 0,
      display: 'inline-flex', alignItems: 'center', gap: 8,
      padding: '10px 12px', borderRadius: 12, border: 'none',
      background: t.bgSubtle, color: t.text, cursor: 'pointer',
      fontFamily: M.fontMono, fontSize: 15, fontWeight: 600,
      fontVariantNumeric: 'tabular-nums',
      textAlign: 'left',
    }}>
      <span style={{
        width: 22, height: 22, borderRadius: 11,
        background: 'rgba(0,0,0,0)', color: t.textMuted,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>{icon}</span>
      <span style={{
        flex: 1, minWidth: 0,
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
      }}>{value}</span>
      <IcChevronRight width="12" height="12" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </button>
  );
}

// Карточка «Откуда / Куда» — название станции (тап → picker) + дата + время.
function StationLegCard({ t, label, station, date, time }) {
  return (
    <PassGroup t={t} label={label}>
      <div style={{
        background: t.surface,
        borderRadius: M.r.cardIOS,
        padding: '14px 16px',
        boxShadow: M.shadow.sm,
        display: 'flex', flexDirection: 'column', gap: 12,
      }}>
        {/* Станция */}
        <button style={{
          width: '100%', padding: '4px 0',
          background: 'transparent', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', gap: 12,
          textAlign: 'left',
        }}>
          <span style={{
            width: 32, height: 32, borderRadius: 16,
            background: t.accentSoft, color: t.accent,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}>
            <IcMapPin width="16" height="16"/>
          </span>
          <span style={{
            flex: 1, minWidth: 0,
            ...M.t.stationName(t), fontSize: 18,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>{station}</span>
        </button>

        <div style={{ height: 1, background: t.border }}/>

        {/* Дата + Время */}
        <div style={{ display: 'flex', gap: 8 }}>
          <PickerChip t={t} icon={<IcCalendar width="14" height="14"/>} value={date} flex={1.4}/>
          <PickerChip t={t} icon={<IcClock width="14" height="14"/>} value={time} flex={1}/>
        </div>
      </div>
    </PassGroup>
  );
}

// Бейдж длительности «В пути · 7 ч 40 мин» — между двумя StationLegCard.
function DurationBadge({ t, label }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10, padding: '14px 4px',
    }}>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
      <span style={{
        padding: '6px 12px', borderRadius: 999,
        background: t.accentSoft, color: t.accent,
        fontFamily: M.fontMono, fontSize: 12, fontWeight: 600,
        letterSpacing: .6,
        display: 'inline-flex', alignItems: 'center', gap: 6,
      }}>
        <IcClock width="13" height="13"/>
        {label}
      </span>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
    </div>
  );
}

// Простой контейнер-карточка как у Card но без shared-ui зависимостей —
// используется для «Поезд №» и «Примечания», чтобы не путать с StationLegCard.
function FlatCard({ t, children, pad = '14px 16px' }) {
  return (
    <div style={{
      background: t.surface,
      borderRadius: M.r.cardIOS,
      padding: pad,
      boxShadow: M.shadow.sm,
    }}>{children}</div>
  );
}

// Карточка «Начало работы по прибытию» — ключевая опция следования пассажиром.
// Когда включена, время работы и отдыха отсчитывается с прибытия пассажиром
// в пункт явки, а НЕ с первоначальной явки в основном депо. Это меняет точку
// отсчёта на экране «Маршрут».
function WorkStartCard({ t, mat, on = true, station, date, time }) {
  return (
    <PassGroup t={t} label="Учёт рабочего времени">
      <FlatCard t={t} pad="4px 0">
        {/* Переключатель */}
        <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>
              Явка по прибытию
            </div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 3, lineHeight: 1.4 }}>
              Явка на работу по прибытию пассажиром.
            </div>
          </div>
          <Switch t={t} on={on} mat={mat}/>
        </div>

        {on && <>
          <div style={{ height: 1, background: t.border, marginLeft: 16 }}/>
          {/* Результат — вычисленное начало отсчёта рабочего времени */}
          <div style={{ padding: '13px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{
              width: 34, height: 34, borderRadius: 17,
              background: t.accentSoft, color: t.accent,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0,
            }}>
              <IcClock width="17" height="17"/>
            </span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{
                fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 700,
                letterSpacing: 1.2, color: t.accent, marginBottom: 3,
              }}>НАЧАЛО РАБОТЫ</div>
              <div style={{
                fontFamily: M.fontMono, fontSize: 15, fontWeight: 600, color: t.text,
                fontVariantNumeric: 'tabular-nums',
              }}>
                {station}<span style={{ color: t.textFaint, fontWeight: 500 }}> · </span>{date} {time}
              </div>
            </div>
          </div>
        </>}
      </FlatCard>
    </PassGroup>
  );
}

// ─────────────────────────────────────────────────────────────
// Контент экрана — общий для iOS и Android
// ─────────────────────────────────────────────────────────────
function PassScreenBody({ t, mat, workStart = true }) {
  return (
    <div style={{
      padding: '8px 16px 32px',
      display: 'flex', flexDirection: 'column', gap: 18,
    }}>
      {/* Поезд */}
      <PassGroup t={t} label="Поезд">
        <FlatCard t={t}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 12,
          }}>
            <span style={{
              width: 36, height: 36, borderRadius: 18,
              background: t.bgSubtle, color: t.textMuted,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0,
            }}>
              <IcRails width="18" height="18"/>
            </span>
            <span style={{
              ...M.t.label(t), fontSize: 13,
            }}>№</span>
            <span style={{
              flex: 1,
              fontFamily: M.fontMono, fontSize: 22, fontWeight: 700,
              color: t.text, letterSpacing: -0.3,
              fontVariantNumeric: 'tabular-nums',
            }}>1945</span>
          </div>
        </FlatCard>
      </PassGroup>

      {/* Откуда */}
      <StationLegCard
        t={t}
        label="Откуда"
        station="СПбСМ"
        date="13.05.26"
        time="19:50"
      />

      {/* В пути */}
      <DurationBadge t={t} label="В пути · 7 ч 40 мин"/>

      {/* Куда */}
      <StationLegCard
        t={t}
        label="Куда"
        station="Веймарн"
        date="14.05.26"
        time="03:30"
      />

      {/* Учёт рабочего времени — начало по прибытию */}
      <WorkStartCard t={t} mat={mat} on={workStart} station="Веймарн" date="14.05.26" time="03:30"/>

      {/* Примечания */}
      <PassGroup t={t} label="Примечания">
        <FlatCard t={t} pad="14px 16px 16px">
          <div style={{
            minHeight: 64,
            color: t.textFaint,
            fontFamily: M.fontSans, fontSize: 15, fontWeight: 400,
            lineHeight: 1.4,
          }}>
            Например: бригада №2, вагон 7, место 32
          </div>
        </FlatCard>
      </PassGroup>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// iOS screen
// ─────────────────────────────────────────────────────────────
function IOSScreenPass({ dark = false, height = 920, workStart = true }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{
        height: '100%', display: 'flex', flexDirection: 'column',
        paddingTop: 54,
      }}>
        <NavBarIOS
          t={t}
          style="text"
          back
          backLabel="Готово"
          title="Пассажиром"
          trailing={
            <button style={{
              width: 40, height: 40, border: 'none',
              background: 'transparent', color: t.text, cursor: 'pointer',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            }} aria-label="Ещё">
              <svg width="4" height="18" viewBox="0 0 4 18" fill="currentColor">
                <circle cx="2" cy="2" r="2"/><circle cx="2" cy="9" r="2"/><circle cx="2" cy="16" r="2"/>
              </svg>
            </button>
          }
        />
        <div style={{ flex: 1, overflowY: 'auto' }}>
          <PassScreenBody t={t} workStart={workStart}/>
        </div>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Android screen
// ─────────────────────────────────────────────────────────────
function AndroidScreenPass({ dark = false, height = 920, workStart = true }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark} height={height}>
      {/* Top app bar (M3 small) */}
      <div style={{
        padding: '8px 4px', display: 'flex', alignItems: 'center', gap: 4,
        background: t.bg,
      }}>
        <button style={{
          width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }} aria-label="Назад">
          <IcChevronLeft width="22" height="22"/>
        </button>
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>Пассажиром</div>
        <button style={{
          width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }} aria-label="Ещё">
          <svg width="4" height="18" viewBox="0 0 4 18" fill="currentColor">
            <circle cx="2" cy="2" r="2"/><circle cx="2" cy="9" r="2"/><circle cx="2" cy="16" r="2"/>
          </svg>
        </button>
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>
        <PassScreenBody t={t} mat workStart={workStart}/>
        {/* M3 filled CTA */}
        <div style={{ padding: '0 16px 24px' }}>
          <button style={{
            width: '100%', height: 52, borderRadius: 26, border: 'none',
            background: t.cta, color: t.ctaInk, cursor: 'pointer',
            fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
          }}>Сохранить</button>
        </div>
      </div>
    </ADevice>
  );
}

Object.assign(window, {
  IOSScreenPass, AndroidScreenPass,
  PassGroup, PickerChip, StationLegCard, DurationBadge, FlatCard, WorkStartCard, PassScreenBody,
});
