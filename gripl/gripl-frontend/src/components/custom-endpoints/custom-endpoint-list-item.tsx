"use client"

import {useState} from "react"
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible"
import {Card, CardHeader, CardTitle} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge"
import {Button} from "@/components/ui/button"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog"
import {ChevronDown, ChevronUp, Trash2} from "lucide-react"
import PromptMarkdown from "@/components/custom-endpoints/prompt-markdown"
import {useRouter} from "next/navigation"
import {useToast} from "@/components/ui/toast"
import {toErrorMessage} from "@/lib/http-error"
import deleteCustomEndpoint from "@/actions/delete-custom-endpoint"
import {CustomAnalysisEndpoint} from "@/models/dto/CustomAnalysisEndpoint"
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider"

interface CustomEndpointListItemProps {
    endpoint: CustomAnalysisEndpoint;
    className?: string;
}

export default function CustomEndpointListItem({endpoint, className}: CustomEndpointListItemProps) {
    const [isOpen, setIsOpen] = useState(false)
    const [showDeleteDialog, setShowDeleteDialog] = useState(false)
    const router = useRouter()
    const {refreshEndpoints} = useAnalysisEndpoint()
    const {showError} = useToast()

    function handleDelete() {
        deleteCustomEndpoint(endpoint.id).then(() => {
            router.refresh()
            refreshEndpoints()
            setShowDeleteDialog(false)
        }).catch(error => {
            console.error("Error deleting custom analysis endpoint:", error)
            showError("Failed to delete endpoint", toErrorMessage(error))
        })
    }

    return <Collapsible open={isOpen} onOpenChange={setIsOpen} className={className}>
        <div className="w-full flex flex-row gap-2">
            <CollapsibleTrigger className="w-full">
                <Card className="w-full">
                    <CardHeader className="flex-row justify-between items-center gap-2">
                        <CardTitle className="flex items-center gap-2">
                            {endpoint.name}
                            <Badge variant={endpoint.responseType === "MULTICLASS" ? "default" : "secondary"}>
                                {endpoint.responseType === "MULTICLASS" ? "Multiclass" : "Binary"}
                            </Badge>
                            <Badge variant="outline">
                                {endpoint.ragEnabled ? `RAG (${endpoint.ragMode ?? "hybrid"})` : "No RAG"}
                            </Badge>
                        </CardTitle>
                        {isOpen ? <ChevronDown/> : <ChevronUp/>}
                    </CardHeader>
                </Card>
            </CollapsibleTrigger>
            <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <Button variant="destructive" className="h-full" onClick={() => setShowDeleteDialog(true)}>
                    <Trash2/>
                </Button>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete {endpoint.name}</DialogTitle>
                        <DialogDescription>
                            Process models already analyzed with this endpoint keep their results — this only
                            removes it from the list of endpoints available for new analyses.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
                            Cancel
                        </Button>
                        <Button variant="destructive" onClick={handleDelete}>Delete</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
        <CollapsibleContent>
            <Card className="mt-2">
                <CardHeader>
                    <PromptMarkdown promptText={endpoint.promptText}/>
                </CardHeader>
            </Card>
        </CollapsibleContent>
    </Collapsible>
}
