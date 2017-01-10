export default class ngbStateParamsService {

    static instance($state) {
        return new ngbStateParamsService($state);
    }


    constructor($state) {
        Object.assign(this, {$state});
    }

    getAccentColors() {
        return [
            'accent-50',
            'accent-100',
            'accent-200',
            'accent-300',
            'accent-400',
            'accent-500',
            'accent-600',
            'accent-700',
            'accent-800',
            'accent-900',
            'accent-A200',
            'accent-A400',
            'accent-A700'
        ];
    }


}