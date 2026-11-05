"use client"

import BpmnEditor from "@/components/bpmn-editor";
import {EvaluationData, ExpectedValues} from "@/models/dto/EvaluationData";
import {BpmnToolCard} from "@/models/BpmnToolCard";
import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {AlertTriangle} from "lucide-react";
import {ChevronDown, ChevronLeft, ChevronRight, Save, Trash2} from "lucide-react";
import {useCallback, useEffect, useState} from "react";
import {Switch} from "@/components/ui/switch";
import {Label} from "@/components/ui/label";
import {BpmnEditorEvent} from "@/models/BpmnEditorEvent";
import LabelingEditorLabelCard from "@/components/labeling/labeling-editor-label-card";
import {
    GdprCategory,
    normalizeFrontendGdprCategories,
    toBackendGdprProcessingClass,
} from "@/models/GdprCategory";
import {Spinner} from "@/components/ui/spinner";
import {Badge} from "@/components/ui/badge";
import {useToast} from "@/components/ui/toast";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {Label as LabelDto} from "@/models/dto/Label";
import {useAuth} from "@/context/auth-context";
import ActiveLabelsPanel from "@/components/labeling/active-labels-panel";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";

interface LabelingEditorProps {
    className?: string;
    evaluationData: EvaluationData;
}

interface BpmnSelectionElement {
    id: string;
    businessObject?: {name?: string};
}

interface BpmnEventPayload {
    newSelection?: BpmnSelectionElement[];
    element?: BpmnSelectionElement;
}

export default function LabelingEditor({ className, evaluationData }: LabelingEditorProps) {
    const [diagram, setDiagram] = useState<string>(evaluationData.bpmnXml)
    const [isLabelMode, setIsLabelMode] = useState<boolean>(false);
    const [criticalActivities, setCriticalActivities] = useState<ExpectedValues[]>(
        (evaluationData.expectedValues || []).map((value) => ({
            ...value,
            classification: normalizeFrontendGdprCategories((value.classification as unknown as string[]) ?? []),
            labelIds: value.labelIds ?? value.labels?.map((label) => label.id),
        }))
    );
    const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
    const [selectedElement, setSelectedElement] = useState<BpmnSelectionElement | null>(null);
    const [isSaveLoading, setIsSaveLoading] = useState(false);
    const [elementNames, setElementNames] = useState<Record<string, string>>({});
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [availableLabels, setAvailableLabels] = useState<LabelDto[]>(
        (evaluationData.expectedValues ?? []).flatMap((value) => value.labels ?? [])
    );
    const {isLoading: isAuthLoading, token} = useAuth();
    const {isMulticlass, backendEndpoint} = useAnalysisEndpoint();

    const handleAvailableLabelsChanged = useCallback((activeLabels: LabelDto[]) => {
        const initialAssignedLabels = (evaluationData.expectedValues ?? []).flatMap((entry) => entry.labels ?? []);
        const activeIds = new Set(activeLabels.map((label) => label.id));
        setAvailableLabels((previous) => {
            const merged = new Map<number, LabelDto>();
            [...activeLabels, ...initialAssignedLabels].forEach((label) => merged.set(label.id, label));
            previous.forEach((label) => {
                if (merged.has(label.id)) return;
                if (activeIds.has(label.id)) return;
                // Label is no longer active (archived) but still known in this session.
                merged.set(label.id, {
                    ...label,
                    deletedAt: label.deletedAt ?? new Date().toISOString(),
                });
            });
            return Array.from(merged.values());
        });
    }, [evaluationData.expectedValues]);

    useEffect(() => {
        if (isAuthLoading) return;
        let isMounted = true;
        authenticatedFetch("/api/labels")
            .then(async (response) => {
                if (!response.ok) throw new Error(await extractErrorDetails(response));
                return response.json() as Promise<LabelDto[]>;
            })
            .then((activeLabels) => {
                if (!isMounted) return;
                handleAvailableLabelsChanged(activeLabels);
                setCriticalActivities((current) => current.map((entry) => {
                    if (entry.labels !== undefined) {
                        return {...entry, labelIds: entry.labels.map((label) => label.id)};
                    }
                    if (entry.labelIds !== undefined) {
                        return entry;
                    }
                    const categoryNames = new Set((entry.classification ?? []).map((category) => category.toUpperCase()));
                    const fallbackIds = activeLabels
                        .filter((label) => label.predefined && categoryNames.has(label.name.toUpperCase()))
                        .map((label) => label.id);
                    return {...entry, labelIds: fallbackIds};
                }));
            })
            .catch((error) => console.error("Could not load active labels:", error));
        return () => { isMounted = false; };
    }, [evaluationData.expectedValues, handleAvailableLabelsChanged, isAuthLoading, token]);

    const categoryBadgeClass: Record<GdprCategory, string> = {
        Collection: "bg-emerald-600 text-white border-transparent",
        Storage: "bg-blue-600 text-white border-transparent",
        Usage: "bg-amber-500 text-black border-transparent",
        Transferal: "bg-violet-600 text-white border-transparent",
        Modification: "bg-sky-600 text-white border-transparent",
        Deletion: "bg-red-600 text-white border-transparent",
        Access: "bg-teal-600 text-white border-transparent",
    };

    const {showToast, showError} = useToast();

    function onSave() {

        setIsSaveLoading(true);

        const xmlBlob = new Blob([diagram], { type: "application/xml" });
        const formData = new FormData();
        formData.append("bpmnFile", xmlBlob, "diagram.bpmn");
        formData.append("name", evaluationData.name);
        const expectedValuesPayload = criticalActivities.map((value) => ({
            value: value.value,
            explanation: value.explanation,
            labelIds: value.labelIds ?? value.labels?.map((label) => label.id) ?? [],
            classification: (value.classification ?? []).map(toBackendGdprProcessingClass),
        }));
        const expectedValuesBlob = new Blob(
            [JSON.stringify(expectedValuesPayload)],
            { type: 'application/json' }
        );
        formData.append('expectedValues', expectedValuesBlob, 'expectedValues.json');

        authenticatedFetch(`/api/dataset/testcase/${evaluationData.id}`, {
            method: "POST",
            headers: {
                Accept: "application/json",
            },
            body: formData,
        }).then(async response => {
            if (!response.ok) {
                const details = await extractErrorDetails(response);
                console.error("Error while saving test case:", details);
                throw new Error(details);
            }
            setHasUnsavedChanges(false)
            setIsSaveLoading(false);
            showToast({title: "Test case saved successfully", variant: "success"});
        }).catch(error => {
            console.error("Error while saving test case:", error);
            setIsSaveLoading(false);
            showError("Failed to save the test case", toErrorMessage(error));
        })
    }

    function handleDiagramChanged(xml: string) {
        setDiagram(xml)
        setHasUnsavedChanges(true)
    }

    function handleElementLabelingChange(
        elementId: string,
        classification: GdprCategory[] | null,
        labelIds: number[],
        explanation: string,
        isCritical: boolean
    ) {
        setCriticalActivities((current) => {
            if (!isCritical) return current.filter((critical) => critical.value !== elementId);
            const currentEntry = current.find((critical) => critical.value === elementId);
            const nextEntry: ExpectedValues = {
                ...currentEntry,
                value: elementId,
                classification: classification ?? [],
                labelIds,
                explanation,
            };
            return currentEntry
                ? current.map((critical) => critical.value === elementId ? nextEntry : critical)
                : [...current, nextEntry];
        });
        setHasUnsavedChanges(true);
    }

    function handleEditorEvent(type: BpmnEditorEvent, event: BpmnEventPayload) {
        if (type === BpmnEditorEvent.SelectionChanged) {
            const selection = event.newSelection ?? [];
            const element = selection.length === 1 ? selection[0] : null;
            setSelectedElement(element);

            if (element?.id) {
                const nextName = element.businessObject?.name || "No Name";
                setElementNames((prev) => ({
                    ...prev,
                    [element.id]: nextName,
                }));
            }
        }

        const changedElement = event.element;
        if (type === BpmnEditorEvent.ElementChanged && changedElement?.id) {
            const nextName = changedElement.businessObject?.name || "No Name";
            setElementNames((prev) => ({
                ...prev,
                [changedElement.id]: nextName,
            }));
        }
    }

    function removeLabel(elementId: string) {
        setCriticalActivities(criticalActivities.filter((critical) => critical.value !== elementId));
        setHasUnsavedChanges(true);
    }

    const unclassifiedCount = criticalActivities.filter(critical => (critical.classification ?? []).length === 0).length;
    const selectedElementName =selectedElement?.businessObject?.name || "No Name";
    const highlightedActivityCategoryMap = criticalActivities.reduce((acc, critical) => {
        acc[critical.value] = critical.classification || [];
        return acc;
    }, {} as Record<string, GdprCategory[]>);

    const highlightedActivityLabelColors = criticalActivities.reduce((acc, critical) => {
        const assignedIds = critical.labelIds ?? critical.labels?.map((label) => label.id) ?? [];
        const labelColors = assignedIds
            .map((id) => {
                const active = availableLabels.find((l) => l.id === id);
                const embedded = critical.labels?.find((l) => l.id === id);
                const label = active ?? embedded;
                return label?.predefined === false ? label.color : null;
            })
            .filter((color): color is string => Boolean(color));
        acc[critical.value] = labelColors.length > 0 ? labelColors : [];
        return acc;
    }, {} as Record<string, string[]>);

    const cards: BpmnToolCard[] = [
        {
            position: "top-right",
            content: <Card className="bg-card/75">
                <CardHeader>
                    <h2 className="text-lg font-semibold">Labeltools</h2>
                </CardHeader>
                <CardContent className="flex flex-col space-y-4">
                    <Button
                        onClick={onSave}
                        variant="default"
                        disabled={!hasUnsavedChanges || isSaveLoading}
                        className={"min-w-[153px]"}
                    >
                        { isSaveLoading ? <>
                            <Spinner className="h-4 w-4 text-foreground" />
                            Saving...
                        </> : <>
                            <Save className="mr-2 h-4 w-4"/>
                            Save Test Case
                        </>}
                    </Button>
                    <div className="flex items-center space-x-2">
                        <Switch id="label-mode" checked={isLabelMode} onCheckedChange={setIsLabelMode}/>
                        <Label htmlFor="label-mode">Label Mode</Label>
                    </div>
                    <div className="text-xs text-muted-foreground break-all">
                        Endpoint mode: {isMulticlass ? "multiclass" : "binary"}
                        <br />
                        Endpoint: {backendEndpoint}
                    </div>
                    <Collapsible className="border-t pt-2">
                        <CollapsibleTrigger asChild>
                            <Button type="button" variant="ghost" className="h-auto w-full justify-between px-2 py-1 text-sm">
                                Manage active labels
                                <ChevronDown className="h-4 w-4" />
                            </Button>
                        </CollapsibleTrigger>
                        <CollapsibleContent className="mt-2 max-h-[40vh] overflow-y-auto overscroll-contain">
                            <ActiveLabelsPanel onLabelsChanged={handleAvailableLabelsChanged} />
                        </CollapsibleContent>
                    </Collapsible>
                </CardContent>
            </Card>
        },
        {
            position: "top-right",
            content: selectedElement && isLabelMode ? <LabelingEditorLabelCard
                className="min-w-[153px]"
                elementName={selectedElementName}
                elementId={selectedElement.id}
                criticalActivities={criticalActivities}
                labels={availableLabels}
                allowMulticlass={isMulticlass}
                onLabelingChange={handleElementLabelingChange}
            /> : <></>
        }
    ]

    return (
        <div className={`h-full w-full flex ${className}`}>
            <div className="h-full min-w-0 flex-1 transition-all duration-200">
                <BpmnEditor
                    title={evaluationData.name || ""}
                    bpmnXml={diagram}
                    cards={cards}
                    highlightedActivityIds={criticalActivities.map(critical => critical.value)}
                    highlightedActivityCategories={highlightedActivityCategoryMap}
                    highlightedActivityLabelColors={highlightedActivityLabelColors}
                    onDiagramChanged={handleDiagramChanged}
                    editorClassName={isLabelMode ? "border border-destructive" : ""}
                    disableEditing={isLabelMode}
                    onEvent={handleEditorEvent}
                />
            </div>

            <aside className={`h-full border-l bg-card transition-all duration-200 ${isSidebarOpen ? "w-[320px]" : "w-10"}`}>
                {isSidebarOpen ? (
                    <div className="h-full flex flex-col">
                        <div className="px-3 py-2 border-b flex items-center justify-between">
                            <h2 className="text-sm font-semibold">Labeled BPMN Elements</h2>
                            <Button
                                type="button"
                                size="icon"
                                variant="ghost"
                                className="h-7 w-7"
                                onClick={() => setIsSidebarOpen(false)}
                                title="Labeled Activities einklappen"
                            >
                                <ChevronRight className="h-4 w-4" />
                            </Button>
                        </div>
                        <div className="px-3 py-2 border-b">
                            <p className="text-xs text-muted-foreground">
                                {criticalActivities.length} labeled
                                {isMulticlass && unclassifiedCount > 0 && (
                                    <span className="text-amber-600"> · {unclassifiedCount} without class</span>
                                )}
                            </p>
                        </div>
                        {criticalActivities.some((critical) => {
                            const assignedLabelIds = critical.labelIds ?? critical.labels?.map((label) => label.id) ?? [];
                            return assignedLabelIds.some((id) => {
                                const label = availableLabels.find((l) => l.id === id) ?? critical.labels?.find((l) => l.id === id);
                                return !label || label.deletedAt !== null;
                            });
                        }) && (
                            <div className="m-3 p-2 border border-destructive/50 bg-destructive/5 rounded-md flex items-start gap-2 text-destructive">
                                <AlertTriangle className="h-4 w-4 mt-0.5 flex-shrink-0" />
                                <p className="text-[10px] leading-tight">
                                    Some elements use archived labels. Please remove or replace them.
                                </p>
                            </div>
                        )}
                        <div className="flex-1 overflow-y-auto p-3 space-y-2">
                            {criticalActivities.length === 0 && (
                                <p className="text-xs text-muted-foreground">
                                    No labels yet. Click an activity and select classes.
                                </p>
                            )}
                            {criticalActivities.map((critical) => {
                                const isSelected = selectedElement?.id === critical.value;
                                const displayName = elementNames[critical.value] || critical.value;
                                const isMultiCategory = isMulticlass && (critical.classification ?? []).length > 1;
                                const assignedLabelIds = critical.labelIds ?? critical.labels?.map((label) => label.id) ?? [];
                                const assignedLabels = assignedLabelIds
                                    .map((id) => {
                                        const activeLabel = availableLabels.find((label) => label.id === id);
                                        const embeddedLabel = critical.labels?.find((label) => label.id === id);
                                        return activeLabel ?? embeddedLabel;
                                    })
                                    .filter((label): label is LabelDto => Boolean(label));
                                return (
                                    <div
                                        key={critical.value}
                                        className={`border rounded p-2 ${
                                            isSelected
                                                ? "border-destructive"
                                                : isMultiCategory
                                                    ? "border-fuchsia-500/70 bg-fuchsia-500/5"
                                                    : "border-border"
                                        }`}
                                    >
                                        <div className="flex items-start justify-between gap-2">
                                            <div className="min-w-0">
                                                <p className="text-xs font-medium break-words">{displayName}</p>
                                                <p className="text-[10px] text-muted-foreground break-all">{critical.value}</p>
                                            </div>
                                            <Button
                                                type="button"
                                                size="icon"
                                                variant="ghost"
                                                className="h-6 w-6"
                                                onClick={() => removeLabel(critical.value)}
                                            >
                                                <Trash2 className="h-3 w-3" />
                                            </Button>
                                        </div>
                                        <div className="mt-2 flex flex-wrap gap-1">
                                            <>{isMulticlass ? (
                                                <>
                                                    {(critical.classification ?? []).length === 0 && (
                                                        <Badge variant="outline" className="text-[10px] border-amber-500 text-amber-600">
                                                            No class
                                                        </Badge>
                                                    )}
                                                    {(critical.classification ?? []).map((category) => (
                                                        <Badge key={`${critical.value}-${category}`} variant="outline" className={`text-[10px] ${categoryBadgeClass[category]}`}>
                                                            {category}
                                                        </Badge>
                                                    ))}
                                                </>
                                            ) : (
                                                <Badge variant="outline" className="text-[10px] bg-destructive text-destructive-foreground border-destructive">
                                                    Critical
                                                </Badge>
                                            )}</>
                                            {assignedLabels.filter((label) => !label.predefined).map((label) => (
                                                <Badge
                                                    key={`${critical.value}-label-${label.id}`}
                                                    variant="outline"
                                                    className="text-[10px]"
                                                    style={{
                                                        backgroundColor: label.color ?? "#06b6d4",
                                                        borderColor: label.color ?? "#06b6d4",
                                                        color: "#ffffff",
                                                    }}
                                                >
                                                    {label.name}{label.deletedAt ? " (archived)" : ""}
                                                </Badge>
                                            ))}
                                        </div>
                                        {critical.explanation && (
                                            <div className="mt-2">
                                                <p className="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">Gold Standard Explanation</p>
                                                <p className="text-[11px] text-muted-foreground break-words">
                                                    {critical.explanation}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                ) : (
                    <button
                        type="button"
                        className="h-full w-full flex flex-col items-center justify-center gap-2 hover:bg-muted/50"
                        onClick={() => setIsSidebarOpen(true)}
                        title="Labeled BPMN Elements aufklappen"
                    >
                        <ChevronLeft className="h-4 w-4" />
                        <span className="text-xs font-medium [writing-mode:vertical-rl] rotate-180 tracking-wide">
                            Labeled BPMN Elements
                        </span>
                    </button>
                )}
            </aside>
        </div>
    );
}