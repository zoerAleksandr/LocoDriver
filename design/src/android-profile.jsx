// ═══════════════════════════════════════════════════════════════
// ANDROID · ПРОФИЛЬ (Material 3)
// Те же данные, что и в IOSProfile, но в Material-обвязке:
//   • ADevice (статус-бар с punch-hole + gesture pill)
//   • Material large top app bar
//   • Material-карточки (радиус 16, без iOS-теней)
//   • Material navigation bar снизу (pill-индикатор активного таба)
//   • Кнопки Material: filled / tonal / text
// ═══════════════════════════════════════════════════════════════

// Material navigation bar (общий для Android профиля/покупок)
function AMTabBar({ t, active = 'profile' }) {
  const items = [
    [IcDocument, 'Главный', 'trips'],
    [IcRuble, 'Зарплата', 'pay'],
    [IcPlus, '', 'add', true],
    [IcSliders, 'Настройки', 'settings'],
    [IcProfile, 'Профиль', 'profile'],
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      background: t.surface, height: 72, zIndex: 50,
      display: 'flex', alignItems: 'center',
      borderTop: `1px solid ${t.border}`,
    }}>
      {items.map(([I, l, id, fab], i) => fab ? (
        <div key={i} style={{ flex: 1, display: 'flex', justifyContent: 'center' }}>
          <button style={{
            width: 56, height: 32, borderRadius: 16, background: t.accent, color: t.accentInk, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: M.shadow.sm, cursor: 'pointer',
          }}><I width="22" height="22"/></button>
        </div>
      ) : (
        <button key={i} style={{
          flex: 1, border: 'none', background: 'transparent', display: 'flex',
          flexDirection: 'column', alignItems: 'center', gap: 2, cursor: 'pointer',
          color: id === active ? t.accent : t.textMuted,
        }}>
          <div style={{
            padding: '4px 16px', borderRadius: 16,
            background: id === active ? t.accentSoft : 'transparent',
          }}><I width="22" height="22"/></div>
          <span style={{ fontSize: 11, fontWeight: 600 }}>{l}</span>
        </button>
      ))}
    </div>
  );
}

// Material large top app bar — крупный заголовок, опциональное действие справа.
function ALargeTopBar({ t, title, trailing }) {
  return (
    <div style={{ padding: '8px 4px 0', background: t.bg }}>
      <div style={{ height: 48, display: 'flex', alignItems: 'center', padding: '0 8px', gap: 4 }}>
        <div style={{ flex: 1 }}/>
        {trailing}
      </div>
      <div style={{ padding: '4px 20px 16px', fontSize: 28, fontWeight: 700, color: t.text, letterSpacing: -0.5 }}>
        {title}
      </div>
    </div>
  );
}

// Карточка-статус подписки (Material). active → tonal hero; free → прогресс + filled-кнопка.
function AndroidSubStatusCard({ t, active, dark = false }) {
  if (active) {
    return (
      <button style={{
        width: '100%', textAlign: 'left', cursor: 'pointer', border: 'none',
        background: t.cta, color: t.ctaInk, borderRadius: 16,
        padding: '16px 18px', display: 'flex', alignItems: 'center', gap: 14,
        boxShadow: M.shadow.sm,
      }}>
        <div style={{
          width: 44, height: 44, borderRadius: 14, flexShrink: 0,
          background: hexToRgba(t.ctaInk, 0.12), color: t.ctaInk,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><IcCrown width={24} height={24}/></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 16, fontWeight: 700 }}>Машинист&nbsp;Pro</span>
            <span style={{
              fontFamily: M.fontMono, fontSize: 10, fontWeight: 700, letterSpacing: 0.6,
              padding: '2px 7px', borderRadius: 999, background: hexToRgba(t.success, 0.12), color: t.success,
            }}>АКТИВНА</span>
          </div>
          <div style={{ fontSize: 12.5, color: hexToRgba(t.ctaInk, 0.6), marginTop: 3 }}>
            Годовая · продлится 14.02.2027
          </div>
        </div>
        <IcChevronRight width={18} height={18} style={{ color: hexToRgba(t.ctaInk, 0.5), flexShrink: 0 }}/>
      </button>
    );
  }
  return (
    <div>
      <div style={{ background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, padding: '16px 18px' }}>
        <span style={{
          display: 'inline-flex', alignItems: 'center', gap: 7,
          fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.8,
          color: t.textMuted, textTransform: 'uppercase',
        }}>
          <span style={{ width: 7, height: 7, borderRadius: 99, background: t.warning, boxShadow: `0 0 0 3px ${hexToRgba(t.warning, 0.18)}`, flexShrink: 0 }}/>
          Бесплатный период
        </span>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 13 }}>
          <span style={{ fontSize: 15, fontWeight: 600, color: t.text }}>Бесплатные маршруты</span>
          <span style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>18&nbsp;/&nbsp;20</span>
        </div>
        <div style={{ height: 8, borderRadius: 99, background: t.bgSubtle, marginTop: 10, overflow: 'hidden' }}>
          <div style={{ width: '90%', height: '100%', borderRadius: 99, background: t.warning }}/>
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 10, lineHeight: 1.4 }}>
          Осталось <span style={{ color: t.text, fontWeight: 600 }}>2 маршрута</span>. Дальше добавление — только по подписке Pro.
        </div>
      </div>
      <button style={{
        marginTop: 12, width: '100%', height: 50, border: 'none', cursor: 'pointer',
        borderRadius: 16, background: t.cta, color: t.ctaInk,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9,
        fontSize: 15.5, fontWeight: 600, boxShadow: M.shadow.sm,
      }}><IcCrown width={19} height={19}/> Оформить Машинист&nbsp;Pro</button>
    </div>
  );
}

function AndroidProfile({ dark = false, height = 980, plan = 'pro', vk = true }) {
  const t = dark ? M.dark : M.light;
  const pro = plan === 'pro';
  return (
    <ADevice dark={dark} height={height}>
      <ALargeTopBar t={t} title="Профиль"/>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 92px - 72px)', padding: '0 16px 28px' }}>
        {/* Шапка профиля */}
        <div style={{
          background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm,
          padding: '20px 18px', display: 'flex', flexDirection: 'column',
          alignItems: 'center', textAlign: 'center', gap: 12,
        }}>
          {vk ? (
            <React.Fragment>
              <Avatar t={t} size={84} photo/>
              <div>
                <div style={{ fontSize: 22, fontWeight: 700, color: t.text, letterSpacing: -0.3 }}>Сергей Кузнецов</div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 6, color: t.textMuted }}>
                  <IcVK size={16} color={t.textMuted}/>
                  <span style={{ ...M.t.captionMuted(t) }}>Вход через VK ID</span>
                </div>
              </div>
            </React.Fragment>
          ) : (
            <React.Fragment>
              <Avatar t={t} size={84} muted/>
              <div style={{ ...M.t.captionMuted(t), maxWidth: 240 }}>Войдите через VK ID — подтянем фото и имя автоматически</div>
              <button style={{
                marginTop: 2, width: '100%', height: 48, border: 'none', cursor: 'pointer',
                borderRadius: 999, background: '#0077FF', color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9,
                fontSize: 15.5, fontWeight: 600,
              }}><IcVK size={20} color="#fff"/> Войти через VK ID</button>
            </React.Fragment>
          )}
        </div>

        {/* Email — Material card с trailing icon-button «Изменить» */}
        <AGroup t={t}>Email</AGroup>
        <MCard t={t}>
          <div style={{ padding: '12px 12px 12px 18px', display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{
              width: 40, height: 40, borderRadius: 12, flexShrink: 0,
              background: t.bgSubtle, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcMailP width={20} height={20}/></div>
            <div style={{ flex: 1, minWidth: 0, fontSize: 15, fontWeight: 600, color: t.text }}>s.kuznetsov@mail.ru</div>
            <button style={{
              width: 40, height: 40, borderRadius: 20, flexShrink: 0, cursor: 'pointer', border: 'none',
              background: 'transparent', color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }} aria-label="Изменить почту"><IcPencil width={20} height={20}/></button>
          </div>
        </MCard>

        {/* Подписка */}
        <AGroup t={t}>Подписка</AGroup>
        <AndroidSubStatusCard t={t} active={pro} dark={dark}/>

        {/* Синхронизация — две tonal-кнопки */}
        <AGroup t={t}>Синхронизация</AGroup>
        <div style={{ display: 'flex', gap: 10 }}>
          {[[IcCloudUp, 'Сохранить', 'в облако'], [IcCloudDown, 'Загрузить', 'из облака']].map(([I, a, b], i) => (
            <button key={i} style={{
              flex: 1, cursor: 'pointer', border: 'none',
              background: t.accentSoft, color: t.accent, borderRadius: 16, padding: '16px 10px',
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
            }}>
              <I width={24} height={24}/>
              <span style={{ fontSize: 13.5, fontWeight: 600, color: t.text }}>{a}<br/>{b}</span>
            </button>
          ))}
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 10, paddingLeft: 4 }}>
          Последнее сохранение: сегодня, 08:42
        </div>

        {/* Выход — Material text/outlined button (error) */}
        <button style={{
          marginTop: 22, width: '100%', background: 'transparent', cursor: 'pointer',
          border: `1px solid ${hexToRgba(t.danger, 0.4)}`, borderRadius: 999,
          padding: '14px 18px', textAlign: 'center',
          fontSize: 15, fontWeight: 600, color: t.danger,
        }}>Выйти из аккаунта</button>
      </div>

      {/* Scrim под навбаром */}
      <div style={{
        position: 'absolute', bottom: 72, left: 0, right: 0, height: 24, zIndex: 40, pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})`,
      }}/>
      <AMTabBar t={t} active="profile"/>
    </ADevice>
  );
}

Object.assign(window, { AndroidProfile, AMTabBar, ALargeTopBar, AndroidSubStatusCard });
