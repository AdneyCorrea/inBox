/* Vue 3, served locally. All dimensions are integers in mm; weights in g. */
const {createApp} = Vue;
const uid = () => crypto.randomUUID();
const copy = value => JSON.parse(JSON.stringify(value));
const pack = () => ({label:'Embalagem',l:null,w:null,h:null,weight:null,quantity:1,rotatable:true,stackable:true,status:'PENDING',source:'',measuredBy:'',measuredAt:''});
const config = () => ({id:uid(),sku:'',name:'',preparedProductId:'',lines:[]});
const roles = {PACKAGED:'Produto embalado',ACCOMPANYING:'Caixa acompanhante',NESTED:'Dentro da caixa da placa-mãe',INSTALLED:'Instalado, sem caixa separada'};
const categories = {PC_PREPARED:'PC preparado para transporte',CPU:'Processador',MOTHERBOARD:'Placa-mãe',MEMORY:'Memória',SSD:'SSD / M.2',FAN:'Ventoinha',COOLER:'Cooler',GPU:'Placa de vídeo',PSU:'Fonte',CASE:'Gabinete',PERIPHERAL:'Periférico',MONITOR:'Monitor',MONITOR_SUPPORT:'Suporte de monitor',UPS:'Nobreak',CHAIR:'Cadeira',DESK:'Mesa',DESK_LEG:'Pé de mesa',OTHER:'Outro'};
const LineEditor = {
  props:{lines:Array,products:Array,roles:Object,standalone:Boolean},
  methods:{
    add(){this.lines.push({id:uid(),productId:'',quantity:1,origin:this.standalone?'STANDALONE':'BASE',role:'PACKAGED'});},
    change(line){const p=this.products.find(p=>p.id===line.productId);line.role=this.standalone?'PACKAGED':p?.role||'PACKAGED';if(!this.standalone)line.origin='CUSTOMIZATION';}
  },
  template:`<div class="line-editor"><div v-for="(line,index) in lines" :key="line.id" class="line-item"><label class="line-product">Produto<select v-model="line.productId" required @change="change(line)"><option value="">Selecione…</option><option v-for="p in products.filter(p=>p.category!=='PC_PREPARED')" :value="p.id">{{p.sku}} · {{p.name}}</option></select></label><label class="line-qty">Qtd.<input type="number" min="1" max="100" step="1" v-model.number="line.quantity" required></label><template v-if="!standalone"><label>Origem<select v-model="line.origin"><option value="BASE">Configuração base</option><option value="CUSTOMIZATION">Personalização</option></select></label><label>Tratamento no PC<select v-model="line.role"><option v-for="(v,k) in roles" :value="k">{{v}}</option></select></label></template><button type="button" class="remove-line" @click="lines.splice(index,1)" aria-label="Remover item">×</button></div><button type="button" class="button secondary small" @click="add">＋ Adicionar item</button><p v-if="!lines.length" class="muted">Nenhum item adicionado.</p></div>`
};
const ConfigurationEditor = {
  props:{config:Object,products:Array,categories:Object,roles:Object},components:{LineEditor},
  template:`<div><div class="form-grid"><label>SKU comercial do PC<input v-model.trim="config.sku" required></label><label>Nome da configuração<input v-model.trim="config.name" required></label><label class="span2">Perfil do PC preparado para esta composição<select v-model="config.preparedProductId"><option value="">Pendente de cadastro</option><option v-for="p in products.filter(p=>p.category==='PC_PREPARED')" :value="p.id">{{p.sku}} · {{p.name}}</option></select></label></div><p class="muted">Uma troca de componente pode mudar peso e preparação. Confirme que o perfil do PC representa a composição final. A cadeira e demais móveis são separados automaticamente.</p><line-editor :lines="config.lines" :products="products" :roles="roles"></line-editor></div>`
};
createApp({
  components:{ConfigurationEditor,LineEditor},
  data:()=>({
    state:{products:[],boxes:[],templates:[],orders:[],history:[],generation:0},loading:true,busy:false,error:'',toast:'',view:'dashboard',search:'',queueFilter:'',
    navigation:[{id:'dashboard',label:'Visão geral',icon:'◫'},{id:'orders',label:'Fila de embalagem',icon:'▧'},{id:'templates',label:'Configurações',icon:'◇'},{id:'products',label:'Produtos',icon:'▦'},{id:'boxes',label:'Caixas',icon:'⬡'},{id:'pending',label:'Medições pendentes',icon:'◷'},{id:'history',label:'Histórico',icon:'↺'}],
    categories,roles,dataStatuses:{PENDING:'Pendente',DEMO:'Demonstração',VALIDATED:'Validado'},statuses:{WAITING:'Aguardando',REVIEW:'Em revisão',PACKING:'Em embalagem',DONE:'Concluído'},groupKinds:{PC_MAIN:'CAIXA PRINCIPAL',STANDALONE_SHARED:'AVULSOS AGRUPADOS',FACTORY_DIRECT:'EMBALAGEM DO FABRICANTE'},
    colors:['#f3ad83','#8fbeb4','#acc3e0','#d8c8a4','#c4afda','#edc96d','#87b6c9'],
    modal:null,draft:null,existingDraft:false,modalError:'',selectedTemplate:'',activeId:null,plan:null,checklist:[],planLoading:false,scanCode:'',noteText:'',physicalConfirmed:false
  }),
  computed:{
    pageTitle(){return this.view==='detail'?'Pedido':this.navigation.find(n=>n.id===this.view)?.label||'Operação';},
    pendingCount(){return this.state.products.filter(p=>this.productStatus(p)!=='VALIDATED').length+this.state.boxes.filter(b=>b.status!=='VALIDATED').length;},
    openOrders(){return this.state.orders.filter(o=>o.status!=='DONE');},
    filteredOrders(){const q=this.search.toLowerCase();return this.state.orders.filter(o=>(!this.queueFilter||o.status===this.queueFilter)&&`${o.reference} ${o.operator}`.toLowerCase().includes(q));},
    filteredProducts(){const q=this.search.toLowerCase();return this.state.products.filter(p=>(this.view!=='pending'||this.productStatus(p)!=='VALIDATED')&&`${p.name} ${p.sku} ${p.barcode}`.toLowerCase().includes(q));},
    filteredHistory(){return this.state.history.filter(e=>`${e.reference} ${e.action} ${e.operator} ${e.detail}`.toLowerCase().includes(this.search.toLowerCase()));},
    activeOrder(){return this.state.orders.find(o=>o.id===this.activeId);},
    checkedTotal(){return this.checklist.reduce((s,e)=>s+(this.activeOrder?.checked[e.key]||0),0);},
    checklistComplete(){return this.checklist.length>0&&this.checklist.every(e=>(this.activeOrder?.checked[e.key]||0)===e.quantity);},
    modalTitle(){return {product:'Produto e embalagens',box:'Caixa de expedição',template:'Modelo de configuração',order:'Pedido e personalizações'}[this.modal]||'';}
  },
  watch:{view(){window.scrollTo(0,0);}},
  async mounted(){await this.refresh();},
  methods:{
    newPack:pack,newConfiguration:config,
    dimensions(p){return [p.l,p.w,p.h].every(v=>v!=null&&v!=='')?`${p.l} × ${p.w} × ${p.h}`:'Medidas pendentes';},
    date(value){return value?new Date(value).toLocaleString('pt-BR'):'—';},
    statusClass(s){return {DONE:'teal',REVIEW:'amber',PACKING:'blue',WAITING:'gray'}[s];},
    productStatus(p){if(!p.packages.length)return p.role==='INSTALLED'?'VALIDATED':'PENDING';if(p.packages.some(x=>x.status==='PENDING'||[x.l,x.w,x.h,x.weight].some(v=>v==null)))return 'PENDING';return p.packages.some(x=>x.status==='DEMO')?'DEMO':'VALIDATED';},
    async request(url,method='GET',body){const response=await fetch('/api'+url,{method,headers:{'Content-Type':'application/json','X-State-Revision':String(this.state.generation)},body:body===undefined?undefined:JSON.stringify(body)});if(!response.ok){let message='Não foi possível concluir. Confira se o servidor está funcionando.';try{const err=await response.json();message=err.message||message;}catch{}if(response.status===409)this.state=await this.request('/state');throw new Error(message);}return response.json();},
    async refresh(){try{this.state=await this.request('/state');if(this.view==='detail'&&this.activeOrder)await this.loadPlan();this.error='';}catch(e){this.error=e.message;}finally{this.loading=false;}},
    notify(message){this.toast=message;clearTimeout(this.toastTimer);this.toastTimer=setTimeout(()=>this.toast='',4500);},
    async mutate(url,method,body){if(this.busy)return false;this.busy=true;try{this.state=await this.request(url,method,body);this.error='';return true;}catch(e){if(this.modal)this.modalError=e.message;else this.error=e.message;return false;}finally{this.busy=false;}},
    async loadDemo(){if(await this.mutate('/demo','POST',{})){this.notify('Demonstração carregada. Todas as medidas são fictícias.');}},
    openModal(kind,value,existing=false){this.modal=kind;this.draft=copy(value);this.existingDraft=existing;this.modalError='';this.selectedTemplate='';},
    closeModal(){if(!this.busy){this.modal=null;this.draft=null;this.modalError='';}},
    editProduct(p){this.openModal('product',p||{id:uid(),sku:'',name:'',category:'OTHER',role:'PACKAGED',barcode:'',oem:false,revision:1,packages:[pack()]},!!p);},
    editBox(b){this.openModal('box',b||{id:uid(),code:'',l:null,w:null,h:null,outerL:null,outerW:null,outerH:null,tare:null,maxWeight:null,available:true,status:'PENDING',revision:1,source:'',measuredBy:'',measuredAt:''},!!b);},
    editTemplate(c){this.openModal('template',c||config(),!!c);},
    cloneConfig(c){const cloned=copy(c);cloned.id=uid();cloned.lines.forEach(l=>l.id=uid());return cloned;},
    newOrder(template){this.openModal('order',{id:uid(),reference:'',operator:'',status:'WAITING',note:'',configurations:template?.lines?[this.cloneConfig(template)]:[],standalone:[],checked:{},physicallyValidated:false});},
    editOrder(o){this.openModal('order',o,true);},
    addTemplate(){const found=this.state.templates.find(c=>c.id===this.selectedTemplate);if(found){this.draft.configurations.push(this.cloneConfig(found));this.selectedTemplate='';}},
    normalize(value){const d=copy(value);const toNumber=v=>v===''||v==null?null:Number(v);if(this.modal==='product')d.packages.forEach(p=>['l','w','h','weight'].forEach(k=>p[k]=toNumber(p[k])));if(this.modal==='box')['l','w','h','outerL','outerW','outerH','tare','maxWeight'].forEach(k=>d[k]=toNumber(d[k]));return d;},
    async saveModal(){this.modalError='';const kind=this.modal;const draft=this.normalize(this.draft);const paths={product:'products',box:'boxes',template:'templates',order:'orders'};if(await this.mutate(`/${paths[kind]}/${draft.id}`,'PUT',draft)){this.closeModal();this.notify('Salvo neste computador.');if(kind==='order')await this.openOrder(draft);else if(this.view==='detail')await this.loadPlan();}},
    async deleteDraft(){if(!confirm('Excluir este cadastro? Produtos utilizados em pedidos ou configurações não podem ser excluídos.'))return;const paths={product:'products',box:'boxes',template:'templates'};if(await this.mutate(`/${paths[this.modal]}/${this.draft.id}`,'DELETE')){this.closeModal();this.notify('Cadastro excluído.');}},
    async openOrder(o){this.activeId=o.id;this.view='detail';this.physicalConfirmed=false;this.scanCode='';this.noteText='';await this.loadPlan();},
    async loadPlan(){this.planLoading=true;this.plan=null;try{[this.plan,this.checklist]=await Promise.all([this.request(`/orders/${this.activeId}/plan`),this.request(`/orders/${this.activeId}/checklist`)]);}catch(e){this.error=e.message;}finally{this.planLoading=false;}},
    async check(item,manual=false){if(await this.mutate(`/orders/${this.activeId}/check`,'POST',{key:item.key,code:this.scanCode.trim(),manual})){this.scanCode='';this.notify('Unidade conferida.');this.$refs.scanner?.focus();}},
    async scan(){const code=this.scanCode.trim();if(!code)return;const item=this.checklist.find(e=>(e.sku.toLowerCase()===code.toLowerCase()||e.barcode===code)&&(this.activeOrder.checked[e.key]||0)<e.quantity);if(!item){this.error='Código não pertence ao pedido ou todas as unidades já foram conferidas.';return;}await this.check(item,false);},
    async complete(){if(await this.mutate(`/orders/${this.activeId}/complete`,'POST',{physicallyValidated:this.physicalConfirmed,note:'Composição, proteção, agrupamento interno e disposição validados fisicamente.'})){this.notify('Embalagem concluída e plano preservado.');await this.loadPlan();}},
    async addNote(){if(await this.mutate(`/orders/${this.activeId}/notes`,'POST',{text:this.noteText})){this.noteText='';this.notify('Ocorrência registrada.');}},
    print(){window.print();},
    download(name,value){const link=document.createElement('a');const url=URL.createObjectURL(new Blob([JSON.stringify(value,null,2)],{type:'application/json'}));link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);},
    async downloadBackup(){try{this.download(`inbox-backup-${new Date().toISOString().slice(0,10)}.json`,await this.request('/backup'));this.notify('Backup exportado.');}catch(e){this.error=e.message;}},
    async restoreBackup(event){const file=event.target.files[0];event.target.value='';if(!file)return;if(file.size>5_000_000){this.error='Backup limitado a 5 MB.';return;}if(!confirm('Restaurar substitui a base local inteira. Exporte um backup antes. Conferências e conclusões importadas precisarão de nova validação. Continuar?'))return;try{const data=JSON.parse(await file.text());if(await this.mutate('/restore','POST',data)){this.view='dashboard';this.activeId=null;this.notify('Backup restaurado. Revise os dados antes de operar.');}}catch{this.error='Arquivo de backup inválido.';}},
    downloadImportExample(){this.download('modelo-pedido-inbox.json',{reference:'PEDIDO-EXEMPLO',operator:'Nome do operador',note:'Substitua os SKUs pelos cadastros reais.',configurations:[{sku:'SKU-COMERCIAL-PC',name:'PC personalizado',preparedSku:'SKU-PC-PREPARADO',lines:[{sku:'SKU-PLACA-MAE',quantity:1,origin:'BASE',role:'ACCOMPANYING'},{sku:'SKU-COOLER-ESCOLHIDO',quantity:1,origin:'CUSTOMIZATION',role:'ACCOMPANYING'}]}],standalone:[{sku:'SKU-FONE',quantity:1}]});},
    async importOrder(event){const file=event.target.files[0];event.target.value='';if(!file)return;try{if(file.size>500_000)throw Error('Arquivo de pedido limitado a 500 KB.');const raw=JSON.parse(await file.text());const resolve=sku=>{const p=this.state.products.find(p=>p.sku===sku);if(!p)throw Error(`SKU não cadastrado: ${sku}`);return p;};const line=(l,standalone)=>{const p=resolve(l.sku);return {id:uid(),productId:p.id,quantity:l.quantity??1,origin:standalone?'STANDALONE':l.origin||'BASE',role:standalone?'PACKAGED':l.role||p.role};};const configurations=(raw.configurations||[]).map(c=>({id:uid(),sku:c.sku,name:c.name,preparedProductId:c.preparedSku?resolve(c.preparedSku).id:'',lines:(c.lines||[]).map(l=>line(l,false))}));this.openModal('order',{id:uid(),reference:raw.reference||'',operator:raw.operator||'',note:raw.note||'',status:'WAITING',configurations,standalone:(raw.standalone||[]).map(l=>line(l,true)),checked:{},physicallyValidated:false});this.notify('Pedido importado para revisão. Confira e salve.');}catch(e){this.error=e.message||'Arquivo de pedido inválido.';}}
  }
}).mount('#app');
