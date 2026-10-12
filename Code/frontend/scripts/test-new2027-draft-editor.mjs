import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'
import {fileURLToPath} from 'node:url'
import {createRequire} from 'node:module'
const require=createRequire(import.meta.url)
const ts=require('typescript'),React=require('react'),{renderToStaticMarkup}=require('react-dom/server')
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..')
const audit=path.resolve(root,'../.workflow-checks/new2027-draft-editor-20261012')
function load(filename,extra={}) {
  if(!path.isAbsolute(filename))filename=path.join(root,'src',filename)
  if(!fs.existsSync(filename))filename+=fs.existsSync(filename+'.tsx')?'.tsx':'.ts'
  const box={exports:{},URLSearchParams,console,require:name=>{
    if(Object.hasOwn(extra,name))return extra[name]
    if(name.startsWith('@/'))return load(path.join(root,'src',name.slice(2)),extra)
    if(name.startsWith('.'))return load(path.resolve(path.dirname(filename),name),extra)
    return require(name)
  }}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,jsx:ts.JsxEmit.ReactJSX}}).outputText,box,{filename})
  return box.exports
}
const lib=load('lib/new2027SyllabusDraft.ts')
const View=load('components/syllabus/New2027SyllabusView.tsx').default
const Form=load('components/syllabus/New2027SyllabusForm.tsx').default
const toForm=load('lib/syllabusFormData.ts').toSyllabusFormData
const oldGrid=load(path.join(audit,'before/SourceRubricGrid.tsx'))
const OldView=load(path.join(audit,'before/New2027SyllabusView.tsx'),{'./SourceRubricGrid':oldGrid}).default
const read=relative=>JSON.parse(fs.readFileSync(path.resolve(root,'..',relative),'utf8'))
const fixture=read('.workflow-checks/ne-ce-extension-20261012/NE2027-EN008IU-response.json')
const gridResource=read('backend/curriculum/src/main/resources/syllabus-import/verified-ds2026-rubric-grids.json')
const readingResource=read('backend/curriculum/src/main/resources/syllabus-import/verified-ds2026-reading-rows.json')
const plain=value=>JSON.parse(JSON.stringify(value))
function walk(node,out=[]) {
  if(Array.isArray(node)){node.forEach(item=>walk(item,out));return out}
  if(!React.isValidElement(node))return out
  if(typeof node.type==='function')return walk(node.type(node.props),out)
  out.push(node)
  walk(node.props.children,out)
  return out
}
const editable=(syllabus,onChange)=>walk(React.createElement(View,{syllabus,onChange}))
const field=(nodes,label)=>nodes.find(node=>node.props['aria-label']===label)

test('View and Edit select the backend profile, without inferring a template from the year',()=>{
  for(const cohort of ['CS2027','DS2027','NE2027','CE2030'])assert.equal(lib.isNew2027Syllabus({targetTemplateProfile:'NEW_2027',cohortName:cohort}),true)
  assert.equal(lib.isNew2027Syllabus({targetTemplateProfile:'SOURCE_TEMPLATE',academicYear:'2027'}),false)
  assert.equal(lib.isNew2027Syllabus({academicYear:'CS2027'}),false)
})

test('read-only output remains byte-identical for real NE/CE snapshots and all DS rubric/reading evidence',()=>{
  const snapshots=[fixture,read('.workflow-checks/ne-ce-extension-20261012/CE2027-EN008IU-response.json'),
    ...gridResource.courses.map(course=>({...fixture,notes:JSON.stringify({sourceSupplementalSections:{11:course.section11}})})),
    ...readingResource.courses.map(course=>({...fixture,notes:JSON.stringify({sourceReadingList:course.legacyReadingList,sourceSupplementalSections:{12:course.section12}})}))]
  for(const syllabus of snapshots)assert.equal(renderToStaticMarkup(React.createElement(View,{syllabus})),renderToStaticMarkup(React.createElement(OldView,{syllabus})))
})

test('actual Draft form renders the same twelve section headings as View, with editable Content and stable identity',()=>{
  const html=renderToStaticMarkup(React.createElement(Form,{syllabus:fixture,initialData:toForm(fixture),onSubmit:()=>{}}))
  const view=renderToStaticMarkup(React.createElement(View,{syllabus:fixture}))
  const headings=value=>[...value.matchAll(/<h2[^>]*>(\d+\. [\s\S]*?)<\/h2>/g)].map(match=>match[1])
  assert.deepEqual(headings(html),headings(view));assert.equal(headings(html).length,12)
  assert.match(html,/aria-label="Content 1 English"/);assert.match(html,/NEW_2027 DRAFT EDITOR/)
  assert.doesNotMatch(html,/aria-label="Course Code"|aria-label="Course Name"/)
})

test('opening and saving unchanged retains exact notes, IDs, unknown values and the source semester list',()=>{
  const initial=toForm(fixture),payload=lib.toNew2027DraftPayload(fixture,initial)
  assert.equal(payload.notes,fixture.notes)
  assert.deepEqual(plain(payload.clos),fixture.clos);assert.deepEqual(plain(payload.topics),fixture.topics)
  assert.deepEqual(plain(payload.assessments),fixture.assessments)
  const empty={...fixture,semester:null,notes:JSON.stringify({sourceContentRows:[{name:'Source row',weekNumber:null,teachingHours:null,weight:null}],creditPoints:null,sourceSemester:'1, 2, 3'})}
  const blank=lib.toNew2027DraftPayload(empty,toForm(empty))
  assert.equal(blank.semester,'');assert.equal(blank.notes,empty.notes)
  assert.equal(JSON.parse(blank.notes).creditPoints,null)
})

test('editing Content changes only the chosen value and preserves hours, source provenance and matrices',()=>{
  const before=structuredClone(fixture);let updated
  const nodes=editable(fixture,value=>{updated=value})
  field(nodes,'Content 1 English').props.onChange({target:{value:'Corrected source title'}})
  const originalNotes=JSON.parse(fixture.notes),notes=JSON.parse(updated.notes)
  assert.equal(notes.sourceContentRows[0].name,'Corrected source title')
  const restored=plain(notes);restored.sourceContentRows[0].name=originalNotes.sourceContentRows[0].name
  assert.deepEqual(restored,originalNotes);assert.deepEqual(fixture,before)
  assert.equal(notes.sourceContentRows[0].weekNumber,null)
  assert.equal(notes.sourceContentRows[0].teachingHours,null)
  assert.deepEqual(plain(lib.new2027RelationPayload(fixture,lib.toNew2027DraftPayload(updated,toForm(fixture)))).notes,'{}')
})

test('editing source Semester never converts a list into the canonical Draft semester',()=>{
  let updated
  field(editable(fixture,value=>{updated=value}),'Semester').props.onChange({target:{value:'1, 2, 3, 4'}})
  assert.equal(JSON.parse(updated.notes).sourceSemester,'1, 2, 3, 4')
  assert.equal(updated.semester,fixture.semester)
})

test('DS source rubric cells remain editable without flattening merged cells or changing source pages',()=>{
  const source=gridResource.courses.find(course=>course.sourceCode==='IT094').section11
  const syllabus={...fixture,notes:JSON.stringify({sourceSupplementalSections:{11:source}})}
  let updated
  const nodes=editable(syllabus,value=>{updated=value})
  const target=nodes.find(node=>node.type==='textarea'&&String(node.props['aria-label']).startsWith('Rubric page')&&String(node.props.value).includes('Abstract clearly'))
  assert.ok(target);target.props.onChange({target:{value:'Instructor criterion correction'}})
  const next=JSON.parse(updated.notes).sourceSupplementalSections[11]
  assert.equal(next.length,source.length)
  assert.deepEqual(next.map(grid=>grid.blocks.map(block=>({page:block.page,widths:block.widths,spans:block.rows?.map(row=>row.map(({colSpan,rowSpan})=>({colSpan,rowSpan})))}))),
    source.map(grid=>grid.blocks.map(block=>({page:block.page,widths:block.widths,spans:block.rows?.map(row=>row.map(({colSpan,rowSpan})=>({colSpan,rowSpan})))}))))
  assert.match(renderToStaticMarkup(React.createElement(View,{syllabus:updated})),/Instructor criterion correction/)
})

test('DS Reading List stays in separate source rows through an edit/save/view roundtrip',()=>{
  const course=readingResource.courses.find(course=>course.sourceCode==='IT056')
  const syllabus={...fixture,notes:JSON.stringify({sourceReadingList:course.legacyReadingList,sourceSupplementalSections:{12:course.section12}})}
  let updated
  const nodes=editable(syllabus,value=>{updated=value})
  const target=nodes.find(node=>node.type==='textarea'&&String(node.props.value).includes('Stellman and Greene'))
  assert.ok(target);target.props.onChange({target:{value:'Updated complete reference row'}})
  const saved=lib.toNew2027DraftPayload(updated,toForm(syllabus)),notes=JSON.parse(saved.notes)
  assert.equal(notes.sourceSupplementalSections[12].flatMap(table=>table.rows).length,3)
  assert.equal(notes.sourceReadingList,course.legacyReadingList)
  const html=renderToStaticMarkup(React.createElement(View,{syllabus:{...updated,notes:saved.notes}}))
  assert.match(html,/Updated complete reference row/);assert.doesNotMatch(html,/Source reading list<\/p>/)
})

test('relation save only includes changed categories and never normalizes source numeric/R/M mappings',()=>{
  const notes={sourceAlignmentPreserved:true,cloPloMatrix:{CLO1:{PLO1:4,PLO2:'R'}},assessmentCloMatrix:{0:{CLO1:60}},readings:[],topicDetails:{}}
  const original={...fixture,notes:JSON.stringify(notes)}
  let values=lib.toNew2027DraftPayload(original,toForm(original))
  assert.deepEqual(JSON.parse(lib.new2027RelationPayload(original,values).notes),{})
  values={...values,notes:JSON.stringify({...notes,assessmentCloMatrix:{0:{CLO1:70}},cloPloMatrix:{CLO1:{PLO1:3,PLO2:'M'}}})}
  assert.deepEqual(JSON.parse(lib.new2027RelationPayload(original,values).notes),{assessmentCloMatrix:{0:{CLO1:70}}})
})

test('removing a CLO cleans only its references; removing an assessment preserves other row identities',()=>{
  const original={...fixture,clos:[{id:1,code:'CLO1',description:'One'},{id:2,code:'CLO2',description:'Two'}],
    assessments:[{id:11,name:'A',weightPercent:20},{id:12,name:'B',weightPercent:80}],
    notes:JSON.stringify({cloPloMatrix:{CLO1:{PLO1:4},CLO2:{PLO1:'R'}},assessmentCloMatrix:{0:{CLO1:50,CLO2:50},1:{CLO2:100}},
      sourceContentRows:[{name:'Original',relatedCloCodes:['CLO1','CLO2'],weekNumber:null}],sourceImportEvidence:{sha256:'unchanged'}})}
  const next=lib.removeDraftClo(original,0),notes=JSON.parse(next.notes)
  assert.deepEqual(plain(next.clos),[original.clos[1]])
  assert.deepEqual(notes.cloPloMatrix,{CLO2:{PLO1:'R'}})
  assert.deepEqual(notes.sourceContentRows[0].relatedCloCodes,['CLO2'])
  const removed=lib.removeDraftAssessment(next,0)
  assert.deepEqual(plain(removed.assessments),[original.assessments[1]])
  assert.deepEqual(JSON.parse(removed.notes).assessmentCloMatrix,{0:{CLO2:100}})
  assert.deepEqual(JSON.parse(removed.notes).sourceImportEvidence,{sha256:'unchanged'})
})

test('actual form state submits edited values and Reset restores the original snapshot',()=>{
  let current,saved
  const StatefulForm=load('components/syllabus/New2027SyllabusForm.tsx',{
    react:{...React,useState:initial=>{current??=initial;return[current,value=>{current=typeof value==='function'?value(current):value}]}},
    '@/components/ui/button':{Button:props=>React.createElement('button',props)},
  }).default
  const props={syllabus:fixture,initialData:toForm(fixture),onSubmit:value=>{saved=value}}
  field(walk(StatefulForm(props)),'Content 1 English').props.onChange({target:{value:'Edited through the actual form state'}})
  StatefulForm(props).props.onSubmit({preventDefault:()=>{}})
  assert.equal(JSON.parse(saved.notes).sourceContentRows[0].name,'Edited through the actual form state')
  assert.equal(saved.clos[0].id,fixture.clos[0].id)
  assert.equal(JSON.parse(saved.notes).sourceContentRows[0].weekNumber,null)
  const reset=walk(StatefulForm(props)).find(node=>node.type==='button'&&node.props.type==='button')
  assert.ok(reset);reset.props.onClick()
  StatefulForm(props).props.onSubmit({preventDefault:()=>{}})
  assert.equal(saved.notes,fixture.notes)
})

test('actual Edit page chooses NEW_2027, preserves legacy form, and retains status/save guards',async()=>{
  let active={...fixture,status:'DRAFT'},submitted,synced,navigated
  const Legacy=()=>null,NewForm=()=>null
  const Page=load('pages/syllabus/SyllabusEditPage.tsx',{
    react:{...React,useMemo:fn=>fn(),useState:value=>[value,()=>{}]},
    'react-router-dom':{useParams:()=>({id:String(fixture.id)}),useNavigate:()=>value=>{navigated=value},useLocation:()=>({pathname:'/admin/syllabus/'+fixture.id+'/edit',search:'',state:{}})},
    '@/components/syllabus/SyllabusForm':{__esModule:true,default:Legacy},'@/components/syllabus/New2027SyllabusForm':{__esModule:true,default:NewForm},
    '@/hooks/useSyllabus':{useSyllabus:()=>({data:active})},
    '@/hooks/useUpdateSyllabus':{useUpdateSyllabus:()=>({mutateAsync:async value=>{submitted=value;return active}})},
    '@/store/authStore':{useAuthStore:selector=>selector({user:{role:'ADMIN'}})},
    '@/lib/syllabusRelationSync':{syncStandardSyllabusRelations:async(id,payload)=>{synced={id,payload}},SyllabusRelationSyncError:class extends Error{}},
    sonner:{toast:{success:()=>{},error:()=>{}}},
  }).default
  function findType(node,type){if(Array.isArray(node))return node.map(item=>findType(item,type)).find(Boolean);if(!React.isValidElement(node))return undefined;return node.type===type?node:findType(node.props.children,type)}
  const form=findType(Page(),NewForm);assert.ok(form)
  await form.props.onSubmit({...toForm(active),courseId:999,versionNumber:99})
  assert.equal(submitted.data.courseId,active.courseId);assert.equal(submitted.data.versionNumber,active.versionNumber)
  assert.equal(synced.payload.notes,'{}');assert.ok(navigated)
  active={...active,targetTemplateProfile:'SOURCE_TEMPLATE'};assert.ok(findType(Page(),Legacy));assert.equal(findType(Page(),NewForm),undefined)
  active={...active,targetTemplateProfile:'NEW_2027',status:'APPROVED'};assert.equal(findType(Page(),NewForm),undefined)
})
