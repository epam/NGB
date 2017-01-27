import * as cellTemplates from './ngbInfoTable/cellTemplateUrls';

export default   {
    ErrorMessage: {
        NoDataAvailable: 'No Data Available'
    },
    dbTypes: {
        ensembl: 'ensembl',
        file: 'gff',
        ncbi: 'ncbi',
        uniprot: 'uniprot'
    },
    uniprotTableConfig: [
        {
            data: {
                field: 'accession',
                linkField: 'url'
            },
            title: 'Entry name',
            templateUrl: cellTemplates.linkCell
        },
        {
            data: {
                field: 'proteinNames'
            },
            title: 'Protein names',
            templateUrl: cellTemplates.defaultCell
        },
        {
            data: {
                field: 'organismName'
            },
            title: 'Organism',
            templateUrl: cellTemplates.defaultCell
        },
        {
            data: {
                field: 'status'
            },
            title: 'Status',
            templateUrl: cellTemplates.defaultCell
        },
        {
            data: {
                field: 'function'
            },
            title: 'Function',
            templateUrl: cellTemplates.defaultCell
        }
    ]
};