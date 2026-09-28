// Профиль — iOS light (без геймификации)
// По референсу реального приложения профиль минималистичный и функциональный:
//   • ФИО + аватар + должность
//   • Статус подписки «Машинист Pro» → ведёт в раздел «Покупки»
//   • Аккаунт: синхронизация · резервная копия · выход
// Никаких достижений, медалей, званий и прогресс-баров.

// ─────────────────────────────────────────────────────────────
// Доп. иконки (нет в icons.jsx)
// ─────────────────────────────────────────────────────────────
const _pIco = {
  width: 24, height: 24, viewBox: '0 0 24 24',
  fill: 'none', stroke: 'currentColor', strokeWidth: 1.3,
  strokeLinecap: 'round', strokeLinejoin: 'round',
};
function IcPencil(p) { return (
  <svg {..._pIco} {...p}>
    <path d="M4 20h4l10-10a2 2 0 0 0-2.8-2.8L5 17.2z"/>
    <path d="M13.5 6.5l4 4"/>
  </svg>
); }
function IcCloudCheck(p) { return (
  <svg {..._pIco} {...p}>
    <path d="M7 18a4 4 0 0 1 0-8 5 5 0 0 1 9.5-1A4 4 0 0 1 17 18z"/>
    <path d="M9.2 14.2l1.8 1.8 3.4-3.4"/>
  </svg>
); }
function IcCrown(p) { return (
  <svg {..._pIco} {...p}>
    <path d="M4 8l3.5 3L12 5l4.5 6L20 8l-1.5 10h-13z"/>
  </svg>
); }
function IcChevronRightP(p) { return (
  <svg {..._pIco} {...p}><path d="M9 5l7 7-7 7"/></svg>
); }
function IcCloudUp(p) { return (
  <svg {..._pIco} {...p}>
    <path d="M7 18a4 4 0 0 1 0-8 5 5 0 0 1 9.5-1A4 4 0 0 1 17 18z"/>
    <path d="M12 16v-5"/><path d="M9.6 12.4L12 10l2.4 2.4"/>
  </svg>
); }
function IcCloudDown(p) { return (
  <svg {..._pIco} {...p}>
    <path d="M7 18a4 4 0 0 1 0-8 5 5 0 0 1 9.5-1A4 4 0 0 1 17 18z"/>
    <path d="M12 11v5"/><path d="M9.6 13.6L12 16l2.4-2.4"/>
  </svg>
); }
function IcLock(p) { return (
  <svg {..._pIco} {...p}>
    <rect x="5" y="11" width="14" height="9" rx="2"/>
    <path d="M8 11V8a4 4 0 0 1 8 0v3"/>
  </svg>
); }
function IcMailP(p) { return (
  <svg {..._pIco} {...p}>
    <rect x="3" y="5" width="18" height="14" rx="2"/>
    <path d="M3 7l9 7 9-7"/>
  </svg>
); }
// VK ID — упрощённый монограммный знак (не точный логотип)
function IcVK({ size = 22, color = '#fff' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M12.8 16.4c-5 0-8.3-3.5-8.4-9.2h2.6c.1 4.2 2 6 3.5 6.3V7.2h2.4v3.6c1.5-.2 3-1.8 3.6-3.6h2.4c-.4 2.2-2 3.8-3.1 4.5 1.1.6 2.9 2 3.6 4.7H17c-.5-1.7-1.9-3.1-3.5-3.3v3.3z" fill={color}/>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// Аватар-фото (подтягивается из VK). Если фото нет — инициалы.
// ─────────────────────────────────────────────────────────────
function Avatar({ t, initials = 'СК', size = 60, photo, muted }) {
  const isPic = photo && !muted;
  return (
    <div style={{
      width: size, height: size, borderRadius: size / 2, flexShrink: 0,
      overflow: 'hidden', position: 'relative',
      background: isPic
        ? 'linear-gradient(135deg, #5b7cc4 0%, #3a5a9e 100%)'
        : (muted ? hexToRgba(t.text, 0.06) : t.accentSoft),
      color: isPic ? '#fff' : t.accent,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.05)',
    }}>
      {(isPic || muted) ? (
        <svg width={size} height={size} viewBox="0 0 60 60" fill="none">
          <circle cx="30" cy="23" r="11" fill={isPic ? 'rgba(255,255,255,0.92)' : hexToRgba(t.text, 0.22)}/>
          <path d="M11 54c1.5-11 9.5-17 19-17s17.5 6 19 17z" fill={isPic ? 'rgba(255,255,255,0.92)' : hexToRgba(t.text, 0.22)}/>
        </svg>
      ) : (
        <span style={{ fontFamily: M.fontDisplay, fontWeight: 700, fontSize: size * 0.4, letterSpacing: 0.5 }}>{initials}</span>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Таб-бар iOS (как в проекте), active='profile'
// ─────────────────────────────────────────────────────────────
function MTabBar({ t, active = 'profile' }) {
  const items = [
    { id: 'trips', I: IcDocument, l: 'Главная' },
    { id: 'pay',   I: IcRuble,    l: 'Зарплата' },
    { id: '',      I: IcPlus,     fab: true },
    { id: 'settings', I: IcSliders, l: 'Настройки' },
    { id: 'profile',  I: IcProfile, l: 'Профиль' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 22, left: 10, right: 10, zIndex: 50,
      background: t.surface, borderRadius: 30,
      boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
      border: `1px solid ${t.border}`,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 14px',
    }}>
      {items.map((it, i) => it.fab ? (
        <button key={i} style={{
          width: 46, height: 46, borderRadius: 23, background: t.cta, color: t.ctaInk,
          border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 18px rgba(0,0,0,0.25)',
        }}><IcPlus width="22" height="22"/></button>
      ) : (
        <button key={i} style={{
          background: 'transparent', border: 'none', cursor: 'pointer',
          display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
          color: it.id === active ? t.text : t.textMuted, padding: '4px 12px',
        }}>
          <it.I width="22" height="22"/>
          <span style={{ fontSize: 10, fontWeight: 600 }}>{it.l}</span>
        </button>
      ))}
    </div>
  );
}

// Большой iOS-заголовок
function LargeTitle({ t, children, trailing }) {
  return (
    <div style={{ paddingTop: 58 }}>
      <div style={{
        padding: '4px 20px 12px', display: 'flex',
        alignItems: 'center', justifyContent: 'space-between',
      }}>
        <div style={{ fontFamily: M.fontDisplay, fontSize: 34, fontWeight: 800, color: t.text, letterSpacing: -0.8 }}>{children}</div>
        {trailing}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Карточка-вход в «Покупки» — статус подписки.
// active=true  → тёмный hero «Машинист Pro · активна»
// active=false → светлый апселл «Оформить Pro»
// В обоих случаях вся карточка — кнопка, ведущая в раздел «Покупки».
// ─────────────────────────────────────────────────────────────
function SubStatusCard({ t, active, dark = false }) {
  if (active) {
    return (
      <button style={{
        width: '100%', textAlign: 'left', cursor: 'pointer', border: 'none',
        background: t.cta, color: t.ctaInk, borderRadius: 18,
        padding: '16px 18px',
        display: 'flex', alignItems: 'center', gap: 14,
        boxShadow: M.shadow.md,
      }}>
        <div style={{
          width: 40, height: 40, borderRadius: 12, flexShrink: 0,
          background: hexToRgba(t.ctaInk, 0.12), color: t.ctaInk,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><IcCrown width={22} height={22}/></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontFamily: M.fontDisplay, fontSize: 16, fontWeight: 700 }}>Машинист&nbsp;Pro</span>
            <span style={{
              fontFamily: M.fontMono, fontSize: 10, fontWeight: 700, letterSpacing: 0.6,
              padding: '2px 6px', borderRadius: 6, background: hexToRgba(t.success, 0.12), color: t.success,
            }}>АКТИВНА</span>
          </div>
          <div style={{ fontFamily: M.fontSans, fontSize: 12.5, color: hexToRgba(t.ctaInk, 0.6), marginTop: 3 }}>
            Годовая · продлится 14.02.2027
          </div>
        </div>
        <IcChevronRightP width={16} height={16} style={{ color: hexToRgba(t.ctaInk, 0.5), flexShrink: 0 }}/>
      </button>
    );
  }
  // Бесплатная версия — прогресс маршрутов + кнопка перехода к оплате
  return (
    <div>
      <div style={{
        background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm,
        border: `1px solid ${t.border}`, padding: '16px 18px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: 7,
            fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.8,
            color: t.textMuted, textTransform: 'uppercase',
          }}>
            <span style={{ width: 7, height: 7, borderRadius: 99, background: t.warning, boxShadow: `0 0 0 3px ${hexToRgba(t.warning, 0.18)}`, flexShrink: 0 }}/>
            Бесплатный период
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 13 }}>
          <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>Бесплатные маршруты</span>
          <span style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 700, color: t.text, whiteSpace: 'nowrap', flexShrink: 0 }}>18&nbsp;/&nbsp;20</span>
        </div>
        <div style={{ height: 8, borderRadius: 99, background: t.bgSubtle, marginTop: 10, overflow: 'hidden' }}>
          <div style={{ width: '90%', height: '100%', borderRadius: 99, background: t.warning }}/>
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 10, lineHeight: 1.4 }}>
          Осталось <span style={{ color: t.text, fontWeight: 600 }}>2 маршрута</span>. Дальше добавление — только по подписке Pro.
        </div>
      </div>

      {/* Кнопка перехода на экран оплаты */}
      <button style={{
        marginTop: 12, width: '100%', height: 50, border: 'none', cursor: 'pointer',
        borderRadius: 14, background: t.cta, color: t.ctaInk,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9,
        fontFamily: M.fontSans, fontSize: 15.5, fontWeight: 600,
        boxShadow: M.shadow.md,
      }}><IcCrown width={19} height={19}/> Оформить Машинист&nbsp;Pro</button>
    </div>
  );
}

// Строка-настройка внутри карточки «Аккаунт»
function SettingsLikeRow({ t, label, value, danger, noIcon }) {
  return (
    <div style={{ padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer' }}>
      {!noIcon && <div style={{ width: 38 }}/>}
      <div style={{ flex: 1, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: danger ? t.danger : t.text }}>{label}</div>
      {value && <span style={{ fontFamily: M.fontSans, fontSize: 14, color: t.textMuted }}>{value}</span>}
      {!danger && <IcChevronRightP width={14} height={14} style={{ color: t.textFaint }}/>}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПРОФИЛЬ — единый чистый экран (idiom настроек)
//   plan='pro'  → подписка активна
//   plan='free' → апселл
// ═══════════════════════════════════════════════════════════════
function IOSProfile({ dark = false, height = 880, plan = 'pro', vk = true }) {
  const t = dark ? M.dark : M.light;
  const pro = plan === 'pro';
  return (
    <MDevice dark={dark} height={height}>
      <LargeTitle t={t}>Профиль</LargeTitle>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 100px - 100px)', padding: '4px 16px 32px' }}>
        {/* Шапка: фото+имя из VK ИЛИ приглашение войти через VK */}
        <div style={{
          background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm,
          padding: '20px 18px', display: 'flex', flexDirection: 'column',
          alignItems: 'center', textAlign: 'center', gap: 12,
        }}>
          {vk ? (
            <React.Fragment>
              <Avatar t={t} size={84} photo/>
              <div>
                <div style={{ fontFamily: M.fontDisplay, fontSize: 22, fontWeight: 700, color: t.text, letterSpacing: -0.3 }}>Сергей Кузнецов</div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 6, color: t.textMuted }}>
                  <IcVK size={16} color={t.textMuted}/>
                  <span style={{ ...M.t.captionMuted(t) }}>Вход через VK ID</span>
                </div>
              </div>
            </React.Fragment>
          ) : (
            <React.Fragment>
              <Avatar t={t} size={84} muted/>
              <div>
                <div style={{ ...M.t.captionMuted(t), maxWidth: 240 }}>Войдите через VK ID — подтянем фото и имя автоматически</div>
              </div>
              <button style={{
                marginTop: 2, width: '100%', height: 46, border: 'none', cursor: 'pointer',
                borderRadius: 14, background: '#0077FF', color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9,
                fontFamily: M.fontSans, fontSize: 15.5, fontWeight: 600,
              }}><IcVK size={20} color="#fff"/> Войти через VK ID</button>
            </React.Fragment>
          )}
        </div>

        {/* Email — редактируемый, действие «Изменить» в заголовке (над полем) */}
        <GroupHead t={t} trailing={
          <button style={{
            background: 'transparent', border: 'none', cursor: 'pointer', padding: 0,
            display: 'inline-flex', alignItems: 'center', gap: 5,
            color: t.accent, fontFamily: M.fontSans, fontSize: 14, fontWeight: 600,
          }}><IcPencil width={15} height={15}/> Изменить</button>
        }>Email</GroupHead>
        <Card t={t}>
          <div style={{
            padding: '14px 18px',
            display: 'flex', alignItems: 'center', gap: 14,
          }}>
            <div style={{
              width: 38, height: 38, borderRadius: 11, flexShrink: 0,
              background: t.accentSoft, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcMailP width={20} height={20}/></div>
            <div style={{ flex: 1, minWidth: 0, fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>s.kuznetsov@mail.ru</div>
          </div>
        </Card>

        {/* Подписка → раздел «Покупки» */}
        <GroupHead t={t}>Подписка</GroupHead>
        <SubStatusCard t={t} active={pro} dark={dark}/>

        {/* Синхронизация — две кнопки */}
        <GroupHead t={t}>Синхронизация</GroupHead>
        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{
            flex: 1, cursor: 'pointer', border: `1px solid ${t.border}`,
            background: t.surface, color: t.text, borderRadius: 16, padding: '14px 10px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
            boxShadow: M.shadow.sm,
          }}>
            <div style={{
              width: 40, height: 40, borderRadius: 12,
              background: t.accentSoft, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcCloudUp width={22} height={22}/></div>
            <span style={{ fontFamily: M.fontSans, fontSize: 13.5, fontWeight: 600 }}>Сохранить<br/>в облако</span>
          </button>
          <button style={{
            flex: 1, cursor: 'pointer', border: `1px solid ${t.border}`,
            background: t.surface, color: t.text, borderRadius: 16, padding: '14px 10px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
            boxShadow: M.shadow.sm,
          }}>
            <div style={{
              width: 40, height: 40, borderRadius: 12,
              background: t.accentSoft, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcCloudDown width={22} height={22}/></div>
            <span style={{ fontFamily: M.fontSans, fontSize: 13.5, fontWeight: 600 }}>Загрузить<br/>из облака</span>
          </button>
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 10, paddingLeft: 4 }}>
          Последнее сохранение: сегодня, 08:42
        </div>

        {/* Выход */}
        <button style={{
          marginTop: 22,
          width: '100%', background: t.surface, cursor: 'pointer',
          border: `1px solid ${hexToRgba(t.danger, 0.25)}`, borderRadius: 16,
          padding: '15px 18px', textAlign: 'center', boxShadow: M.shadow.sm,
          fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.danger,
        }}>Выйти из аккаунта</button>
      </div>

      <ScrimBottom t={t}/>
      <MTabBar t={t} active="profile"/>
    </MDevice>
  );
}

Object.assign(window, {
  IOSProfile,
  MTabBar, Avatar, SubStatusCard, SettingsLikeRow,
  IcCrown, IcCloudCheck, IcCloudUp, IcCloudDown, IcPencil, IcChevronRightP,
  IcMailP, IcLock, IcVK,
});
