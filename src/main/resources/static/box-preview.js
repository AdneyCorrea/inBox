// Miniatura ampliada da caixa; a comparação em escala fica na janela 3D.
const BoxPreview = {
  props: ['box'],
  computed: {
    points() {
      const c = this.box.length,
        l = this.box.width,
        a = this.box.height;
      const projected = [
        [0, 0, 0],
        [c, 0, 0],
        [c, l, 0],
        [0, l, 0],
        [0, 0, a],
        [c, 0, a],
        [c, l, a],
        [0, l, a]
      ].map(([x, y, z]) => {
        x -= c / 2;
        y -= l / 2;
        return [
          x * Math.cos(-0.65) - y * Math.sin(-0.65),
          (x * Math.sin(-0.65) + y * Math.cos(-0.65)) * Math.sin(0.55) - z * Math.cos(0.55)
        ];
      });
      const xs = projected.map((p) => p[0]),
        ys = projected.map((p) => p[1]);
      const minX = Math.min(...xs),
        maxX = Math.max(...xs),
        minY = Math.min(...ys),
        maxY = Math.max(...ys);
      const scale = Math.min(180 / Math.max(1, maxX - minX), 135 / Math.max(1, maxY - minY));
      return projected.map(([x, y]) => [
        110 + (x - (minX + maxX) / 2) * scale,
        90 + (y - (minY + maxY) / 2) * scale
      ]);
    },
    description() {
      const cm = (value) => (value / 10).toLocaleString('pt-BR');
      return `${this.box.name}: ${cm(this.box.length)} × ${cm(this.box.width)} × ${cm(this.box.height)} centímetros`;
    }
  },
  methods: {
    face(indices) {
      return indices.map((index) => this.points[index].join(',')).join(' ');
    }
  },
  template: `
    <svg class="box-preview" viewBox="0 0 220 180" role="img" :aria-label="description">
      <title>{{ description }}</title>
      <polygon :points="face([0,3,7,4])" fill="#ccaa81" stroke="#a9845b" stroke-linejoin="round" />
      <polygon :points="face([3,2,6,7])" fill="#dfbf99" stroke="#a9845b" stroke-linejoin="round" />
      <polygon :points="face([4,5,6,7])" fill="#f0d8b8" stroke="#a9845b" stroke-linejoin="round" />
      <path :d="'M' + points[4].join(',') + ' L' + points[6].join(',')" stroke="#a9845b" stroke-opacity=".35" />
    </svg>`
};

// Janela de inspeção com câmera e escala compartilhadas entre as duas caixas.
const BoxViewer = {
  props: ['box', 'boxes'],
  emits: ['close'],
  data() {
    return {
      comparisonId: '',
      yaw: -0.65,
      pitch: 0.55,
      zoom: 1,
      dragging: false,
      lastX: 0,
      lastY: 0
    };
  },
  computed: {
    alternatives() {
      return this.boxes
        .filter((box) => box.id !== this.box.id)
        .sort((a, b) => a.length * a.width * a.height - b.length * b.width * b.height);
    },
    comparison() {
      return this.boxes.find((box) => String(box.id) === String(this.comparisonId));
    },
    displayed() {
      return this.comparison ? [this.box, this.comparison] : [this.box];
    },
    reference() {
      return Math.max(
        1,
        ...this.displayed.map((box) => Math.hypot(box.length, box.width, box.height))
      );
    }
  },
  mounted() {
    this.opener = document.activeElement;
    this.$refs.dialog.showModal();
  },
  beforeUnmount() {
    if (this.opener?.isConnected) this.opener.focus();
  },
  methods: {
    cm(value) {
      return (value / 10).toLocaleString('pt-BR');
    },
    reset() {
      this.yaw = -0.65;
      this.pitch = 0.55;
      this.zoom = 1;
    },
    topView() {
      this.yaw = 0;
      this.pitch = Math.PI / 2;
    },
    zoomBy(factor) {
      this.zoom = Math.max(0.35, Math.min(3, this.zoom * factor));
    },
    wheel(event) {
      const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? 350 : 1;
      this.zoomBy(Math.exp(-Math.max(-100, Math.min(100, event.deltaY * unit)) * 0.002));
    },
    start(event) {
      this.dragging = true;
      this.lastX = event.clientX;
      this.lastY = event.clientY;
      event.currentTarget.setPointerCapture(event.pointerId);
    },
    move(event) {
      if (!this.dragging) return;
      this.yaw += (event.clientX - this.lastX) * 0.009;
      this.pitch = Math.max(
        -Math.PI / 2,
        Math.min(Math.PI / 2, this.pitch + (event.clientY - this.lastY) * 0.009)
      );
      this.lastX = event.clientX;
      this.lastY = event.clientY;
    },
    key(event) {
      const step = 0.15;
      if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', '+', '=', '-'].includes(event.key))
        return;
      event.preventDefault();
      if (event.key === 'ArrowLeft') this.yaw -= step;
      if (event.key === 'ArrowRight') this.yaw += step;
      if (event.key === 'ArrowUp') this.pitch = Math.min(Math.PI / 2, this.pitch + step);
      if (event.key === 'ArrowDown') this.pitch = Math.max(-Math.PI / 2, this.pitch - step);
      if (event.key === '+' || event.key === '=') this.zoomBy(1.15);
      if (event.key === '-') this.zoomBy(1 / 1.15);
    },
    faces(box, index) {
      const c = box.length,
        l = box.width,
        a = box.height;
      const scale = (275 / this.reference) * this.zoom;
      const points = [
        [0, 0, 0],
        [c, 0, 0],
        [c, l, 0],
        [0, l, 0],
        [0, 0, a],
        [c, 0, a],
        [c, l, a],
        [0, l, a]
      ].map(([x, y, z]) => {
        x -= c / 2;
        y -= l / 2;
        z -= a / 2;
        const rx = x * Math.cos(this.yaw) - y * Math.sin(this.yaw);
        const ry = x * Math.sin(this.yaw) + y * Math.cos(this.yaw);
        return {
          x: 200 + rx * scale,
          y: 175 + (ry * Math.sin(this.pitch) - z * Math.cos(this.pitch)) * scale,
          depth: ry * Math.cos(this.pitch) + z * Math.sin(this.pitch)
        };
      });
      const palette = index ? ['#91b1cc', '#bad1e4', '#a3c0d8'] : ['#cba77b', '#efd6b3', '#ddbc93'];
      return [
        [0, 1, 2, 3],
        [4, 7, 6, 5],
        [0, 4, 5, 1],
        [1, 5, 6, 2],
        [2, 6, 7, 3],
        [3, 7, 4, 0]
      ]
        .map((face, i) => ({
          id: i,
          points: face.map((p) => points[p].x + ',' + points[p].y).join(' '),
          depth: face.reduce((sum, p) => sum + points[p].depth, 0) / 4,
          fill: palette[i % 3]
        }))
        .sort((a, b) => a.depth - b.depth);
    }
  },
  template: `
    <dialog ref="dialog" class="box-viewer" aria-labelledby="box-viewer-title" @cancel.prevent="$emit('close')" @close="$emit('close')" @click.self="$emit('close')">
      <div class="viewer-content">
        <div class="section-heading"><h2 id="box-viewer-title">{{ box.name }}</h2><button class="text-button" @click="$emit('close')" aria-label="Fechar visualização">Fechar ×</button></div>
        <div class="viewer-toolbar">
          <div class="view-controls"><button class="text-button" @click="reset">Restaurar</button><button class="text-button" @click="topView">TopView</button></div>
          <label>Comparar com<select v-model="comparisonId"><option value="">Nenhuma</option><option v-for="other in alternatives" :key="other.id" :value="other.id">{{ other.name }}</option></select></label>
        </div>
        <p class="footnote">Arraste para girar · Scroll para zoom<span v-if="comparison"> · Mesma escala</span></p>
        <div class="viewer-scenes" :class="{ comparing: !!comparison }">
          <section v-for="(current,index) in displayed" :key="current.id" class="viewer-scene">
            <svg viewBox="0 0 400 350" tabindex="0" role="img" :aria-label="current.name + '. Arraste ou use as setas para girar; scroll ou teclas mais e menos para zoom.'"
              @pointerdown="start" @pointermove="move" @pointerup="dragging=false" @pointercancel="dragging=false" @wheel.prevent="wheel" @keydown="key">
              <polygon v-for="face in faces(current,index)" :key="face.id" :points="face.points" :fill="face.fill" :stroke="index ? '#6b8ea9' : '#9c7953'" stroke-width="1" stroke-linejoin="round" />
            </svg>
            <strong>{{ current.name }}</strong><p>{{ cm(current.length) }} × {{ cm(current.width) }} × {{ cm(current.height) }} cm</p>
          </section>
        </div>
      </div>
    </dialog>`
};
