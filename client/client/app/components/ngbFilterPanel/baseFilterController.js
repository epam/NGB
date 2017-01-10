export default class BaseFilterController {

    projectContext;

    constructor(dispatcher, projectContext, $scope) {

        this._dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.projectId = this.projectContext.project.id;

        const setDefault = (event) => {
            this.setDefault(event);
        };

        this._dispatcher.on('ngbFilter:setDefault', setDefault);

        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('ngbFilter:setDefault', setDefault);
        });
    }

    setDefault() {

    }

    emitEvent() {
        this.projectContext.filterVariants();
    }

}
