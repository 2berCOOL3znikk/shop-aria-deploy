'use strict';
// One-time source migration. Explicit keys replace static UI literals/templates.
// This never walks runtime text nodes and never reads shop data.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'../..'),base=path.join(root,'src/main/resources/static'),snapshot=path.join(root,'data/localization-tools/before-keys');
let parser;
const translations=JSON.parse(fs.readFileSync(path.join(__dirname,'translations.en.json'),'utf8'));
const systemTranslations=JSON.parse(fs.readFileSync(path.join(__dirname,'system.en.json'),'utf8'));
const refresh=process.argv.includes('--refresh-dictionary'),reapply=process.argv.includes('--reapply-from-snapshot'),dictionary={},manifest=[],unresolved=new Set(),pendingWrites=new Map();
const ru=/[А-Яа-яЁё]/,protectedValues=new Set(['продукт','продукты','Продукты питания','Дом/текстиль','дней','ru','en']);
const staticAttrs=/\b(aria-label|placeholder|title|alt|data-query|data-label)=(['"])(.*?)\2/gs;
function english(text){
 if(!ru.test(text))return text;
 if(Object.hasOwn(translations,text))return translations[text];
 if(Object.hasOwn(systemTranslations,text))return systemTranslations[text];
 const trimmed=text.trim();if(Object.hasOwn(translations,trimmed))return text.slice(0,text.indexOf(trimmed))+translations[trimmed]+text.slice(text.indexOf(trimmed)+trimmed.length);
 unresolved.add(text);return text;
}
const htmlEscape=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
function translateMarkup(value){
 const html=/[<>]/.test(value),attrs=/\b(?:aria-label|placeholder|title|alt|data-query)\s*=/.test(value);
 if(!html&&!attrs)return english(value);
 let translated=html?value.replace(/(^|>)([^<>]*)(?=<|$)/g,(all,prefix,text)=>prefix+(ru.test(text)?htmlEscape(english(text)):text)):value;
 return translated.replace(staticAttrs,(all,key,quote,text)=>ru.test(text)?key+'='+quote+htmlEscape(english(text))+quote:all);
}
function register(value,kind='text'){
 const key=kind+'.'+crypto.createHash('sha256').update(value).digest('hex').slice(0,14);
 if(dictionary[key]&&dictionary[key][0]!==value)throw new Error('Copy key collision');
 dictionary[key]=[value,translateMarkup(value)];return key;
}
// Normal maintenance works on a clean checkout, without the one-time authoring snapshots.
if(refresh){
 const bundle=path.join(base,'js/app-copy-data.js'),prefix='window.ARIA_COPY_DICTIONARY=',raw=fs.readFileSync(bundle,'utf8'),start=raw.indexOf(prefix);
 if(start<0)throw new Error('Copy dictionary was not found');
 const entries=JSON.parse(raw.slice(start+prefix.length).trim().replace(/;$/,''));
 for(const [key,entry] of Object.entries(entries))dictionary[key]=[entry[0],translateMarkup(entry[0])];
 for(const [source,en] of Object.entries(JSON.parse(fs.readFileSync(path.join(__dirname,'system.en.json'),'utf8'))))dictionary['system.'+crypto.createHash('sha256').update(source).digest('hex').slice(0,14)]=[source,en];
 if(unresolved.size)throw new Error('Translations missing for '+unresolved.size+' source phrases');
 fs.writeFileSync(bundle,'// Explicit source-copy dictionary. No shop data.\nwindow.ARIA_COPY_DICTIONARY='+JSON.stringify(dictionary)+';\n');
 const manifestPath=path.join(__dirname,'manifest.json');if(fs.existsSync(manifestPath)){const metadata=JSON.parse(fs.readFileSync(manifestPath,'utf8'));metadata.keyCount=Object.keys(dictionary).length;fs.writeFileSync(manifestPath,JSON.stringify(metadata,null,2)+'\n');}
 console.log(JSON.stringify({keys:Object.keys(dictionary).length,refreshedOnly:true}));process.exit(0);
}
parser=require(path.join(root,'target/localization-tools/parser/package/lib/index.js'));
function original(relative){const file=path.join(base,relative),saved=path.join(snapshot,relative);if(refresh||reapply){if(!fs.existsSync(saved))throw new Error('Missing authoring snapshot: '+relative);return fs.readFileSync(saved,'utf8');}if(fs.readFileSync(file,'utf8').includes('ariaCopy.text('))throw new Error('Already migrated; use --refresh-dictionary');fs.mkdirSync(path.dirname(saved),{recursive:true});fs.copyFileSync(file,saved);return fs.readFileSync(file,'utf8');}
function walk(n,fn,parents=[]){if(!n||typeof n!=='object')return;fn(n,parents);for(const[k,v]of Object.entries(n))if(!['loc','tokens','comments','extra'].includes(k)){if(Array.isArray(v))v.forEach(x=>walk(x,fn,[...parents,n]));else if(typeof v==='object')walk(v,fn,[...parents,n]);}}
function skipLiteral(n,parents){
 const p=parents.at(-1);if(protectedValues.has(n.value))return true;
 if(p?.type==='ObjectProperty'&&p.key===n)return true;
 if(p?.type==='BinaryExpression'&&p.operator!=='+'||['SwitchCase','Directive'].includes(p?.type))return true;
 const declaration=[...parents].reverse().find(a=>a.type==='VariableDeclarator'),labelMap=/(?:Names|names|Labels|labels|tabs|columns|titles|fields)/.test(declaration?.id?.name||'');
 if(!labelMap&&p?.type==='ObjectProperty'&&['name','sku','category','brand','manufacturer','unit','lotNumber'].includes(p.key.name||p.key.value))return true;
 if(p?.type==='ArrayExpression'&&p.elements.length===2&&p.elements[0]===n&&p.elements[1]?.type==='StringLiteral'&&ru.test(p.elements[1].value))return true;
 for(const a of parents.slice(-3)){
  if(a.type==='CallExpression'&&a.callee.type==='MemberExpression'&&['includes','startsWith','endsWith','replace','replaceAll','split','match','indexOf','querySelector','querySelectorAll'].includes(a.callee.property.name))return true;
  if(a.type==='CallExpression'&&['formField','selectField'].includes(a.callee.name)&&a.arguments[3]&&n.start>=a.arguments[3].start&&n.end<=a.arguments[3].end)return true;
 }
 return false;
}
const files=fs.readdirSync(path.join(base,'js')).filter(f=>f.endsWith('.js')&&f!=='sw.js'&&!/i18n|copy|connected-|social-mvp/.test(f));
for(const name of files){
 const relative='js/'+name,code=original(relative),ast=parser.parse(code),parents=new Map();walk(ast,(n,p)=>parents.set(n,p));let touched=0;
 function render(n){
  if(!n||typeof n!=='object'||typeof n.start!=='number')return '';
  const chain=parents.get(n)||[];
  // Once data populates an element, its initial static placeholder must not overwrite it.
  if(n.type==='AssignmentExpression'&&n.operator==='='&&n.left.type==='MemberExpression'&&!n.left.computed&&['textContent','innerText','innerHTML','title','alt','placeholder','ariaLabel'].includes(n.left.property.name))return 'ariaCopy.setProperty('+render(n.left.object)+','+JSON.stringify(n.left.property.name)+','+render(n.right)+')';
  if(n.type==='CallExpression'&&n.callee.type==='MemberExpression'&&n.callee.property.name==='setAttribute'&&n.arguments[0]?.type==='StringLiteral'&&['title','alt','placeholder','aria-label'].includes(n.arguments[0].value))return 'ariaCopy.setAttribute('+render(n.callee.object)+','+render(n.arguments[0])+','+render(n.arguments[1])+')';
  if(n.type==='StringLiteral'&&ru.test(n.value)&&!skipLiteral(n,chain)){touched++;return 'ariaCopy.text('+JSON.stringify(register(n.value))+')';}
  if(n.type==='StringLiteral'&&['ru-KZ','ru-RU'].includes(n.value)){touched++;return 'ariaCopy.locale()';}
  const merchantExpression=x=>x.type==='CallExpression'&&x.callee.name==='esc'&&/\.(?:name|fullName|description|reason|details|note|category|sku|requirements|explanation)\b/.test(code.slice(x.start,x.end));
  const templateHtml=n.type==='TemplateLiteral'&&n.quasis.some(q=>/[<>]/.test(q.value.cooked||''));
  if(n.type==='TemplateLiteral'&&chain.at(-1)?.type!=='TaggedTemplateExpression'&&(n.quasis.some(q=>ru.test(q.value.cooked||''))||templateHtml&&n.expressions.some(merchantExpression))){
   const text=n.quasis.map((q,i)=>q.value.cooked+(i<n.expressions.length?'§'+i+'§':'')).join('');touched++;
   const expressions=n.expressions.map((x,i)=>{let rendered=render(x);const prefix=n.quasis.slice(0,i+1).map(q=>q.value.cooked).join(''),textOnlyContext=/<(?:textarea|option|title)\b[^>]*>[^<]*$/i.test(prefix);if(templateHtml&&!textOnlyContext&&prefix.lastIndexOf('>')>prefix.lastIndexOf('<')&&merchantExpression(x)){const label=/\.(?:reason|details|note|explanation)\b/.test(code.slice(x.start,x.end));rendered='ariaCopy.original('+rendered+','+label+')';}return rendered;});
   return 'ariaCopy.template('+JSON.stringify(register(text,'template'))+',['+expressions.join(',')+'])';
  }
  const children=[];for(const[k,v]of Object.entries(n))if(!['loc','tokens','comments','extra','quasis'].includes(k)){
   for(const child of Array.isArray(v)?v:[v])if(child&&typeof child==='object'&&typeof child.start==='number'&&child.start>=n.start&&child.end<=n.end)children.push(child);
  }
  let text=code.slice(n.start,n.end);for(const child of children.sort((a,b)=>b.start-a.start)){let value=render(child);if(n.type==='NewExpression'&&n.callee.name==='Error'&&n.arguments[0]===child)value='ariaCopy.errorText('+value+')';text=text.slice(0,child.start-n.start)+value+text.slice(child.end-n.start);}return text;
 }
 const result=render(ast.program);parser.parse(result);if(!refresh)pendingWrites.set(path.join(base,relative),result);manifest.push({file:relative,keys:touched});
}
// Static HTML is migrated separately: only explicitly marked source text/attributes.
const htmlFiles=fs.readdirSync(base).filter(f=>f.endsWith('.html'));
for(const name of htmlFiles){
 let code=original(name),count=0;const blocks=[];
 code=code.replace(/<(script|style)\b[\s\S]*?<\/\1>/gi,text=>{blocks.push(text);return '\uE100'+(blocks.length-1)+'\uE101';});
 code=code.replace(/<([a-z][a-z0-9:-]*)\b([^>]*)>/gi,(all,tag,attrs)=>{
  let extra='';attrs=attrs.replace(staticAttrs,(whole,key,quote,text)=>{if(ru.test(text)){const id=register(text);extra+=' data-copy-'+key+'="'+id+'"';count++;}return whole;});return '<'+tag+attrs+extra+'>';
 });
 code=code.replace(/<textarea\b[\s\S]*?<\/textarea>/gi,text=>{blocks.push(text);return '\uE100'+(blocks.length-1)+'\uE101';});
 code=code.replace(/<([a-z][a-z0-9:-]*)\b([^>]*)>([^<>]+)<\/\1>/gi,(all,tag,attrs,text)=>{
  if(!ru.test(text))return all;const key=register(text);count++;if(tag.toLowerCase()==='option'&&!/\bvalue=/.test(attrs))attrs+=' value="'+htmlEscape(text)+'"';return '<'+tag+attrs+' data-copy="'+key+'">'+text+'</'+tag+'>';
 });
 code=code.replace(/>([^<>]+)(?=<|$)/g,(all,text,offset)=>{
  if(!ru.test(text))return all;count++;const key=register(text);
  const prior=code.slice(0,offset+1).match(/<([a-z][a-z0-9:-]*)\b[^>]*>$/i);
  if(prior&&prior[0].includes('data-copy='))return all;
  if(prior&&['title','option'].includes(prior[1].toLowerCase()))return '><span data-copy="'+key+'">'+text+'</span>';
  return '><span data-copy="'+key+'">'+text+'</span>';
 });
 // title/option may not contain spans; bind their direct source copy to the element.
 code=code.replace(/<(title|option)\b([^>]*)><span data-copy="([^"]+)">([\s\S]*?)<\/span><\/\1>/gi,'<$1$2 data-copy="$3">$4</$1>');
 code=code.replace(/\uE100(\d+)\uE101/g,(_,i)=>blocks[Number(i)]);
 code=code.replace(/<html\b([^>]*)>/i,'<html$1 data-copy-app>');
 code=code.replace(/<head\b[^>]*>/i,all=>all+'<script src="/js/app-copy-data.js"></script><script src="/js/app-copy.js"></script>');
 if(name==='control.html')code=code.replace('<div class="ws-sidebar-tools">','<div class="ws-sidebar-tools"><button id="control-language" type="button" data-copy-language>RU / EN</button>');
 if(!refresh)pendingWrites.set(path.join(base,name),code);manifest.push({file:name,keys:count});
}
const systemPath=path.join(__dirname,'system.en.json');if(fs.existsSync(systemPath))for(const [source,en] of Object.entries(JSON.parse(fs.readFileSync(systemPath,'utf8')))){const key='system.'+crypto.createHash('sha256').update(source).digest('hex').slice(0,14);dictionary[key]=[source,en];}
if(unresolved.size){fs.writeFileSync(path.join(root,'target/localization-tools/unresolved.json'),JSON.stringify([...unresolved],null,2));throw new Error('Untranslated source phrases: '+unresolved.size+'; see target/localization-tools/unresolved.json');}
for(const [file,content] of pendingWrites)fs.writeFileSync(file,content);
fs.writeFileSync(path.join(base,'js/app-copy-data.js'),'// Explicit source-copy dictionary. No shop data.\nwindow.ARIA_COPY_DICTIONARY='+JSON.stringify(dictionary)+';\n');
fs.writeFileSync(path.join(__dirname,'manifest.json'),JSON.stringify({approach:'explicit static source keys, no runtime DOM text translation',files:manifest,keyCount:Object.keys(dictionary).length},null,2));
console.log(JSON.stringify({keys:Object.keys(dictionary).length,files:manifest.length,refreshedOnly:refresh}));
