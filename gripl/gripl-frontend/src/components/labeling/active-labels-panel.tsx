"use client"

import {useCallback, useEffect, useState, type FormEvent} from "react";
import {Pencil, Plus, Save, Trash2, X} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label as FormLabel} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {useAuth} from "@/context/auth-context";
import {authenticatedFetch} from "@/lib/authenticated-fetch";
import {extractErrorDetails, toErrorMessage} from "@/lib/http-error";
import {Label} from "@/models/dto/Label";
import {ConfirmationDialog} from "@/components/ui/confirmation-dialog";
import {useToast} from "@/components/ui/toast";
import {GDPR_CATEGORIES, GdprCategory} from "@/models/GdprCategory";

type LabelGroup = "predefined" | "custom";

interface ActiveLabelsPanelProps {
    onLabelsChanged?: (labels: Label[]) => void;
}

export default function ActiveLabelsPanel({onLabelsChanged}: ActiveLabelsPanelProps) {
    const {griplRole, isLoading: isAuthLoading, token} = useAuth();
    const {showToast} = useToast();
    const DEFAULT_CUSTOM_LABEL_COLOR = "#06b6d4";

    const categoryColor: Record<GdprCategory, string> = {
        Collection: "#059669",
        Storage: "#2563eb",
        Usage: "#f59e0b",
        Transferal: "#7c3aed",
        Modification: "#0284c7",
        Deletion: "#dc2626",
        Access: "#0d9488",
    };

    function categoryFromLabel(label: Label): GdprCategory | null {
        if (!label.predefined) return null;
        const category = GDPR_CATEGORIES.find((item) => item.value.toUpperCase() === label.name.toUpperCase());
        return category?.value ?? null;
    }

    function getLabelColor(label: Label): string {
        const category = categoryFromLabel(label);
        return category ? categoryColor[category] : (label.color ?? DEFAULT_CUSTOM_LABEL_COLOR);
    }

    const [labels, setLabels] = useState<Label[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [color, setColor] = useState(DEFAULT_CUSTOM_LABEL_COLOR);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editName, setEditName] = useState("");
    const [editDescription, setEditDescription] = useState("");
    const [editColor, setEditColor] = useState(DEFAULT_CUSTOM_LABEL_COLOR);
    const [archiveTarget, setArchiveTarget] = useState<Label | null>(null);
    const [formError, setFormError] = useState("");
    const canCreate = griplRole === "researcher" || griplRole === "admin";
    const isAdmin = griplRole === "admin";
    const canEditCustomLabels = isAdmin;

    const loadLabels = useCallback(async () => {
        setIsLoading(true);
        setFormError("");
        try {
            const response = await authenticatedFetch("/api/labels");
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            const data = await response.json() as Label[];
            const activeLabels = Array.isArray(data) ? data.filter((label) => !label.deletedAt) : [];
            setLabels(activeLabels);
            onLabelsChanged?.(activeLabels);
        } catch (error) {
            setFormError(`Labels could not be loaded: ${toErrorMessage(error)}`);
        } finally {
            setIsLoading(false);
        }
    }, [onLabelsChanged]);

    useEffect(() => {
        if (isAuthLoading) return;
        void loadLabels();
    }, [isAuthLoading, loadLabels, token]);

    async function createLabel(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const requestName = name.trim();
        const requestDescription = description.trim();
        if (!requestName || !requestDescription) {
            setFormError("Name and description are required.");
            return;
        }
        setFormError("");
        setIsSaving(true);
        try {
            const response = await authenticatedFetch("/api/labels", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({name: requestName, description: requestDescription, color}),
            });
            if (response.status === 409) {
                setFormError("An active label with this name already exists.");
                showToast({title: "An active label with this name already exists.", variant: "error", description: "Please choose a different name for the label."});
                return;
            }
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            setName("");
            setDescription("");
            setColor(DEFAULT_CUSTOM_LABEL_COLOR);
            showToast({title: `Label ${requestName} created.`, variant: "success"});
            await loadLabels();
        } catch (error) {
            setFormError(toErrorMessage(error));
        } finally {
            setIsSaving(false);
        }
    }

    function beginEdit(label: Label) {
        setEditingId(label.id);
        setEditName(label.name);
        setEditDescription(label.description);
        setEditColor(label.color ?? DEFAULT_CUSTOM_LABEL_COLOR);
        setFormError("");
    }

    async function saveEdit(label: Label) {
        const requestName = label.predefined ? label.name : editName.trim();
        const requestDescription = editDescription.trim();
        if (!requestName || !requestDescription) {
            setFormError("Name and description are required.");
            return;
        }
        setFormError("");
        setIsSaving(true);
        try {
            const body: { name: string; description: string; color?: string } = {
                name: requestName,
                description: requestDescription,
            };
            if (!label.predefined) {
                body.color = editColor;
            }
            const response = await authenticatedFetch(`/api/labels/${label.id}`, {
                method: "PATCH",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(body),
            });
            if (response.status === 409) {
                setFormError("An active label with this name already exists.");
                showToast({title: "An active label with this name already exists.", variant: "error", description: "Please choose a different name for the label."});
                return;
            }
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            setEditingId(null);
            showToast({title: `Label "${requestName}" updated.`, variant: "success"});
            await loadLabels();
        } catch (error) {
            setFormError(toErrorMessage(error));
        } finally {
            setIsSaving(false);
        }
    }

    async function archiveLabel() {
        if (!archiveTarget) return;
        setIsSaving(true);
        try {
            const response = await authenticatedFetch(`/api/labels/${archiveTarget.id}`, {method: "DELETE"});
            if (!response.ok) throw new Error(await extractErrorDetails(response));
            const archivedName = archiveTarget.name;
            setArchiveTarget(null);
            showToast({title: `Label "${archivedName}" archived.`, variant: "success"});
            await loadLabels();
        } catch (error) {
            setFormError(toErrorMessage(error));
            // Close the dialog so backend errors (including attempts rejected by
            // the API, such as archiving a predefined label) remain visible.
            setArchiveTarget(null);
        } finally {
            setIsSaving(false);
        }
    }

    const grouped: Record<LabelGroup, Label[]> = {
        predefined: labels.filter((label) => label.predefined),
        custom: labels.filter((label) => !label.predefined),
    };

    return (
        <div className="space-y-4 text-xs">
                <p className="text-[11px] text-muted-foreground">
                    Predefined GDPR labels and custom labels available for BPMN labeling.
                </p>
                {formError && <p role="alert" className="text-sm text-destructive">{formError}</p>}
                {isLoading ? (
                    <p className="text-sm text-muted-foreground">Loading labels…</p>
                ) : (
                    <div className="space-y-4">
                        {(["predefined", "custom"] as const).map((group) => (
                            <section key={group} aria-labelledby={`label-group-${group}`}>
                                <h3 id={`label-group-${group}`} className="mb-2 font-semibold">
                            {group === "predefined" ? "Predefined labels" : "Custom labels"} ({grouped[group].length})
                                </h3>
                                {grouped[group].length === 0 ? (
                            <p className="text-[11px] text-muted-foreground">
                                            {group === "custom"
                                                ? "No custom labels yet. Create the first label below."
                                                : "No active labels."}
                                    </p>
                                ) : (
                                    <ul className="space-y-2">
                                        {grouped[group].map((label) => (
                                            <li key={label.id} className="rounded-md border p-3">
                                                {editingId === label.id ? (
                                                    <div className="space-y-2">
                                                        <Input
                                                            aria-label="Label name"
                                                            value={label.predefined ? label.name : editName}
                                                            disabled={label.predefined}
                                                            onChange={(event) => setEditName(event.target.value)}
                                                        />
                                                        <Textarea aria-label="Label description" rows={2} value={editDescription} onChange={(event) => setEditDescription(event.target.value)} />
                                                        {!label.predefined && (
                                                            <div className="flex items-center gap-2">
                                                                <input
                                                                    type="color"
                                                                    value={editColor}
                                                                    onChange={(event) => setEditColor(event.target.value)}
                                                                    className="h-8 w-8 cursor-pointer rounded border p-0"
                                                                    aria-label="Label color"
                                                                />
                                                                <Input
                                                                    type="text"
                                                                    value={editColor}
                                                                    onChange={(event) => setEditColor(event.target.value)}
                                                                    className="h-8 w-28 text-xs"
                                                                    aria-label="Label hex color"
                                                                />
                                                            </div>
                                                        )}
                                                        <div className="flex gap-2">
                                                            <Button size="sm" onClick={() => void saveEdit(label)} disabled={isSaving}><Save className="h-4 w-4" /> Save</Button>
                                                            <Button size="sm" variant="outline" onClick={() => setEditingId(null)} disabled={isSaving}><X className="h-4 w-4" /> Cancel</Button>
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <div className="flex items-start justify-between gap-3">
                                                        <div className="min-w-0">
                                                            <div className="flex items-center gap-2">
                                                                <span
                                                                    className="inline-block h-3 w-3 rounded-full border"
                                                                    style={{
                                                                        backgroundColor: getLabelColor(label),
                                                                        borderColor: getLabelColor(label),
                                                                    }}
                                                                    aria-hidden="true"
                                                                />
                                                                <p className="font-medium">{label.name}</p>
                                                            </div>
                                                            <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">{label.description}</p>
                                                        </div>
                                                        {(isAdmin || (!label.predefined && canEditCustomLabels)) && (
                                                            <div className="flex shrink-0 gap-1">
                                                                <Button type="button" size="icon" variant="ghost" title={`Edit ${label.name}`} onClick={() => beginEdit(label)}>
                                                                    <Pencil className="h-4 w-4" />
                                                                </Button>
                                                                {isAdmin && !label.predefined && (
                                                                    <Button type="button" size="icon" variant="ghost" title={`Archive ${label.name}`} onClick={() => setArchiveTarget(label)}>
                                                                        <Trash2 className="h-4 w-4" />
                                                                    </Button>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                {group === "custom" && canCreate && (
                                    <form onSubmit={createLabel} className="mt-3 grid gap-3 rounded-md border p-3">
                                        <div className="space-y-1">
                                            <FormLabel htmlFor="new-label-name">Name</FormLabel>
                                            <Input id="new-label-name" value={name} onChange={(event) => setName(event.target.value)} />
                                        </div>
                                        <div className="space-y-1">
                                            <FormLabel htmlFor="new-label-description">Description</FormLabel>
                                            <Textarea id="new-label-description" rows={2} value={description} onChange={(event) => setDescription(event.target.value)} />
                                        </div>
                                        <div className="space-y-1">
                                            <FormLabel htmlFor="new-label-color">Color</FormLabel>
                                            <div className="flex items-center gap-2">
                                                <input
                                                    id="new-label-color"
                                                    type="color"
                                                    value={color}
                                                    onChange={(event) => setColor(event.target.value)}
                                                    className="h-8 w-8 cursor-pointer rounded border p-0"
                                                    aria-label="Label color"
                                                />
                                                <Input
                                                    type="text"
                                                    value={color}
                                                    onChange={(event) => setColor(event.target.value)}
                                                    className="h-8 w-28 text-xs"
                                                    aria-label="Label hex color"
                                                />
                                            </div>
                                        </div>
                                        <Button type="submit" className="w-full" disabled={isSaving || isAuthLoading}>
                                            <Plus className="h-4 w-4" /> Create label
                                        </Button>
                                    </form>
                                )}
                            </section>
                        ))}
                    </div>
                )}
            <ConfirmationDialog
                isOpen={archiveTarget !== null}
                onClose={() => setArchiveTarget(null)}
                onConfirm={() => void archiveLabel()}
                title="Archive label?"
                description={`The label "${archiveTarget?.name ?? ""}" will no longer be available for new assignments. Existing assignments will be kept.`}
                confirmLabel={isSaving ? "Archiving…" : "Archive label"}
                cancelLabel="Cancel"
            />
        </div>
    );
}
