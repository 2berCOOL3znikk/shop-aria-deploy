'use strict';
// Wire bytes, not fetch's automatically decompressed body. No business-data writes or model calls.
const fs=require('node:fs'),http=require('node:http'),zlib=require('node:zlib'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const read=file=>JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const report={environment:'two owned isolated previews, postgres + free profiles',productionChanged:false,businessWrites:0,aiRequested:false,environments:[]};
const assets=['/js/app-copy-data.js','/js/i18n.js','/js/control.js','/css/experience.css','/control.html'];
function raw(base,resource,encoding='gzip',token){return new Promise((resolve,reject)=>{
 const request=http.get(base+resource,{headers:{'Accept-Encoding':encoding,...(token?{Authorization:'Bearer '+token}:{})}},response=>{
  const parts=[];response.on('data',part=>parts.push(part));response.on('end',()=>resolve({status:response.statusCode,headers:response.headers,body:Buffer.concat(parts)}));
 });request.setTimeout(20000,()=>request.destroy(new Error('Local response timeout')));request.on('error',reject);
});}
const sha=body=>crypto.createHash('sha256').update(body).digest('hex');
(async()=>{try{
 for(const [base,configPath,binding] of [['http://127.0.0.1:8085','data/woocommerce/aria-config.json','woo-e2e'],['http://127.0.0.1:8087','data/localization-preview/config.json','demo']]){
  const cfg=read(configPath),login=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:cfg.email,password:cfg.password})});assert.equal(login.status,200);const {token}=await login.json();assert.ok(token);
  const organization=await raw(base,'/api/admin/control/organization','gzip',token);assert.equal(organization.status,200);assert.equal(JSON.parse(organization.body).binding,binding);
  const environment={base,binding,assets:[],api:[]};report.environments.push(environment);
  for(const resource of assets){
   const identity=await raw(base,resource,'identity'),compressed=await raw(base,resource,'gzip'),source=fs.readFileSync('src/main/resources/static'+resource);
   assert.equal(identity.status,200,resource);assert.equal(compressed.status,200,resource);assert.equal(identity.headers['content-encoding'],undefined);
   assert.equal(compressed.headers['content-encoding'],'gzip',resource);assert.match(compressed.headers.vary.toLowerCase(),/accept-encoding/);
   const decoded=zlib.gunzipSync(compressed.body);assert.equal(sha(decoded),sha(identity.body));assert.equal(sha(decoded),sha(source));assert.ok(compressed.body.length<source.length);
   environment.assets.push({resource,uncompressedBytes:identity.body.length,wireBytes:compressed.body.length,savedPercent:Math.round((1-compressed.body.length/identity.body.length)*1000)/10,decodedMatchesSource:true,varyAcceptEncoding:true});
  }
  for(const resource of ['/api/admin/control/products','/api/admin/control/audit',...(binding==='demo'?['/api/products']:[])]){
   const response=await raw(base,resource,'gzip',token);assert.equal(response.status,200);assert.match(response.headers['content-type'],/^application\/json/);assert.equal(response.headers['content-encoding'],undefined);JSON.parse(response.body);
   if(resource.endsWith('/audit'))assert.ok(response.body.length>2048,'Check a private JSON response above the compression threshold');
   environment.api.push({resource,wireBytes:response.body.length,encoding:'identity',parsed:true});
  }
  const unauthorized=await raw(base,'/api/admin/control/products');assert.equal(unauthorized.status,401);assert.equal(unauthorized.headers['content-encoding'],undefined);
  const binary=await raw(base,'/images/aria-app-192.png');assert.equal(binary.status,200);assert.equal(binary.headers['content-encoding'],undefined);
  environment.unauthorizedStillDenied=true;environment.binaryNotRecompressed=true;
  environment.threeScriptTotals=environment.assets.slice(0,3).reduce((sum,file)=>({uncompressedBytes:sum.uncompressedBytes+file.uncompressedBytes,wireBytes:sum.wireBytes+file.wireBytes}),{uncompressedBytes:0,wireBytes:0});
  environment.threeScriptTotals.savedPercent=Math.round((1-environment.threeScriptTotals.wireBytes/environment.threeScriptTotals.uncompressedBytes)*1000)/10;
 }
 report.passed=true;
}catch(error){report.failure=error.stack;throw error;}finally{report.completedAt=new Date().toISOString();fs.writeFileSync('docs/platform/evidence/static-transfer-after.json',JSON.stringify(report,null,2));}
})().catch(error=>{console.error(error.stack);process.exitCode=1;});
