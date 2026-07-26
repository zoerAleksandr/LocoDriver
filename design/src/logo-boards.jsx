// logo-boards.jsx — Машинист logo lock-up exploration
const { DesignCanvas, DCSection, DCArtboard } = window;

const TAG = "Для тех, у кого всё под контролем.";

// Measure the TRUE painted ink of a glyph using canvas text metrics, then
// stretch + shift it so the ink exactly fills a target width. Canvas reports
// actualBoundingBoxLeft/Right (real ink edges) for the glyph's own font + size,
// so no hand-tuned constants are needed — this stays correct if the font,
// weight or size changes. We anchor the lock-up to ONE shared width: the
// natural width of the word «МАШИНИСТ». The container shrink-wraps to that word,
// the rule is width:100% of it, and the «М» ink is scaled to match it exactly →
// glyph, rule and word share one width and one center.
// Rasterize the glyph and scan its alpha channel for the leftmost/rightmost
// painted columns — this is the TRUE rendered ink extent (what the eye sees),
// which is more reliable than font-metrics' actualBoundingBox (those can be
// conservative and leave the «М» visually narrower than the rule).
function measureInk(text, font) {
  const c = measureInk._c || (measureInk._c = document.createElement("canvas"));
  const ctx = c.getContext("2d");
  ctx.font = font;
  const m = ctx.measureText(text);
  const pad = 8;
  const w = Math.ceil(m.actualBoundingBoxLeft + m.actualBoundingBoxRight) + pad * 2;
  const asc = Math.ceil(m.actualBoundingBoxAscent) || 80;
  const desc = Math.ceil(m.actualBoundingBoxDescent) || 20;
  const h = asc + desc + pad * 2;
  c.width = w; c.height = h;
  ctx.font = font;
  ctx.textBaseline = "alphabetic";
  ctx.fillStyle = "#000";
  const penX = pad + m.actualBoundingBoxLeft;   // x where the pen sits in the canvas
  ctx.fillText(text, penX, pad + asc);
  const data = ctx.getImageData(0, 0, w, h).data;
  let min = w, max = -1;
  for (let x = 0; x < w; x++) {
    for (let y = 0; y < h; y++) {
      if (data[(y * w + x) * 4 + 3] > 16) { if (x < min) min = x; if (x > max) max = x; break; }
    }
  }
  if (max < 0) return { left: m.actualBoundingBoxLeft, width: m.actualBoundingBoxLeft + m.actualBoundingBoxRight };
  return { left: min - penX, width: max - min + 1 };   // left bearing relative to pen, true ink width
}

function fitGlyphToWidth(glyphEl, targetW) {
  if (!targetW) return;
  const cs = getComputedStyle(glyphEl);
  const font = cs.fontWeight + " " + cs.fontSize + " " + cs.fontFamily;
  const ink = measureInk(glyphEl.textContent, font);
  if (!ink.width) return;
  const s = targetW / ink.width;            // stretch ink to span the target width
  const tx = -ink.left * s;                 // shift so ink-left meets box-left (x=0)
  glyphEl.style.transformOrigin = "left center";
  glyphEl.style.transform = "translateX(" + tx + "px) scaleX(" + s + ")";
}

// Size the .mark wrapper to the word's natural width, then fit the «М» ink to it.
function syncLockup(markEl, glyphEl, wordEl) {
  if (!markEl || !glyphEl || !wordEl) return;
  const targetW = wordEl.offsetWidth;
  markEl.style.width = targetW + "px";
  fitGlyphToWidth(glyphEl, targetW);
}

function useFitGlyph(markRef, glyphRef, wordRef) {
  React.useLayoutEffect(() => {
    let cancelled = false;
    const run = () => { if (!cancelled) syncLockup(markRef.current, glyphRef.current, wordRef.current); };
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(run);
    run();
    window.addEventListener("resize", run);
    return () => { cancelled = true; window.removeEventListener("resize", run); };
  });
}

// Vertical lock-up — the «М»'s painted ink is stretched to fill its box, so the
// letter, the rule (width:100%) and the word all share one width and one center.
function Vertical({ ink = "var(--graphite)", word = "var(--graphite)", tag = true, tagColor, rule }) {
  const m = React.useRef(null), g = React.useRef(null), w = React.useRef(null);
  useFitGlyph(m, g, w);
  return (
    <div className="lk v">
      <div className="mark" ref={m}>
        <div className="glyph" ref={g} style={{ color: ink }}>М</div>
        <div className="rule" style={{ borderTopColor: rule || word }}></div>
      </div>
      <div className="word" ref={w} style={{ color: word }}>Машинист</div>
      {tag && <div className="tagline" style={{ marginTop: 22, fontSize: 14, color: tagColor }}>{TAG}</div>}
    </div>
  );
}

// Horizontal lock-up
function Horizontal({ ink = "var(--graphite)", divider = "rgba(10,14,20,0.14)", muted = "var(--muted)" }) {
  return (
    <div className="lk h">
      <div className="glyph" style={{ color: ink }}>М</div>
      <div className="stack" style={{ borderLeftColor: divider }}>
        <div className="word" style={{ color: ink }}>Машинист</div>
        <div className="tagline" style={{ color: muted }}>Всё под контролем</div>
      </div>
    </div>
  );
}

// App icon — rounded square
function AppIcon({ size = 180, bg, fg, radius = 0.225 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: size * radius, background: bg,
      display: "flex", alignItems: "center", justifyContent: "center",
      position: "relative", overflow: "hidden",
      boxShadow: "0 12px 40px rgba(10,14,20,0.18)"
    }}>
      <div className="glyph" style={{ fontSize: size * 0.62, color: fg, lineHeight: 1 }}>М</div>
    </div>
  );
}

function App() {
  return (
    <DesignCanvas title="Машинист — Логотип">

      <DCSection id="primary" title="Основной знак" subtitle="Слоган исправлен · знак «М», линия и слово выровнены по одной ширине · единый шрифт системы (Inter)">
        <DCArtboard id="v-light" label="A · Вертикальный — основной" width={460} height={560}>
          <div className="frame pad" style={{ background: "var(--surface)" }}>
            <Vertical />
            <div className="note">primary · light</div>
          </div>
        </DCArtboard>

        <DCArtboard id="v-nolock" label="B · Знак без слогана" width={460} height={560}>
          <div className="frame pad" style={{ background: "var(--surface)" }}>
            <Vertical tag={false} />
            <div className="note">signature</div>
          </div>
        </DCArtboard>
      </DCSection>

      <DCSection id="horizontal" title="Горизонтальный лок-ап" subtitle="Для шапок, навбаров, писем — компактная высота">
        <DCArtboard id="h-light" label="C · Горизонтальный" width={560} height={300}>
          <div className="frame" style={{ background: "var(--surface)" }}>
            <Horizontal />
            <div className="note">horizontal · light</div>
          </div>
        </DCArtboard>

        <DCArtboard id="h-dark" label="D · Горизонтальный — тёмный" width={560} height={300}>
          <div className="frame" style={{ background: "var(--darkbg)" }}>
            <Horizontal ink="var(--darktext)" divider="rgba(255,255,255,0.18)" muted="rgba(245,245,245,0.55)" />
            <div className="note" style={{ color: "rgba(245,245,245,0.5)" }}>horizontal · dark</div>
          </div>
        </DCArtboard>
      </DCSection>

      <DCSection id="env" title="В среде" subtitle="Тёмный фон — знак держит контраст">
        <DCArtboard id="v-dark" label="E · Вертикальный — тёмный" width={460} height={560}>
          <div className="frame pad" style={{ background: "var(--darkbg)" }}>
            <Vertical ink="var(--darktext)" word="var(--darktext)" tagColor="rgba(245,245,245,0.55)" />
            <div className="note" style={{ color: "rgba(245,245,245,0.5)" }}>primary · dark</div>
          </div>
        </DCArtboard>
      </DCSection>

      <DCSection id="icons" title="Иконка приложения" subtitle="Знак на тач-цели — графит и инверсия">
        <DCArtboard id="icon-ink" label="G · Графит" width={300} height={300}>
          <div className="frame" style={{ background: "var(--bg)" }}>
            <AppIcon bg="var(--ink)" fg="#FFFFFF" />
            <div className="note">icon · ink</div>
          </div>
        </DCArtboard>

        <DCArtboard id="icon-white" label="H · Светлая" width={300} height={300}>
          <div className="frame" style={{ background: "var(--bg)" }}>
            <AppIcon bg="#FFFFFF" fg="var(--ink)" />
            <div className="note">icon · light</div>
          </div>
        </DCArtboard>

        <DCArtboard id="favi" label="I · Мелкие размеры" width={300} height={300}>
          <div className="frame" style={{ background: "var(--bg)" }}>
            <div style={{ display: "flex", alignItems: "flex-end", gap: 20 }}>
              <AppIcon bg="var(--ink)" fg="#FFFFFF" size={72} />
              <AppIcon bg="var(--ink)" fg="#FFFFFF" size={48} />
              <AppIcon bg="var(--ink)" fg="#FFFFFF" size={28} />
              <AppIcon bg="var(--ink)" fg="#FFFFFF" size={16} />
            </div>
            <div className="note">favicon · 72 / 48 / 28 / 16</div>
          </div>
        </DCArtboard>
      </DCSection>

    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
