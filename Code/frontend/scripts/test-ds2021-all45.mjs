import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
const helperSource = fs.readFileSync(new URL('../src/lib/ds2021ImportMapping.ts', import.meta.url), 'utf8')
const helperScope = { exports: {} }
vm.runInNewContext(ts.transpileModule(helperSource, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, helperScope)
const { canConfirmDs2021CloudAppendix: cloud, resolveDs2021ChemistryMapping: chemistry } = helperScope.exports
const context = { cohortName: 'DS2021', sourceType: 'PDF', sourceDocumentId: 100, sourceCode: 'IT164IU', sourceName: 'Cloud Computing', isInstructor: false }

test('only the DS2021 cloud appendix with a stored PDF is delegated to server verification', () => {
  assert.equal(cloud(context), true)
  for (const change of [{ cohortName: 'CS2021' }, { cohortName: 'DS2027' }, { isInstructor: true },
    { sourceType: 'DOCX' }, { sourceDocumentId: undefined }, { sourceDocumentId: 0 },
    { sourceCode: 'IT147IU' }, { sourceName: 'Mobile Cloud Computing' }]) {
    assert.equal(cloud({ ...context, ...change }), false)
  }
})

const audit = new URL('../../.workflow-checks/ds2021-import-20261012/', import.meta.url)
const evidence = JSON.parse(fs.readFileSync(new URL('source-evidence.json', audit), 'utf8'))
const mappings = JSON.parse(fs.readFileSync(new URL('all45/effective-mappings.json', audit), 'utf8'))
const response = { pageCount: 457, syllabusCount: 45, sourceDocumentId: 100, sourceType: 'PDF', targetTemplateProfile: 'SOURCE_TEMPLATE',
  items: evidence.parsed.sections.map(section => ({ startPage: section.startPage, endPage: section.endPage,
    preview: { valid: !section.issues.some(issue => issue.severity === 'ERROR'), data: section.data, issues: section.issues } })) }

test('every real PDF item routes through its current DS mapping or the verified server exception', () => {
  assert.equal(response.items.length, 45)
  const normalize = value => String(value ?? '').replace(/[^A-Z0-9]/gi, '').toUpperCase().replace(/IU$/, '')
  const routed = response.items.map(item => {
    const data = item.preview.data
    const direct = mappings.find(row => normalize(row.courseCode) === normalize(data.sourceCourseCode))
    const alias = chemistry(mappings, { programId: 3, cohortId: 7, cohortName: 'DS2021', sourceCode: data.sourceCourseCode, sourceName: data.sourceCourseName })
    assert.equal(item.preview.valid, true)
    if (direct || alias) return { code: data.sourceCourseCode, courseId: (direct ?? alias).courseId }
    assert.equal(cloud({ ...context, sourceCode: data.sourceCourseCode, sourceName: data.sourceCourseName }), true)
    return { code: data.sourceCourseCode, serverVerification: true }
  })
  assert.equal(routed.filter(row => row.courseId).length, 44)
  assert.equal(routed.find(row => row.code === 'CHE011IU').courseId, 1)
  assert.deepEqual(routed.filter(row => row.serverVerification).map(row => row.code), ['IT164IU'])
  fs.writeFileSync(new URL('all45/frontend-routing-verification.json', audit), JSON.stringify({ count: routed.length, rows: routed }, null, 2))
})

function dialog(preview, extra = {}) {
  const state = [null, preview, false, false]
  const wrap = tag => ({ children, disabled, type, className }) => React.createElement(tag, { disabled, type, className }, children)
  const modules = {
    react: { ...React, useState: () => [state.shift(), () => {}] },
    'react/jsx-runtime': require('react/jsx-runtime'),
    'lucide-react': { FileStack: () => null, Loader2: () => null },
    sonner: { toast: { error: () => {}, success: () => {} } },
    '@/api/syllabusImportApi': {}, '@/lib/syllabusImportReadiness': {},
    '@/components/ui/button': { Button: wrap('button') },
    '@/components/ui/input': { Input: wrap('input') }, '@/components/ui/label': { Label: wrap('label') },
    '@/components/ui/dialog': Object.fromEntries(['Dialog','DialogContent','DialogDescription','DialogFooter','DialogHeader','DialogTitle'].map(key => [key, wrap('div')])),
    '@/lib/syllabusComparisonTemplate': { parseTargetTemplateSections: () => [], validateNew2027TargetTemplateResponse: () => null },
  }
  const scope = { exports: {}, require: name => {
    if (!(name in modules)) throw new Error(`Unexpected dialog dependency ${name}`)
    return modules[name]
  } }
  const source = fs.readFileSync(new URL('../src/components/syllabus/BulkImportSyllabusDialog.tsx', import.meta.url), 'utf8')
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX } }).outputText, scope)
  let started
  const tree = scope.exports.default({ open: true, onClose: () => {}, programId: 3, cohortId: 7, cohortName: 'DS2021', onStart: result => { started = result }, ...extra })
  const html = renderToStaticMarkup(tree)
  const walk = node => Array.isArray(node) ? node.flatMap(walk) : node && typeof node === 'object'
    ? [node, ...walk(node.props?.children)] : []
  const button = walk(tree).find(node => typeof node.props?.onClick === 'function' && String(node.props.children).startsWith('Import '))
  return { html, button, started: () => started }
}

test('actual DS2021 dialog imports the complete 45-item response without any checkbox', () => {
  const rendered = dialog(response)
  assert.ok(!rendered.html.includes('type="checkbox"'))
  assert.match(rendered.html, /Import 45/)
  assert.equal(rendered.button.props.disabled, false)
  rendered.button.props.onClick()
  assert.equal(rendered.started(), response)
  assert.equal(rendered.started().items.length, 45)
})

test('the same dialog still enforces instructor assignment restrictions', () => {
  const rendered = dialog(response, { allowedCourseCodes: ['MA001IU'] })
  assert.equal(rendered.button.props.disabled, true)
  assert.match(rendered.html, /You are not assigned to these courses/)
})
