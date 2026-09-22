"use client"

import {useState} from "react"
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible"
import {Card, CardHeader, CardTitle} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge"
import {ChevronDown, ChevronUp} from "lucide-react"
import PromptMarkdown from "@/components/custom-endpoints/prompt-markdown"
import {DefaultAnalysisPrompt} from "@/models/dto/DefaultAnalysisPrompt"

interface BuiltInPromptListItemProps {
    prompt: DefaultAnalysisPrompt;
}

export default function BuiltInPromptListItem({prompt}: BuiltInPromptListItemProps) {
    const [isOpen, setIsOpen] = useState(false)

    return <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <CollapsibleTrigger className="w-full">
            <Card className="w-full">
                <CardHeader className="flex-row justify-between items-center gap-2">
                    <CardTitle className="flex items-center gap-2">
                        {prompt.name}
                        <Badge variant={prompt.responseType === "MULTICLASS" ? "default" : "secondary"}>
                            {prompt.responseType === "MULTICLASS" ? "Multiclass" : "Binary"}
                        </Badge>
                        <Badge variant="outline">Built-in</Badge>
                    </CardTitle>
                    {isOpen ? <ChevronDown/> : <ChevronUp/>}
                </CardHeader>
            </Card>
        </CollapsibleTrigger>
        <CollapsibleContent>
            <Card className="mt-2">
                <CardHeader>
                    <PromptMarkdown promptText={prompt.promptText}/>
                </CardHeader>
            </Card>
        </CollapsibleContent>
    </Collapsible>
}
