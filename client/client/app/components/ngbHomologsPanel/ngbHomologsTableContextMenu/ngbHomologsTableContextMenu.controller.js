import BaseController from '../../../shared/baseController';

export default class ngbHomologsTableContextMenuController extends BaseController {

    constructor(
        $scope,
        dispatcher,
        appLayout,
        projectContext,
        ngbHomologsTableContextMenu
    ) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            appLayout,
            projectContext,
            ngbHomologsTableContextMenu
        });
        this.entity = $scope.row.entity;
    }

    close() {
        this.ngbHomologsTableContextMenu.close();
    }

    createTarget(event) {
        this.close();
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        this.dispatcher.emitSimpleEvent('homologs:create:target', this.entity.targetInfo);
        event.stopImmediatePropagation();
    }
}
