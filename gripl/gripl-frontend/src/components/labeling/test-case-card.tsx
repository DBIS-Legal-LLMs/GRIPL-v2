"use client"

import {Card} from "@/components/ui/card";
import Link from "next/link";
import DeleteTestCaseButton from "@/components/labeling/delete-test-case-button";
import DownloadTestCaseButton from "@/components/labeling/download-test-case-button";
import {EvaluationDataMeta} from "@/models/dto/EvaluationData";
import {Skeleton} from "@/components/ui/skeleton";
import useLoadPreviewImage from "@/hooks/use-load-preview-image";

interface TestCaseCardProps {
    metadata: EvaluationDataMeta
}

export default function TestCaseCard({ metadata } : TestCaseCardProps) {

    const { previewImage, isLoading} = useLoadPreviewImage({
        testCaseId: metadata.id,
        imageClassName: "w-auto h-28",
    })

    // h-full: all tiles in a grid row get the same height, even with multi-line names
    return <div className="flex flex-row items-stretch h-full">
        <Card className="flex-1 relative hover:bg-card/40">
            <Link href={`/labeling/${metadata.id}`} className="block h-full">
                <div className="p-4">
                    <h2 className="text-lg font-bold mr-24">{metadata.name || "Test Case"} ({metadata.id})</h2>
                    <p className="text-xs text-muted-foreground mb-4 h-4">
                        {metadata.labelCount ?? 0} labeled
                        {(metadata.unclassifiedLabelCount ?? 0) > 0 && (
                            <span className="text-amber-600"> · {metadata.unclassifiedLabelCount} without class</span>
                        )}
                    </p>
                    { isLoading && !previewImage && <Skeleton className="h-28 w-full" /> }
                    { previewImage }
                </div>
            </Link>
            <div className="absolute right-4 top-4 flex space-x-2">
                <DownloadTestCaseButton testCaseId={metadata.id} testCaseName={metadata.name}/>
                <DeleteTestCaseButton testCaseId={metadata.id} testCaseName={metadata.name}/>
            </div>
        </Card>
    </div>
}