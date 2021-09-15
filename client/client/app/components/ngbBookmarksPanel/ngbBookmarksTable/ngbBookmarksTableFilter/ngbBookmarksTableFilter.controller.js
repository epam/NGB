export default class ngbBookmarksTableFilterController {

    isString = false;
    isList = false;
    isLoading = false;

    constructor($scope, $timeout, projectContext, genomeDataService, ngbBookmarksTableService) {
        this.$scope = $scope;
        this.$timeout = $timeout;

        this.dependencies = [{
            obj: 'reference.name',
            subj: 'chromosome.name',
            placeholder: 'Filter reference first',
            onChange: newValue => {
                this.isLoading = true;
                return new Promise((resolve, reject) => {
                    genomeDataService.loadAllChromosomes(newValue)
                        .then((data) => {
                            if (data) {
                                const reference = projectContext.references.filter(ref => ref.id === newValue)[0];
                                const result = data.map(item => ({
                                    ...item,
                                    refName: reference.name
                                }));
                                resolve(result);
                            } else {
                                reject([]);
                            }
                        });
                });
            },
            convertResult: value => ({
                model: value.map(d => d.name.toUpperCase()),
                view: value.map(d => ({
                    refName: d.refName,
                    name: d.name.toUpperCase(),
                    id: d.id
                }))
            }),
            currentObjValue: ngbBookmarksTableService.bookmarksFilter.reference
        }];

        switch (this.column.field) {
            case 'chromosome.name': {
                this.isList = true;
                if (!this.dependencies.filter(dep => dep.subj === this.column.field).length) {
                    this.list = {
                        model: [],
                        view: []
                    };
                }
                break;
            }
            case 'reference.name': {
                this.isList = true;
                this.list = projectContext.references.map(d => d.name.toUpperCase());
                break;
            }
            default:
                this.isString = true;
        }
        this.checkDependency();
    }

    static get UID() {
        return 'ngbBookmarksTableFilterController';
    }

    checkDependency() {
        const filteredDependencies = this.dependencies.filter(dep => dep.subj === this.column.field);
        if (filteredDependencies.length) {
            const isLoadingList = [];
            filteredDependencies.forEach((dep, index) => {
                isLoadingList[index] = true;
                if (dep.currentObjValue !== undefined) {
                    if (Array.isArray(dep.currentObjValue)) {
                        const promiseList = [];
                        for (let i = 0; i < dep.currentObjValue.length; i++) {
                            promiseList.push(dep.onChange(dep.currentObjValue[i]));
                        }
                        Promise.all(promiseList).then(values => {
                            let value;
                            if (this.isList) {
                                value = [];
                                for (let i = 0; i < values.length; i++) {
                                    value = value.concat(values[i]);
                                }
                                this.list = dep.convertResult(value);
                            }
                            isLoadingList[index] = false;
                            this.processIsLoading(isLoadingList);
                        });
                    } else {
                        dep.onChange(dep.currentObjValue).then(value => {
                            if (this.isList) {
                                this.list = dep.convertResult(value);
                            }
                            isLoadingList[index] = false;
                            this.processIsLoading(isLoadingList);
                        });
                    }
                } else {
                    if (this.isList) {
                        this.list = dep.convertResult([]);
                    }
                    isLoadingList[index] = false;
                    this.processIsLoading(isLoadingList);
                }
            });
        } else {
            this.isLoading = false;
        }
    }

    processIsLoading(list) {
        this.isLoading = !list.every(item => !item);
        this.$timeout(() => this.$scope.$apply());
    }
}
