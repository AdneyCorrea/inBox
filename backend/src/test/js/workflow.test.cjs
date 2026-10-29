const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../main/resources/static');
function modules(){
  const c = {crypto:require('node:crypto').webcrypto, setTimeout, clearTimeout, AbortController, uid:()=> 'lista-nova'};
  for(const f of ['js/packing.js','js/catalog.js','js/http.js']) vm.runInNewContext(fs.readFileSync(path.join(root,f),'utf8'),c);
  return c;
}
function app(c){return Object.assign({pcRevision:0,busy:false,error:'',pcPlan:null,result:null,activePc:null},c.InboxPacking);}
const event = ()=>({target:{value:'arquivo',files:[{size:100,text:async()=>'{"items":[]}'}]}});
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject};};

test('limpar a lista durante importação não permite que a resposta antiga recoloque os itens',async()=>{
  const c=modules(), a=app(c), d=deferred();a.request=()=>d.promise;
  const pending=a.readPackingFile(event());await Promise.resolve();a.startPacking();
  d.resolve({composition:{id:'antiga'},plan:{groups:[]}});await pending;
  assert.equal(a.activePc.id,'lista-nova');assert.equal(a.pcPlan,null);
});
test('erro antigo não substitui o estado de um cálculo novo',async()=>{
  const c=modules(),a=app(c),d=deferred();a.request=()=>d.promise;
  const pending=a.readPackingFile(event());await Promise.resolve();a.startPacking();a.busy=true;
  d.reject(Error('erro antigo'));await pending;assert.equal(a.error,'');assert.equal(a.busy,true);
});
test('importação válida apresenta o plano e libera os controles',async()=>{
  const c=modules(),a=app(c);a.request=async()=>({composition:{id:'importada'},plan:{groups:[]}});
  await a.readPackingFile(event());assert.equal(a.activePc.id,'importada');assert.equal(a.busy,false);
});
test('resposta de cálculo descartada quando a composição muda',async()=>{
  const c=modules(),a=app(c),d=deferred();a.activePc={id:'antes'};a.request=()=>d.promise;
  const pending=a.calculatePc();a.startPacking();d.resolve({groups:[]});await pending;
  assert.equal(a.pcPlan,null);assert.equal(a.activePc.id,'lista-nova');
});
test('fabricante é mostrado como volume próprio sem inventar caixa externa',()=>{
  const c=modules(),a=app(c);a.pcPlan={groups:[{id:'monitor',kind:'FACTORY',volumes:2}]};a.activateGroup('monitor');
  assert.equal(a.result.status,'FACTORY');assert.equal(a.result.box,null);assert.match(a.result.message,/2 volume/);
});
test('centímetros viram milímetros sem alterar o rascunho e branco não vira zero',()=>{
  const c=modules();const a={draft:{l:'12.3',w:'',h:'8'},...c.InboxCatalog};const p=a.payload();
  assert.equal(p.l,123);assert.equal(p.w,null);assert.equal(p.h,80);assert.equal(a.draft.l,'12.3');
});
test('categoria escolhida manualmente não é substituída pela sugestão',()=>{
  const c=modules();const a={topicManual:true,draft:{topic:'manual'},...c.InboxCatalog};a.suggestTopic();assert.equal(a.draft.topic,'manual');
});
test('API envia revisão e token CSRF e atualiza catálogo após conflito',async()=>{
  const c=modules();let sent,refreshed=false;c.InboxHttp.fetch=async(url,options)=>{sent=options;return {ok:false,status:409,json:async()=>({message:'Conflito'})};};
  const a={catalog:{generation:7},sessionInfo:{token:'token-teste',headerName:'X-CSRF-TOKEN'},refresh:async()=>{refreshed=true},...c.InboxHttp.methods};
  await assert.rejects(()=>a.request('/simple/products/x','PUT',{}),/Conflito/);
  assert.equal(sent.headers['X-State-Revision'],'7');assert.equal(sent.headers['X-CSRF-TOKEN'],'token-teste');assert.equal(refreshed,true);
});
