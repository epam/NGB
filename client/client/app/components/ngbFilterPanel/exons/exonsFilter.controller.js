import baseFilterController from '../baseFilterController';

export default class exonsFilterController extends baseFilterController {

    static get UID() {
        return 'exonsFilterController';
    }

    constructor(dispatcher, projectContext, $scope) {
        super(dispatcher, projectContext, $scope);
    }

}