export interface Label {
    id: number;
    name: string;
    description: string;
    predefined: boolean;
    deletedAt?: string | null;
}

export interface LabelWriteRequest {
    name: string;
    description: string;
}
