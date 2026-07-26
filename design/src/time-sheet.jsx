// Time bottom sheet — приёмка/сдача с нормами
// Серия и станция — две независимые сущности, у каждой свои нормы и своё «Сохранить норму»

// ─────────────────────────────────────────────────────────────
// SheetTimeRow — одна операция в шторке
// ─────────────────────────────────────────────────────────────
function SheetTimeRow({ t, label, time, delta, isFirst, mat, locked, done = false, prevDone = false }) {
  // Цвета концов вертикальной линии: зелёный (наступило) / серый (ещё нет)
  const greyLine = t.borderStrong;
  let lineBg;
  if (prevDone && done) lineBg = t.success;
  else if (!prevDone && !done) lineBg = greyLine;
  else lineBg = `linear-gradient(to bottom, ${prevDone ? t.success : greyLine}, ${done ? t.success : greyLine})`;

  return (
    <div style={{
      padding: '16px 20px',
      display: 'flex', alignItems: 'center', gap: 14,
      position: 'relative',
    }}>
      {!isFirst && (
        <div style={{
          position: 'absolute', left: 33, top: -16, height: 16,
          width: 2, background: lineBg,
        }}/>
      )}

      <div style={{
        width: 32, height: 32, borderRadius: 16,
        background: done ? hexToRgba(t.success, 0.14) : t.bgSubtle,
        color: done ? t.success : t.textMuted,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        {done
          ? <IcCheck width="16" height="16"/>
          : <IcClock width="16" height="16"/>}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 14, fontWeight: 500, color: t.text,
          display: 'flex', alignItems: 'center', gap: 6,
        }}>
          {label}
          {locked && (
            <span style={{
              fontFamily: M.fontMono, fontSize: 10, color: t.textMuted,
              padding: '1px 6px', background: t.bgSubtle, borderRadius: 4,
              letterSpacing: .5, textTransform: 'uppercase',
            }}>из маршрута</span>
          )}
        </div>
        {delta && (
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 4,
            marginTop: 4, fontSize: 12, color: t.textMuted,
            fontFamily: M.fontMono,
          }}>
            <IcFlash width="10" height="10"/>
            <span>{delta}</span>
            <span style={{ color: t.textFaint }}>норма</span>
          </div>
        )}
      </div>

      <button style={{
        padding: '8px 14px', borderRadius: 12,
        background: time ? t.bg : (mat ? t.bg : t.bgSubtle),
        border: mat ? `1px solid ${t.border}` : (time ? `1px solid ${t.border}` : 'none'),
        color: time ? t.text : t.textFaint,
        fontFamily: M.fontMono, fontSize: 18, fontWeight: 600,
        cursor: locked ? 'default' : 'pointer',
        minWidth: 76, textAlign: 'center',
      }}>
        {time || '—:—'}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// ContextField — строка выбора серии или станции в шапке шторки
// ─────────────────────────────────────────────────────────────
function ContextField({ t, icon, label, value, hint, locked, empty, mat }) {
  return (
    <div style={{
      flex: 1, minWidth: 0,
      padding: '8px 10px 8px 8px', borderRadius: 10,
      background: t.surface,
      border: `1px solid ${t.border}`,
      display: 'flex', alignItems: 'center', gap: 10,
      cursor: locked ? 'default' : 'pointer',
    }}>
      <div style={{
        width: 28, height: 28, borderRadius: 8,
        background: empty ? t.bgSubtle : t.accentSoft,
        color: empty ? t.textFaint : t.accent,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        {React.cloneElement(icon, { width: 14, height: 14 })}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 10, color: t.textMuted, fontFamily: M.fontMono,
          textTransform: 'uppercase', letterSpacing: 1.1, lineHeight: 1,
        }}>{label}</div>
        <div style={{
          marginTop: 3, fontSize: 13, fontWeight: 600, lineHeight: 1.2,
          color: empty ? t.textFaint : t.text,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{value}</div>
      </div>
      {locked ? (
        <span style={{
          fontFamily: M.fontMono, fontSize: 9, color: t.textMuted,
          padding: '2px 6px', background: t.bgSubtle, borderRadius: 4,
          letterSpacing: .5, textTransform: 'uppercase', flexShrink: 0,
        }}>лок</span>
      ) : (
        <IcChevronRight width="12" height="12" style={{ color: t.textFaint, flexShrink: 0 }}/>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// TimeSheetIOS
//   seriesLocked  — серия выбрана в форме Локомотив (read-only)
//   stationEmpty  — станция ещё не выбрана
//   noSeriesNorm  — нет нормы для серии
//   noStationNorm — нет нормы для станции
// ─────────────────────────────────────────────────────────────
function TimeSheetIOS({
  dark = false, kind = 'arrival',
  seriesLocked = true,
  stationEmpty = false,
  noSeriesNorm = false,
  noStationNorm = false,
}) {
  const t = dark ? M.dark : M.light;
  const isArrival = kind === 'arrival';
  const title = isArrival ? 'Приёмка' : 'Сдача';
  const stationName = isArrival ? 'Лянгасово' : 'Балезино';

  const seriesValue = 'ВЛ80с';
  const stationValue = stationEmpty ? 'Выберите станцию' : stationName;

  // norm row state
  const seriesNormReady = !noSeriesNorm; // имеется норма серии
  const stationNormReady = !stationEmpty && !noStationNorm;

  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: 'rgba(0,0,0,0.4)',
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      zIndex: 100,
    }}>
      <div style={{
        background: t.bg,
        borderTopLeftRadius: 14, borderTopRightRadius: 14,
        height: '88%', display: 'flex', flexDirection: 'column',
        boxShadow: '0 -4px 24px rgba(0,0,0,0.15)',
      }}>
        {/* Drag handle */}
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8, paddingBottom: 4 }}>
          <div style={{ width: 36, height: 5, borderRadius: 3, background: t.border }}/>
        </div>

        {/* Header */}
        <div style={{
          padding: '8px 16px 14px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          borderBottom: `1px solid ${t.border}`,
        }}>
          <button style={{
            border: 'none', background: 'transparent', color: t.accent,
            fontSize: 16, fontWeight: 400, cursor: 'pointer', padding: 0,
          }}>Отмена</button>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{title}</div>
          <button style={{
            border: 'none', background: 'transparent', color: t.accent,
            fontSize: 16, fontWeight: 600, cursor: 'pointer', padding: 0,
          }}>Готово</button>
        </div>

        <div style={{ overflowY: 'auto', flex: 1 }}>
          {/* Two independent context fields: серия + станция */}
          <div style={{
            padding: '12px 16px 10px',
            display: 'flex', flexDirection: 'column', gap: 8,
          }}>
            <ContextField
              t={t}
              icon={<IcLocomotive/>}
              label="Серия"
              value={seriesValue}
              locked={seriesLocked}
            />
            <ContextField
              t={t}
              icon={<IcMapPin/>}
              label={isArrival ? 'Станция приёмки' : 'Станция сдачи'}
              value={stationValue}
              empty={stationEmpty}
            />
            <button style={{
              alignSelf: 'flex-end',
              padding: '6px 12px', borderRadius: 999,
              background: (noSeriesNorm || stationEmpty || noStationNorm) ? t.bgSubtle : t.accent,
              color: (noSeriesNorm || stationEmpty || noStationNorm) ? t.textFaint : t.accentInk,
              border: (noSeriesNorm || stationEmpty || noStationNorm) ? `1px solid ${t.border}` : 'none',
              fontSize: 12, fontWeight: 600, cursor: 'pointer',
              display: 'inline-flex', alignItems: 'center', gap: 6,
              marginTop: 2,
            }}>
              <IcFlash width="11" height="11"/>
              По нормам
            </button>
          </div>

          {/* Inline warnings — отдельно для серии и станции */}
          {(noSeriesNorm || stationEmpty || noStationNorm) && (
            <div style={{ padding: '10px 16px 0', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {noSeriesNorm && (
                <NormWarn
                  t={t}
                  text={`Нет нормы для серии ${seriesValue}`}
                  cta="Настроить серию"
                />
              )}
              {stationEmpty ? (
                <NormWarn
                  t={t}
                  text={`Станция ${isArrival ? 'приёмки' : 'сдачи'} не выбрана`}
                  hint="Выберите станцию, чтобы применить нормы интервалов"
                />
              ) : noStationNorm && (
                <NormWarn
                  t={t}
                  text={`Нет нормы для станции ${stationName}`}
                  cta="Настроить станцию"
                />
              )}
            </div>
          )}

          {/* Rows */}
          <div style={{ padding: '8px 0' }}>
            {isArrival ? (
              <React.Fragment>
                <SheetTimeRow t={t} label="Явка" time="08:00" isFirst locked done/>
                <SheetTimeRow t={t} label="Начало приёмки" time="08:20" delta="+20 мин" done prevDone/>
                <SheetTimeRow t={t} label="Окончание приёмки" time="09:00" delta="+40 мин" prevDone/>
                <SheetTimeRow t={t} label="Выход на КП" time="09:05" delta="+5 мин"/>
              </React.Fragment>
            ) : (
              <React.Fragment>
                <SheetTimeRow t={t} label="Заход на КП" time="19:50" isFirst done/>
                <SheetTimeRow t={t} label="Начало сдачи" time="20:00" delta="+10 мин" done prevDone/>
                <SheetTimeRow t={t} label="Окончание сдачи" time="20:50" delta="+50 мин" prevDone/>
                <SheetTimeRow t={t} label="Окончание работы" time="20:50" delta="авто"/>
              </React.Fragment>
            )}
          </div>

          {/* Two independent save-norm buttons */}
          <div style={{ padding: '12px 16px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SaveNormBtn
              t={t}
              icon={<IcLocomotive/>}
              title={`Сохранить норму серии ${seriesValue}`}
              subtitle={isArrival ? 'Длительность приёмки' : 'Длительность сдачи'}
              ready={seriesNormReady}
            />
            <SaveNormBtn
              t={t}
              icon={<IcMapPin/>}
              title={stationEmpty
                ? 'Сохранить норму станции'
                : `Сохранить норму станции ${stationName}`}
              subtitle="Интервалы явки и КП"
              ready={stationNormReady}
              disabled={stationEmpty}
            />
          </div>
        </div>

      </div>
    </div>
  );
}

// Inline warning (компактный)
function NormWarn({ t, text, hint, cta }) {
  return (
    <div style={{
      padding: '10px 12px', borderRadius: 12,
      background: t.warnSoft || 'rgba(217,143,32,0.10)',
      border: `1px solid ${t.warn || '#D98F20'}`,
      display: 'flex', alignItems: 'flex-start', gap: 10,
    }}>
      <div style={{
        width: 20, height: 20, borderRadius: 10,
        background: t.warn || '#D98F20', color: '#fff',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        fontSize: 13, fontWeight: 700, fontFamily: M.fontMono,
      }}>!</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.text, lineHeight: 1.3 }}>
          {text}
        </div>
        {hint && (
          <div style={{ fontSize: 12, color: t.textMuted, marginTop: 3, lineHeight: 1.4 }}>
            {hint}
          </div>
        )}
        {cta && (
          <button style={{
            marginTop: 8,
            padding: '5px 10px', borderRadius: 8,
            background: t.surface, color: t.accent,
            border: `1px solid ${t.border}`,
            fontSize: 12, fontWeight: 600, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', gap: 4,
          }}>{cta} <IcChevronRight width="10" height="10"/></button>
        )}
      </div>
    </div>
  );
}

// Save-norm button (две независимые кнопки)
function SaveNormBtn({ t, icon, title, subtitle, ready, disabled }) {
  return (
    <button style={{
      width: '100%', padding: '12px 14px',
      borderRadius: 12,
      background: disabled ? t.bgSubtle : (ready ? t.accentSoft : t.bgSubtle),
      color: disabled ? t.textFaint : t.text,
      border: disabled ? `1px solid ${t.border}` : 'none',
      cursor: disabled ? 'not-allowed' : 'pointer',
      display: 'flex', alignItems: 'center', gap: 12,
      textAlign: 'left',
    }}>
      <div style={{
        width: 32, height: 32, borderRadius: 10,
        background: disabled ? t.bg : (ready ? t.accent : t.surface),
        color: disabled ? t.textFaint : (ready ? t.accentInk : t.accent),
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        {React.cloneElement(icon, { width: 16, height: 16 })}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: disabled ? t.textFaint : t.text, lineHeight: 1.2 }}>
          {title}
        </div>
        <div style={{ fontSize: 11, color: t.textMuted, marginTop: 2 }}>
          {subtitle}
        </div>
      </div>
      <IcCheck width="16" height="16" style={{ color: disabled ? t.textFaint : t.accent, flexShrink: 0 }}/>
    </button>
  );
}

// Локомотив-иконка — используется каноническая версия из icons.jsx (window.IcLocomotive).

// iOS wheel mock
function IOSTimeWheel({ t }) {
  return (
    <div style={{
      height: 130, position: 'relative',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    }}>
      <div style={{
        position: 'absolute', left: 0, right: 0, height: 36, top: '50%',
        transform: 'translateY(-50%)',
        background: t.surface, borderRadius: 8,
        zIndex: 0,
      }}/>
      {[
        { label: 'час', values: ['06', '07', '08', '09', '10'], selected: 2 },
        { label: 'мин', values: ['00', '10', '20', '30', '40'], selected: 2 },
      ].map((col, ci) => (
        <div key={ci} style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          width: 80, height: 130, justifyContent: 'center',
          fontFamily: M.fontMono, fontSize: 22, fontWeight: 600,
          position: 'relative', zIndex: 1,
        }}>
          {col.values.map((v, i) => {
            const dist = Math.abs(i - col.selected);
            return (
              <div key={i} style={{
                height: 26, lineHeight: '26px',
                color: dist === 0 ? t.text : t.textMuted,
                opacity: dist === 0 ? 1 : Math.max(.25, 1 - dist * .35),
                transform: `scale(${dist === 0 ? 1 : 0.9 - dist * .05})`,
              }}>{v}</div>
            );
          })}
        </div>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// TimeSheetAndroid
// ─────────────────────────────────────────────────────────────
function TimeSheetAndroid({
  dark = false, kind = 'arrival',
  seriesLocked = true,
  stationEmpty = false,
  noSeriesNorm = false,
  noStationNorm = false,
}) {
  const t = dark ? M.dark : M.light;
  const isArrival = kind === 'arrival';
  const title = isArrival ? 'Приёмка' : 'Сдача';
  const stationName = isArrival ? 'Лянгасово' : 'Балезино';

  const seriesValue = 'ВЛ80с';
  const stationValue = stationEmpty ? 'Выберите станцию' : stationName;
  const seriesNormReady = !noSeriesNorm;
  const stationNormReady = !stationEmpty && !noStationNorm;

  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      zIndex: 100,
    }}>
      <div style={{
        background: t.surface,
        borderTopLeftRadius: 28, borderTopRightRadius: 28,
        height: '88%', display: 'flex', flexDirection: 'column',
        boxShadow: '0 -4px 24px rgba(0,0,0,0.18)',
      }}>
        {/* Drag handle */}
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 12, paddingBottom: 8 }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: t.border, opacity: .6 }}/>
        </div>

        {/* Header */}
        <div style={{
          padding: '4px 24px 12px',
          display: 'flex', alignItems: 'center', gap: 16,
        }}>
          <div style={{ flex: 1, fontSize: 22, fontWeight: 500, color: t.text }}>{title}</div>
          <button style={{
            padding: '8px 14px', borderRadius: 999,
            background: (noSeriesNorm || stationEmpty || noStationNorm) ? t.bgSubtle : t.accent,
            color: (noSeriesNorm || stationEmpty || noStationNorm) ? t.textFaint : t.accentInk,
            border: (noSeriesNorm || stationEmpty || noStationNorm) ? `1px solid ${t.border}` : 'none',
            fontSize: 13, fontWeight: 500, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', gap: 6,
          }}>
            <IcFlash width="13" height="13"/>
            По нормам
          </button>
        </div>

        <div style={{ overflowY: 'auto', flex: 1 }}>
          {/* Two context fields */}
          <div style={{ padding: '0 24px 10px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            <ContextField
              t={t}
              icon={<IcLocomotive/>}
              label="Серия"
              value={seriesValue}
              locked={seriesLocked}
              mat
            />
            <ContextField
              t={t}
              icon={<IcMapPin/>}
              label={isArrival ? 'Станция приёмки' : 'Станция сдачи'}
              value={stationValue}
              empty={stationEmpty}
              mat
            />
          </div>

          {/* Warnings */}
          {(noSeriesNorm || stationEmpty || noStationNorm) && (
            <div style={{ padding: '8px 24px 0', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {noSeriesNorm && (
                <NormWarn t={t} text={`Нет нормы для серии ${seriesValue}`} cta="Настроить серию"/>
              )}
              {stationEmpty ? (
                <NormWarn t={t} text={`Станция ${isArrival ? 'приёмки' : 'сдачи'} не выбрана`}
                          hint="Выберите станцию, чтобы применить нормы интервалов"/>
              ) : noStationNorm && (
                <NormWarn t={t} text={`Нет нормы для станции ${stationName}`} cta="Настроить станцию"/>
              )}
            </div>
          )}

          {/* Rows */}
          <div style={{ padding: '8px 0' }}>
            {isArrival ? (
              <React.Fragment>
                <SheetTimeRow t={t} label="Явка" time="08:00" isFirst locked mat done/>
                <SheetTimeRow t={t} label="Начало приёмки" time="08:20" delta="+20 мин" mat done prevDone/>
                <SheetTimeRow t={t} label="Окончание приёмки" time="09:00" delta="+40 мин" mat prevDone/>
                <SheetTimeRow t={t} label="Выход на КП" time="09:05" delta="+5 мин" mat/>
              </React.Fragment>
            ) : (
              <React.Fragment>
                <SheetTimeRow t={t} label="Заход на КП" time="19:50" isFirst mat done/>
                <SheetTimeRow t={t} label="Начало сдачи" time="20:00" delta="+10 мин" mat done prevDone/>
                <SheetTimeRow t={t} label="Окончание сдачи" time="20:50" delta="+50 мин" mat prevDone/>
                <SheetTimeRow t={t} label="Окончание работы" time="20:50" delta="авто" mat/>
              </React.Fragment>
            )}
          </div>

          {/* Two save-norm buttons */}
          <div style={{ padding: '8px 24px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SaveNormBtn t={t} icon={<IcLocomotive/>}
              title={`Сохранить норму серии ${seriesValue}`}
              subtitle={isArrival ? 'Длительность приёмки' : 'Длительность сдачи'}
              ready={seriesNormReady}/>
            <SaveNormBtn t={t} icon={<IcMapPin/>}
              title={stationEmpty ? 'Сохранить норму станции' : `Сохранить норму станции ${stationName}`}
              subtitle="Интервалы явки и КП"
              ready={stationNormReady} disabled={stationEmpty}/>
          </div>
        </div>

      </div>
    </div>
  );
}

// Material 3 clock dial — schematic
function AndroidClockDial({ t }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 76, height: 64, borderRadius: 8,
          background: t.accentSoft, color: t.accent,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 36, fontWeight: 500, fontFamily: M.fontMono,
          border: `2px solid ${t.accent}`,
        }}>08</div>
        <div style={{ fontSize: 36, color: t.textMuted, fontWeight: 300 }}>:</div>
        <div style={{
          width: 76, height: 64, borderRadius: 8,
          background: t.bgSubtle, color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 36, fontWeight: 500, fontFamily: M.fontMono,
        }}>20</div>
      </div>

      <div style={{
        width: 110, height: 110, borderRadius: '50%',
        background: t.bgSubtle,
        position: 'relative', flexShrink: 0,
      }}>
        {[12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map((n, i) => {
          const angle = (i * 30 - 90) * Math.PI / 180;
          const r = 42;
          const cx = 55 + r * Math.cos(angle);
          const cy = 55 + r * Math.sin(angle);
          const isSel = n === 8;
          return (
            <div key={n} style={{
              position: 'absolute', width: 24, height: 24,
              left: cx - 12, top: cy - 12,
              borderRadius: 12,
              background: isSel ? t.accent : 'transparent',
              color: isSel ? t.accentInk : t.text,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 11, fontWeight: 500,
            }}>{n}</div>
          );
        })}
        <div style={{
          position: 'absolute', left: 55, top: 55,
          width: 2, height: 38,
          background: t.accent,
          transform: 'rotate(240deg)',
          transformOrigin: '50% 0%',
        }}/>
        <div style={{
          position: 'absolute', left: 51, top: 51,
          width: 8, height: 8, borderRadius: 4, background: t.accent,
        }}/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// RefPickerSheet — выбор серии / станции из справочника
// Открывается тапом на ContextField. Поиск + список с галочкой
// у текущего значения + кнопка добавления нового.
// ─────────────────────────────────────────────────────────────
const PICKER_SERIES = [
  { primary: 'ВЛ80с', secondary: 'Электровоз · приёмка 40, сдача 50 мин', hasNorm: true },
  { primary: 'ЧС7',   secondary: 'Электровоз · приёмка 35, сдача 45 мин', hasNorm: true },
  { primary: '2ТЭ10М', secondary: 'Тепловоз · приёмка 50, сдача 60 мин', hasNorm: true },
  { primary: '2ТЭ116', secondary: 'Тепловоз · приёмка 45, сдача 55 мин', hasNorm: true },
  { primary: 'ЭП1М',  secondary: 'Электровоз · нормы не заданы', hasNorm: false },
];

const PICKER_STATIONS = [
  { primary: 'Лянгасово', secondary: 'ЛНГ · нормы интервалов заданы', hasNorm: true },
  { primary: 'Балезино',  secondary: 'БЛЗ · нормы интервалов заданы', hasNorm: true },
  { primary: 'Киров',     secondary: 'КРВ · нормы интервалов заданы', hasNorm: true },
  { primary: 'Шарья',     secondary: 'ШРЯ · нормы интервалов заданы', hasNorm: true },
  { primary: 'Котельнич', secondary: 'КТЛ · нормы не заданы', hasNorm: false },
];

function RefPickerSheet({ t, mode = 'series', current, mat }) {
  const isSeries = mode === 'series';
  const title = isSeries ? 'Выберите серию' : 'Выберите станцию';
  const items = isSeries ? PICKER_SERIES : PICKER_STATIONS;
  const addLabel = isSeries ? 'Добавить серию' : 'Добавить станцию';
  const radius = mat ? 28 : 14;

  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: 'rgba(0,0,0,0.4)',
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      zIndex: 200,
    }}>
      <div style={{
        background: t.bg,
        borderTopLeftRadius: radius, borderTopRightRadius: radius,
        height: '82%', display: 'flex', flexDirection: 'column',
        boxShadow: '0 -4px 24px rgba(0,0,0,0.18)',
      }}>
        {/* Drag handle */}
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: mat ? 12 : 8, paddingBottom: mat ? 8 : 4 }}>
          <div style={{ width: mat ? 32 : 36, height: mat ? 4 : 5, borderRadius: 3, background: t.border, opacity: mat ? .6 : 1 }}/>
        </div>

        {/* Header */}
        {mat ? (
          <div style={{ padding: '4px 24px 12px', fontSize: 22, fontWeight: 500, color: t.text }}>{title}</div>
        ) : (
          <div style={{
            padding: '8px 16px 14px',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            borderBottom: `1px solid ${t.border}`,
          }}>
            <button style={{
              border: 'none', background: 'transparent', color: t.accent,
              fontSize: 16, fontWeight: 400, cursor: 'pointer', padding: 0,
            }}>Отмена</button>
            <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{title}</div>
            <span style={{ width: 56 }}/>
          </div>
        )}

        {/* Search field */}
        <div style={{ padding: mat ? '4px 24px 12px' : '12px 16px' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            padding: '10px 14px', borderRadius: mat ? 28 : 12,
            background: t.bgSubtle, color: t.textMuted,
          }}>
            <IcSearch width="16" height="16"/>
            <span style={{ fontSize: 15, color: t.textFaint }}>
              {isSeries ? 'Поиск серии' : 'Поиск станции'}
            </span>
          </div>
        </div>

        {/* List */}
        <div style={{ overflowY: 'auto', flex: 1, padding: mat ? '0 12px 12px' : '0 16px 12px' }}>
          {items.map((it, i) => {
            const selected = it.primary === current;
            return (
              <div key={it.primary} style={{
                display: 'flex', alignItems: 'center', gap: 14,
                padding: mat ? '12px 12px' : '13px 8px',
                borderRadius: mat ? 16 : 0,
                background: selected ? t.accentSoft : 'transparent',
                borderBottom: (!mat && i < items.length - 1) ? `1px solid ${t.border}` : 'none',
                cursor: 'pointer',
              }}>
                <div style={{
                  width: 36, height: 36, borderRadius: isSeries ? 10 : 18,
                  background: it.hasNorm ? t.accentSoft : t.bgSubtle,
                  color: it.hasNorm ? t.accent : t.textFaint,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}>
                  {isSeries ? <IcLocomotive width="18" height="18"/> : <IcMapPin width="18" height="18"/>}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 600, color: t.text, lineHeight: 1.2 }}>{it.primary}</div>
                  <div style={{ fontSize: 12, color: it.hasNorm ? t.textMuted : t.warning, marginTop: 2, lineHeight: 1.3 }}>
                    {it.secondary}
                  </div>
                </div>
                {selected && (
                  <div style={{
                    width: 24, height: 24, borderRadius: 12, background: t.accent, color: t.accentInk,
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                  }}>
                    <IcCheck width="14" height="14"/>
                  </div>
                )}
              </div>
            );
          })}

          {/* Add new */}
          <button style={{
            marginTop: 8, width: '100%',
            display: 'flex', alignItems: 'center', gap: 12,
            padding: mat ? '12px' : '13px 8px',
            borderRadius: mat ? 16 : 0,
            background: 'transparent', border: 'none', cursor: 'pointer',
            color: t.accent, textAlign: 'left',
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: 18,
              background: t.accentSoft, color: t.accent,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <IcPlus width="18" height="18"/>
            </div>
            <span style={{ fontSize: 15, fontWeight: 600 }}>{addLabel}</span>
          </button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { TimeSheetIOS, TimeSheetAndroid, IcLocomotive, RefPickerSheet });

// SheetWrap — артборд: главный экран Локомотива + открытая шторка сверху
function SheetWrap({ dark, platform, kind, seriesLocked, stationEmpty, noSeriesNorm, noStationNorm, picker }) {
  const Screen = platform === 'ios' ? IOSScreenLocomotiveV2 : AndroidScreenLocomotiveV2;
  const Sheet = platform === 'ios' ? TimeSheetIOS : TimeSheetAndroid;
  const mat = platform === 'android';
  const currentStation = kind === 'departure' ? 'Балезино' : 'Лянгасово';
  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      <Screen dark={dark}/>
      <Sheet dark={dark} kind={kind}
             seriesLocked={seriesLocked}
             stationEmpty={stationEmpty}
             noSeriesNorm={noSeriesNorm}
             noStationNorm={noStationNorm}/>
      {picker === 'series' && (
        <RefPickerSheet t={dark ? M.dark : M.light} mode="series" current="ВЛ80с" mat={mat}/>
      )}
      {picker === 'station' && (
        <RefPickerSheet t={dark ? M.dark : M.light} mode="station"
                        current={stationEmpty ? null : currentStation} mat={mat}/>
      )}
    </div>
  );
}
Object.assign(window, { SheetWrap });
