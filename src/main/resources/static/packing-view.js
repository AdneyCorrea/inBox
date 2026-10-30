// Projeção de coordenadas 3D em canvas, sem bibliotecas ou acesso à internet.
const PackingView = {
  props: ['result'],
  data() {
    return {
      yaw: -0.65,
      pitch: 0.55,
      zoom: 1,
      visible: this.result.posicoes.length,
      dragging: false,
      lastX: 0,
      lastY: 0
    };
  },
  template: `
    <section class="panel packing-view">
      <div class="section-heading"><div><h2>Embalagem 3D</h2></div></div>
      <div class="view-controls">
        <button class="text-button" @click="reset">Restaurar</button>
        <button class="text-button" @click="topView">TopView</button>
      </div>
      <canvas ref="canvas" tabindex="0" role="img" aria-label="Vista tridimensional. Arraste ou use as setas para girar. Scroll ou teclas + e - ajustam o zoom. Produtos e medidas na tabela abaixo."
        @wheel.prevent="onWheel" @keydown="zoomKey"
        @pointerdown="start" @pointermove="move" @pointerup="end" @pointercancel="end"
        @keydown.left.prevent="rotate(-0.15)" @keydown.right.prevent="rotate(0.15)"
        @keydown.up.prevent="tilt(0.1)" @keydown.down.prevent="tilt(-0.1)"></canvas>
      <label class="reveal">Unidades visíveis: {{ visible }} de {{ result.posicoes.length }}
        <input type="range" min="0" :max="result.posicoes.length" v-model.number="visible" @input="draw">
      </label>
        <div class="position-table"><table><thead><tr><th>Item</th><th>Produto</th><th>C × L × A (cm)</th></tr></thead>
          <tbody><tr v-for="(p, i) in result.posicoes" :key="i"><td><span class="color-key" :style="{ background: color(p.produtoId) }"></span>{{ i + 1 }}</td><td>{{ p.nome }}</td><td>{{ cm(p.comprimento) }} × {{ cm(p.largura) }} × {{ cm(p.altura) }}</td></tr></tbody>
        </table></div>
    </section>`,
  mounted() {
    this.observer = new ResizeObserver(() => this.draw());
    this.observer.observe(this.$refs.canvas);
    this.draw();
  },
  beforeUnmount() {
    this.observer.disconnect();
  },
  watch: {
    result() {
      this.visible = this.result.posicoes.length;
      this.$nextTick(() => this.draw());
    }
  },
  methods: {
    cm(value) {
      return (value / 10).toLocaleString('pt-BR');
    },
    color(id) {
      const ids = [...new Set(this.result.posicoes.map((p) => p.produtoId))];
      return `hsl(${(ids.indexOf(id) * 137.5 + 22) % 360} 58% 58%)`;
    },
    rotate(amount) {
      this.yaw += amount;
      this.draw();
    },
    tilt(amount) {
      this.pitch = Math.max(0.1, Math.min(Math.PI / 2, this.pitch + amount));
      this.draw();
    },
    changeZoom(amount) {
      this.zoom = Math.max(0.4, Math.min(2.2, this.zoom + amount));
      this.draw();
    },
    onWheel(event) {
      const unit =
        event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? this.$refs.canvas.clientHeight : 1;
      const delta = Math.max(-100, Math.min(100, event.deltaY * unit));
      this.changeZoom(this.zoom * (Math.exp(-delta * 0.002) - 1));
    },
    zoomKey(event) {
      if (['+', '=', '-'].includes(event.key)) {
        event.preventDefault();
        this.changeZoom(event.key === '-' ? -0.15 : 0.15);
      }
    },
    reset() {
      this.yaw = -0.65;
      this.pitch = 0.55;
      this.zoom = 1;
      this.draw();
    },
    topView() {
      this.yaw = 0;
      this.pitch = Math.PI / 2;
      this.draw();
    },
    start(event) {
      this.dragging = true;
      this.lastX = event.clientX;
      this.lastY = event.clientY;
      event.target.setPointerCapture(event.pointerId);
    },
    move(event) {
      if (!this.dragging) return;
      this.yaw += (event.clientX - this.lastX) * 0.008;
      this.pitch = Math.max(
        0.1,
        Math.min(Math.PI / 2, this.pitch + (event.clientY - this.lastY) * 0.008)
      );
      this.lastX = event.clientX;
      this.lastY = event.clientY;
      this.draw();
    },
    end() {
      this.dragging = false;
    },
    draw() {
      const canvas = this.$refs.canvas;
      if (!canvas) return;
      const width = canvas.clientWidth,
        height = canvas.clientHeight;
      if (!width || !height) return;
      const ratio = window.devicePixelRatio || 1;
      canvas.width = Math.round(width * ratio);
      canvas.height = Math.round(height * ratio);
      const ctx = canvas.getContext('2d');
      ctx.scale(ratio, ratio);
      ctx.clearRect(0, 0, width, height);
      const b = this.result.caixa;
      const diagonal = Math.hypot(b.comprimento, b.largura, b.altura);
      const scale = (Math.min(width * 0.78, height * 0.78) / diagonal) * this.zoom;
      const project = ([x, y, z]) => {
        x -= b.comprimento / 2;
        y -= b.largura / 2;
        z -= b.altura / 2;
        const rx = x * Math.cos(this.yaw) - y * Math.sin(this.yaw);
        const ry = x * Math.sin(this.yaw) + y * Math.cos(this.yaw);
        return {
          x: width / 2 + rx * scale,
          y: height / 2 + (ry * Math.sin(this.pitch) - z * Math.cos(this.pitch)) * scale,
          depth: ry * Math.cos(this.pitch) + z * Math.sin(this.pitch)
        };
      };
      const vertices = (x, y, z, c, l, a) =>
        [
          [x, y, z],
          [x + c, y, z],
          [x + c, y + l, z],
          [x, y + l, z],
          [x, y, z + a],
          [x + c, y, z + a],
          [x + c, y + l, z + a],
          [x, y + l, z + a]
        ].map(project);
      const faceIndexes = [
        [0, 1, 2, 3],
        [4, 7, 6, 5],
        [0, 4, 5, 1],
        [1, 5, 6, 2],
        [2, 6, 7, 3],
        [3, 7, 4, 0]
      ];
      const faces = [];
      this.result.posicoes.slice(0, this.visible).forEach((p, index) => {
        const v = vertices(p.x, p.y, p.z, p.comprimento, p.largura, p.altura);
        faceIndexes.forEach((indices, side) => {
          const points = indices.map((i) => v[i]);
          faces.push({
            points,
            depth: points.reduce((sum, point) => sum + point.depth, 0) / 4,
            color: this.color(p.produtoId),
            side,
            index
          });
        });
      });
      // Peças distantes são desenhadas primeiro para preservar a oclusão.
      faces.sort((a, b) => a.depth - b.depth);
      for (const face of faces) {
        ctx.beginPath();
        face.points.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)));
        ctx.closePath();
        ctx.fillStyle = face.color;
        ctx.fill();
        ctx.fillStyle = `rgba(0,0,0,${(face.side % 3) * 0.08})`;
        ctx.fill();
        ctx.strokeStyle = '#263449';
        ctx.lineWidth = 0.8;
        ctx.stroke();
      }
      const outline = vertices(0, 0, 0, b.comprimento, b.largura, b.altura);
      ctx.strokeStyle = '#40536b';
      ctx.lineWidth = 1.5;
      const edges = [
        [0, 1],
        [1, 2],
        [2, 3],
        [3, 0],
        [4, 5],
        [5, 6],
        [6, 7],
        [7, 4],
        [0, 4],
        [1, 5],
        [2, 6],
        [3, 7]
      ];
      ctx.setLineDash([5, 4]);
      edges.forEach(([a, c]) => {
        ctx.beginPath();
        ctx.moveTo(outline[a].x, outline[a].y);
        ctx.lineTo(outline[c].x, outline[c].y);
        ctx.stroke();
      });
      ctx.setLineDash([]);
      ctx.font = '13px Segoe UI, sans-serif';
      ctx.fillStyle = '#40536b';
      ctx.fillText(`${b.nome} · ${this.visible} unidades visíveis`, 16, 25);
    }
  }
};
