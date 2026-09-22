"use client"

import {useState} from "react"
import {Button} from "@/components/ui/button"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog"
import {Label} from "@/components/ui/label"
import {Input} from "@/components/ui/input"
import {Textarea} from "@/components/ui/textarea"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select"
import {Switch} from "@/components/ui/switch"
import {Plus} from "lucide-react"
import {useRouter} from "next/navigation"
import {useToast} from "@/components/ui/toast"
import {toErrorMessage} from "@/lib/http-error"
import createCustomEndpoint from "@/actions/create-custom-endpoint"
import {CustomAnalysisResponseType} from "@/models/dto/CustomAnalysisEndpoint"
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider"

export default function CreateCustomEndpointButton() {
    const router = useRouter()
    const {refreshEndpoints} = useAnalysisEndpoint()
    const [open, setOpen] = useState(false)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [name, setName] = useState("")
    const [promptText, setPromptText] = useState("")
    const [responseType, setResponseType] = useState<CustomAnalysisResponseType>("BINARY")
    const [ragEnabled, setRagEnabled] = useState(false)
    const [ragMode, setRagMode] = useState("hybrid")
    const {showToast, showError} = useToast()

    function resetForm() {
        setName("")
        setPromptText("")
        setResponseType("BINARY")
        setRagEnabled(false)
        setRagMode("hybrid")
    }

    async function handleCreate() {
        if (!name.trim()) {
            showToast({title: "Please enter a name for the endpoint.", variant: "info"})
            return
        }
        if (!promptText.trim()) {
            showToast({title: "Please enter a prompt.", variant: "info"})
            return
        }

        setIsSubmitting(true)
        try {
            await createCustomEndpoint(name, promptText, responseType, ragEnabled, ragEnabled ? ragMode : undefined)
            router.refresh()
            refreshEndpoints()
            setOpen(false)
            resetForm()
        } catch (error) {
            console.error("There was an error creating the custom analysis endpoint:", error)
            showError("Failed to create the endpoint", toErrorMessage(error))
        } finally {
            setIsSubmitting(false)
        }
    }

    return <Dialog open={open} onOpenChange={setOpen}>
        <Button className="h-full" onClick={() => setOpen(true)}>
            <Plus/>
            <span className="pl-2 text-center">Upload Endpoint</span>
        </Button>
        <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
                <DialogTitle>Upload Custom Analysis Endpoint</DialogTitle>
                <DialogDescription>
                    Define a prompt, whether it should classify elements as critical/not-critical (binary) or into
                    GDPR processing classes (multiclass), and whether it uses RAG — same as the built-in endpoints.
                </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-2">
                <div className="space-y-2">
                    <Label htmlFor="custom-endpoint-name">Name</Label>
                    <Input id="custom-endpoint-name" placeholder="e.g. Strict GDPR Reviewer" value={name}
                           onChange={(e) => setName(e.target.value)}/>
                </div>
                <div className="space-y-2">
                    <Label htmlFor="custom-endpoint-response-type">Response shape</Label>
                    <Select value={responseType} onValueChange={(v) => setResponseType(v as CustomAnalysisResponseType)}>
                        <SelectTrigger id="custom-endpoint-response-type" className="w-full">
                            <SelectValue/>
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="BINARY">Binary (critical / not critical)</SelectItem>
                            <SelectItem value="MULTICLASS">Multiclass (GDPR processing classes)</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
                <div className="flex items-center space-x-2">
                    <Switch id="custom-endpoint-rag" checked={ragEnabled} onCheckedChange={setRagEnabled}/>
                    <Label htmlFor="custom-endpoint-rag" className="cursor-pointer">Use RAG</Label>
                </div>
                {ragEnabled && (
                    <div className="space-y-2">
                        <Label htmlFor="custom-endpoint-rag-mode">RAG search mode</Label>
                        <Select value={ragMode} onValueChange={setRagMode}>
                            <SelectTrigger id="custom-endpoint-rag-mode" className="w-full">
                                <SelectValue/>
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="naive">Naive</SelectItem>
                                <SelectItem value="local">Local</SelectItem>
                                <SelectItem value="global">Global</SelectItem>
                                <SelectItem value="hybrid">Hybrid</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                )}
                <div className="space-y-2">
                    <Label htmlFor="custom-endpoint-prompt">Prompt</Label>
                    <Textarea
                        id="custom-endpoint-prompt"
                        placeholder="Your task is to identify..."
                        rows={10}
                        value={promptText}
                        onChange={(e) => setPromptText(e.target.value)}
                    />
                </div>
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)} disabled={isSubmitting}>
                    Cancel
                </Button>
                <Button onClick={handleCreate} disabled={isSubmitting}>Create</Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
