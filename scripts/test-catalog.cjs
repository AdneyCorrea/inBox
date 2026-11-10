// Executar com: node scripts/test-catalog.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { webcrypto } = require('node:crypto');
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/main/resources/static/app.js'), 'utf8');
async function run() {
  const key = 'inbox.catalog.v1';
  const product = { id: 123, name: 'Antigo', length: 100, width: 80, height: 50 };
  const raw = JSON.stringify({ products: [product], boxes: [{ ...product, name: 'Caixa' }] });
  const storage = new Map([[key, raw]]);
  let definition;
  vm.runInNewContext(source, {
    Vue: { createApp(value) { definition = value; return { component() { return this; }, mount() {} }; } },
    PackingView: {}, BoxPreview: {}, BoxViewer: {}, Uint8Array, crypto: webcrypto,
    localStorage: { getItem: (key) => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value), removeItem: (key) => storage.delete(key) }
  });
  const calls = [];
  const context = { apiWrite: async (url, method, body) => { calls.push({ url, method, body }); if (calls.length === 2) throw new Error('Offline'); } };
  await assert.rejects(definition.methods.importLegacyCatalog.call(context), /Offline/);
  assert.equal(storage.get(key), raw, 'Falha parcial preserva legado');
  const firstUrl = calls[0].url;
  context.apiWrite = async (url, method, body) => calls.push({ url, method, body });
  await definition.methods.importLegacyCatalog.call(context);
  assert.equal(calls[2].url, firstUrl, 'Retomada reutiliza o identificador');
  assert.equal(calls[2].body.comprimento, 100, 'Preserva milímetros');
  assert.equal(calls[2].body.categoria, 'UNCATEGORIZED');
  assert.equal(storage.has(key), false, 'Confirmação completa remove legado');
  await definition.methods.importLegacyCatalog.call(context);
  assert.equal(calls.length, 4, 'Não reimporta ao carregar novamente');
  storage.set(key, '{invalid');
  await assert.rejects(definition.methods.importLegacyCatalog.call(context));
  assert.equal(storage.get(key), '{invalid');
  storage.set(key, JSON.stringify({ products: [{ ...product, length: 0 }], boxes: [] }));
  await assert.rejects(definition.methods.importLegacyCatalog.call(context), /inválidos/);
  assert.equal(calls.length, 4, 'Dados inválidos não são enviados');
  console.log('Migração: falha parcial, retomada, unidades, conclusão única e dados inválidos OK');
}
module.exports = run();
