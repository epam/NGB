const PATHWAYS_STATES = {
    KEGG: 'KEGG',
    INTERNAL_PATHWAYS: 'INTERNAL_PATHWAYS',
    INTERNAL_PATHWAYS_RESULT: 'INTERNAL_PATHWAYS_RESULT'
};

function findMaxId(array) {
    let result = 0;
    array.forEach(a => {
        if (a.id > result) {
            result = a.id;
        }
    });
    return result;
}

export default class ngbPathwaysService {
    pathwaysServiceMap = {};
    currentInternalPathway;
    annotationList = [];
    maxAnnotationId = 1;

    constructor(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService
    ) {
        Object.assign(
            this,
            {
                dispatcher,
                projectContext
            }
        );
        this.pathwaysServiceMap = {
            [PATHWAYS_STATES.INTERNAL_PATHWAYS]: ngbInternalPathwaysTableService,
            [PATHWAYS_STATES.INTERNAL_PATHWAYS_RESULT]: ngbInternalPathwaysResultService,
        };
        this.annotationList = JSON.parse(localStorage.getItem('pathwaysAnnotations')) || [];
        this.maxAnnotationId = findMaxId(this.annotationList);
        this.initEvents();
    }

    _currentSearch;

    get currentSearch() {
        return this._currentSearch;
    }

    set currentSearch(value) {
        this._currentSearch = value;
    }

    get pathwaysStates() {
        return PATHWAYS_STATES;
    }

    static instance(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService) {
        return new ngbPathwaysService(dispatcher, projectContext,
            ngbInternalPathwaysTableService, ngbInternalPathwaysResultService);
    }

    initEvents() {
        this.dispatcher.on('read:show:pathways', data => {
            this.currentSearch = data ? data.search : null;
        });
    }

    getAnnotationList() {
        return this.annotationList;
    }

    saveAnnotationList(list) {
        localStorage.setItem('pathwaysAnnotations', JSON.stringify(list));
    }

    getAnnotationById(id) {
        const [result] = this.annotationList.filter(a => a.id === id);
        return result;
    }

    deleteAnnotationById(id) {
        const index = this.annotationList.findIndex(a => a.id === id);
        if (index > -1) {
            this.annotationList.splice(index, 1);
            if (this.maxAnnotationId === id) {
                this.maxAnnotationId = findMaxId(this.annotationList);
            }
        }
        this.saveAnnotationList(this.annotationList);
        this.dispatcher.emitSimpleEvent('pathways:internalPathways:annotations:change');
    }

    setAnnotation(annotation) {
        if (annotation.id) {
            const index = this.annotationList.findIndex(a => a.id === annotation.id);
            if (index > -1) {
                this.annotationList[index] = {
                    ...annotation,
                    isActive: true
                };
            } else {
                this.annotationList.push({
                    ...annotation,
                    id: ++this.maxAnnotationId,
                    isActive: true
                });
            }
        } else {
            this.annotationList.push({
                ...annotation,
                id: ++this.maxAnnotationId,
                isActive: true
            });
        }
        this.saveAnnotationList(this.annotationList);
        this.dispatcher.emitSimpleEvent('pathways:internalPathways:annotations:change');
    }
}
