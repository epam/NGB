import {utilities} from '../utilities';

export default class ngbVariantInfoService{

    static instance(dispatcher, constants, vcfDataService, genomeDataService) {
        return new ngbVariantInfoService(dispatcher, constants, vcfDataService, genomeDataService);
    }

    _constants;
    _dataService;
    _dispatcher;
    _genomeDataService;
    _chromosomeName;

    constructor(dispatcher, constants, dataService, genomeDataService){
        this._dataService = dataService;
        this._constants = constants;
        this._dispatcher = dispatcher;
        this._genomeDataService = genomeDataService;
    }

    loadVariantInfo(variantRequest, callback, errorCallback){
        const request = {
            chromosomeId: variantRequest.chromosomeId,
            id: variantRequest.openByUrl ? undefined : variantRequest.vcfFileId,
            openByUrl: variantRequest.openByUrl,
            fileUrl: variantRequest.openByUrl ? variantRequest.fileUrl : undefined,
            indexUrl: variantRequest.openByUrl ? variantRequest.indexUrl : undefined,
            position: variantRequest.position,
            projectId: variantRequest.projectIdNumber,
        };

        this._genomeDataService.loadChromosome(variantRequest.chromosomeId)
            .then(
                (chr) => {
                    this.chromosomeName = chr.name;
                    this._dataService.getVariantInfo(request)
                        .then(
                            (data) => {
                                callback(this._mapVariantPropertiesData(data, variantRequest));
                            }, () => {
                                errorCallback(this._constants.errorMessages.errorLoadingVariantInfo);
                            }
                        );
                }, () => {
                    errorCallback(this._constants.errorMessages.chromosomeNotFound);
                }
            );
    }

    get chromosomeName() {
        return this._chromosomeName;
    }

    set chromosomeName(value) {
        this._chromosomeName = value ? value : null;
    }

    _mapVariantPropertiesData (variantData, variantRequest) {
        const variantProperties = [];
        const wideFlex = 100;
        const commonFlex = 50;
        const trimGenesMaxItems = 5;
        const trimGenesMaxLength = 30;
        const {sample} = variantRequest || {};
        const sampleInfo = variantData.genotypeData && sample ? variantData.genotypeData[sample] : undefined;
        const info = variantData.info;
        if (sampleInfo) {
            variantData.genotypeData = sampleInfo;
            const {
                info: sInfo = {},
                extendedAttributes = {}
            } = sampleInfo;
            for (const extended of [sInfo, extendedAttributes]) {
                for (const [key, value] of Object.entries(extended || {})) {
                    if (info.hasOwnProperty(key)) {
                        info[key].value = value;
                    } else {
                        info[key] = {value};
                    }
                }
            }
        }

        variantProperties.push({
            displayMode: this._constants.displayModes.wide,
            flex: wideFlex,
            title: 'Description',
            values: [
                `${this.chromosomeName
                    ? `${this.chromosomeName}: `
                    : ''
                }${variantData.startIndex} ${utilities.trimString(variantData.referenceAllele)} > ${utilities.trimString(variantData.alternativeAlleles[0])}`
            ]
        });
        if (variantData.hasOwnProperty('geneNames')) {
            if (variantData.geneNames.length === 1) {
                variantProperties.push({
                    displayMode: this._constants.displayModes.wide,
                    flex: wideFlex,
                    title: 'Gene',
                    values: variantData.geneNames
                });
            }
            else if (variantData.geneNames.length > 1) {

                variantProperties.push({
                    displayMode: this._constants.displayModes.wide,
                    flex: wideFlex,
                    title: 'Genes',
                    values: utilities.trimArray(variantData.geneNames, trimGenesMaxItems, trimGenesMaxLength)
                });
            }
        }
        variantProperties.push({
            displayMode: this._constants.displayModes.wide,
            flex: wideFlex,
            title: 'Location',
            values: [variantData.startIndex]
        });
        if (sample) {
            variantProperties.push({
                displayMode: this._constants.displayModes.common,
                flex: commonFlex,
                title: 'Sample',
                values: [sample]
            });
        }
        variantProperties.push({
            displayMode: this._constants.displayModes.common,
            flex: commonFlex,
            title: 'Quality',
            values: [variantData.quality]
        });

        for (const key in info) {
            if (info.hasOwnProperty(key) && info[key].type !== 'TABLE') {
                let values = [];
                if (Array.isArray(info[key].value)) {
                    values = [...utilities.trimArray(info[key].value)];
                }
                else {
                    values = [utilities.trimString(info[key].value)];
                }

                variantProperties.push({
                    description: info[key].description,
                    displayMode: this._constants.displayModes.common,
                    flex:  commonFlex,
                    title: key,
                    values: values,
                    type: info[key].type
                });

            }
        }

        const variantInfoTables = [];

        for (const key in info) {
            if (info.hasOwnProperty(key) && info[key].type === 'TABLE') {
                const values = info[key].value;
                const gridData = [];
                for (let i = 0; i < values.length; i++) {
                    const data = {};
                    for (let j = 0; j < info[key].header.length; j++) {
                        data[`header_${j}`] = values[i][j];
                    }
                    gridData.push(data);
                }

                const columnDefs = [];
                for (let i = 0; i < info[key].header.length; i++) {
                    columnDefs.push({
                        displayName: info[key].header[i],
                        field: `header_${i}`,
                        minWidth: 100
                    });
                }

                variantInfoTables.push({
                    description: info[key].description,
                    title: key,
                    values: {
                        columnDefs: columnDefs,
                        enableColumnMenus: false,
                        enableFiltering: false,
                        enableScrollbars: true,
                        data: gridData
                    },
                    type: info[key].type
                });
            }
        }
        this.isLoading = false;
        this.hasError = false;

        return {
            identifier: variantData.identifier,
            properties: variantProperties,
            tables: variantInfoTables,
            type: variantData.type
        };
    }

}
