import {HeatmapDataType} from '../../../../modules/render/heatmap';
import ColorScheme, {ColorFormats} from '../../../../modules/render/heatmap/color-scheme';
import HeatmapMetadata from '../../../../modules/render/heatmap/heatmap-data/heatmap-metadata';
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

function initializeCSVConfig(config, isRow) {
    return {
        columnLabels: isRow ? config.otherLabels : config.labels,
        rowLabels: isRow ? config.labels : config.otherLabels,
        cellValues: config.values,
        fileName: config.fileName
    };
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
        if (!lines[i]) {
            continue;
        }
        splitLine = lines[i].split(',');
        columnLabels.push(splitLine.shift());
        cellValues.push(splitLine);
        for (const val of splitLine) {
            const numericValue = Number(val);
            if (isNaN(numericValue)) {
                dataType = HeatmapDataType.string;
                minimum = undefined;
                maximum = undefined;
            } else {
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

function getAllValuesList(cellValues) {
    let result = [];
    for (const row of cellValues) {
        result = new Set([...result, ...row]);
    }
    return Array.from(result);
}

export default class ngbPathwaysAnnotationController {
    configurationType = {
        STRING: 0,
        NUMBER: 1,
        RANGE: 2
    };
    annotationFileHeaderList = null;
    heatmap = null;
    vcfFile = null;
    _vcfFileInfoItem = null;
    vcfFileError = null;
    vcfFileInfoError = null;
    csvFile = null;

    constructor(
        ngbPathwaysService,
        ngbPathwaysAnnotationService,
        heatmapContext,
        projectContext,
        projectDataService,
        vcfDataService,
        $timeout,
        $scope
    ) {
        this.heatmapContext = heatmapContext;
        this.projectContext = projectContext;
        this.projectDataService = projectDataService;
        this.vcfDataService = vcfDataService;
        this.$timeout = $timeout;
        this.$scope = $scope;
        this.annotationTypeList = ngbPathwaysAnnotationService.annotationTypeList;
        this.annotationFileHeaderList = ngbPathwaysAnnotationService.annotationFileHeaderList;
        this.initialize();
    }

    static get UID() {
        return 'ngbPathwaysAnnotationController';
    }

    get vcfFiles() {
        if (this.projectContext) {
            return (this.projectContext.vcfTracks || []).slice();
        }
        return [];
    }

    get vcfFileId() {
        if (this.vcfFile) {
            return this.vcfFile.id;
        }
        return undefined;
    }

    get vcfFileInfoItem() {
        return this._vcfFileInfoItem;
    }

    set vcfFileInfoItem(infoItem) {
        if (this._vcfFileInfoItem !== infoItem) {
            this._vcfFileInfoItem = infoItem;
            this.$timeout(() => {
                this.$scope.$apply();
                this.fetchVCFAnnotation();
            });
        }
    }

    set vcfFileId(id) {
        const isVcfFileChanged = this.vcfFile
            ? this.vcfFile.id !== id
            : !!id;
        if (isVcfFileChanged) {
            this.vcfFileInfoItem = null;
            if (this.vcfFile) {
                this.annotation.config = undefined;
                this.annotation.header = undefined;
            }
            const [vcfFile] = this.vcfFiles.filter(h => h.id === id);
            this.vcfFile = vcfFile;
            this.$timeout(() => {
                this.$scope.$apply();
                this.fetchVCFInfoFields();
            });
        }
    }

    get vcfPending() {
        return this.vcfFileInfoPending || this.vcfAnnotationPending;
    }

    get vcfError() {
        return this.vcfFileError || this.vcfFileInfoError;
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
        const isHeatmapChange = this.heatmap && this.heatmap.id !== id;
        const [heatmap] = this.heatmapContext.heatmaps.filter(h => h.id === id);
        this.heatmap = heatmap;
        const projectId = this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        const options = HeatmapViewOptions.parse(readHeatmapState(id));
        options.data.onDataLoaded(payload => {
            const values = [];
            for (const heatmapDataItem of payload.data.entries()) {
                if (!values[heatmapDataItem.row]) {
                    values[heatmapDataItem.row] = [];
                }
                values[heatmapDataItem.row][heatmapDataItem.column] = heatmapDataItem.value;
            }
            this.annotation.config = {
                rows: payload.metadata.rows,
                columns: payload.metadata.columns,
                heatmapId: id,
                values
            };
            if (isHeatmapChange || !this.colorScheme) {
                const colorSchemeParams = {colorFormat: ColorFormats.hex};
                if (this.annotation.config.minCellValue) {
                    colorSchemeParams.minimum = this.annotation.config.minCellValue;
                }
                if (this.annotation.config.maxCellValue) {
                    colorSchemeParams.maximum = this.annotation.config.maxCellValue;
                }
                this.colorScheme = options.colorScheme.copy(colorSchemeParams);
            }
            this.annotation.config.minCellValue = this.colorScheme.minimum;
            this.annotation.config.maxCellValue = this.colorScheme.maximum;
        });
        options.data.options = {id, projectId};
        this.annotation.name = this.annotation.name || this.heatmap.prettyName || this.heatmap.name;
        this.$timeout(() => this.$scope.$apply());
    }

    $onChanges(changes) {
        if (!!changes.annotation &&
            !!changes.annotation.currentValue) {
            this.initialize();
        }
    }

    initialize() {
        if (this.annotation) {
            switch (this.annotation.type) {
                case this.annotationTypeList.MANUAL: {
                    this.annotation.config = initializeManualConfig(this.annotation.value);
                    break;
                }
                case this.annotationTypeList.HEATMAP: {
                    this.heatmapId = this.annotation.value.heatmapId;
                    break;
                }
                case this.annotationTypeList.CSV: {
                    this.annotation.config = initializeCSVConfig(
                        this.annotation.value,
                        this.annotation.header === this.annotationFileHeaderList.ROW
                    );
                    this.csvFile = {
                        name: this.annotation.value.fileName
                    };
                    break;
                }
                case this.annotationTypeList.VCF: {
                    this.annotation.config = initializeCSVConfig(
                        this.annotation.value,
                        false
                    );
                    this.annotation.header = this.annotationFileHeaderList.COLUMN;
                    this.vcfFileId = this.annotation.value.vcfFile
                        ? this.annotation.value.vcfFile.id
                        : undefined;
                    this._vcfFileInfoItem = this.annotation.value.infoItem;
                    break;
                }
            }
        }
    }

    onAnnotationTypeChange() {
        this.heatmap = null;
        this.csvFile = null;
        this.colorScheme = null;
        this.vcfFile = null;
        this.vcfFileInfoItem = null;
        this.vcfFileError = null;
        this.vcfFileInfoError = null;
        const {id, type, pathwayId} = this.annotation;
        this.annotation = {
            id, type, pathwayId,
            config: null,
            header: this.annotationFileHeaderList.COLUMN
        };
        switch (type) {
            case this.annotationTypeList.VCF: {
                if (this.vcfFiles.length > 0) {
                    this.vcfFileId = this.vcfFiles[0].id;
                }
                break;
            }
        }
    }

    onFileUpload() {
        this.annotation.name = this.csvFile.name;
        this.annotation.config = parseConfigCSV(this.csvFile.value);
        this.annotation.config.fileName = this.csvFile.name;
        if (!this.colorScheme) {
            this.colorScheme = new ColorScheme({
                colorFormat: ColorFormats.hex,
                minimum: this.annotation.config.minimum,
                maximum: this.annotation.config.maximum,
                dataType: this.annotation.config.dataType,
                values: getAllValuesList(this.annotation.config.cellValues)
            });
        }
    }

    fetchVCFInfoFields() {
        const token = (this._vcfFileInfoFetchToken || 0);
        this._vcfFileInfoFetchToken = token;
        const outdated = () => token !== this._vcfFileInfoFetchToken;
        if (this.vcfFile && this.projectDataService) {
            const projectId = this.vcfFile.project ? this.vcfFile.project.id : this.vcfFile.projectIdNumber;
            if (projectId) {
                const fetch = async () => {
                    this.vcfFileError = undefined;
                    this.vcfFileInfoPending = true;
                    const payload = {
                        value: {
                            [projectId]: [this.vcfFile.id]
                        }
                    };
                    try {
                        const {infoItems = []} = await this.projectDataService.getProjectsFilterVcfInfo(payload);
                        if (!outdated()) {
                            this.vcfFileInfoItems = infoItems.slice();
                            const exists = this.vcfFileInfoItems
                                .filter((item) => item.name === this._vcfFileInfoItem)
                                .length > 0;
                            if (!exists) {
                                this.vcfFileInfoItem = null;
                            }
                        }
                    } catch (e) {
                        if (!outdated()) {
                            this.vcfFileError = e.message;
                        }
                    } finally {
                        if (!outdated()) {
                            this.vcfFileInfoPending = false;
                        }
                        this.$scope.$apply();
                    }
                };
                (fetch)();
                return;
            }
        }
        this.vcfFileInfoPending = false;
    }

    fetchVCFAnnotation() {
        const token = (this._vcfAnnotationToken || 0);
        this._vcfAnnotationToken = token;
        const outdated = () => token !== this._vcfAnnotationToken;
        if (this.vcfFileInfoItem && this.vcfFile && this.vcfDataService) {
            const fetch = async () => {
                this.vcfFileInfoError = undefined;
                this.annotation.name = `${this.vcfFile.name} - ${this.vcfFileInfoItem}`;
                this.annotation.config = undefined;
                this.annotation.header = undefined;
                this.colorScheme = undefined;
                this.vcfAnnotationPending = true;
                try {
                    const matrix = await this.vcfDataService.getVcfFieldValues(this.vcfFileId, this.vcfFileInfoItem);
                    if (!matrix) {
                        throw new Error(`Error fetching values for ${this.vcfFileInfoItem}`);
                    }
                    if (!outdated()) {
                        this.annotation.name = `${this.vcfFile.name} - ${this.vcfFileInfoItem}`;
                        const {
                            values: rawValues = []
                        } = matrix;
                        const reducedValues = rawValues.reduce((r, c) => ({...r, ...c}), {});
                        const columnLabels = Object.keys(reducedValues);
                        const rowLabels = [''];
                        const wrapNull = (value) => /^null$/i.test(value) ? undefined : value;
                        const {
                            minimum,
                            maximum,
                            type: dataType
                        } = HeatmapMetadata.fromResponse(matrix);
                        const format = (value) => {
                            if (dataType === HeatmapDataType.number && !Number.isNaN(Number(value))) {
                                return Number(value);
                            }
                            return value;
                        };
                        const prepare = (value) => format(wrapNull(value));
                        const cellValues = columnLabels
                            .map((column) => [prepare(reducedValues[column])]);
                        const values = [...new Set(columnLabels.map((column) => prepare(reducedValues[column])))]
                            .filter(Boolean);
                        this.annotation.config = {
                            minimum,
                            maximum,
                            cellValues,
                            dataType,
                            columnLabels,
                            rowLabels
                        };
                        this.annotation.header = this.annotationFileHeaderList.COLUMN;
                        this.annotation.config.vcfFile = this.vcfFile;
                        this.annotation.config.infoItem = this.vcfFileInfoItem;
                        this.colorScheme = new ColorScheme({
                            colorFormat: ColorFormats.hex,
                            minimum,
                            maximum,
                            dataType,
                            values
                        });
                    }
                } catch (e) {
                    if (!outdated()) {
                        this.vcfFileInfoError = e.message;
                    }
                } finally {
                    if (!outdated()) {
                        this.vcfAnnotationPending = false;
                    }
                    this.$scope.$apply();
                }
            };
            (fetch)();
            return;
        }
        this.vcfAnnotationPending = false;
    }
}
