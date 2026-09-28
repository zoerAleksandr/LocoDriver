// ═══════════════════════════════════════════════════════════════
// ВХОД / РЕГИСТРАЦИЯ / ВОССТАНОВЛЕНИЕ ПАРОЛЯ — iOS light
// Логика приложения:
//   • вход по e-mail + паролю и через VK ID
//   • переключатель Вход / Регистрация (сегмент-контрол сверху)
//   • восстановление: пользователь вводит почту → на неё уходит
//     письмо со ссылкой для смены пароля (без кода — код не приходит)
//
// mode: 'login' | 'register' | 'recover' | 'sent'
// ═══════════════════════════════════════════════════════════════

const DEMO_EMAIL = 's.kuznetsov@mail.ru';

// ── доп. иконки (нет в icons.jsx / profile-screens.jsx) ──────────
const _aIco = {
  width: 24, height: 24, viewBox: '0 0 24 24',
  fill: 'none', stroke: 'currentColor', strokeWidth: 1.4,
  strokeLinecap: 'round', strokeLinejoin: 'round',
};
function IcEye(p) { return (
  <svg {..._aIco} {...p}>
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/>
    <circle cx="12" cy="12" r="3"/>
  </svg>
); }
function IcEyeOff(p) { return (
  <svg {..._aIco} {...p}>
    <path d="M10.6 6.1A9.7 9.7 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3.2 3.7M6.3 7.6A17 17 0 0 0 2 12s3.5 6 10 6a9.6 9.6 0 0 0 4-.8"/>
    <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/>
    <path d="M3 3l18 18"/>
  </svg>
); }
function IcPaperPlane(p) { return (
  <svg {..._aIco} {...p}>
    <path d="M21 4L3 11l6 2.5L11 20l3.5-6L21 4z"/>
    <path d="M9 13.5L21 4"/>
  </svg>
); }
function IcMailOpen(p) { return (
  <svg {..._aIco} {...p}>
    <path d="M3 9.5L12 4l9 5.5V19a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z"/>
    <path d="M3 9.5l9 6 9-6"/>
  </svg>
); }
function IcAlert(p) { return (
  <svg {..._aIco} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M12 8v4.5"/>
    <path d="M12 16h.01"/>
  </svg>
); }

// ── сегмент-контрол Вход / Регистрация ──────────────────────────
function AuthSegment({ t, value, onChange }) {
  const items = [['login', 'Вход'], ['register', 'Регистрация']];
  return (
    <div style={{
      display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4,
      background: t.bgSubtle, borderRadius: 12, padding: 4,
    }}>
      {items.map(([key, label]) => {
        const active = value === key;
        return (
          <button key={key} onClick={() => onChange && onChange(key)} style={{
            height: 38, borderRadius: 9, border: 'none', cursor: 'pointer',
            background: active ? t.surface : 'transparent',
            boxShadow: active ? M.shadow.sm : 'none',
            color: active ? t.text : t.textMuted,
            fontFamily: M.fontSans, fontSize: 15, fontWeight: 600,
            transition: 'all .15s ease',
          }}>{label}</button>
        );
      })}
    </div>
  );
}

// ── поле ввода (иконка + лейбл + значение + caret/eye) ───────────
function AuthField({ t, icon, label, value, placeholder, focused, mono, trailing, error, errColor }) {
  const ec = errColor || t.danger;
  const borderColor = error ? ec : focused ? t.accent : t.border;
  const iconColor = error ? ec : focused ? t.accent : t.textMuted;
  return (
    <div>
      <div style={{ ...M.t.label(t), marginBottom: 7, paddingLeft: 2 }}>{label}</div>
      <div style={{
        background: t.surface, borderRadius: 14,
        boxShadow: error ? `0 0 0 3px ${ec}24` : M.shadow.sm,
        border: `1.5px solid ${borderColor}`,
        padding: '0 14px', height: 52, display: 'flex', alignItems: 'center', gap: 10,
      }}>
        {React.cloneElement(icon, { width: 20, height: 20, style: { color: iconColor, flexShrink: 0 } })}
        <span style={{
          flex: 1, minWidth: 0,
          fontFamily: mono ? M.fontMono : M.fontSans, fontSize: 16,
          color: value ? t.text : t.textFaint, letterSpacing: mono ? 2 : 0,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>{value || placeholder}</span>
        {focused && <span style={{ width: 2, height: 22, background: t.accent, flexShrink: 0 }}/>}
        {trailing}
      </div>
    </div>
  );
}

function authCta(t, disabled) {
  return {
    width: '100%', height: 52, borderRadius: 14, border: 'none',
    cursor: disabled ? 'default' : 'pointer',
    background: disabled ? t.bgSubtle : t.cta,
    color: disabled ? t.textFaint : t.ctaInk,
    fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
    boxShadow: disabled ? 'none' : M.shadow.sm,
  };
}

function authLink(t) {
  return {
    background: 'transparent', border: 'none', cursor: 'pointer', padding: 0,
    color: t.accent, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
  };
}

function VKButton({ t }) {
  return (
    <button style={{
      width: '100%', height: 52, borderRadius: 14, border: 'none', cursor: 'pointer',
      background: '#0077FF', color: '#fff',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      fontFamily: M.fontSans, fontSize: 16, fontWeight: 600,
    }}><IcVK size={22}/> Войти через VK&nbsp;ID</button>
  );
}

function AuthDivider({ t }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0' }}>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
      <span style={{ ...M.t.captionMuted(t) }}>или</span>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
    </div>
  );
}

// ── общий навбар экрана (закрыть ✕ / назад ‹) ───────────────────
function AuthNav({ t, back }) {
  return (
    <div style={{ paddingTop: 58 }}>
      <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center' }}>
        <button style={{
          width: 40, height: 40, borderRadius: 20, border: 'none',
          background: t.surface, boxShadow: M.shadow.sm, color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}>{back
          ? <IcChevronLeft width={18} height={18}/>
          : <span style={{ fontSize: 17, lineHeight: 1 }}>✕</span>}</button>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ГЛАВНЫЙ КОМПОНЕНТ
// mode: 'login' | 'register' | 'recover' | 'sent'
// ═══════════════════════════════════════════════════════════════
function IOSAuth({ dark = false, height = 844, mode = 'login', focus = null, error = false }) {
  const t = dark ? M.dark : M.light;
  const isAuth = mode === 'login' || mode === 'register';
  const back = mode === 'recover' || mode === 'sent';

  const title = {
    login: 'Вход в Машинист',
    register: 'Создать аккаунт',
    recover: 'Забыли пароль?',
    sent: 'Письмо отправлено',
  }[mode];

  const hint = {
    login: null,
    register: null,
    recover: 'Укажите почту, на которую зарегистрирован аккаунт. Пришлём письмо со ссылкой для смены пароля.',
    sent: null,
  }[mode];

  return (
    <MDevice dark={dark} height={height}>
      <AuthNav t={t} back={back}/>

      <div style={{
        padding: '8px 24px 24px', height: 'calc(100% - 110px)',
        display: 'flex', flexDirection: 'column',
      }}>
        {/* ── Шапка: лого + заголовок ─────────────────────────── */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', marginTop: 4 }}>
          {mode === 'sent' ? (
            <div style={{
              width: 72, height: 72, borderRadius: 24, background: t.accentSoft, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcMailOpen width={34} height={34}/></div>
          ) : (
            <div style={{
              width: 64, height: 64, borderRadius: 18, background: t.cta, color: t.ctaInk,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: M.fontDisplay, fontSize: 38, fontWeight: 800,
            }}>М</div>
          )}
          <div style={{
            fontFamily: M.fontDisplay, fontSize: 24, fontWeight: 800,
            color: t.text, letterSpacing: -0.5, marginTop: 16,
          }}>{title}</div>
          {hint && (
            <div style={{ ...M.t.hint(t), marginTop: 8, maxWidth: 290 }}>{hint}</div>
          )}
          {mode === 'sent' && (
            <div style={{ ...M.t.hint(t), marginTop: 8, maxWidth: 290 }}>
              Отправили ссылку для смены пароля на<br/>
              <span style={{ color: t.text, fontWeight: 600 }}>{DEMO_EMAIL}</span>.
              Перейдите по ней, чтобы задать новый пароль.
            </div>
          )}
        </div>

        {/* ── Тело экрана ──────────────────────────────────────── */}
        {isAuth && <AuthBody t={t} mode={mode} focus={focus} error={error}/>}
        {mode === 'recover' && <RecoverBody t={t} focus={focus}/>}
        {mode === 'sent' && <SentBody t={t}/>}
      </div>
    </MDevice>
  );
}

// ── ВХОД / РЕГИСТРАЦИЯ ──────────────────────────────────────────
function AuthBody({ t, mode, focus, error = false }) {
  const register = mode === 'register';
  const showErr = !register && error;
  const red = t === M.dark ? '#FF6575' : '#C81E14'; // насыщенный чистый красный — светлее в тёмной теме
  return (
    <>
      {/* Сегмент-переключатель */}
      <div style={{ marginTop: 22 }}>
        <AuthSegment t={t} value={mode}/>
      </div>

      {/* Баннер ошибки */}
      {showErr && (
        <div style={{
          marginTop: 16, display: 'flex', alignItems: 'flex-start', gap: 10,
          background: `${red}1F`, border: `1px solid ${red}57`,
          borderRadius: 14, padding: '12px 14px',
        }}>
          <IcAlert width={20} height={20} style={{ color: red, flexShrink: 0, marginTop: 1 }}/>
          <div>
            <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: red }}>
              Неверная почта или пароль
            </div>
            <div style={{ fontFamily: M.fontSans, fontSize: 13, fontWeight: 400, color: t.textMuted, marginTop: 2, lineHeight: 1.4 }}>
              Проверьте данные и попробуйте снова.
            </div>
          </div>
        </div>
      )}

      {/* Поля */}
      <div style={{ marginTop: showErr ? 16 : 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <AuthField
          t={t} icon={<IcMailP/>} label="Электронная почта"
          value={DEMO_EMAIL} placeholder="you@example.com"
          focused={focus === 'email'} error={showErr} errColor={red}
        />
        <AuthField
          t={t} icon={<IcLock/>} label="Пароль"
          value={register ? '••••••••' : '••••••••••'} placeholder="Минимум 8 символов"
          focused={focus === 'password'} error={showErr} errColor={red}
          trailing={<button style={{
            background: 'transparent', border: 'none', cursor: 'pointer', padding: 4,
            color: t.textMuted, display: 'flex', flexShrink: 0,
          }}><IcEyeOff width={20} height={20}/></button>}
        />
        {register && (
          <AuthField
            t={t} icon={<IcLock/>} label="Повторите пароль"
            value="••••••••" placeholder="Ещё раз тот же пароль"
            focused={focus === 'password2'}
          />
        )}
        {!register && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: -2 }}>
            <button style={authLink(t)}>Забыли пароль?</button>
          </div>
        )}
      </div>

      <button style={{ ...authCta(t), marginTop: register ? 18 : 16 }}>
        {register ? 'Зарегистрироваться' : 'Войти'}
      </button>

      <AuthDivider t={t}/>
      <VKButton t={t}/>

      <div style={{ flex: 1, minHeight: 12 }}/>

      {register && (
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', lineHeight: 1.45, padding: '0 6px' }}>
          Регистрируясь, вы принимаете <span style={{ color: t.accent }}>Условия</span> и{' '}
          <span style={{ color: t.accent }}>Политику&nbsp;конфиденциальности</span>
        </div>
      )}
    </>
  );
}

// ── ВОССТАНОВЛЕНИЕ: ввод почты ──────────────────────────────────
function RecoverBody({ t, focus }) {
  return (
    <>
      <div style={{ marginTop: 28 }}>
        <AuthField
          t={t} icon={<IcMailP/>} label="Электронная почта"
          value={DEMO_EMAIL} placeholder="you@example.com"
          focused={focus === 'email' || focus == null}
        />
      </div>
      <button style={{ ...authCta(t), marginTop: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9 }}>
        <IcPaperPlane width={19} height={19}/> Отправить ссылку
      </button>
      <div style={{ flex: 1 }}/>
      <button style={{
        width: '100%', padding: '13px', background: 'transparent', border: 'none', cursor: 'pointer',
        color: t.textMuted, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
      }}>Вспомнил пароль — войти</button>
    </>
  );
}

// ── ВОССТАНОВЛЕНИЕ: письмо отправлено ───────────────────────────
function SentBody({ t }) {
  return (
    <>
      <div style={{
        marginTop: 24, background: t.surface, borderRadius: 14, boxShadow: M.shadow.sm,
        border: `1px solid ${t.border}`, padding: '14px 16px',
        display: 'flex', gap: 12, alignItems: 'flex-start',
      }}>
        <div style={{ color: t.textMuted, flexShrink: 0, marginTop: 1 }}><IcMailP width={20} height={20}/></div>
        <div style={{ ...M.t.captionMuted(t), lineHeight: 1.5 }}>
          Не пришло письмо? Проверьте папку «Спам» или подождите пару минут перед повторной отправкой.
        </div>
      </div>

      <button style={{ ...authCta(t), marginTop: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9 }}>
        <IcMailOpen width={19} height={19}/> Открыть почту
      </button>

      <div style={{ textAlign: 'center', marginTop: 20 }}>
        <button style={authLink(t)}>Отправить письмо ещё раз</button>
      </div>

      <div style={{ flex: 1 }}/>
      <button style={{
        width: '100%', padding: '13px', background: 'transparent', border: 'none', cursor: 'pointer',
        color: t.textMuted, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
      }}>Вернуться ко входу</button>
    </>
  );
}

Object.assign(window, { IOSAuth, IcEye, IcEyeOff, IcPaperPlane, IcMailOpen, IcAlert });
