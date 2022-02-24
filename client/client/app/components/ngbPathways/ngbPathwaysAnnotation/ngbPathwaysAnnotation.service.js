import ColorScheme, {ColorFormats} from '../../../../modules/render/heatmap/color-scheme';

const ANNOTATION_FILE_HEADER_LIST = {
    COLUMN: 0,
    ROW: 1
};

const ANNOTATION_TYPE_LIST = {
    HEATMAP: 0,
    CSV: 1,
    MANUAL: 2
};

const ANNOTATION_PREFIX = 'Annotation #';
const ANNOTATION_STORAGE_NAME = 'pathways-annotations';

function findMaxId(array) {
    let result = 0;
    array.forEach(a => {
        if (a.id > result) {
            result = a.id;
        }
    });
    return result;
}

function parseConfigStr(rawStr) {
    const result = [];
    const strList = rawStr.split('\n');
    strList.forEach(str => {
        if (str) {
            const [term, ...colors] = str.split(/\s+/);
            const colorList = colors.join().split(',');
            result.push({
                term: term,
                backgroundColor: colorList[0] ? colorList[0].trim() : undefined,
                foregroundColor: colorList[1] ? colorList[1].trim() : undefined
            });
        }
    });
    return result;
}

function prepareConfigCSV(config, header) {
    return {
        labels: header === ANNOTATION_FILE_HEADER_LIST.COLUMN ? config.columnLabels : config.rowLabels,
        values: config.cellValues
    };
}

function parseConfigHeatmap(config, header) {
    const labels = config[header === ANNOTATION_FILE_HEADER_LIST.COLUMN ? 'rows' : 'columns'].map(item => item.name);
    return {
        labels,
        values: config.values,
        heatmapId: config.heatmapId
    };
}

function prepareAnnotationList(list) {
    const result = [];
    list.forEach(annotation => {
        const annotationClone = {
            ...annotation
        };
        if (annotationClone.colorScheme) {
            annotationClone.serializedColorScheme = annotationClone.colorScheme.serialize();
            delete annotationClone.colorScheme;
        }
        result.push(annotationClone);
    });
    return result;
}

export default class ngbPathwaysAnnotationService {
    annotationList = [];
    _maxAnnotationId = 1;

    constructor(dispatcher
    ) {
        Object.assign(
            this,
            {
                dispatcher
            }
        );
        this.initState([]);
    }

    get annotationFileHeaderList() {
        return ANNOTATION_FILE_HEADER_LIST;
    }

    get annotationTypeList() {
        return ANNOTATION_TYPE_LIST;
    }

    getSessionAnnotationList() {
        return prepareAnnotationList(this.annotationList.filter(item => item.type !== this.annotationTypeList.CSV));
    }

    getPathwayAnnotationList(pathwayId) {
        return this.annotationList.filter(item => item.pathwayId === pathwayId);
    }

    saveAnnotationList(list) {
        localStorage.setItem(
            ANNOTATION_STORAGE_NAME,
            JSON.stringify(prepareAnnotationList(
                list.filter(item => item.type !== this.annotationTypeList.CSV)
            ))
        );
    }

    getAnnotationById(id) {
        const [result] = this.annotationList.filter(a => a.id === id);
        return result;
    }

    deleteAnnotationById(id) {
        const index = this.annotationList.findIndex(a => a.id === id);
        if (index > -1) {
            this.annotationList.splice(index, 1);
            if (this._maxAnnotationId === id) {
                this._maxAnnotationId = findMaxId(this.annotationList);
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
                    id: ++this._maxAnnotationId,
                    isActive: true
                });
            }
        } else {
            if (!annotation.name) {
                annotation.name = ANNOTATION_PREFIX + (this._maxAnnotationId + 1);
            }
            this.annotationList.push({
                ...annotation,
                id: ++this._maxAnnotationId,
                isActive: true
            });
        }
        this.saveAnnotationList(this.annotationList);
        this.dispatcher.emitSimpleEvent('pathways:internalPathways:annotations:change');
    }

    save(annotation) {
        let config = [];
        if (annotation.type === this.annotationTypeList.MANUAL) {
            config = parseConfigStr(annotation.config);
        }
        if (annotation.type === this.annotationTypeList.CSV) {
            config = prepareConfigCSV(annotation.config, annotation.header);
        }
        if (annotation.type === this.annotationTypeList.HEATMAP) {
            config = parseConfigHeatmap(annotation.config, annotation.header);
        }

        if (annotation.colorScheme) {
            annotation.colorScheme.initializeFrom(annotation.colorScheme);
        }
        const savedAnnotation = {
            ...annotation,
            value: config
        };
        delete savedAnnotation.config;
        this.setAnnotation(savedAnnotation);
    }

    initState(annotationList) {
        const rawAnnotationList = annotationList && annotationList.length
            ? annotationList
            : JSON.parse(localStorage.getItem(ANNOTATION_STORAGE_NAME)) || [];
        this.annotationList = [];
        for (const annotation of rawAnnotationList) {
            if (annotation.serializedColorScheme) {
                annotation.colorScheme = ColorScheme.parse(annotation.serializedColorScheme).copy({
                    colorFormat: ColorFormats.hex
                });
                delete annotation.serializedColorScheme;
            }
            this.annotationList.push(annotation);
        }
        this._maxAnnotationId = findMaxId(this.annotationList);
    }
}
