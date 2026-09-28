// flow-canvas.jsx — карта-поток экранов с логическими стрелками.
// Свободный pan/zoom холст. Дерево навигации раскладывается слоями
// (глубина → колонка), стрелки рисуются SVG-безье с подписью кнопки.
//
// API:
//   <FlowMap title="iOS · светлая" platform="ios" dark={false} build={buildFlow}/>
//
// build(platform, dark) -> { root }  где node = {
//   id, label, w, h, render:(platform,dark)=>JSX, edge:'подпись стрелки',
//   children:[...], note:'опц. подпись состояния'
// }

const FC = {
  bgLight: '#efece6',
  bgDark:  '#16140f',
  gridLight: 'rgba(20,16,10,0.05)',
  gridDark:  'rgba(255,255,255,0.05)',
  font: '-apple-system, BlinkMacSystemFont, "Inter", system-ui, sans-serif',
  mono: '"JetBrains Mono", ui-monospace, monospace',
};

// ── Раскладка дерева слоями ──────────────────────────────────
// x = глубина * шаг колонки. y — общий курсор по DFS (листья
// складываются сверху вниз, родитель центрируется по детям).
function layoutTree(root, opts = {}) {
  const COLW = opts.colw || 620;
  const ROWGAP = opts.rowgap || 120;
  const all = [];
  let cursor = 0;

  function place(node, depth, parent) {
    node._depth = depth;
    node._parent = parent || null;
    node.x = depth * COLW;
    all.push(node);
    const kids = node.children || [];
    if (kids.length === 0) {
      node.y = cursor;
      cursor += node.h + ROWGAP;
    } else {
      kids.forEach((k) => place(k, depth + 1, node));
      const top = Math.min(...kids.map((k) => k.y));
      const bot = Math.max(...kids.map((k) => k.y + k.h));
      node.y = (top + bot) / 2 - node.h / 2;
    }
  }
  place(root, 0, null);

  // нормализуем в положительные координаты
  const minY = Math.min(...all.map((n) => n.y));
  all.forEach((n) => { n.y -= minY; });

  const worldW = Math.max(...all.map((n) => n.x + n.w)) + 200;
  const worldH = Math.max(...all.map((n) => n.y + n.h)) + 200;

  // bbox поддерева для кластерных рамок (только дети корня)
  const clusters = (root.children || []).map((c) => {
    const sub = [];
    (function collect(n) { sub.push(n); (n.children || []).forEach(collect); })(c);
    const x0 = Math.min(...sub.map((n) => n.x));
    const y0 = Math.min(...sub.map((n) => n.y));
    const x1 = Math.max(...sub.map((n) => n.x + n.w));
    const y1 = Math.max(...sub.map((n) => n.y + n.h));
    return { id: c.id, label: c.clusterLabel || c.label, x0, y0, x1, y1 };
  });

  return { all, worldW, worldH, clusters };
}

// ── Стрелка: безье от правой грани родителя к левой грани ребёнка ──
function edgePath(p, c, idx, n) {
  // распределяем точки старта по правой грани родителя
  const sy = p.y + p.h * ((idx + 1) / (n + 1));
  const sx = p.x + p.w;
  const ey = c.y + c.h / 2;
  const ex = c.x;
  const dx = Math.max(60, (ex - sx) * 0.5);
  const c1x = sx + dx, c1y = sy;
  const c2x = ex - dx, c2y = ey;
  const d = `M ${sx} ${sy} C ${c1x} ${c1y}, ${c2x} ${c2y}, ${ex} ${ey}`;
  // точка для подписи (t≈0.5 кубической безье)
  const t = 0.5, mt = 1 - t;
  const lx = mt*mt*mt*sx + 3*mt*mt*t*c1x + 3*mt*t*t*c2x + t*t*t*ex;
  const ly = mt*mt*mt*sy + 3*mt*mt*t*c1y + 3*mt*t*t*c2y + t*t*t*ey;
  return { d, lx, ly, sx, sy, ex, ey };
}

function FlowMap({ title, subtitle, platform, dark, build }) {
  const data = React.useMemo(() => {
    const { root } = build(platform, dark);
    const L = layoutTree(root);
    return { root, ...L };
  }, [platform, dark, build]);

  const vpRef = React.useRef(null);
  const worldRef = React.useRef(null);
  const tf = React.useRef({ x: 80, y: 80, scale: 0.5 });

  const apply = React.useCallback(() => {
    const el = worldRef.current;
    if (el) el.style.transform = `translate3d(${tf.current.x}px,${tf.current.y}px,0) scale(${tf.current.scale})`;
  }, []);
  React.useEffect(() => { apply(); }, [apply, data]);

  React.useEffect(() => {
    const vp = vpRef.current;
    if (!vp) return;
    const zoomAt = (cx, cy, factor) => {
      const r = vp.getBoundingClientRect();
      const px = cx - r.left, py = cy - r.top, t = tf.current;
      const next = Math.min(2.5, Math.max(0.08, t.scale * factor));
      const k = next / t.scale;
      t.x = px - (px - t.x) * k; t.y = py - (py - t.y) * k; t.scale = next;
      apply();
    };
    const isWheel = (e) => e.deltaMode !== 0 || (e.deltaX === 0 && Number.isInteger(e.deltaY) && Math.abs(e.deltaY) >= 40);
    const onWheel = (e) => {
      e.preventDefault();
      if (e.ctrlKey) zoomAt(e.clientX, e.clientY, Math.exp(-e.deltaY * 0.01));
      else if (isWheel(e)) zoomAt(e.clientX, e.clientY, Math.exp(-Math.sign(e.deltaY) * 0.18));
      else { tf.current.x -= e.deltaX; tf.current.y -= e.deltaY; apply(); }
    };
    let drag = null;
    const onDown = (e) => {
      const onBg = !e.target.closest('[data-fc-node]');
      if (!(e.button === 1 || (e.button === 0 && onBg))) return;
      e.preventDefault(); vp.setPointerCapture(e.pointerId);
      drag = { id: e.pointerId, lx: e.clientX, ly: e.clientY }; vp.style.cursor = 'grabbing';
    };
    const onMove = (e) => {
      if (!drag || e.pointerId !== drag.id) return;
      tf.current.x += e.clientX - drag.lx; tf.current.y += e.clientY - drag.ly;
      drag.lx = e.clientX; drag.ly = e.clientY; apply();
    };
    const onUp = (e) => { if (!drag || e.pointerId !== drag.id) return; vp.releasePointerCapture(e.pointerId); drag = null; vp.style.cursor = ''; };
    vp.addEventListener('wheel', onWheel, { passive: false });
    vp.addEventListener('pointerdown', onDown);
    vp.addEventListener('pointermove', onMove);
    vp.addEventListener('pointerup', onUp);
    vp.addEventListener('pointercancel', onUp);
    return () => {
      vp.removeEventListener('wheel', onWheel);
      vp.removeEventListener('pointerdown', onDown);
      vp.removeEventListener('pointermove', onMove);
      vp.removeEventListener('pointerup', onUp);
      vp.removeEventListener('pointercancel', onUp);
    };
  }, [apply]);

  // «Вписать» — подгоняет масштаб под весь холст
  const fit = React.useCallback(() => {
    const vp = vpRef.current; if (!vp) return;
    const r = vp.getBoundingClientRect();
    const pad = 120;
    const s = Math.min((r.width - pad) / data.worldW, (r.height - pad) / data.worldH, 1);
    tf.current.scale = s;
    tf.current.x = (r.width - data.worldW * s) / 2;
    tf.current.y = (r.height - data.worldH * s) / 2;
    apply();
  }, [data, apply]);
  React.useEffect(() => { const id = setTimeout(fit, 60); return () => clearTimeout(id); }, [fit]);

  const arrow = dark ? 'rgba(255,255,255,0.34)' : 'rgba(20,16,10,0.3)';
  const arrowStrong = dark ? 'rgba(214,119,87,0.9)' : 'rgba(201,100,66,0.85)';
  const labelBg = dark ? '#211e18' : '#fbf9f5';
  const labelText = dark ? 'rgba(245,242,236,0.92)' : 'rgba(40,32,22,0.92)';
  const labelBorder = dark ? 'rgba(255,255,255,0.12)' : 'rgba(20,16,10,0.1)';
  const clusterStroke = dark ? 'rgba(255,255,255,0.1)' : 'rgba(20,16,10,0.09)';
  const clusterLabel = dark ? 'rgba(245,242,236,0.5)' : 'rgba(40,32,22,0.42)';
  const nodeLabel = dark ? 'rgba(245,242,236,0.9)' : 'rgba(30,24,16,0.88)';
  const noteText = dark ? 'rgba(245,242,236,0.5)' : 'rgba(40,32,22,0.5)';

  // плоский список рёбер
  const edges = [];
  data.all.forEach((p) => {
    const kids = p.children || [];
    kids.forEach((c, i) => {
      const e = edgePath(p, c, i, kids.length);
      edges.push({ key: `${p.id}->${c.id}`, ...e, label: c.edge });
    });
  });

  const gridSvg = `url("data:image/svg+xml,%3Csvg width='80' height='80' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M80 0H0v80' fill='none' stroke='${encodeURIComponent(dark ? FC.gridDark : FC.gridLight)}' stroke-width='1'/%3E%3C/svg%3E")`;

  return (
    <div ref={vpRef} style={{
      position: 'fixed', inset: 0, overflow: 'hidden', touchAction: 'none',
      background: dark ? FC.bgDark : FC.bgLight, fontFamily: FC.font, cursor: 'default',
    }}>
      {/* мировой слой */}
      <div ref={worldRef} style={{ position: 'absolute', top: 0, left: 0, transformOrigin: '0 0', willChange: 'transform' }}>
        <div style={{ position: 'absolute', inset: -4000, backgroundImage: gridSvg, backgroundSize: '80px 80px', pointerEvents: 'none' }}/>

        {/* кластерные рамки */}
        {data.clusters.map((cl) => (
          <div key={cl.id} style={{
            position: 'absolute', left: cl.x0 - 44, top: cl.y0 - 70,
            width: (cl.x1 - cl.x0) + 88, height: (cl.y1 - cl.y0) + 110,
            border: `1.5px dashed ${clusterStroke}`, borderRadius: 28, pointerEvents: 'none',
          }}>
            <div style={{
              position: 'absolute', top: 18, left: 24, fontFamily: FC.mono, fontSize: 19,
              letterSpacing: 2, textTransform: 'uppercase', color: clusterLabel, fontWeight: 600,
            }}>{cl.label}</div>
          </div>
        ))}

        {/* стрелки */}
        <svg style={{ position: 'absolute', top: 0, left: 0, width: data.worldW, height: data.worldH, pointerEvents: 'none', overflow: 'visible' }}>
          <defs>
            <marker id={`fc-arrow-${dark ? 'd' : 'l'}`} markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto">
              <path d="M0.5 0.5 L8 4.5 L0.5 8.5" fill="none" stroke={arrowStrong} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </marker>
          </defs>
          {edges.map((e) => (
            <path key={e.key} d={e.d} fill="none" stroke={arrow} strokeWidth="2"
              markerEnd={`url(#fc-arrow-${dark ? 'd' : 'l'})`}/>
          ))}
        </svg>

        {/* подписи стрелок */}
        {edges.map((e) => e.label && (
          <div key={`lbl-${e.key}`} style={{
            position: 'absolute', left: e.lx, top: e.ly, transform: 'translate(-50%,-50%)',
            background: labelBg, color: labelText, border: `1px solid ${labelBorder}`,
            borderRadius: 8, padding: '5px 10px', fontSize: 14, fontWeight: 500,
            whiteSpace: 'nowrap', boxShadow: dark ? 'none' : '0 1px 3px rgba(0,0,0,0.06)', maxWidth: 240,
          }}>{e.label}</div>
        ))}

        {/* экраны */}
        {data.all.map((n) => (
          <div key={n.id} data-fc-node={n.id} style={{ position: 'absolute', left: n.x, top: n.y, width: n.w, height: n.h }}>
            <div style={{ position: 'absolute', bottom: '100%', left: 4, marginBottom: 11, display: 'flex', alignItems: 'baseline', gap: 10, whiteSpace: 'nowrap' }}>
              <span style={{ fontSize: 19, fontWeight: 600, color: nodeLabel, letterSpacing: -0.2 }}>{n.label}</span>
              {n.note && <span style={{ fontSize: 14, color: noteText }}>{n.note}</span>}
            </div>
            <div style={{
              width: n.w, height: n.h, borderRadius: 2, overflow: 'hidden', background: 'transparent',
            }}>
              {n.render(platform, dark)}
            </div>
          </div>
        ))}
      </div>

      {/* HUD: заголовок + кнопка «Вписать» */}
      <div style={{ position: 'fixed', top: 22, left: 26, pointerEvents: 'none' }}>
        <div style={{ fontSize: 26, fontWeight: 700, letterSpacing: -0.5, color: dark ? '#F2EFE8' : '#1a150f' }}>{title}</div>
        {subtitle && <div style={{ fontSize: 15, marginTop: 3, color: dark ? 'rgba(242,239,232,0.55)' : 'rgba(26,21,15,0.5)' }}>{subtitle}</div>}
      </div>
      <button onClick={fit} style={{
        position: 'fixed', top: 24, right: 26, border: `1px solid ${labelBorder}`,
        background: labelBg, color: labelText, borderRadius: 10, padding: '9px 16px',
        fontSize: 14, fontWeight: 600, cursor: 'pointer', fontFamily: FC.font,
        boxShadow: dark ? 'none' : '0 1px 4px rgba(0,0,0,0.08)',
      }}>Вписать</button>
    </div>
  );
}

Object.assign(window, { FlowMap });
