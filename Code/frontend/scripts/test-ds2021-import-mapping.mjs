import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = fs.readFileSync(new URL('../src/lib/ds2021ImportMapping.ts', import.meta.url), 'utf8')
const scope = { exports: {} }
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, scope)
const resolve = scope.exports.resolveDs2021ChemistryMapping
const choices = scope.exports.ds2021ImportChoices
const context = { programId: 3, cohortId: 7, cohortName: 'DS2021', sourceCode: 'CHE011IU', sourceName: 'Chemistry for Engineers' }
const chemistry = { id: 154, courseId: 1, courseCode: 'CH011IU', courseName: 'Chemistry for Engineer', programId: 3, cohortId: null }

test('DS2021 chemistry resolves to its existing curriculum row and keeps identity unchanged', () => {
  assert.equal(resolve([chemistry], context), chemistry)
  assert.equal(chemistry.courseCode, 'CH011IU')
})
test('cohort mapping takes priority over the same-course general mapping', () => {
  const exact = { ...chemistry, id: 900, cohortId: 7, syllabusId: 8000, syllabusStatus: 'DRAFT' }
  assert.equal(resolve([chemistry, exact], context), exact)
})
test('no alias outside DS2021 or without the verified source and target chemistry names', () => {
  for (const cohortName of ['CS2021', 'IT2021', 'DS2026', 'DS2027']) assert.equal(resolve([chemistry], { ...context, cohortName }), undefined)
  assert.equal(resolve([chemistry], { ...context, sourceName: 'Unrelated course' }), undefined)
  assert.equal(resolve([{ ...chemistry, courseName: 'Unrelated course' }], context), undefined)
  assert.equal(resolve([{ ...chemistry, programId: 1 }], context), undefined)
  assert.equal(resolve([{ ...chemistry, cohortId: 17 }], context), undefined)
})
test('different canonical course identities remain ambiguous and cannot be selected arbitrarily', () => {
  assert.throws(() => resolve([chemistry, { ...chemistry, id: 999, courseId: 999 }], context), /ambiguous/)
})
test('Cloud Computing is never inferred to be Mobile Cloud Computing', () => {
  const mobile = { id: 180, courseId: 75, courseCode: 'IT147IU', courseName: 'Mobile Cloud Computing', programId: 3, cohortId: null }
  assert.equal(resolve([chemistry, mobile], { ...context, sourceCode: 'IT164IU', sourceName: 'Cloud Computing' }), undefined)
})

test('selecting missing imports leaves saved drafts unchecked and the mismatched cloud course blocked', () => {
  const items = ['MA001IU', 'CHE011IU', 'IT164IU'].map((sourceCourseCode, index) => ({ preview: { valid: true,
    data: { sourceCourseCode, sourceCourseName: ['Calculus 1', 'Chemistry for Engineers', 'Cloud Computing'][index] } } }))
  const saved = { id: 768, courseId: 91, courseCode: 'MA001IU', courseName: 'Calculus', programId: 3, cohortId: 7, syllabusId: 5533 }
  const mobile = { id: 180, courseId: 75, courseCode: 'IT147IU', courseName: 'Mobile Cloud Computing', programId: 3, cohortId: null }
  const result = choices(items, [chemistry, saved, mobile], context)
  assert.deepEqual(JSON.parse(JSON.stringify(result.map(row => row.state))), ['existing', 'missing', 'unmapped'])
  assert.equal(result.filter(row => row.state === 'missing')[0].code, 'CHE011IU')
})

test('ambiguous chemistry cannot crash the selection UI or be selected as a missing syllabus', () => {
  const item = { preview: { valid: true, data: { sourceCourseCode: 'CHE011IU', sourceCourseName: 'Chemistry for Engineers' } } }
  assert.equal(choices([item], [chemistry, { ...chemistry, courseId: 999 }], context)[0].state, 'ambiguous')
})

test('DS2021 selection component checks only missing rows and disables unmapped ones', () => {
  const React = require('react')
  const { renderToStaticMarkup } = require('react-dom/server')
  const componentSource = fs.readFileSync(new URL('../src/components/syllabus/Ds2021ImportSelection.tsx', import.meta.url), 'utf8')
  const componentScope = { exports: {}, require }
  vm.runInNewContext(ts.transpileModule(componentSource, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX } }).outputText, componentScope)
  const data = [{ index: 0, code: 'MA001IU', name: 'Calculus 1', state: 'existing' },
    { index: 1, code: 'CHE011IU', name: 'Chemistry for Engineers', state: 'missing' },
    { index: 2, code: 'IT164IU', name: 'Cloud Computing', state: 'unmapped' }]
  const html = renderToStaticMarkup(React.createElement(componentScope.exports.default, { choices: data, selected: [1], onChange: () => {} }))
  assert.equal((html.match(/checked=""/g) ?? []).length, 1)
  assert.match(html, /aria-label="Import IT164IU"[^>]*disabled=""/)
  assert.ok(html.includes('1 already saved'))
})
