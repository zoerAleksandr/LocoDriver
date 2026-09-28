// ═══════════════════════════════════════════════════════════════
// ANDROID · ВХОД / РЕГИСТРАЦИЯ / ВОССТАНОВЛЕНИЕ (Material 3)
// Та же логика, что и в IOSAuth, но в Material-обвязке:
//   • ADevice + Material small top app bar (back-arrow)
//   • Material primary tabs (Вход / Регистрация) с underline-индикатором
//   • Material filled text fields (заливка + нижний индикатор, accent в фокусе)
//   • Material filled / tonal / text buttons
// mode: 'login' | 'register' | 'recover' | 'sent'
// ═══════════════════════════════════════════════════════════════

const A_DEMO_EMAIL = 's.kuznetsov@mail.ru';

// Material primary tabs (две вкладки с подчёркиванием).
function AAuthTabs({ t, value }) {
  const items = [['login', 'Вход'], ['register', 'Регистрация']];
  return (
    <div style={{ display: 'flex', borderBottom: `1px solid ${t.border}` }}>
      {items.map(([key, label]) => {
        const active = value === key;
        return (
          <div key={key} style={{
            flex: 1, position: 'relative', textAlign: 'center',
            padding: '14px 0', cursor: 'pointer',
            color: active ? t.accent : t.textMuted,
            fontSize: 15, fontWeight: active ? 700 : 500,
          }}>
            {label}
            {active && <div style={{
              position: 'absolute', left: '25%', right: '25%', bottom: -1, height: 3,
              borderRadius: '3px 3px 0 0', background: t.accent,
            }}/>}
          </div>
        );
      })}
    </div>
  );
}

// Material filled text field: заливка bgSubtle, лейбл сверху, нижний индикатор.
function AFilledField({ t, icon, label, value, placeholder, focused, mono, trailing, error, errColor }) {
  const ec = errColor || t.danger;
  const indicator = error ? ec : focused ? t.accent : t.borderStrong;
  const iconColor = error ? ec : focused ? t.accent : t.textMuted;
  return (
    <div style={{
      background: t.bgSubtle, borderRadius: '12px 12px 0 0',
      borderBottom: `2px solid ${indicator}`,
      padding: '8px 14px 8px', minHeight: 56,
      display: 'flex', alignItems: 'center', gap: 12,
    }}>
      {icon && React.cloneElement(icon, { width: 22, height: 22, style: { color: iconColor, flexShrink: 0 } })}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, fontWeight: 500, color: error ? ec : focused ? t.accent : t.textMuted }}>{label}</div>
        <div style={{
          fontFamily: mono ? M.fontMono : M.fontSans, fontSize: 16, marginTop: 2,
          color: value ? t.text : t.textFaint, letterSpacing: mono ? 2 : 0,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          display: 'flex', alignItems: 'center',
        }}>
          {value || placeholder}
          {focused && <span style={{ width: 2, height: 18, background: t.accent, marginLeft: 1 }}/>}
        </div>
      </div>
      {trailing}
    </div>
  );
}

function aAuthFilled(t, disabled) {
  return {
    width: '100%', height: 52, borderRadius: 16, border: 'none',
    cursor: disabled ? 'default' : 'pointer',
    background: disabled ? t.bgSubtle : t.cta,
    color: disabled ? t.textFaint : t.ctaInk,
    fontSize: 16, fontWeight: 600,
    boxShadow: disabled ? 'none' : '0 6px 18px rgba(0,0,0,0.16)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9,
  };
}

function AAuthDivider({ t }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0' }}>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
      <span style={{ ...M.t.captionMuted(t) }}>или</span>
      <div style={{ flex: 1, height: 1, background: t.border }}/>
    </div>
  );
}

function AVKButton({ t }) {
  return (
    <button style={{
      width: '100%', height: 52, borderRadius: 16, border: 'none', cursor: 'pointer',
      background: '#0077FF', color: '#fff',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      fontSize: 16, fontWeight: 600,
    }}><IcVK size={22}/> Войти через VK&nbsp;ID</button>
  );
}

function AndroidAuth({ dark = false, height = 844, mode = 'login', focus = null, error = false }) {
  const t = dark ? M.dark : M.light;
  const isAuth = mode === 'login' || mode === 'register';
  const back = mode === 'recover' || mode === 'sent';

  const title = { login: 'Вход в Машинист', register: 'Создать аккаунт', recover: 'Забыли пароль?', sent: 'Письмо отправлено' }[mode];
  const hint = {
    recover: 'Укажите почту, на которую зарегистрирован аккаунт. Пришлём письмо со ссылкой для смены пароля.',
  }[mode];

  return (
    <ADevice dark={dark} height={height}>
      {/* Material small top app bar */}
      <div style={{ padding: '8px 4px', display: 'flex', alignItems: 'center', minHeight: 56, background: t.bg }}>
        <button style={{
          width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }} aria-label={back ? 'Назад' : 'Закрыть'}>
          {back
            ? <IcChevronLeft width={22} height={22}/>
            : <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>}
        </button>
      </div>

      <div style={{ padding: '0 24px 24px', height: 'calc(100% - 56px)', display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        {/* Шапка: лого + заголовок */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', marginTop: 8 }}>
          {mode === 'sent' ? (
            <div style={{
              width: 72, height: 72, borderRadius: 24, background: t.accentSoft, color: t.accent,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}><IcMailOpen width={34} height={34}/></div>
          ) : (
            <div style={{
              width: 64, height: 64, borderRadius: 20, background: t.cta, color: t.ctaInk,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 38, fontWeight: 800,
            }}>М</div>
          )}
          <div style={{ fontSize: 24, fontWeight: 800, color: t.text, letterSpacing: -0.5, marginTop: 16 }}>{title}</div>
          {hint && <div style={{ ...M.t.hint(t), marginTop: 8, maxWidth: 290 }}>{hint}</div>}
          {mode === 'sent' && (
            <div style={{ ...M.t.hint(t), marginTop: 8, maxWidth: 290 }}>
              Отправили ссылку для смены пароля на<br/>
              <span style={{ color: t.text, fontWeight: 600 }}>{A_DEMO_EMAIL}</span>.
              Перейдите по ней, чтобы задать новый пароль.
            </div>
          )}
        </div>

        {isAuth && <AAuthBody t={t} mode={mode} focus={focus} error={error}/>}
        {mode === 'recover' && <ARecoverBody t={t} focus={focus}/>}
        {mode === 'sent' && <ASentBody t={t}/>}
      </div>
    </ADevice>
  );
}

function AAuthBody({ t, mode, focus, error = false }) {
  const register = mode === 'register';
  const showErr = !register && error;
  const ec = t === M.dark ? '#FF6575' : '#C81E14';
  return (
    <React.Fragment>
      <div style={{ marginTop: 22 }}>
        <AAuthTabs t={t} value={mode}/>
      </div>

      {showErr && (
        <div style={{
          marginTop: 16, display: 'flex', alignItems: 'flex-start', gap: 10,
          background: hexToRgba(ec, 0.12), border: `1px solid ${hexToRgba(ec, 0.34)}`,
          borderRadius: 14, padding: '12px 14px',
        }}>
          <IcAlert width={20} height={20} style={{ color: ec, flexShrink: 0, marginTop: 1 }}/>
          <div>
            <div style={{ fontSize: 15, fontWeight: 600, color: ec }}>Неверная почта или пароль</div>
            <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2, lineHeight: 1.4 }}>Проверьте данные и попробуйте снова.</div>
          </div>
        </div>
      )}

      <div style={{ marginTop: showErr ? 16 : 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <AFilledField
          t={t} icon={<IcMailP/>} label="Электронная почта"
          value={A_DEMO_EMAIL} placeholder="you@example.com"
          focused={focus === 'email'} error={showErr} errColor={ec}
        />
        <AFilledField
          t={t} icon={<IcLock/>} label="Пароль"
          value={register ? '••••••••' : '••••••••••'} placeholder="Минимум 8 символов"
          focused={focus === 'password'} error={showErr} errColor={ec}
          trailing={<button style={{
            background: 'transparent', border: 'none', cursor: 'pointer', padding: 4,
            color: t.textMuted, display: 'flex', flexShrink: 0,
          }}><IcEyeOff width={20} height={20}/></button>}
        />
        {register && (
          <AFilledField t={t} icon={<IcLock/>} label="Повторите пароль" value="••••••••" placeholder="Ещё раз тот же пароль" focused={focus === 'password2'}/>
        )}
        {!register && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 2 }}>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 0', color: t.accent, fontSize: 15, fontWeight: 600 }}>Забыли пароль?</button>
          </div>
        )}
      </div>

      <button style={{ ...aAuthFilled(t), marginTop: register ? 18 : 16 }}>
        {register ? 'Зарегистрироваться' : 'Войти'}
      </button>

      <AAuthDivider t={t}/>
      <AVKButton t={t}/>

      <div style={{ flex: 1, minHeight: 12 }}/>

      {register && (
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', lineHeight: 1.45, padding: '0 6px', marginTop: 14 }}>
          Регистрируясь, вы принимаете <span style={{ color: t.accent }}>Условия</span> и{' '}
          <span style={{ color: t.accent }}>Политику&nbsp;конфиденциальности</span>
        </div>
      )}
    </React.Fragment>
  );
}

function ARecoverBody({ t, focus }) {
  return (
    <React.Fragment>
      <div style={{ marginTop: 28 }}>
        <AFilledField t={t} icon={<IcMailP/>} label="Электронная почта" value={A_DEMO_EMAIL} placeholder="you@example.com" focused={focus === 'email' || focus == null}/>
      </div>
      <button style={{ ...aAuthFilled(t), marginTop: 16 }}>
        <IcPaperPlane width={19} height={19}/> Отправить ссылку
      </button>
      <div style={{ flex: 1 }}/>
      <button style={{
        width: '100%', padding: '13px', background: 'transparent', border: 'none', cursor: 'pointer',
        color: t.textMuted, fontSize: 15, fontWeight: 500,
      }}>Вспомнил пароль — войти</button>
    </React.Fragment>
  );
}

function ASentBody({ t }) {
  return (
    <React.Fragment>
      <div style={{
        marginTop: 24, background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm,
        padding: '14px 16px', display: 'flex', gap: 12, alignItems: 'flex-start',
      }}>
        <div style={{ color: t.textMuted, flexShrink: 0, marginTop: 1 }}><IcMailP width={20} height={20}/></div>
        <div style={{ ...M.t.captionMuted(t), lineHeight: 1.5 }}>
          Не пришло письмо? Проверьте папку «Спам» или подождите пару минут перед повторной отправкой.
        </div>
      </div>
      <button style={{ ...aAuthFilled(t), marginTop: 16 }}>
        <IcMailOpen width={19} height={19}/> Открыть почту
      </button>
      <div style={{ textAlign: 'center', marginTop: 20 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 0, color: t.accent, fontSize: 15, fontWeight: 600 }}>Отправить письмо ещё раз</button>
      </div>
      <div style={{ flex: 1 }}/>
      <button style={{
        width: '100%', padding: '13px', background: 'transparent', border: 'none', cursor: 'pointer',
        color: t.textMuted, fontSize: 15, fontWeight: 500,
      }}>Вернуться ко входу</button>
    </React.Fragment>
  );
}

Object.assign(window, { AndroidAuth });
