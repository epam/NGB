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

    loadVariantSnpInfo(variantRequest, callback, errorCallback) {
        let ids = variantRequest.id.slice(variantRequest.id.indexOf('rs')).split(';');
        const rsId = ids[0];

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
        let snpProperties = [],
            variationExists = snpData.hasOwnProperty('variation');

        if(snpData.hasOwnProperty('organism_summary')) {
            snpProperties.push({
                title: 'Organism',
                value: snpData.organism_summary
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('genomeLabel')) {
                snpProperties.push({
                    title: 'Map to Genome Build',
                    value: snpData.variation.genomeLabel
                });
            }
            if (snpData.variation.hasOwnProperty('snp_class')) {
                snpProperties.push({
                    title: 'Variation Class',
                    value: snpData.variation.snp_class
                });
            }
        }
        if(snpData.hasOwnProperty('refsnp_alleles')) {
            snpProperties.push({
                title: 'RefSNP Alleles',
                value: snpData.refsnp_alleles
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('clinical_significance')) {
                snpProperties.push({
                    title: 'Clinical Significance',
                    value: snpData.variation.clinical_significance
                });
            }
            if(snpData.variation.hasOwnProperty('global_maf')) {
                snpProperties.push({
                    title: 'MAF/MinorAlleleCount',
                    value: snpData.variation.global_maf
                });
            }
        }
        if(snpData.hasOwnProperty('maf_source')) {
            snpProperties.push({
                title: 'MAF Source',
                value: snpData.maf_source
            });
        }
        if (variationExists && snpData.variation.hasOwnProperty('docsum')) {
            snpProperties.push({
                title: 'HGVS Names',
                value: snpData.variation.docsum
            });
        }

        if(variationExists) {
            let genomeLabelExists = snpData.variation.hasOwnProperty('genomeLabel'),
                chrExists = snpData.variation.hasOwnProperty('chr'),
                chrposExists = snpData.variation.hasOwnProperty('chrpos'),
                contigLabelExists = snpData.variation.hasOwnProperty('contigLabel'),
                contigposExists = snpData.variation.hasOwnProperty('contigpos');

            if (genomeLabelExists || chrExists || chrposExists || contigLabelExists || contigposExists) {
                snpProperties.push({
                    title: 'From table with variation location (Primary Assembly Mapping)',
                    value: ''
                });
            }

            if(genomeLabelExists) {
                snpProperties.push({
                    title: 'Assembly',
                    value: snpData.variation.genomeLabel
                });
            }
            if(chrExists) {
                snpProperties.push({
                    title: 'Chr',
                    value: snpData.variation.chr
                });
            }
            if(chrposExists) {
                snpProperties.push({
                    title: 'Chr Pos',
                    value: snpData.variation.chrpos
                });
            }
            if(contigLabelExists) {
                snpProperties.push({
                    title: 'Contig',
                    value: snpData.variation.contigLabel
                });
            }
            if(contigposExists) {
                snpProperties.push({
                    title: 'Contig Pos',
                    value: snpData.variation.contigpos
                });
            }
        }


        let ref_seq_geneExists = snpData.hasOwnProperty('ref_seq_gene'),
            genesExists = variationExists && snpData.variation.hasOwnProperty('genes');
        if (ref_seq_geneExists || genesExists) {
            snpProperties.push({
                title: 'From table RefSeq Gene Mapping',
                value: ''
            });
        }
        if (ref_seq_geneExists) {
            snpProperties.push({
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

                snpProperties.push({
                    title: 'Gene (ID)',
                    value: genesInfo
                });
            }
        }


        this.isLoading = false;
        this.hasError = false;


        return {
            snpProperties: snpProperties
        }
    }

}
