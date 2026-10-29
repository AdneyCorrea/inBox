// Requisicoes com prazo, sessao e controle de revisao para evitar sobrescrita.
async function fetchWithTimeout(url, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } catch (error) {
    if (error.name === "AbortError")
      throw Error("O aplicativo demorou para responder. Tente novamente ou reinicie o inBox.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}
globalThis.InboxHttp = {
  fetch: fetchWithTimeout,
  methods: {
    async request(path, method = "GET", body) {
      const response = await InboxHttp.fetch("/api" + path, {
        method,
        headers: {
          "Content-Type": "application/json",
          // É a versão que esta tela conhece, não necessariamente a mais recente.
          // O servidor compara antes de aceitar uma gravação compartilhada.
          "X-State-Revision": String(this.catalog.generation),
          ...(this.sessionInfo?.token
            ? { [this.sessionInfo.headerName]: this.sessionInfo.token }
            : {}),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      if (!response.ok) {
        if (response.status === 401) {
          location.replace("/login.html");
          throw Error("Sua sessão terminou. Entre novamente.");
        }
        let data = {};
        try {
          data = await response.json();
        } catch {}
        // Não reenviamos uma edição em conflito automaticamente: isso poderia
        // substituir o cadastro que outra pessoa acabou de salvar.
        if (response.status === 409) await this.refresh();
        throw Error(
          data.message || "Não foi possível concluir. Confira se o aplicativo está aberto.",
        );
      }
      return response.json();
    },
  },
};
