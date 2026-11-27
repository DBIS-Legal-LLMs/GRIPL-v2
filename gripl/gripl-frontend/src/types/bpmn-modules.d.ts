// Ambient declarations for bpmn packages that ship JS only.
// The production modeler bundle is imported dynamically to keep the main chunk small.
declare module "bpmn-js/dist/bpmn-modeler.production.min.js" {
    const BpmnModeler: unknown;
    export default BpmnModeler;
}

declare module "bpmn-js-token-simulation/lib/features/disable-modeling" {
    const DisableModelingModule: unknown;
    export default DisableModelingModule;
}

declare module "bpmn-js-token-simulation/lib/util/EventHelper" {
    export const TOGGLE_MODE_EVENT: string;
}
