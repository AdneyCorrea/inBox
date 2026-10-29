// Ponto de entrada: conecta os modulos da interface ao estado Vue.
const { createApp } = Vue;
const uid = () => crypto.randomUUID();
createApp({
  components: { Packing3d: globalThis.Packing3D, PcEditor: globalThis.PcEditor },
  data: () => ({
    catalog: { products: [], boxes: [], presets: [], generation: 0 },
    view: "calculate",
    loading: true,
    busy: false,
    error: "",
    message: "",
    search: "",
    result: null,
    modal: null,
    draft: null,
    existing: false,
    wasDemo: false,
    modalError: "",
    activePc: null,
    pcPlan: null,
    pcGroupId: "pc",
    pcRevision: 0,
    sessionInfo: null,
    departments: globalThis.CatalogDepartments || [],
    draftDepartment: "",
    topicManual: false,
    topicSuggested: false,
  }),
  computed: {
    draftTopics() {
      return this.departments.find((d) => d.id === this.draftDepartment)?.topics || [];
    },
    // Agrupa uma vez por categoria; evita percorrer todos os produtos para cada topico.
    catalogDepartments() {
      const query = this.search.trim().toLocaleLowerCase();
      const byTopic = new Map();
      for (const product of this.catalog.products) {
        const id = this.topicOf(product);
        if (!byTopic.has(id)) byTopic.set(id, []);
        byTopic.get(id).push(product);
      }
      return this.departments
        .map((department) => {
          let topics = department.topics.map((topic) => ({
            ...topic,
            products: (byTopic.get(topic.id) || []).filter(
              (product) =>
                !query ||
                [product.name, product.sku, topic.label, department.label]
                  .join(" ")
                  .toLocaleLowerCase()
                  .includes(query),
            ),
          }));
          const count = topics.reduce((total, topic) => total + topic.products.length, 0);
          if (query) topics = topics.filter((topic) => topic.products.length);
          return { ...department, topics, count };
        })
        .filter((department) => !query || department.count);
    },
    sortedBoxes() {
      return [...this.catalog.boxes].sort((a, b) => a.l * a.w * a.h - b.l * b.w * b.h);
    },
    volumeCount() {
      return (this.pcPlan?.groups || []).reduce(
        (total, g) =>
          total + (g.kind === "FACTORY" ? g.volumes : g.result?.status === "FOUND" ? 1 : 0),
        0,
      );
    },
    pendingVolumes() {
      return (this.pcPlan?.groups || []).some(
        (g) => g.kind !== "FACTORY" && g.result?.status !== "FOUND",
      );
    },
    basePresets() {
      return (this.catalog.presets || []).filter((p) => p.kind === "BASE");
    },
  },
  watch: {
    view() {
      this.search = "";
      window.scrollTo(0, 0);
    },
  },
  async mounted() {
    try {
      const response = await InboxHttp.fetch("/auth/session", { cache: "no-store" });
      if (!response.ok) throw Error("Não foi possível verificar seu acesso.");
      this.sessionInfo = await response.json();
      if (this.sessionInfo.enabled && !this.sessionInfo.authenticated) {
        location.replace("/login.html");
        return;
      }
      await this.refresh();
      if (!this.error) this.startPacking();
    } catch (e) {
      this.loading = false;
      this.error = e.message;
    }
  },
  methods: {
    ...InboxHttp.methods,
    ...InboxCatalog,
    ...InboxPacking,
    ...InboxBackup,
    async logout() {
      try {
        const session = await (
          await InboxHttp.fetch("/auth/session", { cache: "no-store" })
        ).json();
        const response = await InboxHttp.fetch("/logout", {
          method: "POST",
          headers: { [session.headerName]: session.token },
        });
        if (!response.ok) throw Error("Não foi possível sair.");
        location.replace("/login.html");
      } catch (e) {
        this.error = e.message;
      }
    },
    notify(text) {
      this.message = text;
      clearTimeout(this.messageTimer);
      this.messageTimer = setTimeout(() => (this.message = ""), 4000);
    },
  },
}).mount("#app");
