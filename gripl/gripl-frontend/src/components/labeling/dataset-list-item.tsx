"use client"

import {useState} from "react";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {Card, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {ChevronDown, ChevronUp} from "lucide-react";
import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import TestCaseCard from "@/components/labeling/test-case-card";
import CreateTestCaseButton from "@/components/labeling/create-test-case-button";
import DeleteDatasetButton from "@/components/labeling/delete-dataset-button";
import ExportDatasetsButton from "@/components/labeling/export-datasets-button";
import {Dataset} from "@/models/dto/Dataset";

interface DatasetListProps {
    dataset: Dataset;
    evaluationMetadata: EvaluationDataMeta[];
    className?: string;
}

export default function DatasetListItem({ dataset, evaluationMetadata, className }: DatasetListProps) {
    const [isOpen, setIsOpen] = useState<boolean>(false);

    return <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        {/* No fixed height: the row grows with multi-line descriptions and the buttons stretch along */}
        <div className="w-full flex flex-row items-stretch min-h-20 gap-2">
            <CollapsibleTrigger className="w-full text-left">
                <Card className="w-full h-full">
                    <CardHeader className="h-full flex-row items-center justify-between gap-4 space-y-0 py-4">
                        <div className="flex flex-col space-y-1.5 min-w-0">
                            <CardTitle>
                                {dataset.name} ({dataset.id})
                                <span className="ml-2 text-sm font-normal text-muted-foreground">
                                    · {evaluationMetadata.length} model{evaluationMetadata.length === 1 ? "" : "s"}
                                </span>
                            </CardTitle>
                            { dataset.description && <CardDescription>{dataset.description}</CardDescription> }
                        </div>
                        <>{ isOpen ? <ChevronDown className="shrink-0" /> : <ChevronUp className="shrink-0" /> }</>
                    </CardHeader>
                </Card>
            </CollapsibleTrigger>
            <CreateTestCaseButton dataset={dataset} />
            <ExportDatasetsButton dataset={dataset} />
            <DeleteDatasetButton dataset={dataset} testCaseCount={evaluationMetadata.length} />
        </div>
        <CollapsibleContent>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4 overflow-y-auto gap-4 pt-4">
                {evaluationMetadata.sort((a, b) => a.id - b.id).map(meta =>
                    <TestCaseCard metadata={meta} key={meta.id}/>
                )}
            </div>
        </CollapsibleContent>
    </Collapsible>
}