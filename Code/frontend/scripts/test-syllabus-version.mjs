import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import ts from "typescript"

// Exercise the production TypeScript helper without adding a test framework.
const source = readFileSync(new URL("../src/lib/syllabusVersion.ts", import.meta.url), "utf8")
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
})
const { formatVersionLabel, compareSyllabusVersions } = await import(
  `data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`
)

test("canonical backend labels and numeric fallbacks use whole revisions", () => {
  for (const number of [1, 2, 3, 4, 9, 10]) {
    assert.equal(formatVersionLabel(number), `v${number}.0`)
    assert.equal(formatVersionLabel(number, `v${number}.0`), `v${number}.0`)
  }
})

test("legacy labels cannot leak into canonical display", () => {
  for (const label of ["Version 2", "Version 2.0", "v2", "2", null, ""]) {
    assert.equal(formatVersionLabel(2, label), "v2.0")
  }
  assert.equal(formatVersionLabel(undefined, "Version 2"), "v2.0")
  assert.equal(formatVersionLabel(2, "v3.0"), "v2.0")
  assert.equal(formatVersionLabel(undefined, "v1.1"), "—")
  assert.equal(formatVersionLabel(undefined), "—")
  assert.equal(formatVersionLabel(0), "—")
})

test("history orders v10 after v9 using numbers, regardless of labels", () => {
  const rows = [
    { versionNumber: 10, versionLabel: "v10.0" },
    { versionNumber: 9, versionLabel: "v9.0" },
    { versionNumber: 2, versionLabel: "Version 2" },
  ]
  assert.deepEqual(rows.sort(compareSyllabusVersions).map(row => row.versionNumber), [2, 9, 10])
})
