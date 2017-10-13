export default class ngbApiService {

    static instance($state) {
        return new ngbApiService($state);
    }


    constructor($state) {
        Object.assign(this, {$state});
    }

    loadDataSet(id) {
        console.log("ngbApiService: loadDataSet");
       //todo implement
    }
}