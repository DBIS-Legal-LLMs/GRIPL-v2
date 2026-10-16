import BuiltInPromptListItem from "@/components/custom-endpoints/built-in-prompt-list-item"
import {DefaultAnalysisPrompt} from "@/models/dto/DefaultAnalysisPrompt"

interface BuiltInPromptListProps {
    prompts: DefaultAnalysisPrompt[];
}

export default function BuiltInPromptList({prompts}: BuiltInPromptListProps) {
    return <div className="flex flex-col gap-4">
        {prompts.map(prompt => (
            <BuiltInPromptListItem prompt={prompt} key={prompt.name}/>
        ))}
    </div>
}
