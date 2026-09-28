// Android · Добавление поезда — Material-обвязка вокруг общего тела экрана.
//
// Тело (карточки «Плечо/Номер/Новый», «Вес/Оси/Удельное», поле плеча,
// таймлайн маршрута, метаданные) переиспользуется из ios-add-train.jsx —
// эти под-компоненты платформонезависимы (Card, Pill, FieldStack,
// ShoulderField, RouteStationRow). Здесь отличается лишь каркас:
//   • ADevice вместо MDevice (Material статус-бар, рамка, gesture pill)
//   • Material small top app bar вместо NavBarIOS
//   • Material filled-кнопка снизу вместо iOS-пилюли
// Шторки «Плечи» и «Настройки поезда» переиспользуются как есть.

function AndroidScreenAddTrain({ dark = false, height = 920, sheet = null }) {
  const t = dark ? M.dark : M.light;

  return (
    <ADevice dark={dark} height={height}>
      {/* Material small top app bar */}
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
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>Поезд</div>
        <button style={{
          position: 'relative',
          width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }} aria-label="Настройки поезда">
          <IcGear width="22" height="22"/>
          <span style={{
            position: 'absolute', top: 11, right: 11,
            width: 7, height: 7, borderRadius: 4,
            background: t.danger || '#E25C3D',
            boxShadow: `0 0 0 1.5px ${t.bg}`,
          }}/>
        </button>
      </div>

      {/* Скроллируемое тело */}
      <div style={{
        overflowY: 'auto', height: 'calc(100% - 64px)',
        padding: '8px 16px 140px',
      }}>
        {/* Чип маршрута справа */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}>
          <Pill t={t} tone="accent" mono={false} style={{ padding: '6px 12px', fontSize: 14, gap: 8 }}>
            <span>№ 3323</span>
            <span style={{ opacity: 0.6, fontSize: 16 }}>×</span>
          </Pill>
        </div>

        {/* Плечо (short) + Номер + Новый */}
        <div style={{
          display: 'grid', gridTemplateColumns: '120px 1fr auto',
          gap: M.s.sm, marginBottom: M.s.md,
        }}>
          <MCard t={t}>
            <button style={{
              width: '100%', height: 64, border: 'none', background: 'transparent',
              padding: '10px 14px', textAlign: 'left', cursor: 'pointer',
              display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 2,
            }}>
              <div style={{ fontFamily: M.fontMono, fontSize: 22, fontWeight: 600, color: t.text, letterSpacing: -0.3 }}>250</div>
              <div style={{ ...M.t.label(t), fontSize: 12 }}>км</div>
            </button>
          </MCard>

          <MCard t={t}>
            <button style={{
              width: '100%', height: 64, border: 'none', background: 'transparent',
              padding: '10px 14px', textAlign: 'left', cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                <div style={{ fontFamily: M.fontMono, fontSize: 22, fontWeight: 600, color: t.text, letterSpacing: -0.3 }}>№&nbsp;2342</div>
                <div style={{ ...M.t.label(t), fontSize: 12 }}>номер поезда</div>
              </div>
              <span style={{
                width: 22, height: 22, borderRadius: 11,
                border: `1.5px solid ${t.accent}`, color: t.accent,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                fontFamily: M.fontSans, fontSize: 13, fontWeight: 700, fontStyle: 'italic',
              }}>i</span>
            </button>
          </MCard>

          <button style={{
            width: 64, height: 64, borderRadius: 18,
            background: t.accentSoft, color: t.accent, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer', boxShadow: M.shadow.sm,
          }} aria-label="Новый поезд">
            <IcPlus width="22" height="22"/>
          </button>
        </div>

        {/* Вес / Оси / Удельное */}
        <MCard t={t}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr' }}>
            <FieldStack t={t} label="Вес, т" value="1 390" mono/>
            <div style={{ borderLeft: `1px solid ${t.border}` }}>
              <FieldStack t={t} label="Оси" value="252" mono/>
            </div>
            <div style={{ borderLeft: `1px solid ${t.border}` }}>
              <FieldStack t={t} label="Удельное" value="57" mono/>
            </div>
          </div>
        </MCard>

        {/* ★ ПЛЕЧО — кликабельный селектор */}
        <div style={{ marginTop: M.s.lg }}>
          <ShoulderField t={t} from="Луга" to="Лужская" km={250}/>
        </div>

        {/* Маршрут */}
        <AGroup t={t}>Маршрут</AGroup>
        <MCard t={t}>
          <div style={{ padding: '12px 8px 12px 12px' }}>
            <RouteStationRow t={t} station="Луга 1 п." arrival="—" departure="12:04" travelTo="1 ч 51 мин" active first/>
            <RouteStationRow t={t} station="Гатчина-Варшавская" arrival="13:55" departure="14:46" stopMin={51} travelTo="23 мин"/>
            <RouteStationRow t={t} station="Владимирская 4 п." arrival="15:09" departure="15:35" stopMin={26} travelTo="1 ч 38 мин"/>
            <RouteStationRow t={t} station="СПбСМ" arrival="17:13" departure="—" active last/>
          </div>
        </MCard>

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

      {/* CTA — Material filled button */}
      {!sheet && (
        <React.Fragment>
          <div style={{
            position: 'absolute', bottom: 0, left: 0, right: 0, height: 120, zIndex: 40,
            pointerEvents: 'none',
            background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.9)} 50%, ${t.bg} 100%)`,
          }}/>
          <div style={{ position: 'absolute', bottom: 24, left: 16, right: 16, zIndex: 50 }}>
            <button style={{
              width: '100%', height: 52, borderRadius: 16,
              background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
              border: 'none', cursor: 'pointer',
              fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
              boxShadow: '0 8px 24px rgba(0,0,0,0.22)',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            }}>
              <IcPlus width="18" height="18"/>
              Добавить станцию
            </button>
          </div>
        </React.Fragment>
      )}

      {/* Шторки (переиспользуются из ios-add-train.jsx) */}
      {sheet === 'shoulders' && <ShouldersSheet t={t}/>}
      {sheet === 'settings' && <SettingsSheet t={t} expanded={false}/>}
      {sheet === 'settings-expanded' && <SettingsSheet t={t} expanded={true}/>}
    </ADevice>
  );
}

Object.assign(window, { AndroidScreenAddTrain });
