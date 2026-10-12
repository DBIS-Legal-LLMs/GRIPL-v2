/**
 * Removes a trailing .bpmn / .xml extension, e.g. from an uploaded file name.
 */
export function stripBpmnExtension(name: string) {
    return name.trim().replace(/\.(bpmn|xml)$/i, "");
}

/**
 * Triggers a browser download of the given BPMN XML as a .bpmn file.
 * The file name is derived from the given name (falls back to "diagram").
 */
export function downloadBpmnXml(xml: string, name?: string) {
    const safeName = stripBpmnExtension(name || "").replace(/[\\/:*?"<>|]+/g, "_") || "diagram";
    const blob = new Blob([xml], { type: "application/xml" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${safeName}.bpmn`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
