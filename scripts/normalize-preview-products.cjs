'use strict';
// A bounded repair of our own historic QA names. Does not change IDs or stock.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {request}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
(async()=>{
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
  const base='http://127.0.0.1:'+cfg.port;
  assert.ok(Number.isInteger(cfg.port)&&cfg.port>=1024&&cfg.port<=65535);
  const http=await request.newContext();const changes=[];
  try{
    const info=await http.get(base+'/api/products/store-info');assert.equal(info.status(),200);assert.equal((await info.json()).demonstration,true);
    const login=await http.post(base+'/api/auth/login',{data:{email:cfg.email,password:cfg.password}});assert.equal(login.status(),200);const headers={Authorization:'Bearer '+(await login.json()).token};
    const list=await http.get(base+'/api/admin/control/products',{headers});assert.equal(list.status(),200);
    for(const p of await list.json()){
      let name=null;
      if(/^E2E-COFFEE-\d{13}$/.test(p.sku)&&/^Кофе — сквозная проверка \d{13}$/.test(p.name))name='Кофе — учебная карточка';
      if(/^E2E-YOGHURT-\d{13}$/.test(p.sku)&&/^Йогурт — социальная проверка \d{13}$/.test(p.name))name='Йогурт — учебная карточка';
      if(/^E2E-CHOCOLATE-\d{13}$/.test(p.sku)&&/^Шоколад — сравнение товаров \d{13}$/.test(p.name))name='Шоколад — учебная карточка';
      if(/^PLATFORM-\d{13}$/.test(p.sku)&&/^Товар кассы и закупки \d{13}$/.test(p.name))name='Товар для проверки кассы и закупки';
      if(!name||p.archived)continue;
      const original=await http.get(base+'/api/admin/control/products/'+p.id,{headers});assert.equal(original.status(),200);const {product,details}=await original.json();
      // Preserve concurrently edited names rather than overwriting a later owner choice.
      if(product.name!==p.name)continue;
      const body={sku:product.sku,name,category:product.category,unitPrice:product.unitPrice,minimumPrice:product.minimumPrice,brand:details?.brand||null,description:details?.descriptionLong||null,imageUrl:product.imageUrl,barcode:product.barcode,unit:product.unit||'шт',lowStockThreshold:product.lowStockThreshold??5};
      const saved=await http.put(base+'/api/admin/control/products/'+p.id,{headers,data:body});assert.equal(saved.status(),200);
      changes.push({id:p.id,sku:p.sku,before:p.name,after:name});
    }
    fs.mkdirSync('target/e2e/storefront',{recursive:true});fs.writeFileSync('target/e2e/storefront/legacy-name-repair.json',JSON.stringify({changes,productionTouched:false,stockChanged:false},null,2));
    console.log('Historic QA names repaired: '+changes.length+'; IDs, stock and order snapshots retained.');
  }finally{await http.dispose();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
