const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
let options;
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/simple.js'),'utf8'),{crypto:require('node:crypto').webcrypto,Vue:{createApp(value){options=value;return {mount(){}};}}});
function app(){return Object.assign(options.data(),options.methods);}
test('centimeters convert to millimeters and blank measures remain absent',()=>{const a=app();a.draft={id:'p',name:'Produto',l:'47.5',w:'',h:null};const p=a.payload();assert.equal(p.l,475);assert.equal(p.w,null);assert.equal(p.h,null);});
test('new products need only a name and optional measures',()=>{const a=app();a.editProduct();assert.equal(a.modal,'products');assert.equal(a.draft.l,'');assert.equal(a.draft.demo,false);});
test('repeated product selections add quantities instead of duplicating rows',()=>{const a=app();a.catalog.products=[{id:'p',name:'Produto'}];a.productId='p';a.quantity=2;a.addSelection();a.productId='p';a.quantity=3;a.addSelection();assert.equal(a.selected.length,1);assert.equal(a.selected[0].quantity,5);});
test('changing quantity clears the old suggestion',()=>{const a=app();const row={productId:'p',quantity:1};a.result={status:'FOUND'};a.changeQuantity(row,'2');assert.equal(row.quantity,2);assert.equal(a.result,null);});
test('backup actions use matching buttons instead of a form label with margins',()=>{const html=fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/simple.html'),'utf8');assert.match(html,/<button class="button quiet" @click="backup">Exportar backup<\/button>/);assert.match(html,/<button class="button quiet" @click="\$refs.backupFile.click\(\)">Restaurar backup<\/button>/);});
