import {AnalysisResponse} from "@/models/dto/AnalysisDto";
import {Brain, ChevronDown, ChevronRight} from "lucide-react";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from "@/components/ui/collapsible";
import {useState} from "react";

interface AnalysisResultCardProps {
    analysisResult: AnalysisResponse | null,
    selectedElementId?: string | null
}

// Turns a raw BPMN type name (e.g. "exclusiveGateway") into a readable label
// ("Exclusive Gateway"). Last-resort fallback when an element has neither a
// real name nor an LLM-generated contextual label.
export function humanizeType(type?: string): string {
    if (!type) return "Unnamed element"
    const spaced = type.replace(/([a-z])([A-Z])/g, "$1 $2")
    return spaced.charAt(0).toUpperCase() + spaced.slice(1)
}

export default function AnalysisResultCard({ analysisResult, selectedElementId }: AnalysisResultCardProps) {
    const [isOpen, setIsOpen] = useState<boolean>(true)

    return analysisResult && <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <Card className="container">
            <CollapsibleTrigger className="w-full">
                <CardHeader className="w-full flex flex-row items-center justify-between hover:bg-muted/50">
                    <CardTitle className="flex items-center gap-2 mr-4">
                        <Brain className="h-4 w-4" />
                        AI Model Reasoning
                    </CardTitle>
                    {isOpen ? (
                        <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    ) : (
                        <ChevronRight className="h-4 w-4 text-muted-foreground" />
                    )}
                </CardHeader>
            </CollapsibleTrigger>
            <CollapsibleContent>
                <CardContent className="max-h-[400px] overflow-y-auto">
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
                            return <tr key={index} className={`border-t ${isSelected ? "bg-destructive/50" : ""}`}>
                                <td className="font-medium text-sm mb-1 p-2">
                                    {element.name || <span className="italic text-muted-foreground">{humanizeType(element.type)}</span>}
                                </td>
                                <td className="text-sm p-2">{element.reason || "No reasoning provided"}</td>
                            </tr>
                        })}
                        </tbody>
                    </table>
                </CardContent>
            </CollapsibleContent>
        </Card>
    </Collapsible>
}