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
import {AlertTriangle, Settings} from "lucide-react"
import {useAnalysisEndpoint} from "@/components/providers/analysis-endpoint-provider"
import AnalysisSettingsFields from "@/components/process-analysis/analysis-settings-fields"
import {AnalysisSettings} from "@/hooks/use-analysis-settings"

interface AnalysisSettingsButtonProps {
    settings: AnalysisSettings;
}

export default function AnalysisSettingsButton({settings}: AnalysisSettingsButtonProps) {
    const [open, setOpen] = useState(false)
    const {backendEndpoint} = useAnalysisEndpoint()

    function handleSave() {
        settings.save()
        setOpen(false)
    }

    return <Dialog open={open} onOpenChange={setOpen}>
        <Button
            variant="outline"
            className={settings.isLoaded && !settings.isConfigured ? "border-amber-500 text-amber-600 dark:text-amber-400" : undefined}
            onClick={() => setOpen(true)}
        >
            {settings.isLoaded && !settings.isConfigured
                ? <AlertTriangle className="mr-2 h-4 w-4"/>
                : <Settings className="mr-2 h-4 w-4"/>}
            Analysis Settings
        </Button>
        <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
                <DialogTitle>Analysis Settings</DialogTitle>
                <DialogDescription>
                    Configure the LLM used for every analysis. These settings are applied whenever an
                    analysis is started — from this list or from a process model&apos;s own page.
                </DialogDescription>
            </DialogHeader>
            <AnalysisSettingsFields settings={settings} idPrefix="settings-"/>
            <div className="text-xs text-muted-foreground break-all">
                Selected endpoint: {backendEndpoint}
            </div>
            <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)}>
                    Close
                </Button>
                <Button onClick={handleSave}>
                    Save Settings
                </Button>
            </DialogFooter>
        </DialogContent>
    </Dialog>
}
