# -*- coding: utf-8 -*-
"""
Merge the original GPT-OSS-20B RAG evaluation with the two gap-fill reruns into a
single complete report (93 test cases x 3 runs, zero errors).

Strategy:
  * Keep every originally-successful test case untouched.
  * For each run, the cases that errored (`text cannot be null`) are filled from
    rerun-1 (same run number = same derived seed); any still-errored there are
    filled from rerun-2.
  * Confusion-matrix and RAG aggregates are obtained by ADDING the filled cases'
    exact contributions to the original per-run totals, so the original baseline
    accounting is preserved. TN per filled case = totalActivities - TP - FP - FN,
    with totalActivities counted from the local BPMN dataset and validated against
    the rerun reports' own TN values.
  * Cross-run aggregate stats use population std (matches the framework).
"""
import json, os, math

BASE = os.path.dirname(__file__)
LABEL = "GPT-OSS-20B"

orig  = json.load(open(os.path.join(BASE, "evaluation_report_multi.json"), encoding="utf-8"))
rr1   = json.load(open(os.path.join(BASE, "reruns 1", "evaluation_report_multi.json"), encoding="utf-8"))
rr2   = json.load(open(os.path.join(BASE, "rerun 2", "evaluation_report_multi.json"), encoding="utf-8"))
N     = {int(k): v for k, v in json.load(open(os.path.join(BASE, "_activity_counts.json"))).items()}

RUNS = ["1", "2", "3"]

def tpfpfn(tc):
    return (len(tc["correctActivityIds"]), len(tc["falsePositiveIds"]), len(tc["falseNegativeIds"]))

# ---- build filled test-case lists per run -------------------------------------
merged_tc = {}
fill_log = {}
for rk in RUNS:
    orig_ok_ids = {tc["testCaseId"] for tc in orig["testCasesByRun"][rk]}
    gap_ids = [e["testCaseId"] for e in orig["errorsByRun"].get(rk, [])]
    rr1_by_id = {tc["testCaseId"]: tc for tc in rr1["testCasesByRun"].get(rk, [])}
    rr2_by_id = {tc["testCaseId"]: tc for tc in rr2["testCasesByRun"].get(rk, [])}

    filled = []
    log = []
    for gid in gap_ids:
        if gid in rr1_by_id:
            filled.append(rr1_by_id[gid]); log.append((gid, "rerun1"))
        elif gid in rr2_by_id:
            filled.append(rr2_by_id[gid]); log.append((gid, "rerun2"))
        else:
            raise RuntimeError(f"Run {rk}: gap case {gid} not found in either rerun!")
    fill_log[rk] = log

    all_tc = list(orig["testCasesByRun"][rk]) + filled
    assert len(all_tc) == 93, f"Run {rk}: {len(all_tc)} cases (expected 93)"
    all_tc.sort(key=lambda t: (t["datasetId"], t["testCaseId"]))
    merged_tc[rk] = all_tc

# ---- recompute per-run summaries ----------------------------------------------
def make_summary_markdown(s):
    lines = ["## Summary", f"Total: {s['total']}", f"Passed: {s['passed']}", f"Failed: {s['failed']}"]
    if s["error"] > 0:
        lines.append(f"Error: {s['error']}")
    else:
        lines.append("")
    lines += [f"Total Retries: {s['amountOfRetries']}", "",
              "### Metrics",
              f"Accuracy: {s['accuracy']:.3f}", f"Precision: {s['precision']:.3f}",
              f"Recall: {s['recall']:.3f}", f"F1-Score: {s['f1Score']:.3f}", "",
              "### Confusion Matrix",
              f"True Positives: {s['totalTruePositives']}", f"False Positives: {s['totalFalsePositives']}",
              f"False Negatives: {s['totalFalseNegatives']}", f"True Negatives: {s['totalTrueNegatives']}", ""]
    rm = s["ragMetrics"]
    lines += [f"### RAG Metrics (Ragas) — averaged across {rm['evaluatedTestCases']} test case(s)",
              f"Faithfulness: {rm['faithfulnessMean']:.3f}",
              f"Context Utilization: {rm['contextUtilizationMean']:.3f}",
              f"Total Samples Scored: {rm['totalSamples']} (failed: {rm['failedSamples']})"]
    return "\n".join(lines)

merged_summaries = {}
for rk in RUNS:
    base = orig["summariesByRun"][rk][0]["summary"]
    tp, fp, fn, tn = (base["totalTruePositives"], base["totalFalsePositives"],
                      base["totalFalseNegatives"], base["totalTrueNegatives"])
    passed, failed, retries = base["passed"], base["failed"], base["amountOfRetries"]
    brm = base["ragMetrics"]
    faith_sum = brm["faithfulnessMean"] * brm["evaluatedTestCases"]
    ctx_sum   = brm["contextUtilizationMean"] * brm["evaluatedTestCases"]
    evaluated = brm["evaluatedTestCases"]
    total_samples = brm["totalSamples"]
    failed_samples = brm["failedSamples"]

    for gid, src in fill_log[rk]:
        tc = (rr1 if src == "rerun1" else rr2)["testCasesByRun"][rk]
        tc = next(t for t in tc if t["testCaseId"] == gid)
        a, b, c = tpfpfn(tc)
        tp += a; fp += b; fn += c
        tn += N[gid] - (a + b + c)
        retries += tc["amountOfRetries"]
        if tc["isSuccessful"]:
            passed += 1
        else:
            failed += 1
        rm = tc.get("ragMetrics")
        if rm:
            faith_sum += rm["faithfulness"]; ctx_sum += rm["contextUtilization"]
            evaluated += 1
            total_samples += rm["sampleCount"]; failed_samples += rm["failedCount"]

    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall    = tp / (tp + fn) if (tp + fn) else 0.0
    f1        = 2 * precision * recall / (precision + recall) if (precision + recall) else 0.0
    accuracy  = (tp + tn) / (tp + fp + fn + tn) if (tp + fp + fn + tn) else 0.0

    s = {
        "type": "summary", "total": 93, "passed": passed, "failed": failed, "error": 0,
        "amountOfRetries": retries, "precision": precision, "recall": recall, "f1Score": f1,
        "accuracy": accuracy, "totalTruePositives": tp, "totalFalsePositives": fp,
        "totalFalseNegatives": fn, "totalTrueNegatives": tn,
        "ragMetrics": {
            "faithfulnessMean": faith_sum / evaluated if evaluated else 0.0,
            "contextUtilizationMean": ctx_sum / evaluated if evaluated else 0.0,
            "evaluatedTestCases": evaluated, "totalSamples": total_samples,
            "failedSamples": failed_samples,
        },
    }
    s["markdown"] = make_summary_markdown(s)
    merged_summaries[rk] = [{"label": LABEL, "summary": s}]

# ---- aggregate stats across runs (population mean/std) ------------------------
def mean(xs): return sum(xs) / len(xs)
def pstd(xs):
    m = mean(xs); return math.sqrt(sum((x - m) ** 2 for x in xs) / len(xs))

S = [merged_summaries[rk][0]["summary"] for rk in RUNS]
def col(key): return [s[key] for s in S]
def ragcol(key): return [s["ragMetrics"][key] for s in S]

agg = {
    "avgPrecision": mean(col("precision")),       "stdPrecision": pstd(col("precision")),
    "avgRecall": mean(col("recall")),             "stdRecall": pstd(col("recall")),
    "avgF1Score": mean(col("f1Score")),           "stdF1Score": pstd(col("f1Score")),
    "avgAccuracy": mean(col("accuracy")),         "stdAccuracy": pstd(col("accuracy")),
    "avgPassed": mean(col("passed")),             "stdPassed": pstd(col("passed")),
    "avgFailed": mean(col("failed")),             "stdFailed": pstd(col("failed")),
    "avgErrors": 0.0,                             "stdErrors": 0.0,
    "avgAmountOfRetries": mean(col("amountOfRetries")), "stdAmountOfRetries": pstd(col("amountOfRetries")),
    "avgContextUtilization": mean(ragcol("contextUtilizationMean")), "stdContextUtilization": pstd(ragcol("contextUtilizationMean")),
    "avgTruePositives": mean(col("totalTruePositives")),  "stdTruePositives": pstd(col("totalTruePositives")),
    "avgFalsePositives": mean(col("totalFalsePositives")),"stdFalsePositives": pstd(col("totalFalsePositives")),
    "avgFalseNegatives": mean(col("totalFalseNegatives")),"stdFalseNegatives": pstd(col("totalFalseNegatives")),
    "avgTrueNegatives": mean(col("totalTrueNegatives")),  "stdTrueNegatives": pstd(col("totalTrueNegatives")),
    "avgFaithfulness": mean(ragcol("faithfulnessMean")),  "stdFaithfulness": pstd(ragcol("faithfulnessMean")),
    "ragRunsCounted": 3,
}

# ---- write merged JSON --------------------------------------------------------
out = {
    "metadata": orig["metadata"],
    "testCasesByRun": merged_tc,
    "summariesByRun": merged_summaries,
    "errorsByRun": {},
    "aggregateStats": {LABEL: agg},
    "colors": orig.get("colors"),
}
outdir = os.path.join(BASE, "final")
os.makedirs(outdir, exist_ok=True)
json.dump(out, open(os.path.join(outdir, "evaluation_report_multi.json"), "w", encoding="utf-8"),
          ensure_ascii=False, indent=1)

# ---- write merged Markdown ----------------------------------------------------
meta = orig["metadata"]
def meta_md():
    # reuse original metadata block verbatim (read from the original .md head)
    head = open(os.path.join(BASE, "evaluation_report_multi.md"), encoding="utf-8").read().splitlines()
    return "\n".join(head[:12])

md = [meta_md(), "", "# Aggregate Statistics Across All Runs", "", f"## Model: {LABEL}", ""]
def line(lbl, a, sd, suffix=""):
    md.append(f"- {lbl}: {a:.3f} ± {sd:.3f}{suffix}"); md.append("")
line("Precision", agg["avgPrecision"], agg["stdPrecision"])
line("Recall", agg["avgRecall"], agg["stdRecall"])
line("F1-Score", agg["avgF1Score"], agg["stdF1Score"])
line("Accuracy", agg["avgAccuracy"], agg["stdAccuracy"])
line("True Positives", agg["avgTruePositives"], agg["stdTruePositives"])
line("False Positives", agg["avgFalsePositives"], agg["stdFalsePositives"])
line("False Negatives", agg["avgFalseNegatives"], agg["stdFalseNegatives"])
line("True Negatives", agg["avgTrueNegatives"], agg["stdTrueNegatives"])
line("Passed", agg["avgPassed"], agg["stdPassed"], " / 93")
line("Failed", agg["avgFailed"], agg["stdFailed"], " / 93")
line("Errors", agg["avgErrors"], agg["stdErrors"], " / 93")
md.append(f"- Amount of Retries: {agg['avgAmountOfRetries']:.3f} ± {agg['stdAmountOfRetries']:.3f}")
md += ["", "", "### RAG Metrics (averaged across 3 run(s))", ""]
md.append(f"- Context Utilization: {agg['avgContextUtilization']:.3f} ± {agg['stdContextUtilization']:.3f}"); md.append("")
md.append(f"- Faithfulness: {agg['avgFaithfulness']:.3f} ± {agg['stdFaithfulness']:.3f}")

for rk in RUNS:
    md += ["", f"# Run {rk}", "", f"## Summary ({LABEL})", "",
           merged_summaries[rk][0]["summary"]["markdown"], "", f"## Model: {LABEL}", ""]
    for tc in merged_tc[rk]:
        md.append(tc["markdown"]); md.append("")

open(os.path.join(outdir, "evaluation_report_multi.md"), "w", encoding="utf-8").write("\n".join(md))

# ---- console report -----------------------------------------------------------
print("Fill sources per run:")
for rk in RUNS:
    print(f"  run {rk}: " + ", ".join(f"{gid}<-{src}" for gid, src in fill_log[rk]))
print()
print(f"{'Run':>4} {'P':>6} {'R':>6} {'F1':>6} {'Acc':>6}  {'TP':>4} {'FP':>4} {'FN':>4} {'TN':>4}  pass/fail/err  Faith  CtxUtil")
for rk in RUNS:
    s = merged_summaries[rk][0]["summary"]; rm = s["ragMetrics"]
    print(f"{rk:>4} {s['precision']:.3f} {s['recall']:.3f} {s['f1Score']:.3f} {s['accuracy']:.3f}  "
          f"{s['totalTruePositives']:>4} {s['totalFalsePositives']:>4} {s['totalFalseNegatives']:>4} {s['totalTrueNegatives']:>4}  "
          f"{s['passed']:>2}/{s['failed']:>2}/{s['error']:>2}      {rm['faithfulnessMean']:.3f}  {rm['contextUtilizationMean']:.3f}")
print()
print("AGGREGATE (mean ± population std, 3 runs, 93 test cases, 0 errors):")
print(f"  Precision : {agg['avgPrecision']:.3f} ± {agg['stdPrecision']:.3f}")
print(f"  Recall    : {agg['avgRecall']:.3f} ± {agg['stdRecall']:.3f}")
print(f"  F1-Score  : {agg['avgF1Score']:.3f} ± {agg['stdF1Score']:.3f}")
print(f"  Accuracy  : {agg['avgAccuracy']:.3f} ± {agg['stdAccuracy']:.3f}")
print(f"  Faithfuln.: {agg['avgFaithfulness']:.3f} ± {agg['stdFaithfulness']:.3f}")
print(f"  CtxUtil.  : {agg['avgContextUtilization']:.3f} ± {agg['stdContextUtilization']:.3f}")
print(f"  Passed/Failed/Errors: {agg['avgPassed']:.3f} / {agg['avgFailed']:.3f} / {agg['avgErrors']:.3f}")
print("\nWrote:", os.path.join(outdir, "evaluation_report_multi.json"))
print("Wrote:", os.path.join(outdir, "evaluation_report_multi.md"))
