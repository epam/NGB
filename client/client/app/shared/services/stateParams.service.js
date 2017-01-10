export default class ngbStateParamsService {

    static instance($state) {
        return new ngbStateParamsService($state);
    }


    constructor($state) {
        Object.assign(this, {$state});
    }


    createUrl(stateParams) {

        return this.$state.href(this.$state.current.name, stateParams, {
            absolute: true, inherit: false
        });
    }

    getPathParams() {
        const items = Object.values(this.$state.$current.params).filter(m => m.location === 'path').map(m => m.id);
        const stateParams = {};
        items.forEach(m => {
            if (this.$state.params[m] !== null) {
                stateParams[m] = this.$state.params[m];
            }
        });

        return stateParams;
    }


}