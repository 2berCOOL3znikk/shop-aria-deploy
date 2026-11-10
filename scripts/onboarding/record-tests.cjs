'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const run=JSON.parse(fs.readFileSync('target/postgres-test-run.json','utf8').replace(/^\uFEFF/,''));assert.equal(run.exitCode,0);assert.equal(run.tests,'','Evidence requires a full test run');
const suites=fs.readdirSync('target/surefire-reports').filter(name=>/^TEST-.*\.xml$/.test(name)).sort().map(name=>{
 const file=path.join('target/surefire-reports',name);assert.ok(fs.statSync(file).mtimeMs>=Date.parse(run.startedAt),'Stale test report: '+name);
 const header=fs.readFileSync(file,'utf8').match(/<testsuite\b[^>]*>/)[0],attribute=key=>header.match(new RegExp('\\b'+key+'="([^"]+)"'))[1];return {name:attribute('name'),...Object.fromEntries(['tests','failures','errors','skipped'].map(key=>[key,Number(attribute(key))]))};
});
const total=key=>suites.reduce((sum,s)=>sum+s[key],0),proof={passed:true,environment:'fresh isolated PostgreSQL; Flyway V1-V34 and schema validation enabled',startedAt:run.startedAt,completedAt:run.completedAt,...Object.fromEntries(['tests','failures','errors','skipped'].map(key=>[key,total(key)])),suites};
assert.equal(proof.tests,276);assert.equal(proof.failures+proof.errors+proof.skipped,0);
for(const dir of ['docs/onboarding/evidence','docs/connected-commerce/evidence']){
 const file=path.join(dir,'java-tests.json');if(fs.existsSync(file)){const old=JSON.parse(fs.readFileSync(file,'utf8'));if(old.tests===259)fs.copyFileSync(file,path.join(dir,'java-tests-f99c403.json'));}
 fs.writeFileSync(file,JSON.stringify(proof,null,2)+'\n');
}
console.log(JSON.stringify({passed:true,tests:proof.tests,suites:suites.length}));
