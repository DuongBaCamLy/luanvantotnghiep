import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = fs.readFileSync(new URL('../src/components/syllabus/BulkImportSyllabusDialog.tsx', import.meta.url), 'utf8')
// Exercise the actual evidence functions without opening a dialog or an API session.
const compiled = ts.transpileModule(source + '\nexport { sourceEvidenceForTargetField, summarizeSourceEvidence };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX },
}).outputText
const sandbox = { exports: {}, require: (name) => name.startsWith('react') ? require(name) : {} }
vm.runInNewContext(compiled, sandbox)
const { sourceEvidenceForTargetField: evidence, summarizeSourceEvidence: summarize } = sandbox.exports

test('parser absence does not establish absence in the original document', () => {
  const result = evidence({ sourceProvenance: { general: { language: { state: 'ABSENT_IN_SOURCE' } } } }, 'general', 'language')
  assert.equal(result.state, 'UNRESOLVED')
  assert.match(result.note, /did not extract/)
})

test('missing provenance remains unverified, while extracted values remain visible', () => {
  assert.equal(evidence({}, 'general', 'language').state, 'UNRESOLVED')
  const extracted = evidence({ language: 'English' }, 'general', 'language')
  assert.equal(extracted.state, 'PARSED')
  assert.equal(extracted.sample, 'English')
})

test('PARSED metadata without an actual value is not displayed as extracted', () => {
  assert.equal(evidence({ sourceProvenance: { general: { language: { state: 'PARSED' } } } }, 'general', 'language').state, 'UNRESOLVED')
})

test('LLOs unavailable in the model are not claimed absent in the source', () => {
  const result = evidence({}, 'cloLlo', 'code')
  assert.equal(result.state, 'UNRESOLVED')
  assert.match(result.note, /Verify the original source/)
})

test('mixed summary identifies courses needing source verification', () => {
  const item = (code, language) => ({ startPage: 1, endPage: 2, preview: { data: { sourceCourseCode: code, language } } })
  const result = summarize([item('MA003IU', 'English'), item('PE015IU', undefined)], 'general', 'language')
  assert.equal(result.state, 'MIXED')
  assert.equal(result.summary, '1/2 extracted · 0 reported absent · 1 not verified')
  assert.equal(result.affected, 'PE015IU')
})
