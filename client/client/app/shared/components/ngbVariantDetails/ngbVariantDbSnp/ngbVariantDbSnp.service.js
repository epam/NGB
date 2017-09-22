// import {utilities} from '../utilities';

export default class ngbVariantDbSnpService{

    static instance(dispatcher, constants, externaldbDataService) {
        return new ngbVariantDbSnpService(dispatcher, constants, externaldbDataService);
    }

    _constants;
    _dataService;
    _dispatcher;

    constructor(dispatcher, constants, dataService){
        this._dataService = dataService;
        this._constants = constants;
        this._dispatcher = dispatcher;
    }

    loadVariantSnpInfo(rsId, callback, errorCallback) {
        this._dataService.getNcbiVariationInfo(rsId)
            .then(
                (data) => {
                    callback(this._mapVariantSnpData(data));
                },
                () => {
                    errorCallback(this._constants.errorMessages.errorLoadingVariantSnpInfo);
                });

    }

    _mapVariantSnpData(snpData) {
        let snpCollapsiblePanels = [],
            variationExists = snpData.hasOwnProperty('variation');

        snpCollapsiblePanels.push({
            title: 'Summary',
            isOpen: true,
            values: []
        });

        if(snpData.hasOwnProperty('organism_summary')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'Organism',
                value: snpData.organism_summary
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('genomeLabel')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Map to Genome Build',
                    value: snpData.variation.genomeLabel
                });
            }
            if (snpData.variation.hasOwnProperty('snp_class')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Variation Class',
                    value: snpData.variation.snp_class
                });
            }
        }
        if(snpData.hasOwnProperty('refsnp_alleles')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'RefSNP Alleles',
                value: snpData.refsnp_alleles
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('clinical_significance')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Clinical Significance',
                    value: snpData.variation.clinical_significance
                });
            }
            if(snpData.variation.hasOwnProperty('global_maf')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'MAF/MinorAlleleCount',
                    value: snpData.variation.global_maf
                });
            }
        }
        if(snpData.hasOwnProperty('maf_source')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'MAF Source',
                value: snpData.maf_source
            });
        }
        if (variationExists && snpData.variation.hasOwnProperty('docsum')) {
            let hgvs = snpData.variation.docsum.slice(snpData.variation.docsum.indexOf('HGVS='))
                                               .split(/\|/, 1)[0]
                                               .slice(5),
                tmpEl = document.createElement('div');

            tmpEl.innerHTML = hgvs;
            hgvs = tmpEl.childNodes[0].nodeValue;

            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'HGVS Names',
                valueType: 'array',
                value: hgvs.split(/,/)
            });
        }

        if(variationExists) {
            let genomeLabelExists = snpData.variation.hasOwnProperty('genomeLabel'),
                chrExists = snpData.variation.hasOwnProperty('chr'),
                chrposExists = snpData.variation.hasOwnProperty('chrpos'),
                contigLabelExists = snpData.variation.hasOwnProperty('contigLabel'),
                contigposExists = snpData.variation.hasOwnProperty('contigpos');

            if (genomeLabelExists || chrExists || chrposExists || contigLabelExists || contigposExists) {
                snpCollapsiblePanels.push({
                    title: 'From table with variation location (Primary Assembly Mapping)',
                    isOpen: false,
                    values: []
                });
            }

            if(genomeLabelExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Assembly',
                    value: snpData.variation.genomeLabel
                });
            }
            if(chrExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Chr',
                    value: snpData.variation.chr
                });
            }
            if(chrposExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Chr Pos',
                    value: snpData.variation.chrpos
                });
            }
            if(contigLabelExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Contig',
                    value: snpData.variation.contigLabel
                });
            }
            if(contigposExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Contig Pos',
                    value: snpData.variation.contigpos
                });
            }
        }


        let ref_seq_geneExists = snpData.hasOwnProperty('ref_seq_gene'),
            genesExists = variationExists && snpData.variation.hasOwnProperty('genes');
        if (ref_seq_geneExists || genesExists) {
            snpCollapsiblePanels.push({
                title: 'From table RefSeq Gene Mapping',
                isOpen: false,
                values: []
            });
        }
        if (ref_seq_geneExists) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'RefSeqGene',
                value: snpData.ref_seq_gene
            });
        }
        if (genesExists) {
            if (snpData.variation.genes.length > 0) {
                let genesInfo = '';
                for (let i = 0; i < snpData.variation.genes.length; i++) {
                    genesInfo += snpData.variation.genes[i].name + ' (' + snpData.variation.genes[i].gene_id + ')';
                    if (!(i + 1 === snpData.variation.genes.length || snpData.variation.genes.length === 1)) {
                        genesInfo += '; ';
                    }
                }

                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Gene (ID)',
                    value: genesInfo
                });
            }
        }


        this.isLoading = false;
        this.hasError = false;


        return {
            snpCollapsiblePanels: snpCollapsiblePanels
        }
    }

}
