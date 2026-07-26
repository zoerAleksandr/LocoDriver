// Шторка «Редактировать станцию» — iOS + Android · light + dark
// Открывается по тапу на строку станции в маршруте экрана «Добавление поезда».

// ─────────────────────────────────────────────────────────────
// Маленькие helpers
// ─────────────────────────────────────────────────────────────

// Поле «label сверху, value снизу» — внутри карточки или standalone.
function SEField({ t, label, value, suffix, flex = 1, width }) {
  return (
    <button style={{
      flex: width ? '0 0 auto' : flex, width, minWidth: 0, padding: '10px 14px',
      background: t.bgSubtle, border: 'none', borderRadius: 12,
      textAlign: 'left', cursor: 'pointer',
      display: 'flex', flexDirection: 'column', gap: 3,
    }}>
      <span style={{ ...M.t.label(t), fontSize: 11 }}>{label}</span>
      <span style={{
        display: 'flex', alignItems: 'baseline', gap: 6, minWidth: 0,
      }}>
        <span style={{
          fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
          color: t.text, letterSpacing: -0.2,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          minWidth: 0,
        }}>{value}</span>
        {suffix}
      </span>
    </button>
  );
}

// Пилюля с датой — кликабельный «13.05.26 ›».
function DateChip({ t, date }) {
  return (
    <button style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '4px 10px', borderRadius: 999, border: 'none',
      background: 'transparent', cursor: 'pointer',
      fontFamily: M.fontMono, fontSize: 13, fontWeight: 500, color: t.textMuted,
    }}>
      <IcCalendar width="13" height="13" style={{ opacity: .8 }}/>
      <span>{date}</span>
      <IcChevronRight width="10" height="10" style={{ color: t.textFaint }}/>
    </button>
  );
}

// Шаг ±N / Сейчас — равномерный сегмент в строке быстрых правок.
function StepCell({ t, label, kind = 'step' }) {
  // kind: 'step' (нейтральная), 'now' (акцент)
  const isNow = kind === 'now';
  return (
    <button style={{
      flex: 1, height: 38, borderRadius: 10, border: 'none',
      background: isNow ? t.accentSoft : t.bgSubtle,
      color: isNow ? t.accent : t.text,
      fontFamily: isNow ? M.fontSans : M.fontMono,
      fontSize: isNow ? 13 : 14,
      fontWeight: isNow ? 600 : 500,
      cursor: 'pointer',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      fontVariantNumeric: 'tabular-nums',
    }}>{label}</button>
  );
}

// Большое значение времени + кнопка очистки.
function TimeBigField({ t, value, empty }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '14px 16px', borderRadius: 14,
      background: t.bgSubtle,
    }}>
      <span style={{
        flex: 1,
        fontFamily: M.fontMono, fontSize: 32, fontWeight: 700,
        color: empty ? t.textFaint : t.text,
        letterSpacing: -0.6, lineHeight: 1,
        fontVariantNumeric: 'tabular-nums',
      }}>{empty ? '—:—' : value}</span>
      {!empty && (
        <button style={{
          width: 34, height: 34, borderRadius: 17, border: 'none',
          background: 'transparent', color: t.textMuted, cursor: 'pointer',
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        }} aria-label="Очистить">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               strokeWidth="2" strokeLinecap="round"><path d="M6 6l12 12M6 18L18 6"/></svg>
        </button>
      )}
    </div>
  );
}

// Бейдж стоянки между «прибытие → отправление».
function StopBadge({ t, label }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '4px 0',
    }}>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
      <span style={{
        padding: '4px 10px', borderRadius: 999,
        background: hexToRgba(t.warning, .15), color: t.warning,
        fontFamily: M.fontMono, fontSize: 11, fontWeight: 600,
        letterSpacing: 1, textTransform: 'uppercase',
      }}>{label}</span>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Один блок «Прибытие / Отправление»
// ─────────────────────────────────────────────────────────────
function TimeBlock({ t, kind, time, date, empty }) {
  const label = kind === 'arrival' ? 'Прибытие' : 'Отправление';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 2px',
      }}>
        <span style={{
          fontFamily: M.fontMono, fontSize: 11, fontWeight: 600,
          color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase',
        }}>{label}</span>
        <DateChip t={t} date={date}/>
      </div>
      <TimeBigField t={t} value={time} empty={empty}/>
      <div style={{ display: 'flex', gap: 6 }}>
        <StepCell t={t} label="−5"/>
        <StepCell t={t} label="−1"/>
        <StepCell t={t} label="Сейчас" kind="now"/>
        <StepCell t={t} label="+1"/>
        <StepCell t={t} label="+5"/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Шторка целиком — iOS / Android, light / dark, edit / new
// ─────────────────────────────────────────────────────────────
function StationEditSheet({
  dark = false, platform = 'ios', mode = 'edit',
  data,
}) {
  const t = dark ? M.dark : M.light;
  const isIOS = platform === 'ios';
  const d = data || {
    name: 'Гатчина-Варшавская', track: '24',
    arrival: '13:55', departure: '14:46',
    arrivalDate: '13.05.26', departureDate: '13.05.26',
    stopMin: 51,
  };
  const isNew = mode === 'new';
  const title = isNew ? 'Новая станция' : 'Редактировать станцию';

  return (
    <div style={{
      background: t.bg,
      borderTopLeftRadius: isIOS ? M.r.sheetIOS : M.r.sheetAndroid,
      borderTopRightRadius: isIOS ? M.r.sheetIOS : M.r.sheetAndroid,
      boxShadow: M.shadow.sheet,
      display: 'flex', flexDirection: 'column',
    }}>
      {/* Grabber */}
      <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
        <div style={{
          width: 36, height: isIOS ? 5 : 4, borderRadius: 3,
          background: t.borderStrong || t.border,
        }}/>
      </div>

      {/* Header */}
      {isIOS ? (
        <div style={{
          padding: '12px 20px 6px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 16, fontWeight: 400, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans,
          }}>Отмена</button>
          <span style={{ ...M.t.navTitle(t) }}>{isNew ? 'Новая станция' : 'Станция'}</span>
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 16, fontWeight: 600, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans,
          }}>Готово</button>
        </div>
      ) : (
        <div style={{
          padding: '8px 20px 6px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <span style={{
            fontFamily: M.fontSans, fontSize: 22, fontWeight: 500,
            color: t.text, letterSpacing: -0.2,
          }}>{title}</span>
          <button style={{
            width: 40, height: 40, borderRadius: 20, border: 'none',
            background: 'transparent', color: t.textMuted, cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          }} aria-label="Закрыть">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 strokeWidth="2" strokeLinecap="round"><path d="M6 6l12 12M6 18L18 6"/></svg>
          </button>
        </div>
      )}

      {/* Content */}
      <div style={{
        padding: '12px 20px 20px',
        display: 'flex', flexDirection: 'column', gap: 18,
      }}>
        {/* Название + Путь */}
        <div style={{ display: 'flex', gap: 8 }}>
          <SEField t={t} label="Название" value={d.name} flex={1}/>
          <SEField t={t} label="Путь" value={d.track} width={96}/>
        </div>

        {/* Прибытие */}
        <TimeBlock t={t} kind="arrival" time={d.arrival} date={d.arrivalDate} empty={isNew}/>

        {/* Стоянка */}
        {!isNew && d.stopMin != null && (
          <StopBadge t={t} label={`Стоянка · ${d.stopMin} мин`}/>
        )}
        {isNew && (
          <div style={{ height: 1, background: t.border }}/>
        )}

        {/* Отправление */}
        <TimeBlock t={t} kind="departure" time={d.departure} date={d.departureDate} empty={isNew}/>

        {/* iOS: «Удалить станцию» — текстовая кнопка-ссылка.
            Android: «Готово» как полно-ширинный CTA + «Удалить станцию» ниже. */}
        {isIOS ? (
          !isNew && (
            <button style={{
              marginTop: 4,
              padding: '12px',
              background: 'transparent', border: 'none', cursor: 'pointer',
              color: t.danger,
              fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            }}>
              <IcTrash width="16" height="16"/>
              Удалить станцию
            </button>
          )
        ) : (
          <div style={{
            marginTop: 4, display: 'flex', flexDirection: 'column', gap: 6,
          }}>
            <button style={{
              width: '100%', height: 52, borderRadius: 26, border: 'none',
              background: t.cta, color: t.ctaInk, cursor: 'pointer',
              fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            }}>Готово</button>
            {!isNew && (
              <button style={{
                width: '100%', height: 48, borderRadius: 24, border: 'none',
                background: 'transparent', color: t.danger, cursor: 'pointer',
                fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              }}>
                <IcTrash width="16" height="16"/>
                Удалить станцию
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// Wrapper — рендер шторки над затемнённым экраном «Добавление поезда».
function StationEditSheetWrap({ dark = false, platform = 'ios', mode = 'edit' }) {
  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ filter: 'saturate(.7)', opacity: .55 }}>
        <IOSScreenAddTrain dark={dark} height={920}/>
      </div>
      <div style={{
        position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)',
      }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
      }}>
        <StationEditSheet dark={dark} platform={platform} mode={mode}/>
      </div>
    </div>
  );
}

window.StationEditSheet = StationEditSheet;
window.StationEditSheetWrap = StationEditSheetWrap;

window.SEField = SEField;
window.DateChip = DateChip;
window.StepCell = StepCell;
window.TimeBigField = TimeBigField;
window.StopBadge = StopBadge;
window.TimeBlock = TimeBlock;
