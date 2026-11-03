"use client"

import dynamic from "next/dynamic";
import BpmnEditor from "@/components/bpmn-editor";
import {useMemo, useState} from "react";
import {BpmnToolCard} from "@/models/BpmnToolCard";
import ProcessModelActions from "@/components/process-analysis/process-model-actions";
import RenameProcessModelButton from "@/components/process-analysis/rename-process-model-button";
import {getAnalysisElements} from "@/models/dto/AnalysisDto";
import AnalysisResultCard, {humanizeType} from "@/components/sandbox/analysis-result-card";
import RagContextCard from "@/components/sandbox/rag-context-card";
import {BpmnEditorEvent} from "@/models/BpmnEditorEvent";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "@/components/ui/tabs";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {AlertTriangle, Brain, BookOpen, Paperclip, FileText, ChevronDown, ChevronRight} from "lucide-react";
import {useProcessModelPolling} from "@/hooks/use-process-model-polling";
import {ProcessModelDetail} from "@/models/dto/ProcessModel";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {useToast} from "@/components/ui/toast";

const PdfViewerModal = dynamic(() => import("@/components/sandbox/pdf-viewer-modal"), { ssr: false });

interface PdfViewerState {
  documentName: string;
  exactText: string;
}

interface ProcessModelDetailViewProps {
  initialModel: ProcessModelDetail;
}

export default function ProcessModelDetailView({initialModel}: ProcessModelDetailViewProps) {
  const {model, setModel} = useProcessModelPolling(initialModel)
  const [diagram, setDiagram] = useState<string>(initialModel.bpmnXml)
  // Last XML persisted to the backend — compared against `diagram` rather than
  // a boolean flag, so edits made while a save is in flight stay "unsaved".
  const [savedDiagram, setSavedDiagram] = useState<string>(initialModel.bpmnXml)
  const [isSaving, setIsSaving] = useState<boolean>(false)
  const hasUnsavedChanges = diagram !== savedDiagram
  const {showToast, showError} = useToast()
  const analysisResult = model.analysisResult ?? null
  const [selectedElementId, setSelectedElementId] = useState<string | null>(null)
  const analysisElements = getAnalysisElements(analysisResult)
  const [pdfViewer, setPdfViewer] = useState<PdfViewerState | null>(null)
  const [isPanelOpen, setIsPanelOpen] = useState<boolean>(true)

  function onEvent(type: BpmnEditorEvent, event: any) {
    if (type === BpmnEditorEvent.SelectionChanged) {
      setSelectedElementId(event.newSelection?.length === 1 ? event.newSelection[0].id : null)
    }
  }

  async function handleSave(): Promise<boolean> {
    const xml = diagram
    setIsSaving(true)
    try {
      const formData = new FormData()
      formData.append("bpmnFile", new Blob([xml], {type: "application/xml"}), "diagram.bpmn")
      const response = await authenticatedFetch(`/api/process-models/${model.id}`, {
        method: "PUT",
        headers: {
          Accept: "application/json",
        },
        body: formData,
      })
      if (!response.ok) {
        throw new Error(await extractErrorDetails(response))
      }
      setModel(await response.json() as ProcessModelDetail)
      setSavedDiagram(xml)
      showToast({title: "Model saved successfully", variant: "success"})
      return true
    } catch (error) {
      console.error("Error while saving process model:", error)
      showError("Failed to save the model", toErrorMessage(error))
      return false
    } finally {
      setIsSaving(false)
    }
  }

  function handleAnalysisStarted() {
    setModel({...model, status: "QUEUED"})
  }

  const hasRagContext = analysisResult && "ragContext" in analysisResult && analysisResult.ragContext && Object.keys(analysisResult.ragContext).length > 0;

  const highlightedActivityIds = analysisElements.map(e => e.id);
  const highlightedActivityCategories = Object.fromEntries(
      analysisElements
          .filter(e => e.classification.length > 0)
          .map(e => [e.id, e.classification])
  );

  const elementNames = useMemo(
      () => {
          const namedElements = analysisResult
              ? "criticalElements" in analysisResult
                  ? analysisResult.criticalElements
                  : analysisResult.classifiedElements
              : [];
          return Object.fromEntries(
              namedElements
                  .filter((e): e is typeof e & { name: string } => Boolean(e.name))
                  .map(e => [e.id, e.name])
          );
      },
      [analysisResult]
  );

  // Unsaved edits count too: the result no longer matches the diagram on screen.
  const isAnalysisOutdated = Boolean(analysisResult) && (Boolean(model.analysisOutdated) || hasUnsavedChanges)

  const outdatedWarning = isAnalysisOutdated ? (
      <div className="rounded-md bg-background">
          <div className="flex items-center gap-2 rounded-md border border-amber-500/50 bg-amber-500/10 px-3 py-2 text-sm text-amber-700 dark:text-amber-400">
              <AlertTriangle className="h-4 w-4 shrink-0"/>
              <span>
                  Outdated analysis: the process model has changed since these results were generated.
                  Click &quot;Analyze for GDPR&quot; to update them.
              </span>
          </div>
      </div>
  ) : null;

  const resultPanel = analysisResult ? (
      hasRagContext && "ragContext" in analysisResult ? (
          <Collapsible open={isPanelOpen} onOpenChange={setIsPanelOpen}>
          <Card className="container">
              <CollapsibleTrigger className="w-full">
                  <CardHeader className="w-full flex flex-row items-center justify-between hover:bg-muted/50 py-2">
                      <CardTitle className="flex items-center gap-2 mr-4 text-sm">
                          <Brain className="h-4 w-4" />
                          Analysis Results
                      </CardTitle>
                      {isPanelOpen ? (
                          <ChevronDown className="h-4 w-4 text-muted-foreground" />
                      ) : (
                          <ChevronRight className="h-4 w-4 text-muted-foreground" />
                      )}
                  </CardHeader>
              </CollapsibleTrigger>
              <CollapsibleContent>
              <Tabs defaultValue="reasoning">
                  <div className="px-4 pt-3">
                      <TabsList>
                          <TabsTrigger value="reasoning" className="gap-1.5">
                              <Brain className="h-3.5 w-3.5" />
                              AI Reasoning
                          </TabsTrigger>
                          <TabsTrigger value="rag-context" className="gap-1.5">
                              <BookOpen className="h-3.5 w-3.5" />
                              Retrieved Knowledge
                          </TabsTrigger>
                      </TabsList>
                  </div>
                  <CardContent className="pt-2 max-h-[640px] overflow-y-auto">
                      <TabsContent value="reasoning">
                          {"criticalElements" in analysisResult ? (
                              <table className="w-full">
                                  <thead>
                                  <tr className="bg-muted">
                                      <th className="text-left text-sm font-semibold p-2">Activity</th>
                                      <th className="text-left text-sm font-semibold p-2">Reasoning</th>
                                  </tr>
                                  </thead>
                                  <tbody>
                                  {analysisResult.criticalElements.map((element, index) => {
                                      const isSelected = element.id === selectedElementId
                                      const hasRefs = element.references && element.references.length > 0
                                      return <tr key={index} className={`border-t align-top ${isSelected ? "bg-destructive/50" : ""}`}>
                                          <td className="font-medium text-sm p-2 whitespace-nowrap">
                                              {element.name || <span className="italic text-muted-foreground">{humanizeType(element.type)}</span>}
                                          </td>
                                          <td className="text-sm p-2">
                                              <p>{element.reason || "No reasoning provided"}</p>
                                              {hasRefs && (
                                                  <details className="mt-2">
                                                      <summary className="cursor-pointer text-xs font-semibold text-primary/80 hover:text-primary select-none w-fit">
                                                          <Paperclip className="inline h-3 w-3 mr-1" />References ({element.references!.length})
                                                      </summary>
                                                      <div className="mt-1.5 space-y-2 pl-1">
                                                          {element.references!.map((ref, ri) => (
                                                              <div key={ri} className="border-l-2 border-primary/30 pl-2 space-y-0.5">
                                                                  <blockquote className="text-xs text-muted-foreground italic">
                                                                      &ldquo;{ref.exactText}&rdquo;
                                                                  </blockquote>
                                                                  {ref.sourceDocument && (
                                                                      <button
                                                                          className="text-xs text-primary/70 font-medium underline-offset-2 hover:underline cursor-pointer text-left"
                                                                          onClick={() => setPdfViewer({
                                                                              documentName: ref.sourceDocument!,
                                                                              exactText: ref.exactText,
                                                                          })}
                                                                      >
                                                                          <FileText className="inline h-3 w-3 mr-1" />{ref.sourceDocument.replace(/[_-]/g, " ")}
                                                                      </button>
                                                                  )}
                                                              </div>
                                                          ))}
                                                      </div>
                                                  </details>
                                              )}
                                          </td>
                                      </tr>
                                  })}
                                  </tbody>
                              </table>
                          ) : (
                              // Multiclass elements have no per-element citations (no `references`
                              // field in that schema) — just the classification badges + reasoning.
                              <AnalysisResultCard analysisResult={analysisResult} selectedElementId={selectedElementId}/>
                          )}
                      </TabsContent>
                      <TabsContent value="rag-context">
                          <RagContextCard
                              ragContext={"ragContext" in analysisResult ? analysisResult.ragContext! : {}}
                              selectedElementId={selectedElementId}
                              elementNames={elementNames}
                          />
                      </TabsContent>
                  </CardContent>
              </Tabs>
              </CollapsibleContent>
          </Card>
          </Collapsible>
      ) : (
          <AnalysisResultCard analysisResult={analysisResult} selectedElementId={selectedElementId} />
      )
  ) : null;

  const bottomPanel = resultPanel ? (
      <div className="flex flex-col gap-2">
          {outdatedWarning}
          {resultPanel}
      </div>
  ) : null;

  const editorToolCards: BpmnToolCard[] = [
    {
      position: "top-right",
      content: <ProcessModelActions
          model={model}
          hasUnsavedChanges={hasUnsavedChanges}
          isSaving={isSaving}
          onSave={handleSave}
          onAnalysisStarted={handleAnalysisStarted}
      />
    } as BpmnToolCard,
    ...(bottomPanel ? [{
      position: "bottom-center" as const,
      content: bottomPanel
    }] : [])
  ]

  return (
      <main className="flex flex-col justify-center items-center h-full w-full">
        <div className="w-full h-full">
          <BpmnEditor
              title={model.name}
              titleAddon={<RenameProcessModelButton
                  id={model.id}
                  name={model.name}
                  onRenamed={name => setModel(current => ({...current, name}))}
              />}
              bpmnXml={diagram}
              highlightedActivityIds={highlightedActivityIds}
              highlightedActivityCategories={highlightedActivityCategories}
              onDiagramChanged={setDiagram}
              exportFileName={model.name}
              askForExportFileName
              cards={editorToolCards}
              onEvent={onEvent}
          />
        </div>

        {pdfViewer && (
            <PdfViewerModal
                open={true}
                onClose={() => setPdfViewer(null)}
                documentName={pdfViewer.documentName}
                exactText={pdfViewer.exactText}
            />
        )}
      </main>
  )
}
