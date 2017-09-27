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
                    callback(this._mapVariantSnpData(data, rsId));
                },
                () => {
                    errorCallback(this._constants.errorMessages.errorLoadingVariantSnpInfo);
                });

    }

    _mapVariantSnpData(snpData, rsId) {
        let snpCollapsiblePanels = [],
            variationExists = snpData.hasOwnProperty('variation');

        let snpHref = `https://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=${rsId.slice(2)}`;//Snp Link
        snpCollapsiblePanels.push({
            title: 'Summary',
            isOpen: true,
            snpHref: snpHref,
            rsId: rsId,
            values: []
        });

        if(snpData.hasOwnProperty('organism_summary')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'Organism',
                values: [snpData.organism_summary]
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('genomeLabel')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Map to Genome Build',
                    values: [snpData.variation.genomeLabel]
                });
            }
            if (snpData.variation.hasOwnProperty('snp_class')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Variation Class',
                    values: [snpData.variation.snp_class]
                });
            }
        }
        if(snpData.hasOwnProperty('refsnp_alleles')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'RefSNP Alleles',
                values: [snpData.refsnp_alleles]
            });
        }
        if (variationExists) {
            if (snpData.variation.hasOwnProperty('clinical_significance')) {

                let clinicalSignificance = {
                    title: 'Clinical Significance',
                    values: [snpData.variation.clinical_significance]
                };

                if(snpData.hasOwnProperty('clinVar') && snpData.clinVar.hasOwnProperty('clinvar_link') && snpData.clinVar.clinvar_link) {
                    clinicalSignificance.hasLink = true;
                    clinicalSignificance.linkHref = snpData.clinVar.clinvar_link;//ClinVar Link
                    clinicalSignificance.linkText = '[ClinVar]';
                }
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push(clinicalSignificance);
            }

            if(snpData.variation.hasOwnProperty('global_maf')) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'MAF/MinorAlleleCount',
                    values: [snpData.variation.global_maf]
                });
            }
        }
        if(snpData.hasOwnProperty('maf_source')) {
            snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                title: 'MAF Source',
                values: [snpData.maf_source]
            });
        }
        if (variationExists && snpData.variation.hasOwnProperty('docsum')) {
            let hgvsIndex = snpData.variation.docsum.indexOf('HGVS=');
            if (hgvsIndex + 1) {
                let hgvs = snpData.variation.docsum.slice(hgvsIndex)
                                                    .split(/\|/, 1)[0]
                                                    .slice(5),
                    tmpEl = document.createElement('div');

                tmpEl.innerHTML = hgvs;
                hgvs = tmpEl.childNodes[0].nodeValue;

                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'HGVS Names',
                    values: hgvs.split(/,/)
                });
            }
        }

        if(variationExists) {
            let genomeLabelExists = snpData.variation.hasOwnProperty('genomeLabel'),
                chrExists = snpData.variation.hasOwnProperty('chr'),
                chrposExists = snpData.variation.hasOwnProperty('chrpos'),
                contigLabelExists = snpData.variation.hasOwnProperty('contigLabel'),
                contigposExists = snpData.variation.hasOwnProperty('contigpos');

            if (genomeLabelExists || chrExists || chrposExists || contigLabelExists || contigposExists) {
                snpCollapsiblePanels.push({
                    title: 'Primary Assembly Mapping',
                    isOpen: false,
                    values: []
                });
            }

            if(genomeLabelExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Assembly',
                    values: [snpData.variation.genomeLabel]
                });
            }
            if(chrExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Chr',
                    values: [snpData.variation.chr]
                });
            }
            if(chrposExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Chr Pos',
                    values: [snpData.variation.chrpos]
                });
            }
            if(contigLabelExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Contig',
                    values: [snpData.variation.contigLabel]
                });
            }
            if(contigposExists) {
                snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                    title: 'Contig Pos',
                    values: [snpData.variation.contigpos]
                });
            }
        }


        if (snpData.hasOwnProperty('ref_seq_gene_mapping')) {
            let refSeqGeneExists = snpData.ref_seq_gene_mapping.hasOwnProperty('refSeqGene'),
                geneExists = snpData.ref_seq_gene_mapping.hasOwnProperty('gene'),
                positionExists = snpData.ref_seq_gene_mapping.hasOwnProperty('position'),
                alleleExists = snpData.ref_seq_gene_mapping.hasOwnProperty('allele');

            if (refSeqGeneExists || geneExists || positionExists || alleleExists) {
                snpCollapsiblePanels.push({
                    title: 'RefSeq Gene Mapping',
                    isOpen: false,
                    values: []
                });
                if (refSeqGeneExists) {
                    snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                        title: 'RefSeqGene',
                        values: [snpData.ref_seq_gene_mapping.refSeqGene]
                    });
                }
                if (geneExists) {
                    snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                        title: 'Gene:ID',
                        values: [snpData.ref_seq_gene_mapping.gene]
                    });
                }
                if (positionExists) {
                    snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                        title: 'Position',
                        values: [snpData.ref_seq_gene_mapping.position]
                    });
                }
                if (alleleExists) {
                    snpCollapsiblePanels[snpCollapsiblePanels.length - 1].values.push({
                        title: 'Allele',
                        values: [snpData.ref_seq_gene_mapping.allele]
                    });
                }
            }
        }


        this.isLoading = false;
        this.hasError = false;


        return {
            snpCollapsiblePanels: snpCollapsiblePanels
        }
    }

}
