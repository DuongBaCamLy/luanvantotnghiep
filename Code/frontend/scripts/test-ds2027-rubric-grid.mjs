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
const resource = JSON.parse(fs.readFileSync(new URL('../../backend/curriculum/src/main/resources/syllabus-import/verified-ds2026-rubric-grids.json', import.meta.url)))

test('all 42 DS source rubric grids survive form open/save and render all table cells', () => {
  for (const course of resource.courses) {
    const source = course.section11[0]
    const notes = { sourceFaithful2027: true, sourceSupplementalSections: { 11: course.section11 } }
    const saved = JSON.parse(serialize(parse(JSON.stringify(notes))))
    assert.deepEqual(saved.sourceSupplementalSections, notes.sourceSupplementalSections)
    const html = renderToStaticMarkup(React.createElement(grid.SourceRubricGrid, { source }))
    const cells = source.blocks.flatMap(block => block.rows ?? []).flat()
    assert.equal((html.match(/<td\b/g) ?? []).length, cells.length, course.sourceCode)
    assert.equal((html.match(/<table\b/g) ?? []).length, source.blocks.filter(block => block.kind === 'table').length)
    assert.doesNotMatch(html, /Date revised|Dean of the School|Nguyen Van Sinh|\/ 513/)
  }
})
test('pictured IT094 has separate criteria/max/blank score/comment cells and source achievement columns', () => {
  const source = resource.courses.find(course => course.sourceCode === 'IT094').section11[0]
  const html = renderToStaticMarkup(React.createElement(grid.SourceRubricGrid, { source }))
  assert.match(html, /Abstract clearly identifies purpose and summarizes principal content<\/td><td[^>]*>10<\/td><td[^>]*><\/td><td[^>]*><\/td>/)
  assert.match(html, /colSpan="2"|colspan="2"/)
  for (const label of ['Max.', 'Score', 'Comments', 'Capstone', 'Milestone', 'Benchmark']) assert.ok(html.includes(label))
  assert.ok(html.includes('Source PDF page: 456'))
})
test('DS section 11 renders its source rubric without misleading empty-model messages and retains assignment edits', () => {
  const source = resource.courses.find(course => course.sourceCode === 'IT094').section11
  const html = renderToStaticMarkup(React.createElement(View, { syllabus: { notes: JSON.stringify({
    assignmentDescription: 'Instructor assignment text', sourceSupplementalSections: { 11: source },
  }) } }))
  assert.ok(html.includes('Instructor assignment text'))
  assert.ok(html.includes('Source rubric table, page 453'))
  assert.doesNotMatch(html, /No structured rubric summary|No separate assignment description/)
})
test('existing CS source tables keep their original renderer and empty-model behavior', () => {
  const html = renderToStaticMarkup(React.createElement(View, { syllabus: { cohortName: 'CS2027', programCode: 'CS',
    notes: JSON.stringify({ sourceSupplementalSections: { 11: [{ title: 'Original CS rubric', headers: ['Criterion', 'Source score'], rows: [['CS criterion', '20']] }] } }),
  } }))
  assert.ok(html.includes('Original CS rubric'))
  assert.ok(html.includes('CS criterion'))
  assert.ok(html.includes('No structured rubric summary'))
  assert.ok(html.includes('No separate assignment description'))
  assert.doesNotMatch(html, /Source rubric table, page/)
})
