/* ── Navbar scroll shadow ───────────────────────────── */
const nav = document.querySelector('nav');
window.addEventListener('scroll', () => {
  nav?.classList.toggle('scrolled', window.scrollY > 10);
}, { passive: true });

/* ── Active nav link ─────────────────────────────────── */
(function() {
  const path = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-links a').forEach(a => {
    const href = a.getAttribute('href').split('/').pop();
    if (href === path || (path === '' && href === 'index.html')) {
      a.classList.add('active');
    }
  });
})();

/* ── IntersectionObserver: fade-in + arch layers ────── */
const io = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      e.target.classList.add('visible');
      io.unobserve(e.target);
    }
  });
}, { threshold: 0.12 });

document.querySelectorAll('.fade-in, .arch-layer').forEach(el => io.observe(el));

/* ── Accordion for architecture layers ───────────────── */
document.querySelectorAll('.arch-layer-header').forEach(header => {
  header.addEventListener('click', () => {
    const layer = header.closest('.arch-layer');
    layer.classList.toggle('open');
  });
});

/* ─────────────────────────────────────────────────────
   TREE ANIMATOR  (used on algorithms.html)
   Draws and animates a minimax / alpha-beta search tree
   onto a <canvas> element.
───────────────────────────────────────────────────── */
class TreeAnimator {
  constructor(canvasId, nodes, edges, steps, stepDelay = 550) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) return;
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width  = this.canvas.offsetWidth  * dpr;
    this.canvas.height = this.canvas.offsetHeight * dpr;
    this.ctx = this.canvas.getContext('2d');
    this.ctx.scale(dpr, dpr);
    this.W = this.canvas.offsetWidth;
    this.H = this.canvas.offsetHeight;

    this.nodes     = nodes;
    this.edges     = edges;
    this.steps     = steps;
    this.stepDelay = stepDelay;
    this.stepIdx   = 0;
    this.state     = {}; // key → 'idle'|'active'|'pruned'|'best'
    this.labels    = {}; // key → override label
    Object.keys(nodes).forEach(k => { this.state[k] = 'idle'; });

    this.draw();
    this._schedule();
  }

  _schedule() {
    setTimeout(() => {
      this._applyStep(this.steps[this.stepIdx]);
      this.draw();
      this.stepIdx = (this.stepIdx + 1) % this.steps.length;
      this._schedule();
    }, this.stepDelay);
  }

  _applyStep(s) {
    if (!s) return;
    switch (s.t) {
      case 'activate': this.state[s.n]  = 'active'; break;
      case 'prune':    this.state[s.n]  = 'pruned'; break;
      case 'best':     this.state[s.n]  = 'best';   break;
      case 'label':    this.labels[s.n] = s.v;      break;
      case 'reset':
        Object.keys(this.nodes).forEach(k => { this.state[k] = 'idle'; delete this.labels[k]; });
        break;
    }
  }

  draw() {
    const { ctx, W, H } = this;
    ctx.clearRect(0, 0, W, H);
    // scale from design space 460×210 to actual canvas
    const sx = W / 460, sy = H / 210;

    const color = s =>
      s === 'active' ? '#d4a853' :
      s === 'pruned' ? '#f85149' :
      s === 'best'   ? '#3fb950' : '#21262d';

    const strokeCol = s =>
      s === 'idle' ? '#30363d' : color(s);

    // edges
    this.edges.forEach(([a, b]) => {
      const na = this.nodes[a], nb = this.nodes[b];
      ctx.beginPath();
      ctx.moveTo(na.x * sx, na.y * sy);
      ctx.lineTo(nb.x * sx, nb.y * sy);
      const stateA = this.state[a], stateB = this.state[b];
      ctx.strokeStyle = (stateA === 'best' && stateB === 'best') ? '#3fb950'
        : (stateB === 'pruned') ? 'rgba(248,81,73,.35)' : '#30363d';
      ctx.lineWidth = (stateA === 'best' && stateB === 'best') ? 2.5 : 1.5;
      ctx.stroke();
    });

    // nodes
    Object.entries(this.nodes).forEach(([k, n]) => {
      const s = this.state[k];
      const r = n.leaf ? 15 : 19;
      const x = n.x * sx, y = n.y * sy;

      // glow
      if (s !== 'idle') {
        ctx.beginPath(); ctx.arc(x, y, r + 5, 0, Math.PI * 2);
        ctx.fillStyle = color(s).replace(')', ',.2)').replace('rgb', 'rgba');
        ctx.fill();
      }

      // fill
      ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.fillStyle = color(s);
      ctx.fill();
      ctx.strokeStyle = strokeCol(s);
      ctx.lineWidth = 2;
      ctx.stroke();

      // pruned X
      if (s === 'pruned') {
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 1.5;
        const d = 5;
        ctx.beginPath(); ctx.moveTo(x-d,y-d); ctx.lineTo(x+d,y+d); ctx.stroke();
        ctx.beginPath(); ctx.moveTo(x+d,y-d); ctx.lineTo(x-d,y+d); ctx.stroke();
      }

      // label
      const lbl = this.labels[k] || n.label;
      ctx.fillStyle = s === 'idle' ? '#8b949e' : s === 'pruned' ? '#fff' : '#0d1117';
      ctx.font = `bold ${n.leaf ? 10 : 9}px sans-serif`;
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(lbl, x, y);

      // type tag below internal nodes
      if (!n.leaf && n.type) {
        ctx.fillStyle = '#8b949e';
        ctx.font = '7px sans-serif';
        ctx.fillText(n.type, x, y + r + 9);
      }
    });
  }
}

/* ─────────────────────────────────────────────────────
   RANDOM VISUALISATION  — shuffle move cards
───────────────────────────────────────────────────── */
function initRandomViz(containerId) {
  const c = document.getElementById(containerId);
  if (!c) return;
  const cards = [...c.querySelectorAll('.move-card')];
  let sel = -1;
  function tick() {
    cards.forEach(el => el.classList.remove('selected'));
    const next = Math.floor(Math.random() * cards.length);
    sel = next;
    cards[sel].classList.add('selected');
  }
  tick();
  setInterval(tick, 1600);
}

/* ─────────────────────────────────────────────────────
   GREEDY BAR CHART ANIMATION
───────────────────────────────────────────────────── */
function initGreedyViz(containerId) {
  const c = document.getElementById(containerId);
  if (!c) return;
  const bars = [...c.querySelectorAll('.bar')];
  const heights = [8, 14, 96, 3, 20]; // % of max
  const bestIdx = 2;

  function animate() {
    bars.forEach((b, i) => {
      b.style.height = '0';
      b.classList.remove('best');
    });
    setTimeout(() => {
      bars.forEach((b, i) => {
        b.style.height = heights[i] + '%';
        if (i === bestIdx) setTimeout(() => b.classList.add('best'), 900);
      });
    }, 300);
  }
  animate();
  setInterval(animate, 3500);
}

/* ─────────────────────────────────────────────────────
   ITERATIVE DEEPENING ANIMATION
───────────────────────────────────────────────────── */
function initIDViz(containerId) {
  const c = document.getElementById(containerId);
  if (!c) return;
  const rows = [
    { bar: c.querySelector('#id-d1'), best: c.querySelector('#id-best1'), label:'e4',   ms:  80 },
    { bar: c.querySelector('#id-d2'), best: c.querySelector('#id-best2'), label:'Nf3',  ms: 250 },
    { bar: c.querySelector('#id-d3'), best: c.querySelector('#id-best3'), label:'Nf3',  ms: 700 },
    { bar: c.querySelector('#id-d4'), best: c.querySelector('#id-best4'), label:'d4',   ms:1800 },
  ];
  const timer = c.querySelector('#id-timer');

  function runCycle() {
    rows.forEach(r => {
      if (!r.bar) return;
      r.bar.style.width = '0';
      r.bar.className = 'id-bar';
      if (r.best) r.best.textContent = '';
    });
    if (timer) timer.textContent = '2000 ms';

    let elapsed = 0;
    const tick = setInterval(() => {
      elapsed += 100;
      if (timer) timer.textContent = Math.max(0, 2000 - elapsed) + ' ms';
    }, 100);

    rows.forEach((r, i) => {
      const delay = i === 0 ? 200 : i === 1 ? 500 : i === 2 ? 1000 : 1500;
      const doneAt = delay + 400;
      setTimeout(() => {
        if (!r.bar) return;
        r.bar.style.width = (i === 3 ? '85' : '100') + '%';
        r.bar.className = 'id-bar' + (i === 3 ? ' partial' : ' done');
      }, delay);
      if (i < 3) {
        setTimeout(() => { if (r.best) r.best.textContent = r.label; }, doneAt);
      } else {
        // depth 4 cut off by timer
        setTimeout(() => {
          if (r.bar) r.bar.style.width = '85%';
        }, 1500);
        setTimeout(() => {
          if (r.best) r.best.textContent = '⚡ cut';
          clearInterval(tick);
          if (timer) timer.textContent = '0 ms → return d3 best';
        }, 2000);
      }
    });

    setTimeout(runCycle, 4000);
  }
  runCycle();
}

/* expose for inline scripts */
window.TreeAnimator   = TreeAnimator;
window.initRandomViz  = initRandomViz;
window.initGreedyViz  = initGreedyViz;
window.initIDViz      = initIDViz;
