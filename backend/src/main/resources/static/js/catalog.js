// Cadastro, categorias e conversao de medidas (tela em cm, banco em mm).
// Os metodos usam o estado reativo da aplicacao Vue.
globalThis.InboxCatalog = {
  topicOf(p) {
    if (p.topic) return p.topic;
    const defaults = {
      OTHER: "other/other",
      PERIPHERAL: "peripherals/kits",
      CABLE: "peripherals/cables",
      MONITOR: "monitors/gaming",
      CHAIR: "furniture/gaming-chair",
      DESK: "furniture/gaming-desk",
    };
    return (
      defaults[p.category] ||
      this.departments.flatMap((d) => d.topics).find((t) => t.category === p.category)?.id ||
      "other/other"
    );
  },
  chooseDepartment() {
    this.topicManual = true;
    this.topicSuggested = false;
    this.draft.topic = "";
  },
  chooseTopic() {
    this.topicManual = true;
    this.topicSuggested = false;
    const t = this.draftTopics.find((t) => t.id === this.draft.topic);
    if (t) this.draft.category = t.category;
  },
  suggestTopic() {
    if (this.topicManual) return;
    const id = globalThis.suggestCatalogTopic?.(this.draft.name) || "";
    this.draft.topic = id;
    this.draftDepartment = id.split("/")[0] || "";
    const t = this.departments.flatMap((d) => d.topics).find((t) => t.id === id);
    this.draft.category = t?.category || "OTHER";
    this.topicSuggested = !!id;
  },
  addInTopic(department, topic) {
    this.editProduct();
    this.draftDepartment = department.id;
    this.draft.topic = topic.id;
    this.draft.category = topic.category;
    this.topicManual = true;
  },
  product(id) {
    return this.catalog.products.find((p) => p.id === id);
  },
  cm(mm) {
    return mm == null ? "" : (mm / 10).toLocaleString("pt-BR", { maximumFractionDigits: 1 });
  },
  dimensions(p) {
    return p && [p.l, p.w, p.h].every((x) => x != null)
      ? `${this.cm(p.l)} × ${this.cm(p.w)} × ${this.cm(p.h)} cm`
      : "Medidas pendentes";
  },
  percent(value) {
    return (value * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
  },
  editProduct(p) {
    this.open("products", p);
  },
  editBox(b) {
    this.open("boxes", b);
  },
  // O formulario usa centimetros; nunca modifica diretamente o objeto persistido.
  open(kind, record) {
    this.topicManual = false;
    this.topicSuggested = false;
    this.draftDepartment = "";
    this.modal = kind;
    this.existing = !!record;
    this.wasDemo = !!record?.demo;
    this.modalError = "";
    this.draft = record
      ? { ...record }
      : {
          id: uid(),
          name: "",
          ...(kind === "products"
            ? { sku: "", category: "OTHER", pcMode: "MAIN", oem: false }
            : {}),
          l: null,
          w: null,
          h: null,
          demo: false,
        };
    if (kind === "products") {
      this.draft.topic = record ? this.topicOf(record) : "";
      this.draftDepartment = this.draft.topic.split("/")[0] || "";
      this.topicManual = !!record;
    }
    for (const k of ["l", "w", "h"])
      this.draft[k] = this.draft[k] == null ? "" : this.draft[k] / 10;
  },
  close() {
    if (!this.busy) {
      this.modal = null;
      this.draft = null;
      this.modalError = "";
    }
  },
  // Converte somente na fronteira da API; branco continua null, nunca zero.
  payload() {
    const out = { ...this.draft };
    for (const k of ["l", "w", "h"])
      out[k] = out[k] === "" || out[k] == null ? null : Math.round(Number(out[k]) * 10);
    return out;
  },
  async save() {
    if (this.busy) return;
    this.busy = true;
    this.modalError = "";
    try {
      const payload = this.payload();
      if (this.modal === "products" && !payload.topic)
        throw Error("Selecione o departamento e a categoria do produto.");
      this.catalog = await this.request(`/simple/${this.modal}/${payload.id}`, "PUT", payload);
      this.result = null;
      this.busy = false;
      this.close();
      this.notify("Cadastro salvo.");
    } catch (e) {
      this.modalError = e.message;
    } finally {
      this.busy = false;
    }
  },
  async removeRecord() {
    if (!confirm(`Excluir ${this.draft.name}?`)) return;
    this.busy = true;
    try {
      this.catalog = await this.request(`/simple/${this.modal}/${this.draft.id}`, "DELETE");
      this.result = null;
      this.busy = false;
      this.close();
      this.notify("Cadastro excluído.");
    } catch (e) {
      this.modalError = e.message;
    } finally {
      this.busy = false;
    }
  },
  async refresh() {
    try {
      this.catalog = await this.request("/simple/state");
    } catch (e) {
      this.error = e.message;
    } finally {
      this.loading = false;
    }
  },
};
