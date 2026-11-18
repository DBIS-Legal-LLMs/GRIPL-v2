import {Card, CardContent, CardHeader} from "@/components/ui/card";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {useEffect, useState} from "react";
import {ExpectedValues} from "@/models/dto/EvaluationData";
import {GDPR_CATEGORIES, GdprCategory} from "@/models/GdprCategory";
import {cn} from "@/lib/utils";
import {Label as LabelDto} from "@/models/dto/Label";
import {Badge} from "@/components/ui/badge";

export interface LabelingEditorLabelCardProps {
    className?: string;
    elementName: string;
    elementId: string;
    criticalActivities: ExpectedValues[];
    labels: LabelDto[];
    allowMulticlass: boolean;
    onLabelingChange: (
        elementId: string,
        classification: GdprCategory[] | null,
        labelIds: number[],
        explanation: string,
        isCritical: boolean
    ) => void;
}

export default function LabelingEditorLabelCard({ className, elementName, elementId, criticalActivities, labels, allowMulticlass, onLabelingChange }: LabelingEditorLabelCardProps) {

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
    const [selectedCategories, setSelectedCategories] = useState<GdprCategory[]>(
        getSelectedCategories(existing, labels, allowMulticlass)
    );
    const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>(
        getSelectedLabelIds(existing, labels, allowMulticlass)
    );
    const [explanation, setExplanation] = useState<string>(existing?.explanation ?? "");

    useEffect(() => {
        const found = criticalActivities.find(c => c.value === elementId);
        setSelectedCategories(getSelectedCategories(found, labels, allowMulticlass));
        setSelectedLabelIds(getSelectedLabelIds(found, labels, allowMulticlass));
        setExplanation(found?.explanation ?? "");
    }, [elementId, criticalActivities, labels, allowMulticlass]);

    // An element is critical as soon as it has a label, with or without classes
    const isCritical = existing !== undefined;

    function toggleCategory(cat: GdprCategory) {
        const next = selectedCategories.includes(cat)
            ? selectedCategories.filter(c => c !== cat)
            : [...selectedCategories, cat];
        const linkedLabel = labels.find((label) => label.predefined && categoryFromLabel(label) === cat);
        const nextLabelIds = linkedLabel
            ? next.includes(cat)
                ? Array.from(new Set([...selectedLabelIds, linkedLabel.id]))
                : selectedLabelIds.filter((id) => id !== linkedLabel.id)
            : selectedLabelIds;
        setSelectedCategories(next);
        setSelectedLabelIds(nextLabelIds);
        const remainsCritical = next.length > 0 || nextLabelIds.length > 0;
        onLabelingChange(elementId, remainsCritical ? next : null, nextLabelIds, explanation, remainsCritical);
    }

    function toggleBinaryCritical() {
        // Binary labels carry no class; existing classes are kept until the label is removed
        onLabelingChange(elementId, isCritical ? null : selectedCategories, isCritical ? [] : selectedLabelIds, explanation, !isCritical);
    }

    function toggleLabel(label: LabelDto) {
        const isSelected = selectedLabelIds.includes(label.id);
        const nextLabelIds = isSelected
            ? selectedLabelIds.filter((id) => id !== label.id)
            : [...selectedLabelIds, label.id];
        const category = label.predefined ? categoryFromLabel(label) : null;
        const nextCategories = category
            ? isSelected
                ? selectedCategories.filter((item) => item !== category)
                : Array.from(new Set([...selectedCategories, category]))
            : selectedCategories;
        const remainsCritical = allowMulticlass
            ? nextLabelIds.length > 0 || nextCategories.length > 0
            : isCritical || nextLabelIds.length > 0 || nextCategories.length > 0;

        setSelectedLabelIds(nextLabelIds);
        setSelectedCategories(nextCategories);
        onLabelingChange(
            elementId,
            remainsCritical ? nextCategories : null,
            nextLabelIds,
            explanation,
            remainsCritical
        );
    }

    function handleExplanationChange(value: string) {
        setExplanation(value);
        if (isCritical) {
            onLabelingChange(elementId, selectedCategories, selectedLabelIds, value, true);
        }
    }

    const groupedLabels = {
        predefined: labels.filter((label) => label.predefined || selectedLabelIds.includes(label.id) && label.deletedAt),
        custom: labels.filter((label) => !label.predefined),
    };

    return <Card className={cn("w-full bg-card/75", className)}>
        <CardHeader className="pb-2">
            <h2 className="text-base font-semibold leading-tight">{elementName}</h2>
            <p className={cn("text-xs font-medium", isCritical ? "text-destructive" : "text-muted-foreground")}>
                {isCritical ? "GDPR Critical" : "Not Critical"}
            </p>
            {allowMulticlass && isCritical && selectedCategories.length === 0 && selectedLabelIds.length === 0 && (
                <p className="text-xs font-medium text-amber-600">
                    No GDPR class or label assigned yet.
                </p>
            )}
        </CardHeader>
        <CardContent className="flex flex-col space-y-3">
            <>{allowMulticlass ? (
                <div>
                    <Label className="text-xs mb-2 block">GDPR Processing Classes</Label>
                    <p className="text-[11px] text-muted-foreground mb-2">
                        Multiclass mode: multiple classes allowed.
                    </p>
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
            ) : (
                <div>
                    <Label className="text-xs mb-2 block">Critical Label</Label>
                    <p className="text-[11px] text-muted-foreground mb-2">
                        Binary mode: mark this activity as GDPR critical or not critical.
                    </p>
                    <button
                        type="button"
                        onClick={toggleBinaryCritical}
                        className={cn(
                            "px-2 py-0.5 rounded-full border text-xs font-medium transition-colors",
                            isCritical
                                ? "bg-destructive border-destructive text-destructive-foreground"
                                : "bg-background border-border text-muted-foreground hover:border-primary hover:text-primary"
                        )}
                    >
                        {isCritical ? "Critical" : "Not Critical"}
                    </button>
                </div>
            )}</>
            {allowMulticlass && (
                <div>
                    <Label className="text-xs mb-2 block">Custom Labels</Label>
                    <p className="text-[11px] text-muted-foreground mb-2">
                        Predefined labels are selected above with their GDPR processing classes.
                    </p>
                    <div className="space-y-3">
                        <div>
                            {groupedLabels.custom.length === 0 ? (
                                <p className="text-[11px] text-muted-foreground">No custom labels available.</p>
                            ) : (
                                <div className="flex flex-wrap gap-1">
                                    {groupedLabels.custom.map((label) => {
                                        const selected = selectedLabelIds.includes(label.id);
                                        const archived = Boolean(label.deletedAt);
                                        return archived && !selected ? null : (
                                            <button
                                                key={label.id}
                                                type="button"
                                                title={archived
                                                    ? "This label has been archived and cannot be assigned to new elements."
                                                    : label.description}
                                                disabled={archived && !selected}
                                                aria-pressed={selected}
                                                onClick={() => toggleLabel(label)}
                                                className={cn(
                                                    "rounded-full border px-2 py-0.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60",
                                                    selected
                                                        ? "text-white"
                                                        : "bg-background text-muted-foreground hover:opacity-80"
                                                )}
                                                style={selected ? {
                                                    backgroundColor: label.color ?? "#06b6d4",
                                                    borderColor: label.color ?? "#06b6d4",
                                                } : {
                                                    borderColor: label.color ?? "#06b6d4",
                                                    color: label.color ?? "#06b6d4",
                                                }}
                                            >
                                                {label.name}
                                                {archived && (
                                                    <Badge 
    variant="outline" 
    className="ml-1 text-[9px] bg-amber-50 text-amber-800 border-amber-300 dark:bg-amber-950 dark:text-amber-200 dark:border-amber-700"
>
    Archived
</Badge>
                                                )}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            <div className="flex flex-col space-y-1">
                <Label htmlFor="explanation" className="text-xs">Notes </Label>
                <Textarea
                    id="explanation"
                    className="p-2 border rounded text-xs"
                    placeholder="Optional additional notes..."
                    rows={3}
                    value={explanation}
                    onChange={(e) => handleExplanationChange(e.target.value)}
                />
            </div>
        </CardContent>
    </Card>
}

function categoryFromLabel(label: LabelDto): GdprCategory | null {
    if (!label.predefined) return null;
    const name = label.name.toUpperCase();
    const category = GDPR_CATEGORIES.find((item) => item.value.toUpperCase() === name);
    return category?.value ?? null;
}

function getSelectedCategories(
    value: ExpectedValues | undefined,
    labels: LabelDto[],
    allowMulticlass: boolean
): GdprCategory[] {
    const assignedCategories = value?.classification ?? [];
    if (!allowMulticlass || !value) return assignedCategories;

    const assignedLabelIds = value.labelIds ?? value.labels?.map((label) => label.id) ?? [];
    const knownLabels = [...labels, ...(value.labels ?? [])];
    const legacyCategories = knownLabels
        .filter((label) => label.predefined && assignedLabelIds.includes(label.id))
        .map(categoryFromLabel)
        .filter((category): category is GdprCategory => category !== null);
    return Array.from(new Set([...assignedCategories, ...legacyCategories]));
}

function getSelectedLabelIds(
    value: ExpectedValues | undefined,
    labels: LabelDto[],
    allowMulticlass: boolean
): number[] {
    const assignedLabelIds = value?.labelIds ?? value?.labels?.map((label) => label.id) ?? [];
    if (!allowMulticlass) return assignedLabelIds;

    const knownLabels = [...labels, ...(value?.labels ?? [])];
    const categoryLabelIds = (value?.classification ?? [])
        .map((category) => knownLabels.find((label) =>
            label.predefined && categoryFromLabel(label) === category
        )?.id)
        .filter((id): id is number => id !== undefined);
    return Array.from(new Set([...assignedLabelIds, ...categoryLabelIds]));
}