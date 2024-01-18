export default class ngbTargetsFormService {

    static instance (ngbTargetsTabService, targetDataService) {
        return new ngbTargetsFormService(ngbTargetsTabService, targetDataService);
    }

    constructor(ngbTargetsTabService, targetDataService) {
        Object.assign(this, {ngbTargetsTabService, targetDataService});
    }

    get targetModel() {
        return this.ngbTargetsTabService.targetModel;
    }

    setColumnsList() {
        const targetId = this.targetModel.id;
        return new Promise(resolve => {
            this.targetDataService.getTargetGenesFields(targetId)
                .then(columns => resolve(columns))
                .catch(err => resolve([]));
        });
    }
}
