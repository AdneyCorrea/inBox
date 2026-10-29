// Exportacao e restauracao com confirmacao e limite de tamanho.
// Os metodos usam o estado reativo da aplicacao Vue.
globalThis.InboxBackup = {
  async backup() {
    try {
      const data = await this.request("/backup");
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }),
      );
      const a = document.createElement("a");
      a.href = url;
      a.download = "inbox-backup.json";
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      this.error = e.message;
    }
  },
  async restore(event) {
    const file = event.target.files[0];
    event.target.value = "";
    if (!file) return;
    if (file.size > 5_000_000) {
      this.error = "O backup deve ter até 5 MB.";
      return;
    }
    if (
      !confirm(
        "Restaurar substitui os cadastros atuais. Exporte uma cópia antes de continuar. Restaurar este arquivo?",
      )
    )
      return;
    this.busy = true;
    try {
      const body = JSON.parse(await file.text());
      await this.request("/restore", "POST", body);
      this.clearPc();
      await this.refresh();
      this.notify("Backup restaurado.");
    } catch (e) {
      this.error = e.message || "Arquivo inválido.";
    } finally {
      this.busy = false;
    }
  },
};
