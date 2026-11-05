// Local TLS termination only. The certificate is trusted by the isolated ARIA JVM,
// never installed in Windows or used to disable certificate verification.
const https=require('node:https'),http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../../data/woocommerce');
const server=https.createServer({key:fs.readFileSync(path.join(root,'tls-key.pem')),cert:fs.readFileSync(path.join(root,'tls-cert.pem'))},(req,res)=>{
 const upstream=http.request({hostname:'127.0.0.1',port:9089,path:req.url,method:req.method,headers:{...req.headers,'x-forwarded-proto':'https'}},reply=>{
  const faultFile=path.join(root,'drop-next-response.json');let drop=false;
  // Explicit local transport fault after WooCommerce has handled one real PUT.
  if(req.method==='PUT'&&reply.statusCode===200&&fs.existsSync(faultFile)){
   const fault=JSON.parse(fs.readFileSync(faultFile,'utf8'));
   if(req.url===fault.path&&/^\/wp-json\/wc\/v3\/products\/\d+(\/variations\/\d+)?$/.test(fault.path)){drop=true;fs.unlinkSync(faultFile);}
  }
  if(req.url.startsWith('/wp-json/wc/v3/'))fs.appendFileSync(path.join(root,'request-audit.jsonl'),JSON.stringify({at:new Date().toISOString(),method:req.method,path:req.url.split('?')[0],status:reply.statusCode,responseDropped:drop})+'\n');
  if(drop){reply.resume();reply.on('end',()=>res.destroy());return;}
  res.writeHead(reply.statusCode,reply.headers);reply.pipe(res);
 });
 upstream.on('error',()=>{res.writeHead(503,{'content-type':'text/plain'});res.end('Isolated WordPress is not available');});req.pipe(upstream);
});
server.listen(8449,'127.0.0.1',()=>console.log('Isolated WooCommerce: https://127.0.0.1:8449'));
