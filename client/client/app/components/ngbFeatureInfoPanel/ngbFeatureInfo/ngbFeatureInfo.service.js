import {TableInfo} from '../../../../modules/render/';
export default class ngbFeatureInfoService {

    static instance($q, externaldbDataService, ngbFeatureInfoConstant) {
        return new ngbFeatureInfoService($q, externaldbDataService, ngbFeatureInfoConstant);
    }

    constructor($q, externaldbDataService, ngbFeatureInfoConstant) {
        Object.assign(this, {$q, externaldbDataService, ngbFeatureInfoConstant});
    }

    getFeatureInfo(featureId, db) {
        switch (db) {
            case this.ngbFeatureInfoConstant.dbTypes.ncbi:
                return this.externaldbDataService.getNcbiGeneInfo(featureId).then(data => this._transformNcbiDataToSummaryProperties(data));
            case this.ngbFeatureInfoConstant.dbTypes.ensembl:
                return this.externaldbDataService.getEnsemblGeneInfo(featureId).then(data => this._transformEnsemblDataToSummaryProperties(data, featureId));
            case this.ngbFeatureInfoConstant.dbTypes.uniprot:
                return this.externaldbDataService.getUniprotGeneInfo(featureId).then(data => this._transformUniprotData(data));
            default:
                return this.$q.reject(`The application doesn't support ${db} database`);
        }
    }

    _transformNcbiDataToSummaryProperties(data){
        const knownAs = data.also_known_as ? data.also_known_as.join('; ') : '';
        const organism = data.organism_common ? `${data.organism_scientific} (${data.organism_common})` : data.organism_scientific;
        const source = data.primary_source_prefix ? `${data.primary_source_prefix}:${data.primary_source}` : data.primary_source;

        const summaryProperties = [];
        summaryProperties.push(['Official Symbol', data.official_symbol ]);
        summaryProperties.push(['Official Full Name', data.official_full_name]);
        summaryProperties.push(['Primary source', source]);
        summaryProperties.push(['Locus tag', data.locus_tag]);
        summaryProperties.push(['Gene type', data.gene_type]);
        summaryProperties.push(['RNA name', data.rna_name]);
        summaryProperties.push(['RefSeq status', data.ref_seq_status]);
        summaryProperties.push(['Organism', organism]);
        summaryProperties.push(['Lineage', data.lineage]);
        summaryProperties.push(['Also known as', knownAs ]);
        summaryProperties.push(['Summary', data.gene_summary ]);

        return {data, summaryProperties};
    }

    _transformEnsemblDataToSummaryProperties(data, featureId){
        const summaryProperties = [];
        summaryProperties.push(['Official Symbol', data.display_name ]);
        summaryProperties.push(['Description', data.description]);
        summaryProperties.push(['Ensembl version', `${featureId}.${data.version}`]);
        summaryProperties.push(['Gene type', data.biotype]);
        summaryProperties.push(['Species', data.species]);
        summaryProperties.push(['Source', data.source]);

        //summaryProperties.push(['CCDS', data.lineage]);
        //summaryProperties.push(['UniProtKB', organism]);
        //summaryProperties.push(['RefSeq', data.ref_seq_status]);
        //summaryProperties.push(['Also known as', knownAs ]);
        //summaryProperties.push(['About gene', data.gene_summary ]);
        data.gene_link = `http://www.ensembl.org/id/${featureId}`;

        return {summaryProperties, data};
    }

    _transformUniprotData(data){
        const table = new TableInfo(this.ngbFeatureInfoConstant.uniprotTableConfig, data);
        data.tableTitle = 'List of Proteins';

        return {tableProperties: table, data};

    }
}