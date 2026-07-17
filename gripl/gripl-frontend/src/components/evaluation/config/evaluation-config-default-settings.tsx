"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import {EndpointChoice} from "@/models/evaluation/Config";
import {GenerateRandomInput} from "@/components/ui/input-generate-random";

interface EvaluationConfigDefaultSettingsProps {
    availableEvaluationEndpoints: AnalysisEndpoint[];
    defaultEndpointChoice: EndpointChoice;
    defaultPresetEndpoint: string;
    defaultCustomEndpoint: string;
    seed: number | null;
    maxConcurrent: number;
    repetitions: number;
    useRag: boolean;
    ragMode: string;
    evaluateRag: boolean;
    activitiesOnly: boolean;
    setDefaultEndpointChoice: (endpoint: EndpointChoice) => void;
    setDefaultPresetEndpoint: (endpoint: string) => void;
    setDefaultCustomEndpoint: (endpoint: string) => void;
    setSeed: (seed: number | null) => void;
    onMaxConcurrentChange: (v: number) => void;
    onRepetitionsChange: (v: number) => void;
    setUseRag: (v: boolean) => void;
    setRagMode: (v: string) => void;
    setEvaluateRag: (v: boolean) => void;
    setActivitiesOnly: (v: boolean) => void;
}

const RAG_MODES = [
    { value: "hybrid", label: "Hybrid (recommended)" },
    { value: "local", label: "Local" },
    { value: "global", label: "Global" },
    { value: "naive", label: "Naive" },
];

export default function EvaluationConfigDefaultSettings(props: EvaluationConfigDefaultSettingsProps) {
    const {
        availableEvaluationEndpoints,
        defaultEndpointChoice,
        defaultPresetEndpoint,
        defaultCustomEndpoint,
        seed,
        maxConcurrent,
        repetitions,
        useRag,
        ragMode,
        evaluateRag,
        activitiesOnly,
        setDefaultEndpointChoice,
        setDefaultPresetEndpoint,
        setDefaultCustomEndpoint,
        setSeed,
        onMaxConcurrentChange,
        onRepetitionsChange,
        setUseRag,
        setRagMode,
        setEvaluateRag,
        setActivitiesOnly,
    } = props;

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-lg">Default Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label>Default Evaluation Endpoint</Label>
                    <Select value={defaultEndpointChoice}
                            onValueChange={(v: EndpointChoice) => setDefaultEndpointChoice(v)}>
                        <SelectTrigger>
                            <SelectValue/>
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="preset">Use preset</SelectItem>
                            <SelectItem value="custom">Custom endpoint</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <>{defaultEndpointChoice === "preset" && (
                    <div className="space-y-2">
                        <Label>Preset Endpoint</Label>
                        <Select value={defaultPresetEndpoint} onValueChange={setDefaultPresetEndpoint}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select preset endpoint"/>
                            </SelectTrigger>
                            <SelectContent>
                                <>{availableEvaluationEndpoints.map((ep) => (
                                    <SelectItem key={ep.endpoint} value={ep.endpoint}>
                                        {ep.name}
                                    </SelectItem>
                                ))}</>
                            </SelectContent>
                        </Select>
                    </div>
                )}</>

                <>{defaultEndpointChoice === "custom" && (
                    <div className="space-y-2">
                        <Label>Custom Endpoint</Label>
                        <Input type="text" placeholder="https://example.com/analysis/v1"
                               value={defaultCustomEndpoint}
                               onChange={(e) => setDefaultCustomEndpoint(e.target.value)}/>
                    </div>
                )}</>

                <div className="space-y-2">
                    <Label>Max Concurrent LLM Requests</Label>
                    <Input id="max-requests" type="number" min={1} placeholder="4"
                           value={Number.isFinite(maxConcurrent) ? maxConcurrent : ""}
                           onChange={(e) => onMaxConcurrentChange(parseInt(e.target.value, 10) || 1)}
                           className="w-full"/>
                </div>
                <div className="space-y-2">
                    <Label>Number of Repetitions</Label>
                    <Input id="repetitions" type="number" min={1} placeholder="1"
                           value={Number.isFinite(repetitions) ? repetitions : 1}
                           onChange={(e) => onRepetitionsChange(parseInt(e.target.value, 10) || 1)}
                           className="w-full"/>
                    <p className="text-sm text-muted-foreground">The evaluation will be repeated n times to gather statistics.</p>
                </div>
                <div className="space-y-2">
                    <Label>Seed</Label>
                    <GenerateRandomInput id="seed" placeholder="Optional seed for reproducibility"
                           length={8}
                           value={seed || ""}
                           alphabet={"0123456789"}
                           onChange={(e) => setSeed(parseInt(e.target.value))}
                           className="w-full"/>
                    <p className="text-sm text-muted-foreground">Warning: Not all models support a seed, but it will be used for models that support them.</p>
                </div>

                <Separator />

                {/* ── Evaluation Scope ──────────────────────────────── */}
                <div className="flex items-center justify-between">
                    <div>
                        <Label htmlFor="activities-only-toggle">Evaluate Activities Only</Label>
                        <p className="text-xs text-muted-foreground">
                            Score only activities; events, gateways and data
                            objects/stores are not evaluated.
                        </p>
                    </div>
                    <Switch
                        id="activities-only-toggle"
                        checked={activitiesOnly}
                        onCheckedChange={setActivitiesOnly}
                    />
                </div>

                <Separator />

                {/* ── RAG Configuration ─────────────────────────────── */}
                <div className="space-y-4">
                    <div>
                        <p className="text-sm font-medium">RAG Configuration</p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                            Enable to evaluate with retrieval-augmented generation.
                        </p>
                    </div>

                    <div className="flex items-center justify-between">
                        <div>
                            <Label htmlFor="use-rag-toggle">Use RAG</Label>
                            <p className="text-xs text-muted-foreground">
                                The LLM receives GDPR knowledge retrieved from the graph.
                            </p>
                        </div>
                        <Switch
                            id="use-rag-toggle"
                            checked={useRag}
                            onCheckedChange={setUseRag}
                        />
                    </div>

                    {useRag && (
                        <>
                            <div className="space-y-2">
                                <Label>RAG Mode</Label>
                                <Select value={ragMode} onValueChange={setRagMode}>
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {RAG_MODES.map((m) => (
                                            <SelectItem key={m.value} value={m.value}>
                                                {m.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <p className="text-xs text-muted-foreground">
                                    Hybrid uses both local (entity) and global (community) graph search.
                                </p>
                            </div>

                            <div className="flex items-center justify-between">
                                <div>
                                    <Label htmlFor="evaluate-rag-toggle">Evaluate RAG Quality (Ragas)</Label>
                                    <p className="text-xs text-muted-foreground">
                                        Scores context utilization and faithfulness using an LLM judge.
                                        Adds latency per test case.
                                    </p>
                                </div>
                                <Switch
                                    id="evaluate-rag-toggle"
                                    checked={evaluateRag}
                                    onCheckedChange={setEvaluateRag}
                                />
                            </div>
                        </>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
