'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/resources/static/js/login-page.js','utf8'),start=source.indexOf('    function safeNext('),end=source.indexOf('\n    //',start);
assert.ok(start>=0&&end>start);const safe=vm.runInNewContext('('+source.slice(start,end)+')',{URL,location:{origin:'https://aria.example'}});
for(const next of ['https://other.example/path','//other.example','/\\other.example','/\t/other.example','/\r\n/other.example','\\other.example',null])assert.equal(safe(next,'ADMIN'),'/control.html');
assert.equal(new URL('/\\other.example','https://aria.example').origin,'https://other.example','Baseline bypass uses real browser URL semantics');
assert.equal(safe('/company-requests.html','ADMIN'),'/company-requests.html');assert.equal(safe('/control.html?storeId=3#stores','ADMIN'),'/control.html?storeId=3#stores');
assert.equal(safe('/admin.html','USER'),'/index.html');assert.equal(safe('/social.html','ORG'),'/social.html');assert.equal(safe('/control.html','ORG'),'/social.html');
console.log(JSON.stringify({passed:true,externalAndBackslashRedirectsBlocked:true,operatorReturnPreserved:true}));
