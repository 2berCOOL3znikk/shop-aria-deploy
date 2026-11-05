'use strict';
// Deterministic checks; no browser, model download, network or shop data required.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),base=path.join(root,'src/main/resources/static'),file=path.join(base,'js/app-copy-data.js'),raw=fs.readFileSync(file,'utf8'),prefix='window.ARIA_COPY_DICTIONARY=';
assert.ok(raw.includes(prefix));const dictionary=JSON.parse(raw.slice(raw.indexOf(prefix)+prefix.length).trim().replace(/;$/,''));
const slots=value=>[...value.matchAll(/§(\d+)§/g)].map(m=>m[1]).sort();
for(const [key,entry] of Object.entries(dictionary)){
 assert.equal(entry.length,2,key);assert.ok(entry.every(v=>typeof v==='string'),key);
 assert.equal(key.split('.')[1],crypto.createHash('sha256').update(entry[0]).digest('hex').slice(0,14),'Source key changed: '+key);
 assert.deepEqual(slots(entry[1]),slots(entry[0]),'Template parameters changed: '+key);
}
let scripts=0,references=0;
for(const relative of [...fs.readdirSync(path.join(base,'js')).filter(f=>f.endsWith('.js')).map(f=>'js/'+f),'sw.js',...fs.readdirSync(base).filter(f=>f.endsWith('.html'))]){
 const source=fs.readFileSync(path.join(base,relative),'utf8');
 if(relative.endsWith('.js')){new vm.Script(source,{filename:relative});scripts++;}
 for(const match of source.matchAll(/ariaCopy\.(?:text|template)\("([^"]+)"|data-copy(?:-[a-z-]+)?="((?:text|template|system)\.[a-f0-9]+)"/g)){
  const key=match[1]||match[2];assert.ok(Object.hasOwn(dictionary,key),'Missing copy key in '+relative+': '+key);references++;
 }
}
const en=JSON.parse(fs.readFileSync(path.join(base,'manifest-en.webmanifest'),'utf8'));assert.equal(en.lang,'en');
console.log(JSON.stringify({passed:true,keys:Object.keys(dictionary).length,references,scripts,templatesPreserveParameters:true}));
