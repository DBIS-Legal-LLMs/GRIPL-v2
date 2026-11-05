export interface Label {
    id: number;
    name: string;
    description: string;
    predefined: boolean;
    color?: string | null;
    deletedAt?: string | null;
}

export interface LabelWriteRequest {
    name: string;
    description: string;
    color?: string | null;
}
