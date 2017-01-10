import  baseController from '../../shared/baseController';

export default class <%= name %>Controller extends baseController {
    static get UID() {
        return '<%= name %>Controller';
    }

    events = {};

    constructor($scope, dispatcher, <%= name %>Service) {
        super();

        Object.assign(this, {$scope, dispatcher, <%= name %>Service});

        this.initEvents();
    }

    $onInit() {

        this.loadData();
    }


}