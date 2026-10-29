/* Local software-rendered 3D view; no graphics driver or external service needed. */
(() => {
  const G = PackingGeometry;
  const palette = [
    "#c18e5e",
    "#67a79d",
    "#87a5ce",
    "#d5b666",
    "#ac91bd",
    "#c78279",
    "#799e68",
    "#86b7c5",
  ];
  const clamp = (n, min, max) => Math.max(min, Math.min(max, n));
  function shade(color, factor) {
    const n = parseInt(color.slice(1), 16);
    return `rgb(${[n >> 16, (n >> 8) & 255, n & 255].map((v) => Math.round(clamp(v * factor, 0, 255))).join(",")})`;
  }
  class PackingViewer {
    constructor(canvas, result, onError) {
      this.canvas = canvas;
      this.result = result;
      this.ctx = canvas.getContext("2d");
      this.selected = -1;
      this.pointers = new Map();
      this.listeners = [];
      if (!this.ctx) throw Error("Canvas indisponível");
      this.onError = onError;
      this.defaultCamera();
      this.listen("pointerdown", (e) => {
        if (e.pointerType === "mouse" && e.button !== 0) return;
        e.preventDefault();
        canvas.focus({ preventScroll: true });
        canvas.setPointerCapture(e.pointerId);
        this.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
        this.pinch = this.distance();
        canvas.classList.add("dragging");
      });
      this.listen("pointermove", (e) => {
        if (!this.pointers.has(e.pointerId)) return;
        const previous = this.pointers.get(e.pointerId);
        this.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
        if (this.pointers.size > 1) {
          const distance = this.distance();
          if (this.pinch > 0) this.zoom = clamp((this.zoom * distance) / this.pinch, 0.55, 2.8);
          this.pinch = distance;
        } else {
          this.yaw += (e.clientX - previous.x) * 0.009;
          this.elevation = clamp(
            this.elevation + (e.clientY - previous.y) * 0.007,
            0.12,
            Math.PI / 2,
          );
        }
        this.schedule();
      });
      const end = (e) => {
        this.pointers.delete(e.pointerId);
        this.pinch = this.distance();
        if (!this.pointers.size) canvas.classList.remove("dragging");
      };
      this.listen("pointerup", end);
      this.listen("pointercancel", end);
      this.listen("lostpointercapture", end);
      this.listen(
        "wheel",
        (e) => {
          e.preventDefault();
          this.zoomBy(Math.exp(-e.deltaY * 0.0015));
        },
        { passive: false },
      );
      this.listen("keydown", (e) => {
        if (e.key === "ArrowLeft") this.yaw -= 0.15;
        else if (e.key === "ArrowRight") this.yaw += 0.15;
        else if (e.key === "ArrowUp")
          this.elevation = clamp(this.elevation - 0.1, 0.12, Math.PI / 2);
        else if (e.key === "ArrowDown")
          this.elevation = clamp(this.elevation + 0.1, 0.12, Math.PI / 2);
        else if (["+", "="].includes(e.key)) this.zoom = clamp(this.zoom * 1.15, 0.55, 2.8);
        else if (e.key === "-") this.zoom = clamp(this.zoom / 1.15, 0.55, 2.8);
        else if (e.key === "Home") this.defaultCamera();
        else return;
        e.preventDefault();
        this.schedule();
      });
      this.observer = new ResizeObserver(() => this.schedule());
      this.observer.observe(canvas.parentElement);
      this.schedule();
    }
    listen(name, fn, options) {
      this.canvas.addEventListener(name, fn, options);
      this.listeners.push([name, fn, options]);
    }
    distance() {
      const points = [...this.pointers.values()];
      return points.length > 1
        ? Math.hypot(points[0].x - points[1].x, points[0].y - points[1].y)
        : 0;
    }
    defaultCamera() {
      this.yaw = Math.PI / 4;
      this.elevation = 0.48;
      this.zoom = 1;
    }
    reset() {
      this.defaultCamera();
      this.schedule();
    }
    top() {
      this.elevation = Math.PI / 2;
      this.yaw = 0;
      this.zoom = 1;
      this.schedule();
    }
    zoomBy(factor) {
      this.zoom = clamp(this.zoom * factor, 0.55, 2.8);
      this.schedule();
    }
    highlight(index) {
      this.selected = this.selected === index ? -1 : index;
      this.schedule();
      return this.selected;
    }
    schedule() {
      if (this.frame || this.destroyed) return;
      this.frame = requestAnimationFrame(() => {
        this.frame = null;
        try {
          this.draw();
        } catch {
          this.onError();
        }
      });
    }
    draw() {
      const canvas = this.canvas,
        ctx = this.ctx,
        box = this.result.box;
      const width = canvas.parentElement.clientWidth,
        height = canvas.parentElement.clientHeight;
      if (!width || !height) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      if (canvas.width !== Math.round(width * dpr) || canvas.height !== Math.round(height * dpr)) {
        canvas.width = Math.round(width * dpr);
        canvas.height = Math.round(height * dpr);
      }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, width, height);
      const frame = G.fit(box, width, height, this.yaw, this.elevation, this.zoom);
      const project = (point) => {
        const p = G.project(point, box, this.yaw, this.elevation);
        return { x: frame.x + p.x * frame.scale, y: frame.y + p.y * frame.scale, depth: p.depth };
      };
      const path = (points) => {
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        points.slice(1).forEach((p) => ctx.lineTo(p.x, p.y));
        ctx.closePath();
      };
      const outer = G.corners(0, 0, 0, box.l, box.w, box.h).map(project);
      // A lightly shaded floor makes the empty space visible without hiding packages.
      path([outer[0], outer[1], outer[2], outer[3]]);
      ctx.fillStyle = "#55556640";
      ctx.fill();
      ctx.strokeStyle = "#777789";
      ctx.lineWidth = 1;
      ctx.setLineDash([4, 4]);
      G.edges.forEach(([a, b]) => {
        ctx.beginPath();
        ctx.moveTo(outer[a].x, outer[a].y);
        ctx.lineTo(outer[b].x, outer[b].y);
        ctx.stroke();
      });
      ctx.setLineDash([]);
      const surfaces = [];
      this.result.placements.forEach((item, index) => {
        const vertices = G.corners(item.x, item.y, item.z, item.l, item.w, item.h).map(project);
        for (const face of G.faces) {
          if (!G.visible(face.normal, this.yaw, this.elevation)) continue;
          const points = face.indices.map((i) => vertices[i]);
          surfaces.push({
            points,
            index,
            face,
            depth: points.reduce((sum, p) => sum + p.depth, 0) / 4,
          });
        }
      });
      surfaces.sort((a, b) => a.depth - b.depth);
      for (const surface of surfaces) {
        const { points, index, face } = surface;
        const active = this.selected === index;
        path(points);
        ctx.fillStyle = shade(palette[index % palette.length], face.shade);
        ctx.fill();
        ctx.lineWidth = active ? 2.5 : 1;
        ctx.strokeStyle = active ? "#ff405c" : "#ffffffaa";
        ctx.stroke();
        if (face.normal[2] === 1) {
          const x = points.reduce((s, p) => s + p.x, 0) / 4,
            y = points.reduce((s, p) => s + p.y, 0) / 4;
          ctx.beginPath();
          ctx.arc(x, y, active ? 12 : 10, 0, Math.PI * 2);
          ctx.fillStyle = "#fffffff0";
          ctx.fill();
          ctx.fillStyle = "#354d46";
          ctx.font = '600 11px "Segoe UI", sans-serif';
          ctx.textAlign = "center";
          ctx.textBaseline = "middle";
          ctx.fillText(String(index + 1), x, y + 0.5);
        }
      }
      // The outer volume stays transparent: the top is open and its edges show the limit.
      ctx.lineWidth = 1.4;
      ctx.strokeStyle = "#a4a4bd99";
      for (const [a, b] of G.edges) {
        ctx.beginPath();
        ctx.moveTo(outer[a].x, outer[a].y);
        ctx.lineTo(outer[b].x, outer[b].y);
        ctx.stroke();
      }
      const cm = (value) =>
        (value / 10).toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + " cm";
      const labels = [
        { a: 0, b: 1, text: cm(box.l), dx: 0, dy: 17 },
        { a: 1, b: 2, text: cm(box.w), dx: 20, dy: 9 },
        { a: 3, b: 7, text: cm(box.h), dx: -24, dy: 0 },
      ];
      ctx.font = '11px "Segoe UI", sans-serif';
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      for (const label of labels) {
        const a = outer[label.a],
          b = outer[label.b];
        if (Math.hypot(a.x - b.x, a.y - b.y) < 15) continue;
        const x = (a.x + b.x) / 2 + label.dx,
          y = (a.y + b.y) / 2 + label.dy;
        const tw = ctx.measureText(label.text).width;
        ctx.fillStyle = "#20202bf0";
        ctx.fillRect(x - tw / 2 - 4, y - 8, tw + 8, 16);
        ctx.fillStyle = "#e0dfe9";
        ctx.fillText(label.text, x, y);
      }
    }
    destroy() {
      this.destroyed = true;
      cancelAnimationFrame(this.frame);
      this.observer.disconnect();
      for (const [name, fn, options] of this.listeners)
        this.canvas.removeEventListener(name, fn, options);
      this.pointers.clear();
    }
  }
  globalThis.Packing3D = {
    props: { result: { type: Object, required: true } },
    data: () => ({ selected: -1, failed: false, palette }),
    mounted() {
      try {
        this._viewer = new PackingViewer(
          this.$refs.canvas,
          this.result,
          () => (this.failed = true),
        );
      } catch {
        this.failed = true;
      }
    },
    beforeUnmount() {
      this._viewer?.destroy();
    },
    methods: {
      zoom(factor) {
        this._viewer?.zoomBy(factor);
      },
      reset() {
        this._viewer?.reset();
      },
      top() {
        this._viewer?.top();
      },
      select(index) {
        this.selected = this._viewer?.highlight(index) ?? -1;
      },
      cm(value) {
        return (value / 10).toLocaleString("pt-BR", { maximumFractionDigits: 1 });
      },
    },
    template: `<section class="packing-3d card"><div class="scene-heading"><div><h2>Como fica dentro da caixa</h2><p>Arraste para girar. Role ou use + / − para aproximar.</p></div><div class="scene-controls"><button type="button" class="button secondary" @click="zoom(1.2)" aria-label="Aproximar modelo 3D">＋</button><button type="button" class="button secondary" @click="zoom(1/1.2)" aria-label="Afastar modelo 3D">−</button><button type="button" class="button secondary" @click="top">Vista superior</button><button type="button" class="button secondary" @click="reset">Recentrar</button></div></div><div class="scene-stage"><canvas ref="canvas" tabindex="0" role="img" aria-label="Modelo 3D das embalagens dentro da caixa sugerida. Arraste para girar ou use as setas do teclado; mais e menos ajustam a aproximação.">Visualização 3D das embalagens.</canvas><p v-if="failed" class="scene-failed">Não foi possível abrir o modelo 3D neste navegador.</p></div><div class="scene-legend" aria-label="Embalagens no modelo 3D"><button v-for="(p,index) in result.placements" :key="index" :class="{selected:selected===index}" :aria-pressed="selected===index" @click="select(index)"><span class="scene-swatch" :style="{background:palette[index%palette.length]}">{{index+1}}</span><span><strong>{{p.name}}</strong><small>{{cm(p.l)}} × {{cm(p.w)}} × {{cm(p.h)}} cm</small></span></button></div><p class="scene-note">Os blocos representam as embalagens externas. O espaço sem blocos fica livre para bolha.<span v-if="result.demo"> Medidas de exemplo.</span></p></section>`,
  };
})();
