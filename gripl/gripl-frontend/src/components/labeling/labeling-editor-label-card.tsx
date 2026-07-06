import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {useEffect, useState} from "react";
import {ExpectedValues} from "@/models/dto/EvaluationData";
import {GDPR_CATEGORIES, GdprCategory} from "@/models/GdprCategory";
import {cn} from "@/lib/utils";

export interface LabelingEditorLabelCardProps {
    className?: string;
    elementName: string;
    elementId: string;
    criticalActivities: ExpectedValues[];
    onLabelingChange: (elementId: string, classification: GdprCategory[], reason?: string) => void;
}

export default function LabelingEditorLabelCard({ className, elementName, elementId, criticalActivities, onLabelingChange }: LabelingEditorLabelCardProps) {

    const categoryChipClass: Record<GdprCategory, string> = {
        Collection: "bg-emerald-600 border-emerald-600 text-white",
        Storage: "bg-blue-600 border-blue-600 text-white",
        Usage: "bg-amber-500 border-amber-500 text-black",
        Transferal: "bg-violet-600 border-violet-600 text-white",
        Modification: "bg-sky-600 border-sky-600 text-white",
        Deletion: "bg-red-600 border-red-600 text-white",
        Access: "bg-teal-600 border-teal-600 text-white",
    }

    const existing = criticalActivities.find(c => c.value === elementId);
    const [selectedCategories, setSelectedCategories] = useState<GdprCategory[]>(existing?.classification ?? []);
    const [reason, setReason] = useState<string>(existing?.reason ?? "");

    useEffect(() => {
        const found = criticalActivities.find(c => c.value === elementId);
        setSelectedCategories(found?.classification ?? []);
        setReason(found?.reason ?? "");
    }, [elementId, criticalActivities]);

    function toggleCategory(cat: GdprCategory) {
        const next = selectedCategories.includes(cat)
            ? selectedCategories.filter(c => c !== cat)
            : [...selectedCategories, cat];
        setSelectedCategories(next);
        onLabelingChange(elementId, next, reason);
    }

    function handleReasonChange(value: string) {
        setReason(value);
        onLabelingChange(elementId, selectedCategories, value);
    }

    const isCritical = selectedCategories.length > 0;

    return <Card className={cn("w-full", className)}>
        <CardHeader className="pb-2">
            <h2 className="text-base font-semibold leading-tight">{elementName}</h2>
            <p className={cn("text-xs font-medium", isCritical ? "text-destructive" : "text-muted-foreground")}>
                {isCritical ? "GDPR Critical" : "Not Critical"}
            </p>
        </CardHeader>
        <CardContent className="flex flex-col space-y-3">
            <div>
                <Label className="text-xs mb-2 block">GDPR Processing Classes</Label>
                <div className="flex flex-wrap gap-1">
                    {GDPR_CATEGORIES.map(cat => {
                        const active = selectedCategories.includes(cat.value);
                        return (
                            <button
                                key={cat.value}
                                type="button"
                                title={cat.description}
                                onClick={() => toggleCategory(cat.value)}
                                className={cn(
                                    "px-2 py-0.5 rounded-full border text-xs font-medium transition-colors",
                                    active
                                        ? categoryChipClass[cat.value]
                                        : "bg-background border-border text-muted-foreground hover:border-primary hover:text-primary"
                                )}
                            >
                                {cat.label}
                            </button>
                        );
                    })}
                </div>
            </div>
            <div className="flex flex-col space-y-1">
                <Label htmlFor="reason" className="text-xs">Notes (optional)</Label>
                <Textarea
                    id="reason"
                    className="p-2 border rounded text-xs"
                    placeholder="Additional notes..."
                    rows={3}
                    value={reason}
                    onChange={(e) => handleReasonChange(e.target.value)}
                />
            </div>
        </CardContent>
    </Card>
}