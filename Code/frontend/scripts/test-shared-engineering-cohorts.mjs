import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const ts = require('typescript')
const scope = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(new URL('../src/lib/sharedEngineeringCohorts.ts',import.meta.url),'utf8'),
  {compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,scope)
const select = scope.exports.engineeringCohortProgramIds
const programs = [{id:1,majorCode:'CS'},{id:2,majorCode:'IT'},{id:3,majorCode:'DS'},
  {id:5,majorCode:'NE'},{id:6,majorCode:'CE'},{id:7,majorCode:'NE'},{id:8,majorCode:'CE'}]
test('NE and CE selectors retain all source curriculum versions under their own major',()=>{
  assert.deepEqual([...select(programs,'NE')],[5,7]);assert.deepEqual([...select(programs,'CE')],[6,8])
})
test('existing CS DS IT and other major filters retain the established program-scoped behavior',()=>{
  for(const key of ['CS','DS','IT','SE','all']) assert.equal(select(programs,key),undefined)
})
test('future curriculum records join the same family without year-dependent logic',()=>{
  assert.deepEqual([...select([...programs,{id:20,majorCode:'NE'}],'NE')],[5,7,20])
})

const chemistryScope={exports:{}}
vm.runInNewContext(ts.transpileModule(fs.readFileSync(new URL('../src/lib/ds2021ImportMapping.ts',import.meta.url),'utf8'),
  {compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,chemistryScope)
const chemistry=chemistryScope.exports.resolveEngineeringChemistryMapping
test('NE CE chemistry spelling uses the same scoped resolver without relaxing NEW_2027 identity',()=>{
  const row={id:1,programId:5,cohortId:19,courseId:1,courseCode:'CH011IU',courseName:'Chemistry for Engineer'}
  const context={programId:5,cohortId:19,cohortName:'NE2021',sourceCode:'CHE011IU',sourceName:'Chemistry for Engineers',targetTemplateProfile:'SOURCE_TEMPLATE'}
  assert.equal(chemistry([row],context),row)
  assert.equal(chemistry([row],{...context,cohortName:'CE2021'}),row)
  assert.equal(chemistry([row],{...context,targetTemplateProfile:'NEW_2027'}),undefined)
  assert.equal(chemistry([row],{...context,cohortName:'CS2021'}),undefined)
  assert.equal(chemistry([row],{...context,programId:6}),undefined)
})

test('all real NE CE legacy preview items route to their own verified curriculum assignments',()=>{
  const root=new URL('../../.workflow-checks/ne-ce-extension-20261012/',import.meta.url)
  const plan=JSON.parse(fs.readFileSync(new URL('verified-seed-plan.json',root),'utf8'))
  const normalize=code=>String(code).replace(/[^A-Z0-9]/gi,'').toUpperCase().replace(/IU$/,'')
  let count=0
  for(const program of plan.programs){
    const preview=JSON.parse(fs.readFileSync(new URL(program.major+program.source.slice(2)+'-real-preview.json',root),'utf8'))
    const mappings=program.assignments.map((row,index)=>({...row,id:index+1,programId:50,cohortId:60}))
    for(const item of preview.items){
      const data=item.preview.data
      const direct=mappings.find(row=>normalize(row.courseCode)===normalize(data.sourceCourseCode))
      const alias=chemistry(mappings,{programId:50,cohortId:60,cohortName:program.major+program.source.slice(2),
        sourceCode:data.sourceCourseCode,sourceName:data.sourceCourseName,targetTemplateProfile:preview.targetTemplateProfile})
      assert.ok(direct??alias,program.code+': '+data.sourceCourseCode)
      count++
    }
  }
  assert.equal(count,233) // 52 + 56 + 65 + 60
})
