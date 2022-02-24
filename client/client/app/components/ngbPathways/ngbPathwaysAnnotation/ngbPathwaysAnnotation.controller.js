import {HeatmapDataType} from '../../../../modules/render/heatmap';
import ColorScheme, {ColorFormats} from '../../../../modules/render/heatmap/color-scheme';
import HeatmapViewOptions from '../../../../modules/render/heatmap/heatmap-view-options';
import {readHeatmapState} from '../../../../modules/render/heatmap/heatmap-view-options/manager';

function initializeManualConfig(config) {
    let result = '';
    config.forEach(configStr => {
        result += `${configStr.term}\t`;
        result += configStr.backgroundColor;
        if (configStr.foregroundColor) {
            result += `,${configStr.foregroundColor}`;
        }
        result += '\n';
    });
    return result;
}

function parseConfigCSV(config) {
    let maximum = -Infinity, minimum = Infinity;
    let dataType = HeatmapDataType.number;
    const cellValues = [];
    const splitter = config.indexOf('\r') > -1 ? '\r\n' : '\n';
    const lines = config.split(splitter);
    let splitLine = [];
    const rowLabels = lines[0].split(',');
    const columnLabels = [];
    for (let i = 1; i < lines.length; i++) {
        splitLine = lines[i].split(',');
        columnLabels.push(splitLine.shift());
        for (const val of splitLine) {
            const numericValue = Number(val);
            if (isNaN(numericValue)) {
                cellValues.push(val);
                dataType = HeatmapDataType.string;
                minimum = undefined;
                maximum = undefined;
            } else {
                cellValues.push(numericValue);
                if (numericValue < minimum) {
                    minimum = numericValue;
                }
                if (numericValue > maximum) {
                    maximum = numericValue;
                }
            }
        }
    }
    return {
        columnLabels, rowLabels,
        minimum, maximum,
        dataType,
        cellValues
    };
}

function heatmapNameSorter(a, b) {
    const aName = (a.prettyName || a.name || '').toLowerCase();
    const bName = (b.prettyName || b.name || '').toLowerCase();
    if (!aName) {
        return 1;
    }
    if (!bName) {
        return -1;
    }
    if (aName < bName) {
        return -1;
    }
    if (aName > bName) {
        return 1;
    }
    return 0;
}

export default class ngbPathwaysAnnotationController {
    configurationType = {
        STRING: 0,
        NUMBER: 1,
        RANGE: 2
    };
    annotationFileHeaderList = null;
    heatmap = null;
    csvFile = {};

    constructor(ngbPathwaysService, ngbPathwaysAnnotationService, heatmapContext) {
        this.heatmapContext = heatmapContext;
        this.annotationTypeList = ngbPathwaysAnnotationService.annotationTypeList;
        this.annotationFileHeaderList = ngbPathwaysAnnotationService.annotationFileHeaderList;
        this.initialize();
    }

    static get UID() {
        return 'ngbGenesTableDownloadDlgController';
    }

    get heatmapsFromTrack() {
        return this.heatmapContext.heatmaps.filter(heatmap => heatmap.isTrack).sort(heatmapNameSorter);
    }

    get heatmapsFromAnnotations() {
        return this.heatmapContext.heatmaps.filter(heatmap => heatmap.isAnnotation).sort(heatmapNameSorter);
    }

    get heatmapId() {
        if (this.heatmap) {
            return this.heatmap.id;
        }
        return undefined;
    }

    set heatmapId(id) {
        const [heatmap] = this.heatmapContext.heatmaps.filter(h => h.id === id);
        this.heatmap = heatmap;
        const projectId = this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        const options = HeatmapViewOptions.parse(readHeatmapState(id));
        options.data.onMetadataLoaded(() => {
            this.annotation.colorScheme = options.colorScheme.copy({colorFormat: ColorFormats.hex});
            this.annotation.config = options.data.metadata;
        });
        options.data.options = {id, projectId};
        this.annotation.name = this.annotation.name || this.heatmap.prettyName || this.heatmap.name;
    }

    $onChanges(changes) {
        if (!!changes.annotation &&
            !!changes.annotation.currentValue) {
            this.initialize();
        }
    }

    initialize() {
        if (this.annotation) {
            if (this.annotation.type === this.annotationTypeList.MANUAL) {
                this.annotation.config = initializeManualConfig(this.annotation.value);
            }
        }
    }

    onAnnotationTypeChange() {
        this.heatmap = null;
        this.csvFile = {};
        const {id, name, type, pathwayId} = this.annotation;
        this.annotation = {id, name, type, pathwayId, config: null};
    }

    onFileUpload() {
        this.annotation.name = this.csvFile.name;
        this.annotation.config = parseConfigCSV(this.csvFile.value);
        if (!this.annotation.colorScheme) {
            this.annotation.colorScheme = new ColorScheme({
                colorFormat: ColorFormats.hex,
                minimum: this.annotation.config.minimum,
                maximum: this.annotation.config.maximum,
                dataType: this.annotation.config.dataType
            });
        }
    }
}
