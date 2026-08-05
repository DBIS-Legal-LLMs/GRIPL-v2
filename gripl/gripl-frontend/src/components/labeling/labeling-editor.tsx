"use client"

import BpmnEditor from "@/components/bpmn-editor";
import {EvaluationData, ExpectedValues} from "@/models/dto/EvaluationData";
import {BpmnToolCard} from "@/models/BpmnToolCard";
import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {ChevronLeft, ChevronRight, Save, Trash2} from "lucide-react";
import {useEffect, useState} from "react";
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

interface LabelingEditorProps {
    className?: string;
    evaluationData: EvaluationData;
}

export default function LabelingEditor({ className, evaluationData }: LabelingEditorProps) {
    const [diagram, setDiagram] = useState<string>(evaluationData.bpmnXml)
    const [isLabelMode, setIsLabelMode] = useState<boolean>(false);
    const [criticalActivities, setCriticalActivities] = useState<ExpectedValues[]>(
        (evaluationData.expectedValues || []).map((value) => ({
            ...value,
            classification: normalizeFrontendGdprCategories((value.classification as unknown as string[]) ?? []),
        }))
    );
    const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
    const [selectedElement, setSelectedElement] = useState<any | null>(null);
    const [isSaveLoading, setIsSaveLoading] = useState(false);
    const [elementNames, setElementNames] = useState<Record<string, string>>({});
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const {isMulticlass, backendEndpoint} = useAnalysisEndpoint();

    useEffect(() => {
        if (isMulticlass) {
            return;
        }

        setCriticalActivities((prev) => {
            let changed = false;
            const normalized = prev.map((value) => {
                if ((value.classification?.length ?? 0) <= 1) {
                    return value;
                }
                changed = true;
                return {
                    ...value,
                    classification: value.classification.slice(0, 1),
                };
            });

            if (changed) {
                setHasUnsavedChanges(true);
                return normalized;
            }

            return prev;
        });
    }, [isMulticlass]);

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
            ...value,
            classification: (value.classification ?? []).map(toBackendGdprProcessingClass),
        }));
        const expectedValuesBlob = new Blob(
            [JSON.stringify(expectedValuesPayload)],
            { type: 'application/json' }
        );
        formData.append('expectedValues', expectedValuesBlob, 'expectedValues.json');

        fetch(`/api/dataset/testcase/${evaluationData.id}`, {
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

    function handleElementLabelingChange(elementId: string, classification: GdprCategory[], reason?: string) {
        if (classification.length > 0) {
            if (!criticalActivities.some(critical => critical.value === elementId)) {
                setCriticalActivities([...criticalActivities, { value: elementId, classification, reason }]);
            } else {
                setCriticalActivities(criticalActivities.map(critical =>
                    critical.value === elementId ? { ...critical, classification, reason } : critical
                ));
            }
        } else {
            setCriticalActivities(criticalActivities.filter(critical => critical.value !== elementId));
        }
        setHasUnsavedChanges(true);
    }

    function handleEditorEvent(type: BpmnEditorEvent, event: any) {
        if (type === BpmnEditorEvent.SelectionChanged) {
            const element = event.newSelection.length === 1 ? event.newSelection[0] : null;
            setSelectedElement(element);

            if (element?.id) {
                const nextName = element.businessObject?.name || "No Name";
                setElementNames((prev) => ({
                    ...prev,
                    [element.id]: nextName,
                }));
            }
        }

        if (type === BpmnEditorEvent.ElementChanged && event?.element?.id) {
            const nextName = event.element.businessObject?.name || "No Name";
            setElementNames((prev) => ({
                ...prev,
                [event.element.id]: nextName,
            }));
        }
    }

    function removeLabel(elementId: string) {
        setCriticalActivities(criticalActivities.filter((critical) => critical.value !== elementId));
        setHasUnsavedChanges(true);
    }

    const selectedElementName = selectedElement?.businessObject?.name || "No Name";
    const highlightedActivityCategoryMap = criticalActivities.reduce((acc, critical) => {
        acc[critical.value] = critical.classification || [];
        return acc;
    }, {} as Record<string, GdprCategory[]>);

    const cards: BpmnToolCard[] = [
        {
            position: "top-right",
            content: <Card>
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
                </CardContent>
            </Card>
        },
        {
            position: "top-right",
            content: selectedElement && isLabelMode ? <LabelingEditorLabelCard
                className="w-[260px]"
                elementName={selectedElementName}
                elementId={selectedElement.id}
                criticalActivities={criticalActivities}
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
                            <p className="text-xs text-muted-foreground">{criticalActivities.length} labeled</p>
                        </div>
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
                                                    {isMultiCategory && (
                                                        <Badge variant="outline" className="text-[10px] bg-fuchsia-600 text-white border-fuchsia-600">
                                                            Multiple
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
                                        </div>
                                        {critical.reason && (
                                            <p className="mt-2 text-[11px] text-muted-foreground break-words">
                                                {critical.reason}
                                            </p>
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