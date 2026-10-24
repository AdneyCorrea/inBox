const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const crypto = require('node:crypto').webcrypto;
let options;
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/app.js'),'utf8'),{
  crypto, Vue:{createApp(value){options=value;return {mount(){}};}}
});
function instance(){const obj=options.data();Object.assign(obj,options.methods);return obj;}
test('new order opens with empty groups before any measurements exist',()=>{const app=instance();app.newOrder();assert.equal(app.modal,'order');assert.equal(app.draft.configurations.length,0);assert.equal(app.draft.standalone.length,0);});
test('a click event must not be treated as a configuration',()=>{const app=instance();app.newOrder({type:'click'});assert.equal(app.draft.configurations.length,0);});
test('two purchased PCs receive independent configuration and line identities',()=>{const app=instance();const template={id:'template',sku:'PC',name:'PC',lines:[{id:'line',productId:'product',quantity:1}]};app.newOrder(template);const first=app.draft.configurations[0];app.newOrder(template);const second=app.draft.configurations[0];assert.notEqual(first.id,second.id);assert.notEqual(first.lines[0].id,second.lines[0].id);first.lines[0].quantity=2;assert.equal(template.lines[0].quantity,1);assert.equal(second.lines[0].quantity,1);});
test('empty dimensions remain null while zero actual weight stays zero',()=>{const app=instance();app.modal='product';const product={packages:[{l:'',w:null,h:'100',weight:'0'}]};const out=app.normalize(product);assert.equal(out.packages[0].l,null);assert.equal(out.packages[0].w,null);assert.equal(out.packages[0].h,100);assert.equal(out.packages[0].weight,0);});
test('an allegedly validated profile without dimensions is shown as pending',()=>{const app=instance();assert.equal(app.productStatus({packages:[{status:'VALIDATED',l:null,w:10,h:10,weight:10}]}),'PENDING');});
test('scan assigns a repeated SKU to the next line still needing a unit',async()=>{const app=instance();app.scanCode='MONITOR';app.activeOrder={checked:{included:1}};app.checklist=[{key:'included',sku:'MONITOR',barcode:'',quantity:1},{key:'standalone',sku:'MONITOR',barcode:'',quantity:1}];let selected;app.check=async item=>{selected=item.key;};await app.scan();assert.equal(selected,'standalone');});
