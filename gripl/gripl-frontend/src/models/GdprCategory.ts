export type GdprCategory =
    | "Collection"
    | "Storage"
    | "Usage"
    | "Transferal"
    | "Modification"
    | "Deletion"
    | "Access";

export type BackendGdprProcessingClass =
    | "COLLECTION"
    | "STORAGE"
    | "USAGE"
    | "TRANSFERAL"
    | "MODIFICATION"
    | "DELETION"
    | "ACCESS";

const BACKEND_FROM_FRONTEND: Record<GdprCategory, BackendGdprProcessingClass> = {
    Collection: "COLLECTION",
    Storage: "STORAGE",
    Usage: "USAGE",
    Transferal: "TRANSFERAL",
    Modification: "MODIFICATION",
    Deletion: "DELETION",
    Access: "ACCESS",
};

const FRONTEND_FROM_BACKEND: Record<BackendGdprProcessingClass, GdprCategory> = {
    COLLECTION: "Collection",
    STORAGE: "Storage",
    USAGE: "Usage",
    TRANSFERAL: "Transferal",
    MODIFICATION: "Modification",
    DELETION: "Deletion",
    ACCESS: "Access",
};

export interface GdprCategoryMeta {
    value: GdprCategory;
    label: string;
    description: string;
}

export const GDPR_CATEGORIES: GdprCategoryMeta[] = [
    { value: "Collection",  label: "Collection",  description: "Gathers personal information (e.g., names, emails, health data)" },
    { value: "Storage",     label: "Storage",     description: "Saves personal data in databases, systems, or files" },
    { value: "Usage",       label: "Usage",       description: "Employs personal data for operational or analytical purposes" },
    { value: "Transferal",  label: "Transferal",  description: "Shares personal data with third parties or externally" },
    { value: "Modification",label: "Modification",description: "Updates, corrects, or alters previously stored personal data" },
    { value: "Deletion",    label: "Deletion",    description: "Removes or anonymizes personal data from storage" },
    { value: "Access",      label: "Access",      description: "Retrieves or makes personal data available to users or systems" },
];

export function toBackendGdprProcessingClass(category: GdprCategory): BackendGdprProcessingClass {
    return BACKEND_FROM_FRONTEND[category];
}

export function toFrontendGdprCategory(value: string): GdprCategory | null {
    if (!value) {
        return null;
    }

    const normalized = value.toUpperCase() as BackendGdprProcessingClass;
    return FRONTEND_FROM_BACKEND[normalized] ?? null;
}

export function normalizeFrontendGdprCategories(values: string[] | undefined | null): GdprCategory[] {
    if (!values || values.length === 0) {
        return [];
    }

    const normalized = values
        .map((value) => toFrontendGdprCategory(value))
        .filter((value): value is GdprCategory => value !== null);

    return Array.from(new Set(normalized));
}
