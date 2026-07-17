"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Check, ChevronDown, ChevronRight, Copy } from "lucide-react"
import { useState } from "react"

interface RagPromptContextCardProps {
    contexts: string[]
}

/**
 * Shows the exact list of context strings that were fed to Ragas
 * (also the same lines injected into the LLM prompt's "Retrieved GDPR
 * Knowledge" section). One-click copy of the whole list so it can be
 * pasted into the Ragas Swagger endpoint or compared offline.
 */
export default function RagPromptContextCard({ contexts }: RagPromptContextCardProps) {
    const [copied, setCopied] = useState(false)
    const [open, setOpen] = useState(false)

    if (!contexts || contexts.length === 0) return null

    const handleCopy = async () => {
        const payload = JSON.stringify(contexts, null, 2)
        try {
            await navigator.clipboard.writeText(payload)
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        } catch {
            // navigator.clipboard can fail on non-secure contexts; fall back to a textarea trick.
            const ta = document.createElement("textarea")
            ta.value = payload
            document.body.appendChild(ta)
            ta.select()
            try { document.execCommand("copy") } finally { document.body.removeChild(ta) }
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        }
    }

    return (
        <Card>
            <Collapsible open={open} onOpenChange={setOpen}>
                <CardHeader className="flex flex-row items-center justify-between gap-2 py-3">
                    <CollapsibleTrigger className="flex items-center gap-2 text-left flex-1">
                        {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                        <CardTitle className="text-base font-semibold">
                            Contexts sent to Ragas ({contexts.length})
                        </CardTitle>
                    </CollapsibleTrigger>
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={(e) => { e.stopPropagation(); handleCopy() }}
                        className="gap-2"
                    >
                        {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                        {copied ? "Copied" : "Copy JSON"}
                    </Button>
                </CardHeader>
                <CollapsibleContent>
                    <CardContent className="pt-0">
                        <ol className="space-y-1 text-xs font-mono list-decimal list-inside text-muted-foreground">
                            {contexts.map((c, i) => (
                                <li key={i} className="break-words whitespace-pre-wrap">
                                    <span className="text-foreground">{c}</span>
                                </li>
                            ))}
                        </ol>
                    </CardContent>
                </CollapsibleContent>
            </Collapsible>
        </Card>
    )
}
