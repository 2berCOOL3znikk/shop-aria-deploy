'use strict';
// Opt-in repair for this loopback demo's known historical QA fixtures. No deletion or production access.
const fs=require('node:fs'),assert=require('node:assert/strict');
async function main(){
 const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));assert.equal(cfg.port,8084);
 const base='http://127.0.0.1:'+cfg.port,apply=process.argv.includes('--apply-known-fixtures');let token;
 const api=async(path,method='GET',data)=>{const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:data?JSON.stringify(data):undefined}),out=await r.json();assert.equal(r.status,200,path+' '+JSON.stringify(out));return out;};
 token=(await api('/api/auth/login','POST',{email:cfg.email,password:cfg.password})).token;
 const boundary=await api('/api/admin/control/organization'),listing=await api('/api/admin/control/stores');assert.equal(boundary.binding,'demo');assert.equal(listing.organization.demonstration,true);
 const known=s=>s.platform==='WOOCOMMERCE'&&['https://shop.example.invalid','https://queue.example.invalid'].includes(s.url)&&s.products===0&&s.orders===0&&(/^(WooCommerce [·—] проверка|Проверка сохранения запроса) \d{10,16}$/.test(s.name)||s.testConnection&&s.name==='Учебное подключение WooCommerce');
 const targets=listing.stores.filter(known),report={applied:apply,scope:base,records:[],productionChanged:false};
 for(const original of targets){let current=original;if(apply){if(!current.testConnection||/\d{10,16}$/.test(current.name))current=await api('/api/admin/control/stores/'+current.id+'/presentation','PUT',{version:current.version,name:'Учебное подключение WooCommerce',testConnection:true,informationConfirmed:true,reason:'Распознана собственная историческая QA-запись в отдельном loopback-preview, без товаров и заказов'});if(current.archived&&current.status!=='DISCONNECTED')current=await api('/api/admin/control/stores/'+current.id+'/restore','POST',{version:current.version,reason:'Подготовка исторического QA-архива к совместимому отключённому состоянию'});if(!current.archived)current=await api('/api/admin/control/stores/'+current.id+'/archive','POST',{version:current.version,reason:'Историческое учебное подключение скрыто из актуальных; каталог, задания и история сохранены'});assert.equal(current.id,original.id);assert.equal(current.products,original.products);assert.equal(current.orders,original.orders);assert.equal(current.hasCredentials,original.hasCredentials);}
  report.records.push({id:original.id,productsRetained:current.products,ordersRetained:current.orders,accessRetained:current.hasCredentials,archived:current.archived,testConnection:current.testConnection});
 }
 const after=await api('/api/admin/control/stores');assert.equal(after.stores.length,listing.stores.length);report.connectionCount=after.stores.length;report.currentCount=after.stores.filter(s=>!s.archived&&!s.testConnection).length;
 fs.mkdirSync('target/e2e/product-experience',{recursive:true});fs.writeFileSync('target/e2e/product-experience/preview-store-organization.json',JSON.stringify(report,null,2));console.log(JSON.stringify({apply,recognized:targets.length,current:report.currentCount,retained:report.connectionCount}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
