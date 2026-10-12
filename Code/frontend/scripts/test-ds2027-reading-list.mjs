import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
function load(path, extra = {}) {
  const source = fs.readFileSync(new URL(path, import.meta.url), 'utf8')
  const scope = { exports: {}, require: name => extra[name] ?? (name.startsWith('@/') ? {} : require(name)) }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX } }).outputText, scope)
  return scope.exports
}
const grid = load('../src/components/syllabus/SourceRubricGrid.tsx')
const View = load('../src/components/syllabus/New2027SyllabusView.tsx', { './SourceRubricGrid': grid }).default
const { parseStandardSyllabusNotes: parse, serializeStandardSyllabusNotes: serialize } = load('../src/components/syllabus/StandardSyllabusFormSections.tsx')
const source = JSON.parse(fs.readFileSync(new URL('../../backend/curriculum/src/main/resources/syllabus-import/verified-ds2026-reading-rows.json', import.meta.url)))
const render = notes => renderToStaticMarkup(React.createElement(View, { syllabus: { notes: JSON.stringify(notes) } }))
const section12 = html => html.slice(html.indexOf('12. Reading List'))

test('verified DS reading lists render and roundtrip every complete source row without duplicating the combined text', () => {
  let references = 0
  for (const course of source.courses) {
    const notes = { sourceFaithful2027: true, sourceReadingList: course.legacyReadingList, sourceSupplementalSections: { 12: course.section12 } }
    const saved = JSON.parse(serialize(parse(JSON.stringify(notes))))
    assert.equal(saved.sourceReadingList, notes.sourceReadingList)
    assert.deepEqual(saved.sourceSupplementalSections, notes.sourceSupplementalSections)
    const html = section12(render(saved))
    const rows = course.section12.flatMap(table => table.rows)
    references += rows.length
    assert.equal((html.match(/<td\b/g) ?? []).length, rows.length * 2)
    assert.equal((html.match(/Reference \(source text\)/g) ?? []).length, course.section12.length)
    assert.doesNotMatch(html, /SOURCE READING LIST|Source reading list<\/p>/)
    assert.doesNotMatch(html, /<th[^>]*>(?:Author|Publisher|Publication Year|Usage Type)<\/th>/)
  }
  assert.equal(references, 108)
})
test('IT056 displays three references and keeps wrapped publication details in the same row', () => {
  const course = source.courses.find(row => row.sourceCode === 'IT056')
  const html = section12(render({ sourceReadingList: course.legacyReadingList, sourceSupplementalSections: { 12: course.section12 } }))
  assert.equal((html.match(/<tr\b/g) ?? []).length, 4)
  assert.match(html, /Stellman and Greene, Applied Software Project Management, O’Reilly Media, 2006\./)
  assert.match(html, /Providing Measureable Organizational Value 5th, 2016/)
})
test('CS and sources without verified row evidence retain their existing reading-list display', () => {
  const html = section12(render({ sourceReadingList: 'Original source text retained' }))
  assert.ok(html.includes('Original source text retained'))
  assert.ok(html.includes('Source reading list'))
  assert.doesNotMatch(html, /Reference \(source text\)/)
})
