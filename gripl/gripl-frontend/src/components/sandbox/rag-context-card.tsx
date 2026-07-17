"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { RagElementContext } from "@/models/dto/AnalysisDto";
import { Badge } from "@/components/ui/badge";
import { BookOpen, FileText, ChevronDown, ChevronRight, Network } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";

const KnowledgeGraph = dynamic(() => import("@/components/sandbox/knowledge-graph"), { ssr: false });
const PdfViewerModal = dynamic(() => import("@/components/sandbox/pdf-viewer-modal"), { ssr: false });

interface RagContextCardProps {
    ragContext: Record<string, RagElementContext>;
    selectedElementId?: string | null;
    elementNames?: Record<string, string>;
}

interface ViewerState {
    documentName: string;
    exactText: string;
}

export default function RagContextCard({ ragContext, selectedElementId, elementNames }: RagContextCardProps) {
    const entries = Object.entries(ragContext);
    const [viewerState, setViewerState] = useState<ViewerState | null>(null);
    const [isOpen, setIsOpen] = useState(true);

    // Active tab: "all" or an elementId
    const [activeTab, setActiveTab] = useState<string>("all");

    if (entries.length === 0) {
        return <p className="text-sm text-muted-foreground p-4">No RAG context was retrieved for any activity.</p>;
    }

    // Determine which entries to show in the graph
    const visibleEntries =
        activeTab === "all" ? entries : entries.filter(([id]) => id === activeTab);

    // Merge entities and relationships for visible entries (deduplicated)
    const mergedEntities = visibleEntries.flatMap(([, ctx]) => ctx.entities);
    const mergedRelationships = visibleEntries.flatMap(([, ctx]) => ctx.relationships);
    const mergedDocuments = visibleEntries.flatMap(([, ctx]) => ctx.documents);

    return (
        <>
            <Collapsible open={isOpen} onOpenChange={setIsOpen}>
            <CollapsibleTrigger className="w-full flex items-center justify-between px-1 py-1.5 rounded-md hover:bg-muted/50 transition-colors">
                <span className="flex items-center gap-2 text-sm font-semibold">
                    <Network className="h-4 w-4" />
                    Knowledge Retrieved
                </span>
                {isOpen ? (
                    <ChevronDown className="h-4 w-4 text-muted-foreground" />
                ) : (
                    <ChevronRight className="h-4 w-4 text-muted-foreground" />
                )}
            </CollapsibleTrigger>
            <CollapsibleContent>
            <div className="flex flex-col gap-3">
                {/* Activity tabs */}
                <div className="flex flex-wrap gap-1.5 px-1 pt-1">
                    <button
                        onClick={() => setActiveTab("all")}
                        className={`px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors ${
                            activeTab === "all"
                                ? "bg-primary text-primary-foreground border-primary"
                                : "border-border text-muted-foreground hover:text-foreground hover:border-foreground/40"
                        }`}
                    >
                        All activities
                    </button>
                    {entries.map(([elementId, ctx]) => {
                        const isActive = activeTab === elementId;
                        const isSelected = elementId === selectedElementId;
                        const total = ctx.entities.length + ctx.relationships.length;
                        return (
                            <button
                                key={elementId}
                                onClick={() => setActiveTab(elementId)}
                                className={`px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors ${
                                    isActive
                                        ? "bg-primary text-primary-foreground border-primary"
                                        : isSelected
                                        ? "border-primary/60 text-primary hover:border-primary"
                                        : "border-border text-muted-foreground hover:text-foreground hover:border-foreground/40"
                                }`}
                            >
                                {ctx.activityName || elementNames?.[elementId] || elementId}
                                {total > 0 && (
                                    <span className={`ml-1.5 ${isActive ? "opacity-80" : "opacity-50"}`}>
                                        {total}
                                    </span>
                                )}
                            </button>
                        );
                    })}
                </div>

                {/* Main content: graph + documents */}
                <div className="flex gap-3" style={{ height: "420px" }}>
                    {/* Graph panel */}
                    <div className="flex-1 min-w-0 rounded-lg border border-border bg-muted/20 overflow-hidden relative" style={{ height: "420px" }}>
                        {mergedEntities.length === 0 ? (
                            <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
                                No entities retrieved for this selection.
                            </div>
                        ) : (
                            <KnowledgeGraph
                                entities={mergedEntities}
                                relationships={mergedRelationships}
                            />
                        )}
                        {/* Stats overlay */}
                        {mergedEntities.length > 0 && (
                            <div className="absolute top-2 left-2 flex gap-1.5">
                                <Badge variant="secondary" className="text-xs">
                                    {mergedEntities.length} entities
                                </Badge>
                                <Badge variant="secondary" className="text-xs">
                                    {mergedRelationships.length} relationships
                                </Badge>
                            </div>
                        )}
                    </div>

                    {/* Documents panel */}
                    <div className="w-56 flex-shrink-0 flex flex-col gap-1.5 overflow-y-auto" style={{ maxHeight: "420px" }}>
                        <div className="flex items-center gap-1 text-xs font-semibold text-muted-foreground px-0.5">
                            <BookOpen className="h-3 w-3" />
                            Legal Excerpts
                            {mergedDocuments.length > 0 && (
                                <span className="ml-auto opacity-60">{mergedDocuments.length}</span>
                            )}
                        </div>
                        {mergedDocuments.length === 0 ? (
                            <p className="text-xs text-muted-foreground italic px-0.5">No documents retrieved.</p>
                        ) : (
                            mergedDocuments.map((doc, i) => (
                                <div
                                    key={i}
                                    className="rounded-md border border-border bg-background p-2 space-y-1"
                                >
                                    <blockquote className="text-xs text-muted-foreground border-l-2 border-primary/30 pl-2 italic line-clamp-4">
                                        {doc.preview}
                                    </blockquote>
                                    {doc.sourceDocument && (
                                        <button
                                            className="text-xs text-primary/70 font-medium underline-offset-2 hover:underline cursor-pointer text-left flex items-center gap-1"
                                            onClick={() =>
                                                setViewerState({
                                                    documentName: doc.sourceDocument!,
                                                    exactText: doc.preview,
                                                })
                                            }
                                        >
                                            <FileText className="h-3 w-3 flex-shrink-0" />
                                            <span className="truncate">
                                                {doc.sourceDocument.replace(/[_-]/g, " ")}
                                            </span>
                                        </button>
                                    )}
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
            </CollapsibleContent>
            </Collapsible>

            {viewerState && (
                <PdfViewerModal
                    open={true}
                    onClose={() => setViewerState(null)}
                    documentName={viewerState.documentName}
                    exactText={viewerState.exactText}
                />
            )}
        </>
    );
}
