// A interface recebe centímetros; a lista mantém as dimensões em milímetros.
const emptyForm = () => ({
  name: '',
  sku: '',
  category: 'CPU',
  length: '',
  width: '',
  height: '',
  quantity: 1
});
const categoryNames = {
  CABLE: 'Cabos',
  CASE: 'Gabinetes',
  PREPARED_PC: 'Computadores montados',
  CPU: 'Processadores',
  MOTHERBOARD: 'Placas-mãe',
  MEMORY: 'Memórias',
  GPU: 'Placas de vídeo',
  SSD: 'Armazenamento / SSD',
  PSU: 'Fontes',
  COOLER: 'Coolers',
  FAN: 'Ventoinhas',
  UNCATEGORIZED: 'Sem categoria'
};

Vue.createApp({
  data() {
    return {
      form: emptyForm(),
      products: [],
      boxes: [],
      selected: {},
      productSearch: '',
      categoryFilter: '',
      catalogSearch: '',
      catalogCategory: '',
      tab: 'packing',
      catalogType: 'product',
      previewBox: null,
      editingId: null,
      saving: false,
      loading: false,
      loadError: '',
      calculating: false,
      calculation: null,
      calculationError: '',
      notifications: [],
      notificationSequence: 0,
      notificationTimers: new Map()
    };
  },
  mounted() {
    this.loadCatalog();
  },
  beforeUnmount() {
    this.notificationTimers.forEach((timer) => clearTimeout(timer));
  },
  computed: {
    displayedCatalogItems() {
      if (this.catalogType !== 'product') return this.catalogItems;
      const query = this.normalizeSearch(this.catalogSearch.trim());
      return this.products.filter(
        (product) =>
          (!this.catalogCategory ||
            (product.category || 'UNCATEGORIZED') === this.catalogCategory) &&
          (!query ||
            this.normalizeSearch(product.name).includes(query) ||
            this.normalizeSearch(product.sku).includes(query))
      );
    },
    categoryOptions() {
      return Object.entries(categoryNames)
        .filter(([code]) => code !== 'UNCATEGORIZED')
        .map(([code, name]) => ({ code, name }));
    },
    categories() {
      return [...new Set(this.products.map((product) => product.category || 'UNCATEGORIZED'))]
        .map((code) => ({ code, name: this.categoryName(code) }))
        .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
    },
    filteredProducts() {
      const query = this.normalizeSearch(this.productSearch.trim());
      return this.products.filter(
        (product) =>
          (!this.categoryFilter || (product.category || 'UNCATEGORIZED') === this.categoryFilter) &&
          (!query ||
            this.normalizeSearch(product.name).includes(query) ||
            this.normalizeSearch(product.sku).includes(query))
      );
    },
    productGroups() {
      return this.categories
        .map((category) => ({
          ...category,
          products: this.filteredProducts.filter(
            (product) => (product.category || 'UNCATEGORIZED') === category.code
          )
        }))
        .filter((group) => group.products.length);
    },
    canCalculate() {
      return (
        !this.loading &&
        !this.saving &&
        !this.loadError &&
        !this.invalidQuantity &&
        this.items.length > 0 &&
        this.totalQuantity <= 200
      );
    },
    catalogItems() {
      if (this.catalogType === 'product') return this.products;
      return [...this.boxes].sort(
        (a, b) =>
          a.length * a.width * a.height - b.length * b.width * b.height ||
          a.name.localeCompare(b.name, 'pt-BR', { numeric: true })
      );
    },
    invalidQuantity() {
      return Object.values(this.selected).some((q) => !Number.isInteger(q) || q < 1 || q > 999);
    },
    items() {
      return this.products
        .filter((p) => this.selected[p.id] !== undefined)
        .map((p) => ({
          ...p,
          quantity:
            Number.isInteger(this.selected[p.id]) &&
            this.selected[p.id] >= 1 &&
            this.selected[p.id] <= 999
              ? this.selected[p.id]
              : 0
        }));
    },
    totalQuantity() {
      return this.items.reduce((total, item) => total + item.quantity, 0);
    }
  },
  watch: {
    catalogType() {
      this.cancelEdit();
    },
    selected: {
      deep: true,
      handler() {
        this.calculation = null;
        this.calculationError = '';
      }
    }
  },
  methods: {
    notify(message, type = 'success') {
      const id = ++this.notificationSequence;
      this.notifications.push({ id, message, type });
      this.notificationTimers.set(
        id,
        setTimeout(() => this.dismissNotification(id), 3000)
      );
    },
    dismissNotification(id) {
      clearTimeout(this.notificationTimers.get(id));
      this.notificationTimers.delete(id);
      this.notifications = this.notifications.filter((notification) => notification.id !== id);
    },
    categoryName(code) {
      return categoryNames[code] || code || categoryNames.UNCATEGORIZED;
    },
    normalizeSearch(value) {
      return String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLocaleLowerCase('pt-BR');
    },
    async calculateBox() {
      if (!this.canCalculate || this.calculating) return;
      const selection = JSON.stringify(this.selected);
      this.calculating = true;
      this.calculation = null;
      this.calculationError = '';
      try {
        const response = await fetch('/api/embalagens/calcular', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            itens: this.items.map((item) => ({ produtoId: item.id, quantidade: item.quantity }))
          })
        });
        if (!response.ok)
          throw new Error(
            'Não foi possível calcular a caixa. Verifique a seleção e tente novamente.'
          );
        const result = await response.json();
        if (selection === JSON.stringify(this.selected)) {
          this.calculation = result;
          this.notify(
            result.caixa ? 'Caixa encontrada.' : 'Nenhuma caixa compatível.',
            result.caixa ? 'success' : 'error'
          );
        }
      } catch (error) {
        if (selection === JSON.stringify(this.selected)) {
          this.calculationError = error.message;
          this.notify(error.message, 'error');
        }
      } finally {
        this.calculating = false;
      }
    },
    async loadCatalog() {
      this.loading = true;
      this.loadError = '';
      try {
        // O backend retorna os campos Java em português e as medidas em milímetros.
        const read = async (url) => {
          const response = await fetch(url, { cache: 'no-store' });
          if (!response.ok) throw new Error('Falha ao consultar o catálogo.');
          const data = await response.json();
          if (!Array.isArray(data)) throw new Error('Resposta inválida do servidor.');
          if (url === '/api/produtos' && data.some((item) => !Object.hasOwn(item, 'categoria'))) {
            throw new Error(
              'A aplicação está desatualizada. Pare a execução anterior e execute InboxApplication.java novamente.'
            );
          }
          return data.map((item) => ({
            id: item.id,
            name: item.nome,
            sku: item.sku,
            category: item.categoria || 'UNCATEGORIZED',
            length: item.comprimento,
            width: item.largura,
            height: item.altura
          }));
        };
        await this.importLegacyCatalog();
        const [products, boxes] = await Promise.all([read('/api/produtos'), read('/api/caixas')]);
        this.products = products;
        this.boxes = boxes;
      } catch (error) {
        this.loadError =
          error instanceof TypeError
            ? 'Não foi possível conectar à aplicação. Verifique se o Spring Boot está rodando.'
            : error.message;
        this.notify(this.loadError, 'error');
      } finally {
        this.loading = false;
      }
    },
    async apiWrite(url, method, body) {
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        if ((response.status === 404 || response.status === 405) && !error.mensagem) {
          throw new Error(
            'A rota de edição não está disponível. Reinicie InboxApplication.java para carregar a versão atual.'
          );
        }
        throw new Error(error.mensagem || 'Não foi possível salvar a alteração. Tente novamente.');
      }
      return response.status === 204 ? null : response.json();
    },
    startEdit(item) {
      this.editingId = item.id;
      this.form = {
        name: item.name,
        sku: item.sku || '',
        category: item.category || 'UNCATEGORIZED',
        length: item.length / 10,
        width: item.width / 10,
        height: item.height / 10,
        quantity: 1
      };
      this.$nextTick(() => this.$refs.catalogForm.querySelector('input').focus());
    },
    cancelEdit() {
      this.editingId = null;
      this.form = emptyForm();
    },
    async addItem() {
      if (this.saving || this.calculating) return;
      const { name, length, width, height, quantity } = this.form;
      if (
        !name ||
        ![length, width, height].every(
          (value) => Number.isFinite(value) && value >= 0.1 && value <= 10000
        ) ||
        !Number.isInteger(quantity) ||
        quantity < 1 ||
        quantity > 999
      ) {
        this.notify('Confira o nome e as medidas.', 'error');
        return;
      }
      const target = this.catalogType;
      const previous = this.catalogItems.find((item) => item.id === this.editingId);
      const url = target === 'product' ? '/api/produtos' : '/api/caixas';
      const body = {
        nome: name,
        sku: this.form.sku,
        categoria: this.form.category,
        comprimento: Math.round(length * 10),
        largura: Math.round(width * 10),
        altura: Math.round(height * 10)
      };
      this.saving = true;
      try {
        await this.apiWrite(
          previous ? url + '/' + encodeURIComponent(previous.id) : url,
          previous ? 'PUT' : 'POST',
          body
        );
        this.calculation = null;
        this.cancelEdit();
        await this.loadCatalog();
        if (!this.loadError)
          this.notify(
            target === 'product'
              ? previous
                ? 'Produto atualizado com sucesso.'
                : 'Produto cadastrado com sucesso.'
              : previous
                ? 'Caixa atualizada com sucesso.'
                : 'Caixa cadastrada com sucesso.'
          );
      } catch (error) {
        this.notify(
          error instanceof TypeError
            ? 'Não foi possível salvar. Verifique a conexão.'
            : error.message,
          'error'
        );
      } finally {
        this.saving = false;
      }
    },
    async removeItem(id) {
      if (this.saving || this.calculating) return;
      const item = this.catalogItems.find((item) => item.id === id);
      if (!item) return;
      this.saving = true;
      try {
        await this.apiWrite(
          (this.catalogType === 'product' ? '/api/produtos/' : '/api/caixas/') +
            encodeURIComponent(id),
          'DELETE'
        );
        if (this.catalogType === 'product') {
          this.products = this.products.filter((item) => item.id !== id);
          delete this.selected[id];
        } else this.boxes = this.boxes.filter((item) => item.id !== id);
        if (this.editingId === id) this.cancelEdit();
        this.calculation = null;
        this.notify(
          this.catalogType === 'product'
            ? 'Produto removido com sucesso.'
            : 'Caixa removida com sucesso.'
        );
      } catch (error) {
        this.notify(
          error instanceof TypeError
            ? 'Não foi possível remover. Verifique a conexão.'
            : error.message,
          'error'
        );
      } finally {
        this.saving = false;
      }
    },
    toggleProduct(id, checked) {
      if (checked) this.selected[id] = 1;
      else delete this.selected[id];
    },
    async importLegacyCatalog() {
      const key = 'inbox.catalog.v1';
      const raw = localStorage.getItem(key);
      if (!raw) return;
      const saved = JSON.parse(raw);
      const valid = (list) => Array.isArray(list) && list.every((item) =>
        item && typeof item.name === 'string' && item.name.trim() &&
        Number.isSafeInteger(item.id) && item.id >= 0 &&
        ['length', 'width', 'height'].every((field) =>
          Number.isInteger(item[field]) && item[field] >= 1 && item[field] <= 100000)
      ) && new Set(list.map((item) => item.id)).size === list.length;
      if (!saved || !valid(saved.products) || !valid(saved.boxes)) {
        throw new Error('Cadastros antigos inválidos. Os dados do navegador foram preservados.');
      }
      // Salvar antes de enviar permite retomar após falhas sem duplicar cadastros.
      const originKey = 'inbox.catalog.import.v1';
      let origin = localStorage.getItem(originKey);
      if (!origin) {
        origin = '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, (digit) =>
          (Number(digit) ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (Number(digit) / 4)))).toString(16)
        );
        localStorage.setItem(originKey, origin);
      }
      for (const [endpoint, list] of [['produtos', saved.products], ['caixas', saved.boxes]]) {
        for (const item of list) {
          await this.apiWrite(`/api/${endpoint}/importar/${encodeURIComponent(origin)}/${item.id}`, 'PUT', {
            nome: item.name,
            sku: item.sku || '',
            categoria: item.category || 'UNCATEGORIZED',
            comprimento: item.length,
            largura: item.width,
            altura: item.height
          });
        }
      }
      // Preservar o legado em caso de falha parcial ou alteração por outra aba.
      if (localStorage.getItem(key) === raw) localStorage.removeItem(key);
    },
    format(value) {
      return value.toLocaleString('pt-BR', { maximumFractionDigits: 3 });
    }
  }
})
  .component('packing-view', PackingView)
  .component('box-preview', BoxPreview)
  .component('box-viewer', BoxViewer)
  .mount('#app');
