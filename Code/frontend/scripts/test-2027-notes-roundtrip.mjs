import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = fs.readFileSync(new URL('../src/components/syllabus/StandardSyllabusFormSections.tsx', import.meta.url), 'utf8')
const sandbox = { exports: {}, require: () => ({}) }
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX } }).outputText, sandbox)
const { parseStandardSyllabusNotes: parse, serializeStandardSyllabusNotes: serialize } = sandbox.exports

test('2027 Content survives form open/save with order, long names and empty weeks/hours', () => {
  const rows = [
    { name: 'First source row', weekNumber: null, teachingHours: null, weight: '1', relatedCloCodes: [] },
    { name: 'Long source content '.repeat(35), weekNumber: null, teachingHours: null, weight: '2', level: 'T,U', relatedCloCodes: [] },
  ]
  const stored = { sourceFaithful2027: true, sourceContentRows: rows, sourceAlignmentPreserved: true,
    cloPloMatrix: { CLO1: { PLO1: 4, PLO7: 3 } }, plannedActivities: [{ week: null, topic: 'Separate weekly row' }],
    readings: [{ title: 'Actual source reference', usageType: '' }], sourceReadingList: 'Textbooks: Alpha.\nReferences: Beta.', assessmentPassNote: '', contentNote: '' }
  const saved = JSON.parse(serialize(parse(JSON.stringify(stored))))
  assert.deepEqual(saved.sourceContentRows, rows)
  assert.equal(saved.cloPloMatrix.CLO1.PLO1, '4')
  assert.equal(saved.cloPloMatrix.CLO1.PLO7, '3')
  assert.equal(saved.plannedActivities[0].week, '')
  assert.equal(saved.readings[0].usageType, '')
  assert.equal(saved.assessmentPassNote, '')
  assert.equal(saved.contentNote, '')
  assert.equal(saved.sourceReadingList, stored.sourceReadingList)
})

test('legacy form defaults are unchanged', () => {
  const result = parse(JSON.stringify({ readings: [{ title: 'Legacy reference' }] }))
  assert.equal(result.readings[0].usageType, 'REQUIRED')
  assert.match(result.contentNote, /3 hours/)
})

test('readiness separates optional missing fields from source and relationship errors', () => {
  const code = fs.readFileSync(new URL('../src/lib/syllabusImportReadiness.ts', import.meta.url), 'utf8')
  const scope = { exports: {} }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, scope)
  const classify = scope.exports.syllabusImportReadiness
  const data = { workloadContact: '45', workloadPrivate: '90', personResponsible: 'Verified teacher', topics: [] }
  assert.equal(classify({ valid: true, data, issues: [] }), 'READY_COMPLETE')
  assert.equal(classify({ valid: true, data: { ...data, topics: [{ name: 'Source Content', weekNumber: null }] }, issues: [] }), 'READY_WITH_WARNINGS')
  assert.equal(classify({ valid: false, data, issues: [{ severity: 'ERROR', message: 'Unrelated CLO mapping' }] }), 'BLOCKED_DATA_ERROR')
  assert.equal(classify({ valid: false, data, issues: [{ severity: 'ERROR', message: 'SOURCE_NOT_VERIFIED: OCR source needs review' }] }), 'SOURCE_REVIEW_REQUIRED')
})
