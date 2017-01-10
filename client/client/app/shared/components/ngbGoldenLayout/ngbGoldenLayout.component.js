import controller from './ngbGoldenLayout.controller';

export default {
    controller: controller.UID,
    template: '<div ngb-golden-layout-container on-init="$ctrl.onInitLayout()" on-resize="$ctrl.onResize()" ></div>',
};