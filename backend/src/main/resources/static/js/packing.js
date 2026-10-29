// Lista final de pecas, importacao e sugestao dos volumes de embalagem.
// Os metodos usam o estado reativo da aplicacao Vue.
globalThis.InboxPacking = {
  clearPacking() {
    this.startPacking();
  },
  packingChanged() {
    this.busy = false;
    this.pcRevision++;
    this.pcPlan = null;
    this.result = null;
    this.error = "";
  },
  startPacking() {
    this.clearPc();
    this.activePc = {
      id: uid(),
      code: "SIMULACAO",
      name: "Lista final de peças",
      kind: "ORDER",
      baseCode: "",
      sourceUrl: "",
      note: "",
      aliases: [],
      items: [],
      demo: false,
    };
  },
  async readPackingFile(event) {
    const file = event.target.files[0];
    event.target.value = "";
    if (!file || this.busy) return;
    if (file.size > 500000) {
      this.error = "Use um arquivo de até 500 KB.";
      return;
    }
    this.busy = true;
    this.error = "";
    // A pessoa pode limpar a lista enquanto o arquivo ainda está sendo lido.
    // Nesse caso, a resposta antiga não deve ressuscitar os itens removidos.
    const revision = ++this.pcRevision;
    try {
      const input = JSON.parse(await file.text());
      const response = await this.request("/simple/packing/preview", "POST", input);
      if (revision !== this.pcRevision) return;
      this.clearPc();
      this.activePc = response.composition;
      this.pcPlan = response.plan;
      this.activateGroup(response.plan.groups[0]?.id);
    } catch (e) {
      if (revision === this.pcRevision) this.error = e.message;
    } finally {
      // clearPc avança a revisão quando a importação termina com sucesso.
      // Nesse caminho ele já liberou busy; nas respostas antigas não mexemos nele.
      if (revision === this.pcRevision) this.busy = false;
    }
  },
  async loadPc(code) {
    if (this.busy) return;
    const lookup = code;
    this.clearPc();
    this.busy = true;
    try {
      const record = await this.request("/simple/pc/lookup?code=" + encodeURIComponent(lookup));
      this.activePc = JSON.parse(JSON.stringify(record));
      this.pcGroupId = "pc";
      this.busy = false;
      await this.calculatePc();
    } catch (e) {
      this.error = e.message;
    } finally {
      this.busy = false;
    }
  },
  async loadPcExamples() {
    if (this.busy) return;
    this.busy = true;
    this.error = "";
    try {
      this.catalog = await this.request("/simple/pc/examples", "POST", {});
      this.notify("Configuração Highflyer carregada. Medidas e códigos das peças são de exemplo.");
    } catch (e) {
      this.error = e.message;
    } finally {
      this.busy = false;
    }
  },
  clearPc() {
    this.pcRevision++;
    this.busy = false;
    this.activePc = null;
    this.pcPlan = null;
    this.result = null;
    this.error = "";
  },
  async calculatePc() {
    if (!this.activePc) return;
    // Ignora respostas antigas se a lista mudou enquanto o servidor calculava.
    const revision = ++this.pcRevision;
    const pcId = this.activePc.id;
    this.busy = true;
    this.error = "";
    try {
      const plan = await this.request("/simple/pc/calculate", "POST", this.activePc);
      if (this.activePc?.id !== pcId || revision !== this.pcRevision) return;
      this.pcPlan = plan;
      this.activateGroup(
        plan.groups.some((g) => g.id === this.pcGroupId) ? this.pcGroupId : plan.groups[0]?.id,
      );
    } catch (e) {
      if (revision === this.pcRevision) {
        this.error = e.message;
        this.result = null;
      }
    } finally {
      if (revision === this.pcRevision) this.busy = false;
    }
  },
  activateGroup(id) {
    const group = this.pcPlan?.groups.find((g) => g.id === id);
    if (!group) return;
    this.pcGroupId = id;
    this.result =
      group.kind === "FACTORY"
        ? {
            status: "FACTORY",
            message: `Enviar na embalagem do fabricante: ${group.volumes} volume(s). Não precisa de caixa externa.`,
            box: null,
            demo: this.pcPlan.demo,
          }
        : group.result;
  },
  show3d() {
    this.$nextTick(() =>
      document
        .getElementById("packing-preview")
        ?.scrollIntoView({ behavior: "smooth", block: "start" }),
    );
  },
};
