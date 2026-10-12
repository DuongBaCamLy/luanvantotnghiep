import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import { test } from "node:test"
import ts from "typescript"

const source = readFileSync(new URL("../src/lib/approvalTimeline.ts", import.meta.url), "utf8")
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
})
const { buildApprovalTimeline, approvalStageLabel } = await import(
  `data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`
)
// Isolated fixtures exercise presentation of the existing API contract.
const record = (overrides = {}) => ({
  id: 1, versionNumber: 1, versionLabel: "v1.0",
  step: "STEP1_DEPT_HEAD", status: "APPROVED",
  requestedByUsername: "instructor", reviewedByUsername: "head",
  comment: "Reviewed", createdAt: "2026-10-01T08:00:00",
  resolvedAt: "2026-10-01T09:00:00", ...overrides,
})

test("review feedback remains scoped to the numeric syllabus version", () => {
  const history = [record(), record({ id: 2, versionNumber: 2, versionLabel: "legacy label", status: "PENDING", resolvedAt: null })]
  assert.deepEqual(buildApprovalTimeline(history, 2).map(event => event.item.id), [2, 2])
  assert.equal(buildApprovalTimeline(history, 3).length, 0)
})

test("events are chronological even when API requests arrive newest first", () => {
  const history = [record({ id: 2, step: "STEP3_DEAN", createdAt: "2026-10-01T09:01:00", resolvedAt: "2026-10-01T10:00:00" }), record()]
  assert.deepEqual(buildApprovalTimeline(history, 1).map(event => event.key),
    ["1-submission", "1-decision", "2-submission", "2-decision"])
})

test("pending authority has no fabricated reviewer or resolution timestamp", () => {
  const history = [record({ id: 2, status: "PENDING", reviewedByUsername: null, resolvedAt: null })]
  const events = buildApprovalTimeline(history, 1)
  assert.equal(events.length, 2)
  assert.equal(events[1].kind, "pending")
  assert.equal(events[1].actor, null)
  assert.equal(events[1].timestamp, null)
  assert.equal(events[0].actor, "instructor")
})

test("Rejected and Revision Requested retain separate decisions and feedback", () => {
  const history = [record({ id: 2, status: "REJECTED", comment: "Correct the assessment" }),
    record({ id: 3, status: "REVISION_REQUESTED", comment: "Update the outcomes" })]
  for (const [status, expected] of [["REJECTED", "Correct the assessment"], ["REVISION_REQUESTED", "Update the outcomes"]]) {
    const events = buildApprovalTimeline(history, 1, status)
    assert.equal(events.length, 2)
    assert.equal(events[1].item.status, status)
    assert.equal(events[1].item.comment, expected)
  }
})

test("missing decision data does not produce a fake decision", () => {
  const events = buildApprovalTimeline([record({ status: null, resolvedAt: null, reviewedByUsername: null })], 1)
  assert.equal(events.length, 1)
  assert.equal(events[0].kind, "submission")
})

test("shared and legacy final steps never claim the actual reviewer role is Dean alone", () => {
  assert.match(approvalStageLabel("STEP3_DEAN"), /Dean \/ Dean Secretary/)
  assert.match(approvalStageLabel("STEP2_DEAN_SECRETARY"), /Dean \/ Dean Secretary/)
  assert.equal(approvalStageLabel("STEP1_DEPT_HEAD"), "Department Head")
})

test("status filters cannot bring another revision into the selected timeline", () => {
  const history = [record({ id: 4, status: "REJECTED", versionNumber: 2 }), record({ status: "APPROVED" })]
  assert.deepEqual(buildApprovalTimeline(history, 1, "REJECTED"), [])
})

test("missing or invalid historical times are preserved without guessing dates", () => {
  const history = [record({ createdAt: null, resolvedAt: "invalid date" })]
  const events = buildApprovalTimeline(history, 1)
  assert.equal(events[0].timestamp, null)
  assert.equal(events[1].timestamp, "invalid date")
})

