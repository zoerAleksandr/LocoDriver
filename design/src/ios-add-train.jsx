// iOS · Добавление поезда — приведён в соответствие стилю проекта.
//
// Что изменилось vs. v1:
//   • Контент сдвинут вниз — теперь не уезжает под status bar.
//   • Появилось ОТДЕЛЬНОЕ поле «Плечо» (маршрут следования) — по тапу
//     открывает шторку со списком сохранённых плеч.
//   • Шторка настроек поезда (⚙): три раздела (Толкач / Двойная тяга /
//     Сдвоенный поезд) — в свёрнутом и развёрнутом виде.
//   • В шапке появилась индикация «есть активные настройки» — точка над ⚙.
//
// Пропсы:
//   • sheet:  null | 'shoulders' | 'settings' | 'settings-expanded'
//
// Структура контента:
//   1. NavBar: «Готово» (back, accent) · «Поезд» (title) · ⚙ (trailing, с badge)
//   2. Чип «№ 3323 ×» — связь с маршрутом (route id)
//   3. Card · «250 км» (Плечо · short) + «№ 2342» (i) + ⊕ «Новый»
//   4. Card · «Вес / Оси / У.Д.» — три инпута в ряд
//   5. ★ Card · «Плечо» — полное название + × (clear) — открывает шторку «Плечи»
//   6. GroupHead «Маршрут» + Card со станциями (timeline)
//   7. Метаданные снизу (Время в пути / Уч / Техн)
//   8. Sticky-кнопка «Добавить станцию»

const SAVED_SHOULDERS = [
  { from: 'Лужская', to: 'Луга',       km: 250 },
  { from: 'Луга',    to: 'Лужская',    km: 250, active: true },
  { from: 'Луга',    to: 'Веймарн',    km: 200 },
  { from: 'Веймарн', to: 'Луга',       km: 200 },
  { from: 'Лужская', to: 'СПбСМ',      km: 200 },
  { from: 'СПбСМ',   to: 'Лужская',    km: 200 },
  { from: 'Луга',    to: 'Ульрамар',   km: 280 },
  { from: 'Ульрамар',to: 'Луга',       km: 280 },
  { from: 'Лужская', to: 'Гатчина ТБ', km: 150 },
  { from: 'Гатчина ТБ', to: 'Лужская', km: 150 },
];

function IOSScreenAddTrain({ dark = false, height = 920, sheet = null }) {
  const t = dark ? M.dark : M.light;

  return (
    <MDevice dark={dark} height={height}>
      {/* Status-bar safe area */}
      <div style={{
        height: '100%', display: 'flex', flexDirection: 'column',
        paddingTop: 54,
      }}>
        <NavBarIOS
          t={t}
          style="text"
          back
          backLabel="Готово"
          title="Поезд"
          trailing={
            <button style={{
              position: 'relative',
              width: 40, height: 40, border: 'none',
              background: 'transparent', color: t.text, cursor: 'pointer',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            }} aria-label="Настройки поезда">
              <IcGear width="22" height="22"/>
              {/* badge — есть активные настройки */}
              <span style={{
                position: 'absolute', top: 8, right: 8,
                width: 7, height: 7, borderRadius: 4,
                background: t.danger || '#E25C3D',
                boxShadow: `0 0 0 1.5px ${t.bg}`,
              }}/>
            </button>
          }
        />

        <div style={{
          flex: 1, overflowY: 'auto',
          padding: '10px 16px 140px',
        }}>
          {/* Чип маршрута справа */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}>
            <Pill t={t} tone="accent" mono={false} style={{
              padding: '6px 12px', fontSize: 14, gap: 8,
            }}>
              <span>№ 3323</span>
              <span style={{ opacity: 0.6, fontSize: 16 }}>×</span>
            </Pill>
          </div>

          {/* Плечо (short) + Номер + Новый */}
          <div style={{
            display: 'grid', gridTemplateColumns: '120px 1fr auto',
            gap: M.s.sm, marginBottom: M.s.md,
          }}>
            <Card t={t}>
              <button style={{
                width: '100%', height: 64, border: 'none', background: 'transparent',
                padding: '10px 14px', textAlign: 'left', cursor: 'pointer',
                display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 2,
              }}>
                <div style={{
                  fontFamily: M.fontMono, fontSize: 22, fontWeight: 600,
                  color: t.text, letterSpacing: -0.3,
                }}>250</div>
                <div style={{ ...M.t.label(t), fontSize: 12 }}>км</div>
              </button>
            </Card>

            <Card t={t}>
              <button style={{
                width: '100%', height: 64, border: 'none', background: 'transparent',
                padding: '10px 14px', textAlign: 'left', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 10,
              }}>
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <div style={{
                    fontFamily: M.fontMono, fontSize: 22, fontWeight: 600,
                    color: t.text, letterSpacing: -0.3,
                  }}>№&nbsp;2342</div>
                  <div style={{ ...M.t.label(t), fontSize: 12 }}>номер поезда</div>
                </div>
                <span style={{
                  width: 22, height: 22, borderRadius: 11,
                  border: `1.5px solid ${t.accent}`, color: t.accent,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  fontFamily: M.fontSans, fontSize: 13, fontWeight: 700, fontStyle: 'italic',
                }}>i</span>
              </button>
            </Card>

            <button style={{
              width: 64, height: 64, borderRadius: M.r.cardIOS,
              background: t.accentSoft, color: t.accent, border: 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', boxShadow: M.shadow.sm,
            }} aria-label="Новый поезд">
              <IcPlus width="22" height="22"/>
            </button>
          </div>

          {/* Вес / Оси / У.Д. */}
          <Card t={t} style={{ marginBottom: M.s.md }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr' }}>
              <FieldStack t={t} label="Вес, т" value="1 390" mono/>
              <div style={{ borderLeft: `1px solid ${t.border}` }}>
                <FieldStack t={t} label="Оси" value="252" mono/>
              </div>
              <div style={{ borderLeft: `1px solid ${t.border}` }}>
                <FieldStack t={t} label="Удельное" value="57" mono/>
              </div>
            </div>
          </Card>

          {/* ★ ПЛЕЧО — кликабельный селектор */}
          <div style={{ marginTop: M.s.lg }}>
            <ShoulderField t={t} from="Луга" to="Лужская" km={250}/>
          </div>

          {/* Маршрут */}
          <GroupHead t={t} mt={M.semantic.groupHeadMt}>Маршрут</GroupHead>

          <Card t={t}>
            <div style={{ padding: '12px 8px 12px 12px' }}>
              <RouteStationRow t={t} station="Луга 1 п." arrival="—" departure="12:04" travelTo="1 ч 51 мин" active first/>
              <RouteStationRow t={t} station="Гатчина-Варшавская" arrival="13:55" departure="14:46" stopMin={51} travelTo="23 мин"/>
              <RouteStationRow t={t} station="Владимирская 4 п." arrival="15:09" departure="15:35" stopMin={26} travelTo="1 ч 38 мин"/>
              <RouteStationRow t={t} station="СПбСМ" arrival="17:13" departure="—" active last/>
            </div>
          </Card>

          {/* Метаданные пути */}
          <div style={{ marginTop: M.s.lg, display: 'flex', flexWrap: 'wrap', gap: M.s.sm }}>
            <Pill t={t} tone="muted" mono={false}>
              <span style={{ color: t.textMuted }}>Время в&nbsp;пути:</span>
              <span style={{ color: t.text, marginLeft: 4 }}>5&nbsp;ч&nbsp;9&nbsp;мин</span>
            </Pill>
            <Pill t={t} tone="muted" mono>
              <span style={{ color: t.textMuted }}>Уч</span>
              <span style={{ color: t.text, marginLeft: 4 }}>29,1&nbsp;км/ч</span>
            </Pill>
            <Pill t={t} tone="muted" mono>
              <span style={{ color: t.textMuted }}>Техн</span>
              <span style={{ color: t.text, marginLeft: 4 }}>38,8&nbsp;км/ч</span>
            </Pill>
          </div>
        </div>
      </div>

      {/* CTA */}
      {!sheet && (
        <React.Fragment>
          <ScrimBottom t={t}/>
          <div style={{
            position: 'absolute', bottom: 30, left: 16, right: 16, zIndex: 50,
          }}>
            <button style={{
              width: '100%', height: 52, borderRadius: 26,
              background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
              border: 'none', cursor: 'pointer',
              fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
              boxShadow: '0 12px 32px rgba(0,0,0,0.28)',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            }}>
              <IcPlus width="18" height="18"/>
              Добавить станцию
            </button>
          </div>
        </React.Fragment>
      )}

      {/* Шторки */}
      {sheet === 'shoulders' && <ShouldersSheet t={t}/>}
      {sheet === 'settings' && <SettingsSheet t={t} expanded={false}/>}
      {sheet === 'settings-expanded' && <SettingsSheet t={t} expanded={true}/>}
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════════
// ShoulderField — полное название плеча с × и chevron
// ════════════════════════════════════════════════════════════════════
function ShoulderField({ t, from, to, km }) {
  return (
    <Card t={t}>
      <button style={{
        width: '100%', minHeight: 56, padding: '10px 14px',
        border: 'none', background: 'transparent', cursor: 'pointer',
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{
          width: 32, height: 32, borderRadius: 16,
          background: t.accentSoft, color: t.accent,
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          <IcRails width="18" height="18"/>
        </div>
        <div style={{
          flex: 1, minWidth: 0,
          display: 'flex', flexDirection: 'column', gap: 2,
        }}>
          <div style={{ ...M.t.label(t), fontSize: 11 }}>Плечо</div>
          <div style={{
            fontFamily: M.fontSans, fontSize: 16, fontWeight: 600, color: t.text,
            letterSpacing: -0.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>{from} — {to}</div>
        </div>
        <span style={{
          fontFamily: M.fontMono, fontSize: 13, fontWeight: 500,
          color: t.textMuted, marginRight: 4,
        }}>{km}&nbsp;км</span>
        <span style={{
          width: 22, height: 22, borderRadius: 11,
          background: t.bgSubtle, color: t.textMuted,
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 14, lineHeight: 1, flexShrink: 0,
        }}>×</span>
      </button>
    </Card>
  );
}


// ════════════════════════════════════════════════════════════════════
// ShouldersSheet — список сохранённых плеч
// ════════════════════════════════════════════════════════════════════
function ShouldersSheet({ t }) {
  return (
    <React.Fragment>
      <div style={{
        position: 'absolute', inset: 0, zIndex: 70,
        background: 'rgba(0,0,0,0.45)',
      }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 71,
        background: t.bg,
        borderTopLeftRadius: M.r.sheetIOS, borderTopRightRadius: M.r.sheetIOS,
        boxShadow: M.shadow.sheet,
        maxHeight: '82%', display: 'flex', flexDirection: 'column',
        paddingBottom: 24,
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
          <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong || t.border }}/>
        </div>

        {/* Header */}
        <div style={{
          padding: '14px 20px 10px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{
            fontFamily: M.fontSans, fontSize: 22, fontWeight: 700,
            color: t.text, letterSpacing: -0.3,
          }}>Плечи</div>
          <button style={{
            width: 36, height: 36, borderRadius: 18, border: 'none',
            background: t.bgSubtle, color: t.accent, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          }} aria-label="Изменить">
            <IcEditPencil width="16" height="16"/>
          </button>
        </div>

        {/* Поиск */}
        <div style={{ padding: '4px 16px 10px' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '9px 12px', borderRadius: 12,
            background: t.bgSubtle, color: t.textFaint,
          }}>
            <IcSearch width="16" height="16"/>
            <span style={{ fontSize: 15 }}>Поиск плеча</span>
          </div>
        </div>

        {/* Список */}
        <div style={{ overflowY: 'auto', flex: 1, padding: '4px 0 8px' }}>
          {SAVED_SHOULDERS.map((s, i) => (
            <button key={i} style={{
              width: '100%', padding: '14px 20px',
              border: 'none', background: s.active ? t.accentSoft : 'transparent',
              cursor: 'pointer', textAlign: 'left',
              display: 'flex', alignItems: 'center', gap: 12,
              borderBottom: i < SAVED_SHOULDERS.length - 1 ? `0.5px solid ${t.border}` : 'none',
            }}>
              <div style={{
                flex: 1, minWidth: 0,
                fontFamily: M.fontSans, fontSize: 16, fontWeight: s.active ? 600 : 500,
                color: t.text, letterSpacing: -0.2,
                whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              }}>
                {s.from} — {s.to}
              </div>
              {s.active && (
                <IcCheck width="18" height="18" style={{ color: t.accent, flexShrink: 0 }}/>
              )}
              <span style={{
                fontFamily: M.fontMono, fontSize: 14, fontWeight: 500,
                color: t.textMuted, minWidth: 56, textAlign: 'right',
              }}>{s.km}&nbsp;км</span>
            </button>
          ))}
        </div>

        {/* CTA — добавить плечо */}
        <div style={{ padding: '8px 16px 0' }}>
          <button style={{
            width: '100%', padding: '12px',
            borderRadius: 14, background: t.bgSubtle, color: t.accent,
            border: 'none', fontSize: 15, fontWeight: 500, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            fontFamily: M.fontSans,
          }}>
            <IcPlus width="16" height="16"/>
            Новое плечо
          </button>
        </div>
      </div>
    </React.Fragment>
  );
}


// ════════════════════════════════════════════════════════════════════
// SettingsSheet — Толкач / Двойная тяга / Сдвоенный поезд
// ════════════════════════════════════════════════════════════════════
function SettingsSheet({ t, expanded }) {
  return (
    <React.Fragment>
      <div style={{
        position: 'absolute', inset: 0, zIndex: 70,
        background: 'rgba(0,0,0,0.45)',
      }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 71,
        background: t.bg,
        borderTopLeftRadius: M.r.sheetIOS, borderTopRightRadius: M.r.sheetIOS,
        boxShadow: M.shadow.sheet,
        maxHeight: '92%', display: 'flex', flexDirection: 'column',
        paddingBottom: 24,
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
          <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong || t.border }}/>
        </div>

        {/* Header */}
        <div style={{
          padding: '14px 20px 14px',
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        }}>
          <div style={{
            fontFamily: M.fontSans, fontSize: 22, fontWeight: 700,
            color: t.text, letterSpacing: -0.3,
          }}>Настройки поезда</div>
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 16, fontWeight: 500, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans,
          }}>Готово</button>
        </div>

        {/* Sections */}
        <div style={{ overflowY: 'auto', flex: 1, padding: '0 16px 16px' }}>
          <SettingsSection
            t={t}
            title="Толкач"
            hint="Локомотив сзади, помогающий составу на подъёме."
            expanded={expanded}
          />
          <SettingsSection
            t={t}
            title="Двойная тяга"
            hint="Второй локомотив в голове состава."
            expanded={expanded}
          />
          <SettingsSection
            t={t}
            title="Сдвоенный поезд"
            hint="Два состава соединены вместе."
            expanded={expanded}
            withPosition
          />
        </div>
      </div>
    </React.Fragment>
  );
}

function SettingsSection({ t, title, hint, expanded, withPosition }) {
  return (
    <div style={{ marginTop: 14 }}>
      {/* Заголовок секции */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '4px 4px 10px',
      }}>
        <div style={{
          fontFamily: M.fontSans, fontSize: 17, fontWeight: 600,
          color: t.text, letterSpacing: -0.2,
        }}>{title}</div>
        {expanded ? (
          <button style={{
            width: 28, height: 28, borderRadius: 14, border: 'none',
            background: t.bgSubtle, color: t.textMuted, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 16,
          }} aria-label="Свернуть">×</button>
        ) : (
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 15, fontWeight: 500, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans, display: 'inline-flex', alignItems: 'center', gap: 4,
          }}>
            <IcPlus width="14" height="14"/>
            Добавить
          </button>
        )}
      </div>

      {/* Содержимое */}
      {expanded ? (
        <Card t={t}>
          {withPosition && (
            <React.Fragment>
              <div style={{ padding: '12px 12px 8px' }}>
                <SegmentedToggle t={t} options={['Я первый', 'Я второй']} active={0}/>
              </div>
              <Sep t={t} inset={12}/>
            </React.Fragment>
          )}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
            <div style={{ padding: '12px 12px 10px' }}>
              <div style={{ ...M.t.label(t), fontSize: 11, marginBottom: 6 }}>Серия</div>
              <FieldInput t={t} size="sm" mono value="" placeholder="ВЛ80с"/>
            </div>
            <div style={{ padding: '12px 12px 10px' }}>
              <div style={{ ...M.t.label(t), fontSize: 11, marginBottom: 6 }}>Номер</div>
              <FieldInput t={t} size="sm" mono value="" placeholder="1234"/>
            </div>
          </div>
          <Sep t={t} inset={12}/>
          <div style={{ padding: '12px 12px 10px' }}>
            <div style={{ ...M.t.label(t), fontSize: 11, marginBottom: 6 }}>Машинист</div>
            <FieldInput t={t} size="sm" mono={false} value="" placeholder="Иванов И. И."/>
          </div>
          <Sep t={t} inset={12}/>
          <div style={{ padding: '12px 12px 12px' }}>
            <div style={{ ...M.t.label(t), fontSize: 11, marginBottom: 6 }}>Примечание</div>
            <FieldInput t={t} size="sm" mono={false} value="" placeholder="—"/>
          </div>
        </Card>
      ) : (
        <div style={{ ...M.t.captionMuted(t), padding: '0 4px 14px', fontSize: 12 }}>{hint}</div>
      )}

      <Sep t={t}/>
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// SegmentedToggle — iOS-style сегментированный контрол
// ════════════════════════════════════════════════════════════════════
function SegmentedToggle({ t, options, active = 0 }) {
  return (
    <div style={{
      display: 'grid', gridTemplateColumns: `repeat(${options.length}, 1fr)`,
      gap: 2, padding: 2, borderRadius: 10,
      background: t.bgSubtle,
    }}>
      {options.map((label, i) => (
        <div key={i} style={{
          padding: '8px 10px', textAlign: 'center',
          borderRadius: 8,
          background: i === active ? (t.surface || t.bg) : 'transparent',
          boxShadow: i === active ? '0 1px 3px rgba(0,0,0,0.08), 0 0 0 0.5px rgba(0,0,0,0.04)' : 'none',
          fontFamily: M.fontSans, fontSize: 14,
          fontWeight: i === active ? 600 : 500,
          color: t.text,
          cursor: 'pointer',
        }}>{label}</div>
      ))}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// IcEditPencil — карандаш для edit-кнопки
// ════════════════════════════════════════════════════════════════════
function IcEditPencil(p) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M14 4l6 6L9 21H3v-6L14 4z"/>
      <path d="M13 5l6 6"/>
    </svg>
  );
}


// ─────────────────────────────────────────────────────────────
// RouteStationRow — строка станции в timeline (Добавление поезда)
// ─────────────────────────────────────────────────────────────
// Имя отличается от norms-screens.jsx::StationRow намеренно: оба
// файла грузятся как text/babel в общий глобальный scope, и одинаковые
// имена молча перекрывают друг друга.
function RouteStationRow({ t, station, arrival, departure, stopTime, stopMin, stopTone = 'muted', travelTo, active = false, first = false, last = false }) {
  // ─────────────────────────────────────────────────────────────
  // Структура:
  //   ● Станция          13:55  [51']  14:46
  //   │
  //   │   [ 23 мин ]
  //   │
  //   ● След. станция    …
  //
  // stopMin → ≤30 мин — жёлтый, >30 — красный.
  // ─────────────────────────────────────────────────────────────
  const TIMELINE_COL = 32;
  const DOT_SIZE = 14;
  const TIME_COL_W = 48;     // ширина колонки времени (HH:MM)
  const BADGE_COL_W = 44;    // ширина колонки стоп-бэйджа (фиксированная)

  let badgeLabel = null;
  let badgeBg = null;
  if (typeof stopMin === 'number') {
    badgeLabel = `${stopMin}'`;
    badgeBg = stopMin > 30 ? t.danger : t.warning;
  } else if (stopTime) {
    badgeLabel = stopTime;
    badgeBg = stopTone === 'danger' ? t.danger
            : stopTone === 'warning' ? t.warning
            : null;
  }

  return (
    <div style={{ position: 'relative', display: 'flex' }}>
      {/* Колонка таймлайна — dot + dashed line (full height row) */}
      <div style={{
        position: 'relative', width: TIMELINE_COL, flexShrink: 0,
      }}>
        {/* Пунктирная линия */}
        <div style={{
          position: 'absolute',
          left: TIMELINE_COL / 2 - 0.75,
          top: first ? 17 : 0,
          bottom: last ? 'auto' : 0,
          height: last ? 17 : undefined,
          width: 0,
          borderLeft: `1.5px dashed ${t.borderStrong}`,
        }}/>
        {/* Точка — на уровне центра первой строки */}
        <div style={{
          position: 'absolute',
          left: (TIMELINE_COL - DOT_SIZE) / 2,
          top: 17 - DOT_SIZE / 2,
          width: DOT_SIZE, height: DOT_SIZE, borderRadius: DOT_SIZE / 2,
          background: active ? t.text : t.surface,
          border: active ? 'none' : `2px solid ${t.borderStrong}`,
          zIndex: 1,
        }}/>
      </div>

      {/* Контент строки */}
      <div style={{
        flex: 1, minWidth: 0,
        padding: '6px 16px 6px 4px',
      }}>
        {/* Строка с названием и временами */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, minHeight: 22 }}>
          <div style={{
            flex: 1,
            fontFamily: M.fontSans, fontSize: 14, fontWeight: 500,
            color: t.text, lineHeight: 1.2,
            letterSpacing: -0.1,
          }}>{station}</div>
          {/* Правый блок: время-прибытия | бэйдж | время-отправления.
             Все три колонки имеют ФИКСИРОВАННУЮ ширину, поэтому
             одинаковые времена выстраиваются ровно друг под другом
             во всех строках таблицы маршрута. */}
          <div style={{
            display: 'inline-flex', alignItems: 'center',
            fontFamily: M.fontMono,
          }}>
            <div style={{
              width: TIME_COL_W, textAlign: 'right',
              fontSize: 15, fontWeight: 500,
              color: arrival === '—' ? t.textFaint : t.text,
            }}>{arrival}</div>
            <div style={{
              width: BADGE_COL_W,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            }}>
              {badgeLabel ? (
                badgeBg ? (
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    padding: '2px 7px', minWidth: 30, borderRadius: 6,
                    background: badgeBg, color: '#FFFFFF',
                    fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.2,
                  }}>{badgeLabel}</span>
                ) : (
                  <Pill t={t} tone="muted" mono style={{
                    padding: '2px 7px', fontSize: 11, minWidth: 28, justifyContent: 'center',
                  }}>{badgeLabel}</Pill>
                )
              ) : null}
            </div>
            <div style={{
              width: TIME_COL_W, textAlign: 'right',
              fontSize: 15, fontWeight: 500,
              color: departure === '—' ? t.textFaint : t.text,
            }}>{departure}</div>
          </div>
        </div>

        {/* travelTo — время в пути до следующей станции */}
        {travelTo && !last && (
          <div style={{ padding: '8px 0 10px' }}>
            <span style={{
              display: 'inline-block',
              padding: '3px 9px', borderRadius: 10,
              background: t.bg, color: t.textMuted,
              border: `1px solid ${t.borderStrong}`,
              fontFamily: M.fontSans, fontSize: 12, fontWeight: 500,
            }}>{travelTo}</span>
          </div>
        )}
      </div>
    </div>
  );
}

Object.assign(window, { IOSScreenAddTrain });
