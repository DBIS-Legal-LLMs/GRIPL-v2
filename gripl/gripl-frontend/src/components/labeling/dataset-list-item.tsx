"use client"

import {useState} from "react";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {Card, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {ChevronDown, ChevronUp} from "lucide-react";
import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import TestCaseCard from "@/components/labeling/test-case-card";
import CreateTestCaseButton from "@/components/labeling/create-test-case-button";
import DeleteDatasetButton from "@/components/labeling/delete-dataset-button";
import EditDatasetButton from "@/components/labeling/edit-dataset-button";
import DatasetDescription from "@/components/labeling/dataset-description";
import {Dataset} from "@/models/dto/Dataset";

interface DatasetListProps {
    dataset: Dataset;
    evaluationMetadata: EvaluationDataMeta[];
    className?: string;
}

export default function DatasetListItem({ dataset, evaluationMetadata, className }: DatasetListProps) {
    const [isOpen, setIsOpen] = useState<boolean>(false);

    return <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <div className="w-full flex flex-row h-20 mb-4 gap-2">
            <CollapsibleTrigger className="w-full">
                <Card className="w-full">
                    <CardHeader className="flex-row justify-between items-center gap-4">
                        <CardTitle>{dataset.name}</CardTitle>
                        { dataset.description && <DatasetDescription description={dataset.description} /> }
                        <>{ isOpen ? <ChevronDown /> : <ChevronUp /> }</>
                    </CardHeader>
                </Card>
            </CollapsibleTrigger>
            <CreateTestCaseButton dataset={dataset} />
            <EditDatasetButton dataset={dataset} />
            <DeleteDatasetButton dataset={dataset} />
        </div>
        <CollapsibleContent>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4 overflow-y-auto gap-4 pb-4">
                {evaluationMetadata.sort((a, b) => a.id - b.id).map(meta =>
                    <TestCaseCard metadata={meta} key={meta.id}/>
                )}
            </div>
        </CollapsibleContent>
    </Collapsible>
}