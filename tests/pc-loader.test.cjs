const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const context={crypto:require('node:crypto').webcrypto,setTimeout,clearTimeout};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/pc-loader.js'),'utf8'),context);
let options;context.Vue={createApp(value){options=value;return {mount(){}};}};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/simple.js'),'utf8'),context);
function app(){return Object.assign(options.data(),options.methods);}
test('reading a preset copies its items instead of changing the stored base',async()=>{const a=app();const stored={id:'base',code:'66705',kind:'BASE',items:[{id:'line',productId:'gpu',quantity:1}]};a.request=async()=>stored;a.calculatePc=async()=>{};await a.loadPc('66705');a.activePc.items[0].quantity=3;assert.equal(stored.items[0].quantity,1);});
test('a failed lookup clears the preceding order suggestion',async()=>{const a=app();a.activePc={id:'old'};a.result={status:'FOUND'};a.request=async()=>{throw Error('Código não cadastrado');};await a.loadPc('missing');assert.equal(a.activePc,null);assert.equal(a.result,null);assert.match(a.error,/não cadastrado/);});
test('saving a personalized base creates an independent order code',async()=>{const a=app();a.activePc={id:'base',code:'66705',kind:'BASE',items:[{productId:'gpu',quantity:1}],aliases:['66705']};a.orderCode='PEDIDO-123';a.notify=()=>{};let saved;a.request=async(path,method,payload)=>{saved=payload;return a.catalog;};await a.savePc();assert.notEqual(saved.id,'base');assert.equal(saved.kind,'ORDER');assert.equal(saved.code,'PEDIDO-123');assert.equal(saved.baseCode,'66705');assert.equal(saved.aliases.length,0);});
test('manufacturer groups do not masquerade as an external box suggestion',()=>{const a=app();a.pcPlan={demo:true,groups:[{id:'monitor',kind:'FACTORY',volumes:1,selection:[{productId:'monitor',quantity:1}]}]};a.activateGroup('monitor');assert.equal(a.result.status,'FACTORY');assert.equal(a.result.box,null);});
test('replacing a part applies its packaging treatment without retaining the old piece',()=>{const editor=context.PcEditor;const products=[{id:'old',pcMode:'MAIN'},{id:'new',category:'MEMORY',pcMode:'MAIN'}];const line={productId:'new',origin:'CONFIG',mode:'MAIN'};let changed=false;const instance=Object.assign({products,$emit(){changed=true;}},editor.methods);instance.changed(line,true);assert.equal(line.mode,'NESTED');assert.equal(line.customized,true);assert.equal(changed,true);});

test('standalone memory keeps its own packaging despite legacy PC treatment',()=>{const editor=context.PcEditor;const p={id:'ram',category:'MEMORY',pcMode:'NESTED'};const a=Object.assign(editor.data(),{products:[p],pc:{items:[]},$emit(){}},editor.methods);a.addId='ram';a.addOrigin='EXTRA';a.add();assert.equal(a.pc.items[0].mode,'MAIN');});

test('OEM products and cables are excluded only when part of the PC',()=>{for(const product of [{id:'oem',category:'CPU',oem:true},{id:'cable',category:'CABLE'}]){for(const origin of ['CONFIG','EXTRA']){const editor=context.PcEditor;const a=Object.assign(editor.data(),{products:[product],pc:{items:[]},$emit(){}},editor.methods);a.addId=product.id;a.addOrigin=origin;a.add();assert.equal(a.pc.items[0].mode,origin==='CONFIG'?'OEM':'MAIN');}}});
