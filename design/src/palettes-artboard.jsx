// Palette showcase — compare color variants side by side

function PaletteCard({ paletteKey }) {
  const p = M_PALETTES[paletteKey];
  const theme = makeTheme(paletteKey);
  const L = theme.light;
  const D = theme.dark;
  return (
    <div style={{
      width: 420, background: '#fff', borderRadius: 20, overflow: 'hidden',
      boxShadow: M.shadow.md, fontFamily: M.fontSans,
    }}>
      {/* header */}
      <div style={{ padding: '20px 24px 16px', borderBottom: '1px solid rgba(0,0,0,0.06)' }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 11, color: 'rgba(0,0,0,0.5)', letterSpacing: 1.4 }}>
          ВАРИАНТ · {paletteKey.toUpperCase()}
        </div>
        <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -.3, marginTop: 2 }}>{p.name}</div>
        <div style={{ fontSize: 13, color: 'rgba(0,0,0,0.55)', marginTop: 4 }}>{p.note}</div>
      </div>

      {/* light preview */}
      <div style={{ background: L.bg, padding: 20 }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ color: L.text }}><IcLogo size={28}/></span>
            <span style={{ fontSize: 16, fontWeight: 700, color: L.text }}>Машинист</span>
          </div>
          <span style={{ fontSize: 11, fontFamily: M.fontMono, color: L.textMuted, letterSpacing: 1.2 }}>LIGHT</span>
        </div>
        <MiniPreview t={L}/>
        <SwatchRow swatches={[
          ['bg', L.bg], ['surface', L.surface], ['text', L.text], ['accent', L.accent],
        ]}/>
      </div>

      {/* dark preview */}
      <div style={{ background: D.bg, padding: 20 }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ color: D.text }}><IcLogo size={28}/></span>
            <span style={{ fontSize: 16, fontWeight: 700, color: D.text }}>Машинист</span>
          </div>
          <span style={{ fontSize: 11, fontFamily: M.fontMono, color: D.textMuted, letterSpacing: 1.2 }}>DARK</span>
        </div>
        <MiniPreview t={D}/>
        <SwatchRow swatches={[
          ['bg', D.bg], ['surface', D.surface], ['text', D.text], ['accent', D.accent],
        ]}/>
      </div>
    </div>
  );
}

function MiniPreview({ t }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 14, padding: 16, marginBottom: 12,
      boxShadow: t === M.light ? M.shadow.sm : 'none',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <div style={{ fontSize: 10, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2 }}>ОТРАБОТАНО</div>
          <div style={{ fontSize: 32, fontWeight: 800, fontFamily: M.fontMono, color: t.text, letterSpacing: -1, lineHeight: 1 }}>
            205:05
          </div>
        </div>
        <div style={{
          background: t.accentSoft, color: t.accent, padding: '4px 10px', borderRadius: 999,
          fontSize: 12, fontFamily: M.fontMono, fontWeight: 700,
        }}>+30:05</div>
      </div>
      <div style={{ height: 4, background: t.bgSubtle, borderRadius: 2, marginTop: 12 }}>
        <div style={{ width: '90%', height: '100%', background: t.accent, borderRadius: 2 }}/>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <button style={{
          background: t.accent, color: t.accentInk, border: 'none',
          padding: '8px 14px', borderRadius: 10, fontSize: 13, fontWeight: 600, fontFamily: M.fontSans,
        }}>Сохранить</button>
        <button style={{
          background: 'transparent', color: t.text, border: `1px solid ${t.borderStrong}`,
          padding: '8px 14px', borderRadius: 10, fontSize: 13, fontWeight: 600, fontFamily: M.fontSans,
        }}>+ Локомотив</button>
      </div>
    </div>
  );
}

function SwatchRow({ swatches }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6 }}>
      {swatches.map(([n, hex]) => (
        <div key={n} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <div style={{
            height: 28, borderRadius: 6, background: hex,
            boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.08)',
          }}/>
          <div style={{ fontSize: 9, fontFamily: M.fontMono, color: 'rgba(0,0,0,0.5)', letterSpacing: .3 }}>
            {n} · {hex.length > 10 ? 'rgba' : hex}
          </div>
        </div>
      ))}
    </div>
  );
}

// Logo artboard — the new monogram «М» (Inter Black) + lock-up. Two colors only.
// Vertical lock-up — the SAME resource used in «Машинист — Логотип».
// «М», the rule, and the word «МАШИНИСТ» share ONE width and one center.
// The true painted ink of the «М» is measured by rasterizing the glyph and scanning
// its alpha channel for the leftmost/rightmost painted columns (no hand-tuned font
// constants — stays correct across font/weight/size). The lock-up is anchored to the
// word's natural width: the container is sized to the word, the rule is 100% of it,
// and the «М» ink is stretched (scaleX) + shifted to span exactly that width.
function measureLogoInk(text, font) {
  const c = measureLogoInk._c || (measureLogoInk._c = document.createElement('canvas'));
  const ctx = c.getContext('2d');
  ctx.font = font;
  const m = ctx.measureText(text);
  const pad = 8;
  const w = Math.ceil(m.actualBoundingBoxLeft + m.actualBoundingBoxRight) + pad * 2;
  const asc = Math.ceil(m.actualBoundingBoxAscent) || 80;
  const desc = Math.ceil(m.actualBoundingBoxDescent) || 20;
  const h = asc + desc + pad * 2;
  c.width = w; c.height = h;
  ctx.font = font;
  ctx.textBaseline = 'alphabetic';
  ctx.fillStyle = '#000';
  const penX = pad + m.actualBoundingBoxLeft;
  ctx.fillText(text, penX, pad + asc);
  const data = ctx.getImageData(0, 0, w, h).data;
  let min = w, max = -1;
  for (let x = 0; x < w; x++) {
    for (let y = 0; y < h; y++) {
      if (data[(y * w + x) * 4 + 3] > 16) { if (x < min) min = x; if (x > max) max = x; break; }
    }
  }
  if (max < 0) return { left: m.actualBoundingBoxLeft, width: m.actualBoundingBoxLeft + m.actualBoundingBoxRight };
  return { left: min - penX, width: max - min + 1 };
}

function JustifiedLockup({ ink, glyphSize = 150 }) {
  const mark = React.useRef(null), g = React.useRef(null), w = React.useRef(null);
  React.useLayoutEffect(() => {
    let cancelled = false;
    const fit = () => {
      if (cancelled || !mark.current || !g.current || !w.current) return;
      const targetW = w.current.offsetWidth;
      if (!targetW) return;
      mark.current.style.width = targetW + 'px';
      const cs = getComputedStyle(g.current);
      const font = cs.fontWeight + ' ' + cs.fontSize + ' ' + cs.fontFamily;
      const ink2 = measureLogoInk(g.current.textContent, font);
      if (!ink2.width) return;
      const s = targetW / ink2.width;
      const tx = -ink2.left * s;
      g.current.style.transformOrigin = 'left center';
      g.current.style.transform = 'translateX(' + tx + 'px) scaleX(' + s + ')';
    };
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(fit);
    fit();
    window.addEventListener('resize', fit);
    return () => { cancelled = true; window.removeEventListener('resize', fit); };
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div ref={mark} style={{ display: 'block' }}>
        <div ref={g} style={{
          display: 'block', fontFamily: M.fontSans, fontWeight: 900, fontSize: glyphSize,
          lineHeight: 0.8, letterSpacing: 0, color: ink,
        }}>М</div>
        <div style={{ width: '100%', borderTop: '3px solid ' + ink, marginTop: 14 }}></div>
      </div>
      <div ref={w} style={{
        fontFamily: M.fontSans, fontWeight: 600, fontSize: 22, marginTop: 14,
        textTransform: 'uppercase', whiteSpace: 'nowrap', textAlign: 'center', color: ink,
      }}>Машинист</div>
    </div>
  );
}

function LogoArtboard() {
  const t = M.light;
  const INK = t.text;        // графит
  const PAPER = '#FFFFFF';   // бумага
  return (
    <div style={{
      width: 1400, minHeight: 1100, background: t.bg, padding: 56, fontFamily: M.fontSans,
      color: t.text, boxSizing: 'border-box',
    }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 2, marginBottom: 8 }}>ЛОГОТИП</div>
      <div style={{ fontSize: 32, fontWeight: 800, letterSpacing: -.8, marginBottom: 8 }}>Монограмма «М»</div>
      <div style={{ fontSize: 15, color: t.textMuted, maxWidth: 680, marginBottom: 36 }}>
        Монолитная «М» в Inter Black. Два цвета — графит и бумага. Знак и слово из одного шрифта,
        слоган исправлен. Работает от фавикона до вывески.
      </div>

      {/* hero — mark on ink + lock-up on paper */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <div style={{
          background: INK, borderRadius: 24, padding: 48, minHeight: 320,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{ fontFamily: M.fontSans, fontWeight: 900, fontSize: 200, lineHeight: 0.8, letterSpacing: -8, color: PAPER }}>М</div>
        </div>
        <div style={{
          background: PAPER, borderRadius: 24, padding: 48, minHeight: 320, boxShadow: M.shadow.sm,
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 0,
        }}>
          <JustifiedLockup ink={INK} glyphSize={150}/>
          <div style={{ fontSize: 14, color: t.textMuted, marginTop: 22 }}>Для тех, у кого всё под контролем.</div>
        </div>
      </div>

      {/* size scale */}
      <div style={{ background: t.surface, borderRadius: 20, padding: 36, marginBottom: 20, boxShadow: M.shadow.sm }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.2, marginBottom: 24 }}>
          ШКАЛА · РАБОТАЕТ В ЛЮБОМ РАЗМЕРЕ
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 40 }}>
          {[96, 64, 44, 28, 18].map(s => (
            <div key={s} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
              <div style={{ color: INK }}><IcLogo size={s}/></div>
              <div style={{ fontFamily: M.fontMono, fontSize: 10, color: t.textMuted }}>{s}</div>
            </div>
          ))}
        </div>
      </div>

      {/* app tiles — two colors */}
      <div style={{ background: t.surface, borderRadius: 20, padding: 32, boxShadow: M.shadow.sm }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.2, marginBottom: 20 }}>
          ИКОНКА ПРИЛОЖЕНИЯ · iOS · ANDROID · FAVICON
        </div>
        <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
          <div style={{
            width: 120, height: 120, borderRadius: 28, background: INK,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><IcLogo size={84} color={PAPER}/></div>
          <div style={{
            width: 120, height: 120, borderRadius: 28, background: PAPER,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '1px solid ' + t.border,
          }}><IcLogo size={84} color={INK}/></div>
          <div style={{
            width: 120, height: 120, borderRadius: '50%', background: INK,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><IcLogo size={72} color={PAPER}/></div>
          <div style={{ flex: 1, paddingLeft: 12, fontSize: 13, color: t.textMuted, lineHeight: 1.5 }}>
            Графит на бумаге и наоборот. Круглая — для Android adaptive / аватара.<br/>
            <span style={{ fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1 }}>
              один знак &lt;IcLogo/&gt; — меняется везде разом
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

function PalettesArtboard() {
  const t = M.light;
  return (
    <div style={{
      width: 1400, minHeight: 1900, background: t.bg, padding: 56,
      fontFamily: M.fontSans, color: t.text, boxSizing: 'border-box',
    }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.accent, letterSpacing: 2, marginBottom: 8 }}>ПАЛИТРЫ</div>
      <div style={{ fontSize: 32, fontWeight: 800, letterSpacing: -.8, marginBottom: 8 }}>Четыре настроения</div>
      <div style={{ fontSize: 15, color: t.textMuted, maxWidth: 680, marginBottom: 36 }}>
        Один и тот же интерфейс в разных цветовых режимах. Скажите, какой ближе —
        и я перекрашу весь проект (iOS, Android и лендинг) одной командой.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 20, justifyContent: 'center' }}>
        {['gold', 'red', 'avito', 'navy', 'orange', 'forest'].map(k => (
          <div key={k} style={{ display: 'flex', justifyContent: 'center' }}>
            <PaletteCard paletteKey={k}/>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { PalettesArtboard, LogoArtboard, PaletteCard, JustifiedLockup });
