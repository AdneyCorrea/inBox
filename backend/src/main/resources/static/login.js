document.addEventListener("DOMContentLoaded", async () => {
  const message = document.getElementById("login-message");
  if (new URLSearchParams(location.search).has("error")) {
    message.textContent = "Usuário ou senha incorretos.";
    message.hidden = false;
  }
  try {
    const response = await InboxHttp.fetch("/auth/session", { cache: "no-store" });
    if (!response.ok) throw Error();
    const session = await response.json();
    if (!session.enabled || session.authenticated) {
      location.replace("/");
      return;
    }
    if (!session.token) throw Error();
    const token = document.getElementById("csrf");
    token.name = session.parameterName;
    token.value = session.token;
    document.getElementById("login-submit").disabled = false;
  } catch {
    message.textContent = "Não foi possível iniciar o acesso. Atualize a página e tente novamente.";
    message.hidden = false;
  }
});
