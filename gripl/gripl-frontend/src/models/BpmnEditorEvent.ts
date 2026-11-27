export enum BpmnEditorEvent {
    ElementHover = 'element.hover',
    ElementOut = 'element.out',
    ElementClick = 'element.click',
    ElementDblClick = 'element.dblclick',
    ElementMouseDown = 'element.mousedown',
    ElementMouseUp = 'element.mouseup',
    BpmnElementAdded = 'bpmnElement.added',
    ElementChanged = 'element.changed',
    DragInit = 'drag.init',
    SelectionChanged = 'selection.changed',
}

/** Payload shape of the bpmn-js events surfaced by the editor. */
export interface BpmnEditorEventPayload {
    newSelection?: { id: string; businessObject?: { name?: string } }[]
    element?: { id: string; businessObject?: { name?: string } }
}