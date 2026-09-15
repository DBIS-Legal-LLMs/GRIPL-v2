import ReactMarkdown from "react-markdown"

interface PromptMarkdownProps {
    promptText: string;
}

export default function PromptMarkdown({promptText}: PromptMarkdownProps) {
    return <div className="prose prose-sm dark:prose-invert max-w-none">
        <ReactMarkdown>{promptText}</ReactMarkdown>
    </div>
}
