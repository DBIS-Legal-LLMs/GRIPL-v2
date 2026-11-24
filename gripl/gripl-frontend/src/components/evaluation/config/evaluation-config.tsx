"use client";

import React, { JSX, useEffect, useState } from "react";
import { ChevronDown } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import {Dataset} from "@/models/dto/Dataset";
import {MultiEvaluationRequest} from "@/models/dto/MultiEvaluationRequest";
import {useEvaluationConfig} from "@/hooks/evaluation/use-evaluation-config";
import {useYamlImportExport} from "@/hooks/evaluation/use-yaml-import-export";
import EvaluationConfigHeader from "@/components/evaluation/config/evaluation-config-header";
import EvaluationConfigDefaultSettings from "@/components/evaluation/config/evaluation-config-default-settings";
import EvaluationConfigDatasetSettings from "@/components/evaluation/config/evaluation-config-dataset-settings";
import EvaluationConfigModelsSettings from "@/components/evaluation/config/evaluation-config-models-settings";
import {nextLabel} from "@/lib/evaluation-config-utils";

const CONFIG_OPEN_KEY = "gripl.evaluation.configOpen";

interface EvaluationConfigCardMultiProps {
    className?: string;
    children?: JSX.Element;
    datasets: Dataset[];
    onMultiConfigChanged: (config: MultiEvaluationRequest) => void;
}

export default function EvaluationConfig({ className, children, datasets, onMultiConfigChanged }: EvaluationConfigCardMultiProps) {
    const config = useEvaluationConfig(datasets, onMultiConfigChanged);

    const { fileInputRef, onClickImportYaml, onFileChange, onClickExportYaml } = useYamlImportExport({
        availableEvaluationEndpoints: config.availableEvaluationEndpoints,
        effectiveDefaultEndpoint: config.effectiveDefaultEndpoint,
        models: config.models,
        selectedDatasets: config.selectedDatasets,
        experimentName: config.experimentName,
        seed: config.seed,
        maxConcurrent: config.maxConcurrent,
        repetitions: config.repetitions,
        useRag: config.useRag,
        ragMode: config.ragMode,
        evaluateRag: config.evaluateRag,
        activitiesOnly: config.activitiesOnly,
        setDefaultEndpointChoice: (v) => config.setDefaultEndpointChoice(v),
        setDefaultPresetEndpoint: config.setDefaultPresetEndpoint,
        setDefaultCustomEndpoint: config.setDefaultCustomEndpoint,
        setExperimentName: config.setExperimentName,
        setSeed: config.setSeed,
        setMaxConcurrent: config.setMaxConcurrent,
        setRepetitions: config.setRepetitions,
        setSelectedDatasets: config.setSelectedDatasets,
        setModels: config.setModels,
        setUseRag: config.setUseRag,
        setRagMode: config.setRagMode,
        setEvaluateRag: config.setEvaluateRag,
        setActivitiesOnly: config.setActivitiesOnly,
    });

    // Remembered across visits, so someone who only looks at results keeps the settings out of the way.
    const [isOpen, setIsOpen] = useState(true);
    useEffect(() => {
        if (localStorage.getItem(CONFIG_OPEN_KEY) === "false") setIsOpen(false);
    }, []);
    const toggleOpen = (open: boolean) => {
        setIsOpen(open);
        localStorage.setItem(CONFIG_OPEN_KEY, String(open));
    };

    return (
        <Collapsible open={isOpen} onOpenChange={toggleOpen}>
        <div className={`bg-background dark ${className ?? ""}`}>
            <div className="border-b border-border bg-card">
                <div className="container mx-auto px-6 py-4">
                    <div className="flex items-center justify-between">
                        <CollapsibleTrigger asChild>
                            <button type="button" className="flex items-center gap-3 text-left group"
                                    aria-label={isOpen ? "Collapse the evaluation settings" : "Expand the evaluation settings"}>
                                <ChevronDown className={`h-5 w-5 text-muted-foreground transition-transform ${isOpen ? "" : "-rotate-90"}`}/>
                                <div>
                                    <h1 className="text-2xl font-bold text-card-foreground group-hover:underline">Evaluation Config</h1>
                                    <p className="text-sm text-muted-foreground mt-1">
                                        {isOpen ? "Settings, datasets and models" : `${config.models.length} model(s), ${config.selectedDatasets.length} dataset(s) selected. Click to show the settings.`}
                                    </p>
                                </div>
                            </button>
                        </CollapsibleTrigger>
                        <EvaluationConfigHeader
                            fileInputRef={fileInputRef}
                            onFileChange={onFileChange}
                            onClickImportYaml={onClickImportYaml}
                            onClickExportYaml={onClickExportYaml}
                        />
                    </div>
                </div>
            </div>

            <div className={`container mx-auto px-6 space-y-8 ${isOpen ? "py-8" : "pt-4 pb-2"}`}>
                <CollapsibleContent className="space-y-8">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    <EvaluationConfigDefaultSettings
                        availableEvaluationEndpoints={config.availableEvaluationEndpoints}
                        defaultEndpointChoice={config.defaultEndpointChoice}
                        defaultPresetEndpoint={config.defaultPresetEndpoint}
                        defaultCustomEndpoint={config.defaultCustomEndpoint}
                        experimentName={config.experimentName}
                        setExperimentName={config.setExperimentName}
                        seed={config.seed}
                        maxConcurrent={config.maxConcurrent}
                        repetitions={config.repetitions}
                        useRag={config.useRag}
                        ragMode={config.ragMode}
                        evaluateRag={config.evaluateRag}
                        activitiesOnly={config.activitiesOnly}
                        setDefaultEndpointChoice={config.setDefaultEndpointChoice}
                        setDefaultPresetEndpoint={config.setDefaultPresetEndpoint}
                        setDefaultCustomEndpoint={config.setDefaultCustomEndpoint}
                        setSeed={config.setSeed}
                        onMaxConcurrentChange={(v) => config.setMaxConcurrent(v)}
                        onRepetitionsChange={(v) => config.setRepetitions(v)}
                        setUseRag={config.setUseRag}
                        setRagMode={config.setRagMode}
                        setEvaluateRag={config.setEvaluateRag}
                        setActivitiesOnly={config.setActivitiesOnly}
                    />

                    <EvaluationConfigDatasetSettings
                        datasets={datasets}
                        selectedDatasets={config.selectedDatasets}
                        selectedTestCaseIds={config.selectedTestCaseIds}
                        onDatasetsChange={config.setSelectedDatasets}
                        onTestCasesChange={config.setSelectedTestCaseIds}
                    />
                </div>

                <EvaluationConfigModelsSettings
                    models={config.models}
                    effectiveDefaultEndpoint={config.effectiveDefaultEndpoint}
                    availableEvaluationEndpoints={config.availableEvaluationEndpoints}
                    addModel={config.addModel}
                    updateModel={config.updateModel}
                    duplicateModel={(id) => config.duplicateModel(id, nextLabel)}
                    removeModel={config.removeModel}
                />
                </CollapsibleContent>

                {children}
            </div>
        </div>
        </Collapsible>
    );
}
