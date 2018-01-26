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
            position: variantRequest.position
        };

        this._genomeDataService.loadChromosome(variantRequest.chromosomeId)
            .then(
                (chr) => {
                    this.chromosomeName = chr.name;
                    this._dataService.getVariantInfo(request)
                        .then(
                            (data) => {
                                callback(this._mapVariantPropertiesData(data));
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

    _mapVariantPropertiesData(variantData){
        const variantProperties = [];
        const wideFlex = 100;
        const commonFlex = 50;
        const trimGenesMaxItems = 5;
        const trimGenesMaxLength = 30;

        variantProperties.push({
            displayMode: this._constants.displayModes.wide,
            flex: wideFlex,
            title: 'Description',
            values: [
                `${this.chromosomeName
                    ? `${this.chromosomeName}: `
                    : ''
                }${variantData.startIndex} ${variantData.referenceAllele} > ${variantData.alternativeAlleles[0]}`
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
        variantProperties.push({
            displayMode: this._constants.displayModes.common,
            flex: commonFlex,
            title: 'Quality',
            values: [variantData.quality]
        });

        for (const key in variantData.info) {
            if (variantData.info.hasOwnProperty(key) && variantData.info[key].type !== 'TABLE') {
                let values = [];
                if (Array.isArray(variantData.info[key].value)) {
                    values = [...utilities.trimArray(variantData.info[key].value)];
                }
                else {
                    values = [utilities.trimString(variantData.info[key].value)];
                }

                variantProperties.push({
                    description: variantData.info[key].description,
                    displayMode: this._constants.displayModes.common,
                    flex:  commonFlex,
                    title: key,
                    values: values,
                    type: variantData.info[key].type
                });

            }
        }

        const variantInfoTables = [];

        for (const key in variantData.info) {
            if (variantData.info.hasOwnProperty(key) && variantData.info[key].type === 'TABLE') {
                const values = variantData.info[key].value;
                const gridData = [];
                for (let i = 0; i < values.length; i++) {
                    const data = {};
                    for (let j = 0; j < variantData.info[key].header.length; j++) {
                        data[`header_${j}`] = values[i][j];
                    }
                    gridData.push(data);
                }

                const columnDefs = [];
                for (let i = 0; i < variantData.info[key].header.length; i++) {
                    columnDefs.push({
                        displayName: variantData.info[key].header[i],
                        field: `header_${i}`,
                        minWidth: 100
                    });
                }

                variantInfoTables.push({
                    description: variantData.info[key].description,
                    title: key,
                    values: {
                        columnDefs: columnDefs,
                        enableColumnMenus: false,
                        enableFiltering: false,
                        enableScrollbars: true,
                        data: gridData
                    },
                    type: variantData.info[key].type
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
