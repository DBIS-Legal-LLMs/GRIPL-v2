export type GdprCategory =
    | "Collection"
    | "Storage"
    | "Usage"
    | "Transferal"
    | "Modification"
    | "Deletion"
    | "Access";

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
