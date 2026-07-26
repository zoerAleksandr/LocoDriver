// Экран "Локомотив" — вариант 2-hybrid (iOS + Android)
// v3: каждая секция — отдельная карточка; Расход и Рекуперация в одном стиле;
// добавлен блок "Итого" с нормой и результатом (экономия / перерасход).
// Для электровоза убран k секции; коэффициент остался только в тепловозе.

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

function GroupHead({ t, children, trailing, mt = 22 }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 4px 8px', marginTop: mt,
    }}>
      <div style={{
        fontFamily: M.fontMono, fontSize: 11, color: t.textMuted,
        letterSpacing: 1.4, textTransform: 'uppercase',
      }}>{children}</div>
      {trailing}
    </div>
  );
}

function TractionPill({ t, isElectric, mat }) {
  return (
    <button style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '5px 10px 5px 8px', borderRadius: 999,
      background: mat ? t.surface : t.bgSubtle,
      border: mat ? `1px solid ${t.border}` : 'none',
      cursor: 'pointer', color: t.text, fontSize: 13, fontWeight: mat ? 500 : 600,
    }}>
      <span style={{
        width: 18, height: 18, borderRadius: 9,
        background: isElectric ? t.accentSoft : hexToRgba(t.rail || '#5B8FB9', .18),
        color: isElectric ? t.accent : (t.rail || '#5B8FB9'),
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      }}>
        {isElectric ? <IcFlash width="11" height="11"/> : <IcDroplet width="11" height="11"/>}
      </span>
      {isElectric ? 'Электровоз' : 'Тепловоз'}
    </button>
  );
}

// Time summary row — открывает шторку.
function TimeSummaryRow({ t, label, kind, mat }) {
  const stops = kind === 'arrival'
    ? [
        { time: '08:20', cap: 'начало' },
        { time: '09:00', cap: 'конец' },
        { time: '09:05', cap: 'КП' },
      ]
    : [
        { time: '19:50', cap: 'КП' },
        { time: '20:00', cap: 'начало' },
        { time: '20:50', cap: 'конец' },
      ];

  return (
    <button style={{
      width: '100%', padding: '12px 20px 14px',
      display: 'flex', flexDirection: 'column', alignItems: 'stretch', gap: 10,
      background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
    }}>
      {/* Верхняя строка: лейбл + chevron */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}>
        <span style={{
          fontSize: 12, color: t.textMuted, fontFamily: M.fontMono,
          textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600,
        }}>{label}</span>
        <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
      </div>

      {/* Нижняя строка: 3 значения времени на всю ширину */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr auto 1fr',
        alignItems: 'center',
      }}>
        {stops.map((stop, i) => (
          <React.Fragment key={i}>
            {i > 0 && (
              <span style={{
                color: t.textFaint, fontFamily: M.fontMono, fontSize: 14,
                textAlign: 'center', padding: '0 8px',
                alignSelf: 'flex-start', marginTop: 4,
              }}>→</span>
            )}
            <div style={{
              display: 'inline-flex', flexDirection: 'column', alignItems: 'center', gap: 3,
            }}>
              <span style={{
                fontFamily: M.fontMono, fontSize: 18, fontWeight: 600, color: t.text,
                lineHeight: 1, fontVariantNumeric: 'tabular-nums',
              }}>{stop.time}</span>
              <span style={{
                fontFamily: M.fontSans, fontSize: 9, fontWeight: 600,
                letterSpacing: .8, color: t.textMuted,
                textTransform: 'uppercase', lineHeight: 1,
              }}>{stop.cap}</span>
            </div>
          </React.Fragment>
        ))}
      </div>
    </button>
  );
}

// ─────────────────────────────────────────────────────────────
// Reading row — счётчики Отопление / Собств. нужды
// ─────────────────────────────────────────────────────────────
function ReadingRowLong({ t, icon, label, unit, a, b, mat }) {
  return (
    <div style={{ padding: '14px 20px 14px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 12 }}>
        <span style={{
          display: 'inline-flex', alignItems: 'center', gap: 8,
          fontSize: 15, fontWeight: mat ? 500 : 600, color: t.text,
        }}>
          <span style={{
            width: 22, height: 22, borderRadius: 11,
            background: t.bgSubtle, color: t.text, opacity: .85,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            {React.cloneElement(icon, { width: 13, height: 13 })}
          </span>
          {label}
        </span>
        {unit && (
          <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 600, color: t.accent }}>{unit}</span>
        )}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 12px 1fr', gap: 8, alignItems: 'center' }}>
        <SectionInput t={t} placeholder="Принял" value={a} mat={mat}/>
        <div style={{ color: t.textFaint, textAlign: 'center', fontSize: 14 }}>→</div>
        <SectionInput t={t} placeholder="Сдал" value={b} mat={mat}/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Унифицированный «двойной счётчик» — Принял → Сдал
// (используется для Расхода, Рекуперации, чтобы стиль был общим)
// ─────────────────────────────────────────────────────────────
function MeterBlock({ t, label, unit, a, b, mat, onSurface, hideUnit, action }) {
  return (
    <div>
      <div style={{
        display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        marginBottom: 10, gap: 8,
      }}>
        <span style={{
          fontSize: 12, color: t.textMuted, fontFamily: M.fontMono,
          textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600,
        }}>{label}</span>
        {action ? action : (!hideUnit && (
          <span style={{
            fontFamily: M.fontMono, fontSize: 13, fontWeight: 600, color: t.accent,
          }}>{unit}</span>
        ))}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 12px 1fr', gap: 8, alignItems: 'center' }}>
        <SectionInput t={t} placeholder="Принял" value={a} mat={mat} onSurface={onSurface}/>
        <div style={{ color: t.textFaint, textAlign: 'center', fontSize: 14 }}>→</div>
        <SectionInput t={t} placeholder="Сдал" value={b} mat={mat} onSurface={onSurface}/>
      </div>
    </div>
  );
}

// Сегментированный переключатель единиц — для приёмки/сдачи топлива (л ↔ кг).
function UnitSeg({ t, options = ['л', 'кг'], value = 'л' }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center',
      background: t.bgSubtle, borderRadius: 999, padding: 2,
      fontFamily: M.fontMono, fontSize: 11, fontWeight: 700,
      textTransform: 'uppercase', letterSpacing: 1,
    }}>
      {options.map((opt) => {
        const active = opt === value;
        return (
          <button key={opt} style={{
            border: 'none', cursor: 'pointer',
            padding: '3px 10px', borderRadius: 999,
            background: active ? (t.surface || t.bg) : 'transparent',
            color: active ? t.text : t.textMuted,
            boxShadow: active ? '0 1px 2px rgba(0,0,0,.08)' : 'none',
            fontFamily: 'inherit', fontSize: 'inherit', fontWeight: 'inherit',
            textTransform: 'inherit', letterSpacing: 'inherit',
          }}>{opt}</button>
        );
      })}
    </div>
  );
}

// Пилюля коэффициента (k секции / k экипировки) — общий стиль.
function KPill({ t, label, value }) {
  return (
    <button style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '4px 10px', borderRadius: 999,
      border: `1px solid ${t.border}`, background: 'transparent', color: t.text,
      fontFamily: M.fontMono, fontSize: 12, fontWeight: 600, cursor: 'pointer',
      flexShrink: 0,
    }}>
      <span style={{ color: t.textMuted }}>{label}</span>
      <span style={{ color: t.text }}>{value}</span>
      <IcChevronRight width="10" height="10" style={{ color: t.textFaint }}/>
    </button>
  );
}

// Простой текстовый link-button (для «+ Рекуперация» / «+ Экипировка»)
function TextAction({ t, label }) {
  return (
    <button style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      padding: '2px 0', background: 'transparent', border: 'none',
      color: t.accent, fontSize: 13, fontWeight: 500,
      cursor: 'pointer', fontFamily: M.fontSans,
    }}>
      + {label}
    </button>
  );
}

// Свёрнутая строка «+ Экипировка» / «+ Рекуперация» — просто синий текст
function CollapsedRow({ t, label, mat }) {
  return (
    <div style={{ padding: '6px 0 0' }}>
      <TextAction t={t} label={label}/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SectionCard — одна секция локомотива (отдельная карточка)
// ─────────────────────────────────────────────────────────────
function SectionCard({
  t, isElectric, sectionNo = 1, mat,
  expandRecup = false, expandSupply = false,
  // данные
  consumedA, consumedB, recupA = '', recupB = '',
}) {
  const labelWeight = mat ? 500 : 600;

  // подсчитанные итоги
  const consumed = isElectric
    ? (consumedA && consumedB ? Number(consumedA) - Number(consumedB) : null)
    : null;
  const recup = isElectric && recupA && recupB ? Number(recupB) - Number(recupA) : null;

  return (
    <div style={{ padding: '14px 0 0' }}>
      {/* Header: «Секция N» текстом + в правом углу: «+ Рекуперация» / k секции */}
      <div style={{
        padding: '0 20px 12px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}>
        <span style={{
          fontSize: 16, fontWeight: mat ? 600 : 700, color: t.text,
        }}>
          Секция {sectionNo}
        </span>

        {isElectric && !expandRecup && (
          <TextAction t={t} label="Рекуперация"/>
        )}

        {!isElectric && (
          <KPill t={t} label="k секции" value="0.83"/>
        )}
      </div>

      {/* Расход — без пунктира между шапкой и блоком */}
      <div style={{ padding: '0 20px 0' }}>
        <MeterBlock
          t={t} mat={mat} onSurface hideUnit
          label={isElectric ? 'Расход' : 'Топливо'}
          unit={isElectric ? 'кВт·ч' : 'литры'}
          a={isElectric ? (consumedA ?? '15820') : '4200'}
          b={isElectric ? (consumedB ?? '14260') : ''}
          action={!isElectric ? <UnitSeg t={t} value="л"/> : undefined}
        />
      </div>

      {/* Рекуперация только когда развёрнута (свёрнутая — в кнопке шапки) */}
      {isElectric && expandRecup && (
        <div style={{ padding: '12px 20px 0' }}>
          <div style={{
            paddingTop: 12,
            borderTop: `1px dashed ${t.borderStrong || t.border}`,
          }}>
            <MeterBlock
              t={t} mat={mat} onSurface hideUnit
              label="Рекуперация"
              unit="кВт·ч"
              a={recupA}
              b={recupB}
            />
          </div>
        </div>
      )}

      {/* Тепловоз — Экипировка */}
      {!isElectric && (
        <div style={{ padding: '12px 20px 0' }}>
          {expandSupply ? (
            <div style={{
              paddingTop: 12,
              borderTop: `1px dashed ${t.borderStrong || t.border}`,
            }}>
              {/* Заголовок «Экипировка» + пилюля k экипировки в стиле k секции */}
              <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                marginBottom: 10, gap: 8,
              }}>
                <span style={{
                  fontSize: 12, color: t.textMuted, fontFamily: M.fontMono,
                  textTransform: 'uppercase', letterSpacing: 1, fontWeight: 600,
                }}>Экипировка</span>
                <KPill t={t} label="k экипировки" value="0.91"/>
              </div>

              {/* Объём (л) + Масса (кг) — два поля бок-о-бок */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, alignItems: 'center' }}>
                <SectionInput t={t} placeholder="Объём, л" value="" mat={mat} onSurface/>
                <SectionInput t={t} placeholder="Масса, кг" value="" mat={mat} onSurface/>
              </div>
            </div>
          ) : (
            <CollapsedRow t={t} mat={mat} label="Экипировка"/>
          )}
        </div>
      )}

      {/* Итог секции — на том же фоне что и секция, отделён пунктиром-корешком */}
      <div style={{
        marginTop: 14,
        borderTop: `1px dashed ${t.borderStrong || t.border}`,
        padding: '12px 20px 14px',
      }}>
        <div style={{
          display: 'flex', flexDirection: 'column', gap: 6,
        }}>
          <SummaryRow
            t={t}
            label={isElectric ? 'Расход' : 'Расход'}
            value={isElectric ? '1 560' : '120'}
            unit={isElectric ? 'кВт·ч' : 'л'}
          />
          {isElectric && expandRecup && (
            <SummaryRow t={t} label="Рекуперация" value="210" unit="кВт·ч" tone="positive"/>
          )}
          {!isElectric && (
            <SummaryRow t={t} label="" value="99" unit="кг" muted/>
          )}
        </div>
      </div>
    </div>
  );
}

// SummaryRow — итог-строка одного стиля
function SummaryRow({ t, label, value, unit, tone, muted }) {
  const color = tone === 'positive' ? t.success : (muted ? t.textMuted : t.text);
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      fontFamily: M.fontMono,
    }}>
      <span style={{ fontSize: 12, color: t.textMuted, fontWeight: 500 }}>
        {label || <span style={{ visibility: 'hidden' }}>·</span>}
      </span>
      <span>
        <span style={{ fontSize: 15, fontWeight: 700, color }}>
          {tone === 'positive' ? `−${value}` : value}
        </span>
        <span style={{ fontSize: 12, color: t.textMuted, marginLeft: 4 }}>{unit}</span>
      </span>
    </div>
  );
}

// Простой icon для рекуперации
function IcRecup({ width = 14, height = 14, style }) {
  return (
    <svg width={width} height={height} viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={style}>
      <path d="M3 12a9 9 0 1 0 3-6.7"/>
      <path d="M3 4v5h5"/>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// TotalsBlock — общий итог по всем секциям + норма + результат
// ─────────────────────────────────────────────────────────────
function TotalsBlock({ t, mat, isElectric }) {
  // Демо-данные для статичного макета
  const totalConsumed = isElectric ? 3120 : 240;     // сумма по секциям
  const totalRecup    = isElectric ? 410 : null;
  const netConsumed   = isElectric ? totalConsumed - totalRecup : totalConsumed;
  const totalKg       = !isElectric ? 198 : null;    // 240 л · ρ
  const norm          = isElectric ? 2900 : 220;     // введённая пользователем
  const delta         = netConsumed - norm;          // + перерасход / − экономия
  const deltaPct      = Math.round((delta / norm) * 100);
  const isOver        = delta > 0;
  const unit          = isElectric ? 'кВт·ч' : 'л';

  return (
    <div style={{
      borderRadius: 16, overflow: 'hidden',
      background: t.surface || '#FFFFFF',
      border: mat ? `1px solid ${t.border}` : 'none',
    }}>
      {/* Большое число итога */}
      <div style={{ padding: '18px 20px 14px' }}>
        <div style={{
          fontFamily: M.fontMono, fontSize: 11, fontWeight: 600,
          color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase',
          marginBottom: 8,
        }}>
          {isElectric ? 'Чистый расход' : 'Общий расход'}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
          <span style={{
            fontFamily: M.fontMono, fontSize: 32, fontWeight: 700, color: t.text, lineHeight: 1,
          }}>
            {netConsumed.toLocaleString('ru-RU')}
          </span>
          <span style={{ fontFamily: M.fontMono, fontSize: 15, color: t.textMuted }}>{unit}</span>
          {!isElectric && totalKg && (
            <span style={{
              marginLeft: 6, padding: '3px 8px', borderRadius: 999,
              background: t.bgSubtle, fontFamily: M.fontMono, fontSize: 13,
              fontWeight: 600, color: t.text,
            }}>
              {totalKg} кг
            </span>
          )}
        </div>

        {/* Раскладка: сумма по секциям + рекуперация */}
        <div style={{
          marginTop: 12, display: 'flex', flexDirection: 'column', gap: 6,
        }}>
          <BreakdownLine t={t} label={isElectric ? 'Расход всех секций' : 'Заправка всех секций'} value={totalConsumed.toLocaleString('ru-RU')} unit={unit}/>
          {isElectric && totalRecup != null && (
            <BreakdownLine t={t} label="Рекуперация всех секций" value={`−${totalRecup}`} unit={unit} tone="positive"/>
          )}
        </div>
      </div>

      {/* Норма + результат */}
      <div style={{ borderTop: `1px solid ${t.border}` }}/>

      <div style={{ padding: '14px 20px' }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginBottom: 10, gap: 10,
        }}>
          <span style={{
            fontFamily: M.fontMono, fontSize: 11, fontWeight: 600,
            color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase',
          }}>Норма</span>
          <NormPill t={t} value={norm.toLocaleString('ru-RU')} unit={unit}/>
        </div>

        {/* Результат — экономия / перерасход */}
        <div style={{
          padding: '12px 14px', borderRadius: 12,
          background: isOver ? hexToRgba(t.danger, .10) : hexToRgba(t.success, .12),
          border: `1px solid ${isOver ? hexToRgba(t.danger, .25) : hexToRgba(t.success, .25)}`,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{
            width: 32, height: 32, borderRadius: 16,
            background: isOver ? hexToRgba(t.danger, .18) : hexToRgba(t.success, .22),
            color: isOver ? t.danger : t.success,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
              {isOver
                ? <React.Fragment><path d="M7 17l10-10"/><path d="M9 7h8v8"/></React.Fragment>
                : <React.Fragment><path d="M7 7l10 10"/><path d="M9 17h8V9"/></React.Fragment>}
            </svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              fontSize: 13, fontWeight: 600, color: isOver ? t.danger : t.success,
              textTransform: 'uppercase', fontFamily: M.fontMono, letterSpacing: 1,
            }}>
              {isOver ? 'Перерасход' : 'Экономия'}
            </div>
            <div style={{
              fontFamily: M.fontMono, fontSize: 20, fontWeight: 700, color: t.text,
              marginTop: 2, lineHeight: 1.1,
            }}>
              {isOver ? '+' : '−'}{Math.abs(delta).toLocaleString('ru-RU')}
              <span style={{ fontSize: 13, color: t.textMuted, marginLeft: 4, fontWeight: 500 }}>{unit}</span>
              <span style={{
                marginLeft: 8, fontSize: 12, color: t.textMuted, fontWeight: 500,
              }}>
                ({isOver ? '+' : '−'}{Math.abs(deltaPct)}%)
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function BreakdownLine({ t, label, value, unit, tone }) {
  const color = tone === 'positive' ? t.success : t.text;
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      fontFamily: M.fontMono,
    }}>
      <span style={{ fontSize: 12, color: t.textMuted }}>{label}</span>
      <span>
        <span style={{ fontSize: 14, fontWeight: 600, color }}>{value}</span>
        <span style={{ fontSize: 12, color: t.textMuted, marginLeft: 4 }}>{unit}</span>
      </span>
    </div>
  );
}

function NormPill({ t, value, unit }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'stretch', gap: 0,
      borderRadius: 10, overflow: 'hidden',
      border: `1px solid ${t.border}`,
      background: t.surface || t.bg,
    }}>
      <input
        type="text"
        defaultValue={value}
        style={{
          width: 90, padding: '8px 10px',
          background: 'transparent', border: 'none', outline: 'none',
          fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, color: t.text,
          textAlign: 'right',
        }}
      />
      <span style={{
        padding: '8px 12px 8px 0',
        display: 'inline-flex', alignItems: 'center',
        fontFamily: M.fontMono, fontSize: 13, color: t.textMuted,
        background: 'transparent',
      }}>{unit}</span>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// iOS
// ─────────────────────────────────────────────────────────────
function IOSScreenLocomotiveV2({ dark = false, traction = 'electric', height = 1240, sheet = null, supplyExpanded = true }) {
  const t = dark ? M.dark : M.light;
  const isElectric = traction === 'electric';
  const series = isElectric ? 'ВЛ80с' : '2ТЭ10М';
  const number = isElectric ? '1234' : '0287';
  const sectionsCount = isElectric ? 2 : 1;

  return (
    <MDevice dark={dark} height={height}>
      {/* Navigation */}
      <div style={{ position: 'relative', paddingTop: 58 }}>
        <div style={{
          padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronLeft width="18" height="18"/></button>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>Локомотив</div>
          <button style={pillBtn(t, { px: 14, h: 40, accent: true })}>Сохранить</button>
        </div>
      </div>

      <div style={{ padding: '8px 16px 140px', overflowY: 'auto', height: 'calc(100% - 110px)' }}>
        {/* Основные данные + pill */}
        <GroupHead t={t} mt={4} trailing={<TractionPill t={t} isElectric={isElectric}/>}>
          Основные данные
        </GroupHead>
        <Card t={t}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
            <FieldCell t={t} label="Серия" value={series} mono/>
            <div style={{ borderLeft: `1px solid ${t.border}` }}>
              <FieldCell t={t} label="Номер" value={number} mono/>
            </div>
          </div>
        </Card>

        {/* Время */}
        <GroupHead t={t}>Время</GroupHead>
        <Card t={t}>
          <TimeSummaryRow t={t} label="Приёмка" kind="arrival"/>
          <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
          <TimeSummaryRow t={t} label="Сдача" kind="departure"/>
        </Card>

        {/* Секций — каждая отдельной карточкой */}
        <GroupHead t={t} trailing={<Stepper t={t} value={sectionsCount}/>}>
          Секций
        </GroupHead>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {Array.from({ length: sectionsCount }).map((_, i) => (
            <Card key={i} t={t}>
              <SectionCard
                t={t}
                isElectric={isElectric}
                sectionNo={i + 1}
                expandRecup={isElectric && i === 0}
                expandSupply={!isElectric && i === 0 && supplyExpanded}
              />
            </Card>
          ))}
        </div>

        {/* Показания */}
        <GroupHead t={t}>Показания</GroupHead>
        <Card t={t}>
          <ReadingRowLong t={t} icon={<IcHome/>} label="Отопление" unit="кВт·ч" a={isElectric ? '1283456' : ''} b={isElectric ? '1283398' : ''}/>
          <div style={{ height: 1, background: t.border }}/>
          <ReadingRowLong t={t} icon={<IcFlash/>} label="Собств. нужды" unit="кВт·ч" a="" b=""/>
        </Card>

        {/* Итого — общий расход + норма + результат */}
        <GroupHead t={t}>Итого</GroupHead>
        <TotalsBlock t={t} isElectric={isElectric}/>

        {/* Настроить разделы */}
        <button style={{
          display: 'block', margin: '24px auto 8px',
          padding: '8px 14px', background: 'transparent', border: 'none',
          color: t.accent, fontSize: 14, fontWeight: 500, cursor: 'pointer',
          fontFamily: M.fontSans,
        }}>
          Настроить разделы →
        </button>
      </div>
      {sheet && <CoeffSheet dark={dark} kind={sheet.kind} value={sheet.value}/>}
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Android
// ─────────────────────────────────────────────────────────────
function AndroidScreenLocomotiveV2({ dark = false, traction = 'electric', height = 1240, sheet = null, supplyExpanded = true }) {
  const t = dark ? M.dark : M.light;
  const isElectric = traction === 'electric';
  const series = isElectric ? 'ВЛ80с' : '2ТЭ10М';
  const number = isElectric ? '1234' : '0287';
  const sectionsCount = isElectric ? 2 : 1;

  return (
    <ADevice dark={dark} height={height}>
      {/* Top app bar */}
      <div style={{
        padding: '8px 4px 8px 4px', display: 'flex', alignItems: 'center', gap: 4, background: t.bg,
      }}>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <IcChevronLeft width="22" height="22"/>
        </button>
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>Локомотив</div>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <svg width="4" height="18" viewBox="0 0 4 18" fill="currentColor">
            <circle cx="2" cy="2" r="2"/><circle cx="2" cy="9" r="2"/><circle cx="2" cy="16" r="2"/>
          </svg>
        </button>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '8px 16px 24px' }}>
        {/* Основные данные + pill */}
        <GroupHead t={t} mt={4} trailing={<TractionPill t={t} isElectric={isElectric} mat/>}>
          Основные данные
        </GroupHead>
        <MCard t={t}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
            <AFieldCell t={t} label="Серия" value={series}/>
            <div style={{ borderLeft: `1px solid ${t.border}` }}>
              <AFieldCell t={t} label="Номер" value={number}/>
            </div>
          </div>
        </MCard>

        {/* Время */}
        <GroupHead t={t}>Время</GroupHead>
        <MCard t={t}>
          <TimeSummaryRow t={t} label="Приёмка" kind="arrival" mat/>
          <div style={{ height: 1, background: t.border }}/>
          <TimeSummaryRow t={t} label="Сдача" kind="departure" mat/>
        </MCard>

        {/* Секций — каждая отдельной карточкой */}
        <GroupHead t={t} trailing={<Stepper t={t} value={sectionsCount} mat/>}>
          Секций
        </GroupHead>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {Array.from({ length: sectionsCount }).map((_, i) => (
            <MCard key={i} t={t}>
              <SectionCard
                t={t} mat
                isElectric={isElectric}
                sectionNo={i + 1}
                expandRecup={isElectric && i === 0}
                expandSupply={!isElectric && i === 0 && supplyExpanded}
              />
            </MCard>
          ))}
        </div>

        {/* Показания */}
        <GroupHead t={t}>Показания</GroupHead>
        <MCard t={t}>
          <ReadingRowLong t={t} icon={<IcHome/>} label="Отопление" unit="кВт·ч" a={isElectric ? '1283456' : ''} b={isElectric ? '1283398' : ''} mat/>
          <div style={{ height: 1, background: t.border }}/>
          <ReadingRowLong t={t} icon={<IcFlash/>} label="Собств. нужды" unit="кВт·ч" a="" b="" mat/>
        </MCard>

        {/* Итого */}
        <GroupHead t={t}>Итого</GroupHead>
        <TotalsBlock t={t} mat isElectric={isElectric}/>

        {/* Настроить разделы */}
        <button style={{
          display: 'block', margin: '24px auto 8px',
          padding: '8px 14px', background: 'transparent', border: 'none',
          color: t.accent, fontSize: 14, fontWeight: 500, cursor: 'pointer',
          fontFamily: M.fontSans,
        }}>
          Настроить разделы →
        </button>
      </div>
      {sheet && <CoeffSheet dark={dark} mat kind={sheet.kind} value={sheet.value}/>}
    </ADevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Stepper
// ─────────────────────────────────────────────────────────────
function Stepper({ t, value = 1, mat }) {
  const btn = {
    width: 32, height: 32, borderRadius: 16,
    background: mat ? t.surface : t.bgSubtle,
    border: mat ? `1px solid ${t.border}` : 'none',
    color: t.text, cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  };
  const accentBtn = {
    ...btn,
    background: mat ? t.surface : t.accent,
    color: mat ? t.accent : t.accentInk,
  };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <button style={btn} aria-label="Минус">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
          <path d="M5 12h14"/>
        </svg>
      </button>
      <span style={{
        fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text,
        minWidth: 16, textAlign: 'center',
      }}>{value}</span>
      <button style={accentBtn} aria-label="Плюс">
        <IcPlus width="14" height="14"/>
      </button>
    </div>
  );
}

// Section input — placeholder когда пусто
function SectionInput({ t, placeholder, value, mat }) {
  // Единый стиль для всех инпутов: прозрачный фон + тонкий контур.
  const empty = !value;
  return (
    <div style={{
      background: 'transparent',
      border: `1px solid ${t.border}`,
      borderRadius: 12, padding: '10px 14px',
      minHeight: 44, display: 'flex', alignItems: 'center',
    }}>
      <span style={{
        fontFamily: M.fontMono, fontSize: 17, fontWeight: 600,
        color: empty ? t.textFaint : t.text,
      }}>
        {empty ? placeholder : value}
      </span>
    </div>
  );
}

// Field cell — для серии/номера
function FieldCell({ t, label, value, mono }) {
  return (
    <div style={{ padding: '14px 18px' }}>
      <div style={{ fontSize: 12, color: t.textMuted, marginBottom: 6 }}>{label}</div>
      <div style={{
        fontSize: 17, fontWeight: 600, color: t.text,
        fontFamily: mono ? M.fontMono : M.fontSans,
      }}>{value}</div>
    </div>
  );
}
function AFieldCell({ t, label, value }) {
  return (
    <div style={{ padding: '14px 18px' }}>
      <div style={{ fontSize: 12, color: t.textMuted, marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 16, fontWeight: 500, color: t.text, fontFamily: M.fontMono }}>{value}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// BottomSheet — для редактирования коэффициента (k секции / k экипировки)
// ─────────────────────────────────────────────────────────────
function CoeffSheet({ dark = false, mat = false, kind = 'section', value = '0.83' }) {
  const t = dark ? M.dark : M.light;
  const title = kind === 'section' ? 'Коэффициент секции' : 'Коэффициент экипировки';
  const hint  = kind === 'section'
    ? 'Применяется ко всем расчётам секции'
    : 'Может отличаться от k секции';
  const presets = ['0.79', '0.83', '0.87', '0.91', '0.95'];

  return (
    <React.Fragment>
      {/* Backdrop */}
      <div style={{
        position: 'absolute', inset: 0, zIndex: 70,
        background: 'rgba(0,0,0,0.45)',
      }}/>
      {/* Sheet */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 71,
        background: t.surface || t.bg,
        borderTopLeftRadius: mat ? 28 : 18,
        borderTopRightRadius: mat ? 28 : 18,
        padding: '12px 20px 32px',
        boxShadow: '0 -8px 24px rgba(0,0,0,0.18)',
      }}>
        {/* grabber */}
        <div style={{
          width: 36, height: 4, borderRadius: 4,
          background: t.borderStrong || t.border,
          margin: '0 auto 14px',
        }}/>

        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{title}</span>
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 15, fontWeight: 500, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans,
          }}>Готово</button>
        </div>
        <div style={{ fontSize: 13, color: t.textMuted, marginBottom: 16 }}>{hint}</div>

        {/* Большое числовое поле */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12,
          padding: '20px 12px',
          background: t.bgSubtle, borderRadius: 14, marginBottom: 14,
        }}>
          <button style={{
            width: 40, height: 40, borderRadius: 20,
            border: `1px solid ${t.border}`, background: t.surface || t.bg,
            color: t.text, fontSize: 22, fontWeight: 500, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: M.fontMono, lineHeight: 1,
          }}>−</button>

          <input
            type="text"
            defaultValue={value}
            style={{
              width: 130, padding: '6px 0',
              background: 'transparent', border: 'none', outline: 'none',
              fontFamily: M.fontMono, fontSize: 40, fontWeight: 700,
              color: t.text, textAlign: 'center',
              letterSpacing: '-0.02em',
            }}
          />

          <button style={{
            width: 40, height: 40, borderRadius: 20,
            border: `1px solid ${t.border}`, background: t.surface || t.bg,
            color: t.text, fontSize: 22, fontWeight: 500, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: M.fontMono, lineHeight: 1,
          }}>+</button>
        </div>

        {/* Пресеты */}
        <div style={{
          fontSize: 11, color: t.textMuted, fontFamily: M.fontMono,
          letterSpacing: 1.4, textTransform: 'uppercase', fontWeight: 600,
          marginBottom: 8,
        }}>Из истории</div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {presets.map((p) => {
            const active = p === value;
            return (
              <button key={p} style={{
                padding: '8px 14px', borderRadius: 999,
                border: `1px solid ${active ? t.accent : t.border}`,
                background: active ? hexToRgba(t.accent, .12) : 'transparent',
                color: active ? t.accent : t.text,
                fontFamily: M.fontMono, fontSize: 14, fontWeight: 600,
                cursor: 'pointer',
              }}>{p}</button>
            );
          })}
        </div>
      </div>
    </React.Fragment>
  );
}

Object.assign(window, { IOSScreenLocomotiveV2, AndroidScreenLocomotiveV2, CoeffSheet });
