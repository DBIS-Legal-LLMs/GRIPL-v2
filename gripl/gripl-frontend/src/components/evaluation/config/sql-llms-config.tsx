export type ModelConfig = {
    model: string;
    apiKeyName: string;
    baseUrl: string;
};

export type LLMComponentKey = 'INTENTION_MODEL' |
    'SQL_GENERATION_MODEL'  |
    'POST_PROCESSING_MODEL' |
    'VERIFICATION_MODEL'    |
    'EMBEDDING_MODEL'
    ;

export const LLM_COMPONENTS: { key: LLMComponentKey; label: string; description: string }[] = [
    {key: 'INTENTION_MODEL', label: 'Intention Model', description: 'Modell für die Intentionserkennung.'},
    {key: 'SQL_GENERATION_MODEL', label: 'SQL Generation Model', description: 'Modell für die SQL‑Generierung.'},
    {key: 'POST_PROCESSING_MODEL', label: 'Post Processing Model', description: 'Modell für die Nachbearbeitung.'},
    {key: 'VERIFICATION_MODEL', label: 'Verification Model', description: 'Modell für die Verifikation.'},
    {key: 'EMBEDDING_MODEL', label: 'Embedding Model', description: 'Modell für das Embedden der Daten'},
];