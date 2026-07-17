"use client";

import { useEffect, useRef, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Dataset } from "@/models/dto/Dataset";
import { EvaluationDataMeta } from "@/models/dto/EvaluationData";
import getTestcasesByDataset from "@/actions/get-testcases-by-dataset";

interface EvaluationConfigDatasetSettingsProps {
    datasets: Dataset[];
    selectedDatasets: number[];
    selectedTestCaseIds: number[];
    onDatasetsChange: (ids: number[]) => void;
    onTestCasesChange: (ids: number[]) => void;
}

export default function EvaluationConfigDatasetSettings({
    datasets,
    selectedDatasets,
    selectedTestCaseIds,
    onDatasetsChange,
    onTestCasesChange,
}: EvaluationConfigDatasetSettingsProps) {
    const [testCasesByDataset, setTestCasesByDataset] = useState<Record<number, EvaluationDataMeta[]>>({});
    const [loadingDatasets, setLoadingDatasets] = useState<Record<number, boolean>>({});
    const [expandedDatasetId, setExpandedDatasetId] = useState<number | null>(null);
    const selectedDatasetsRef = useRef(selectedDatasets);
    selectedDatasetsRef.current = selectedDatasets;
    const selectedTestCaseIdsRef = useRef(selectedTestCaseIds);
    selectedTestCaseIdsRef.current = selectedTestCaseIds;

    useEffect(() => {
        const missing = selectedDatasets.filter(
            (id) => testCasesByDataset[id] === undefined && !loadingDatasets[id]
        );
        if (missing.length === 0) return;

        setLoadingDatasets((prev) => {
            const next = { ...prev };
            missing.forEach((id) => (next[id] = true));
            return next;
        });

        missing.forEach((datasetId) => {
            getTestcasesByDataset(datasetId).then((tcs) => {
                setTestCasesByDataset((prev) => ({ ...prev, [datasetId]: tcs }));
                setLoadingDatasets((prev) => ({ ...prev, [datasetId]: false }));
                // Skip the auto-select if the dataset was deselected mid-load.
                if (!selectedDatasetsRef.current.includes(datasetId)) return;
                // Default: newly loaded dataset's test cases are all selected.
                const merged = Array.from(
                    new Set([...selectedTestCaseIdsRef.current, ...tcs.map((tc) => tc.id)])
                );
                selectedTestCaseIdsRef.current = merged;
                onTestCasesChange(merged);
            });
        });
    }, [selectedDatasets]);

    function toggleDataset(datasetId: number) {
        const isSelected = selectedDatasets.includes(datasetId);
        const isExpanded = expandedDatasetId === datasetId;

        if (isSelected && isExpanded) {
            // Second click on the currently expanded dataset deselects it.
            const tcIds = (testCasesByDataset[datasetId] ?? []).map((tc) => tc.id);
            onDatasetsChange(selectedDatasets.filter((id) => id !== datasetId));
            if (tcIds.length > 0) {
                onTestCasesChange(selectedTestCaseIds.filter((id) => !tcIds.includes(id)));
            }
            setExpandedDatasetId(null);
        } else if (isSelected) {
            // Already selected but collapsed — just expand it (collapsing any other).
            setExpandedDatasetId(datasetId);
        } else {
            // Newly selected — select and expand, collapsing any other.
            onDatasetsChange([...selectedDatasets, datasetId]);
            setExpandedDatasetId(datasetId);
        }
    }

    function toggleTestCase(id: number) {
        if (selectedTestCaseIds.includes(id)) {
            onTestCasesChange(selectedTestCaseIds.filter((x) => x !== id));
        } else {
            onTestCasesChange([...selectedTestCaseIds, id]);
        }
    }

    function toggleAllForDataset(datasetId: number) {
        const tcs = testCasesByDataset[datasetId] ?? [];
        const tcIds = tcs.map((tc) => tc.id);
        const allSelected = tcIds.length > 0 && tcIds.every((id) => selectedTestCaseIds.includes(id));
        if (allSelected) {
            onTestCasesChange(selectedTestCaseIds.filter((id) => !tcIds.includes(id)));
        } else {
            onTestCasesChange(Array.from(new Set([...selectedTestCaseIds, ...tcIds])));
        }
    }

    const totalSelected = selectedTestCaseIds.length;
    const totalAvailable = selectedDatasets.reduce(
        (sum, id) => sum + (testCasesByDataset[id]?.length ?? 0),
        0
    );

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-lg">Datasets</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label>Select Datasets</Label>
                    <div className="flex flex-wrap gap-2">
                        {datasets.map((ds) => {
                            const active = selectedDatasets.includes(ds.id);
                            return (
                                <button
                                    key={ds.id}
                                    onClick={() => toggleDataset(ds.id)}
                                    className={`px-3 py-1.5 rounded-md text-sm border transition-colors ${
                                        active
                                            ? "bg-primary text-primary-foreground border-primary"
                                            : "bg-muted text-muted-foreground border-border hover:bg-accent hover:text-accent-foreground"
                                    }`}
                                >
                                    {ds.name}
                                </button>
                            );
                        })}
                    </div>
                </div>

                {selectedDatasets.length > 0 && (
                    <div className="space-y-3">
                        <Label>
                            Test Cases
                            {totalAvailable > 0 && (
                                <span className="ml-2 text-muted-foreground font-normal">
                                    ({totalSelected}/{totalAvailable} selected across {selectedDatasets.length} dataset
                                    {selectedDatasets.length === 1 ? "" : "s"})
                                </span>
                            )}
                        </Label>

                        {selectedDatasets.map((datasetId) => {
                            const isExpanded = expandedDatasetId === datasetId;
                            if (!isExpanded) return null;

                            const dataset = datasets.find((d) => d.id === datasetId);
                            const tcs = testCasesByDataset[datasetId];
                            const isLoading = loadingDatasets[datasetId];
                            const tcIds = (tcs ?? []).map((tc) => tc.id);
                            const allSelected =
                                tcIds.length > 0 && tcIds.every((id) => selectedTestCaseIds.includes(id));
                            const selectedHere = tcIds.filter((id) => selectedTestCaseIds.includes(id)).length;

                            return (
                                <div key={datasetId} className="space-y-2">
                                    <div className="flex items-center justify-between">
                                        <span className="text-sm font-medium">
                                            {dataset?.name ?? `Dataset ${datasetId}`}
                                            {tcs && tcs.length > 0 && (
                                                <span className="ml-2 text-muted-foreground font-normal">
                                                    ({selectedHere}/{tcs.length})
                                                </span>
                                            )}
                                        </span>
                                        {tcs && tcs.length > 0 && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => toggleAllForDataset(datasetId)}
                                            >
                                                {allSelected ? "Deselect All" : "Select All"}
                                            </Button>
                                        )}
                                    </div>

                                    {(isLoading || tcs === undefined ? (
                                        <p className="text-sm text-muted-foreground">Loading...</p>
                                    ) : tcs.length === 0 ? (
                                        <p className="text-sm text-muted-foreground">No test cases found.</p>
                                    ) : (
                                        <div className="max-h-56 overflow-y-auto space-y-1 rounded-md border border-border p-2">
                                            {tcs.map((tc) => (
                                                <div
                                                    key={tc.id}
                                                    className="flex items-center gap-2 px-2 py-1 rounded hover:bg-accent cursor-pointer"
                                                    onClick={() => toggleTestCase(tc.id)}
                                                >
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedTestCaseIds.includes(tc.id)}
                                                        onChange={() => toggleTestCase(tc.id)}
                                                        className="h-4 w-4 rounded border-border accent-primary"
                                                        onClick={(e) => e.stopPropagation()}
                                                    />
                                                    <span className="text-sm truncate">
                                                        {tc.name ?? `Test Case ${tc.id}`}
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    ))}
                                </div>
                            );
                        })}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
