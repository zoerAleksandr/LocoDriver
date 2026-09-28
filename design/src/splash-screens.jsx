// Splash screens — Машинист
// Centered vertical logo lock-up (the SAME resource as «Логотип»: window.JustifiedLockup),
// slogan dropped to the bottom of the screen. iOS + Android, light + dark.
// On-brand two-color treatment: paper background + graphite logo (light),
// graphite background + paper logo (dark).

const SPLASH_SLOGAN = 'Для тех, у кого всё под контролем.';

// Shared splash body — fills the device, logo dead-center, slogan pinned low.
function SplashBody({ dark, glyphSize = 132 }) {
  const ink = dark ? '#0A0E14' : '#FFFFFF';   // background
  const mark = dark ? '#F5F5F5' : '#0A0E14';  // logo color
  const Lockup = window.JustifiedLockup;
  return (
    <div style={{
      position: 'absolute', inset: 0, background: ink,
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      fontFamily: M.fontSans,
    }}>
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%' }}>
        <Lockup ink={mark} glyphSize={glyphSize}/>
      </div>
      <div style={{
        paddingBottom: 64, fontSize: 15, fontWeight: 500, letterSpacing: 0.1,
        color: dark ? 'rgba(245,245,245,0.62)' : 'rgba(10,14,20,0.55)',
        textAlign: 'center', maxWidth: 280,
      }}>
        {SPLASH_SLOGAN}
      </div>
    </div>
  );
}

// ── iOS splash — uses the shared MDevice frame ──
function IOSSplash({ dark = false }) {
  const MDevice = window.MDevice;
  return (
    <MDevice dark={dark}>
      <SplashBody dark={dark} glyphSize={132}/>
    </MDevice>
  );
}

// ── Android splash — Material device frame (mirrors ADevice in android-screens) ──
function AndroidSplashStatus({ dark }) {
  const c = dark ? '#F5F5F5' : '#0A0E14';
  return (
    <div style={{
      height: 36, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 20px', position: 'relative', flexShrink: 0, zIndex: 10,
    }}>
      <div style={{ fontSize: 14, fontWeight: 500, color: c, fontFamily: M.fontSans }}>15:13</div>
      <div style={{
        position: 'absolute', left: '50%', top: 8, transform: 'translateX(-50%)',
        width: 20, height: 20, borderRadius: 10, background: dark ? '#000' : 'rgba(0,0,0,0.85)',
      }}></div>
      <div style={{ display: 'flex', gap: 6, color: c }}>
        <svg width="14" height="14" viewBox="0 0 16 16"><path d="M8 13.3L.67 5.97a10.37 10.37 0 0114.66 0L8 13.3z" fill="currentColor"/></svg>
        <svg width="14" height="14" viewBox="0 0 16 16"><rect x="3.75" y="2" width="8.5" height="13" rx="1.5" fill="currentColor"/><rect x="5.5" y="0.9" width="5" height="2" rx="0.5" fill="currentColor"/></svg>
      </div>
    </div>
  );
}

function AndroidSplash({ dark = false, width = 390, height = 844 }) {
  const ink = dark ? '#0A0E14' : '#FFFFFF';
  return (
    <div style={{
      width, height, borderRadius: 32, overflow: 'hidden',
      background: ink, border: '6px solid ' + (dark ? '#2a2a2a' : 'rgba(0,0,0,0.3)'),
      boxShadow: '0 40px 80px rgba(0,0,0,0.22)',
      fontFamily: M.fontSans, display: 'flex', flexDirection: 'column',
      boxSizing: 'border-box', position: 'relative',
    }}>
      <AndroidSplashStatus dark={dark}/>
      <div style={{ flex: 1, position: 'relative' }}>
        <SplashBody dark={dark} glyphSize={132}/>
      </div>
      <div style={{ height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, position: 'relative', zIndex: 10 }}>
        <div style={{ width: 120, height: 3, borderRadius: 2, background: dark ? 'rgba(245,245,245,0.6)' : 'rgba(10,14,20,0.4)' }}></div>
      </div>
    </div>
  );
}

Object.assign(window, { IOSSplash, AndroidSplash });
