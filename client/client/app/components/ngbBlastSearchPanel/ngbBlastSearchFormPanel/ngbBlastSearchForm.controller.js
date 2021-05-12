import $ from 'jquery';

export default class ngbBlastSearchFormController {
    static get UID(){
        return 'ngbBlastSearchFormController';
    }
    blastTools = [
        'blastn',
        'blastp',
        'blastx',
        'tblastn',
        'tblastx',
    ];
    algorithms = {
        blastn: ['megablast', 'discontiguous megablast','blastn'],
        blastp:['blastp','quick BLASTP', 'PSI-BLAST', 'PHI-BLAST', 'DELTA-BLAST']
    }
    setVariants = [
        'custom',
        'db'
    ];
    readSequence = '';
    speciesList = [];
    selectedSet = this.setVariants[0];
    selectedOrganisms = [];
    task = '';
    tool = '';
    algorithm = '';
    formElement = null;
    searchRequest = null;

    constructor($element, ngbBlastSearchService){
        this.ngbBlastSearchService = ngbBlastSearchService;
        this.speciesList = this.ngbBlastSearchService.generateSpeciesList();
        this.readSequence =  this.sequence;
        this.formElement = $($element[0]).find('.md-inline-form')[0];
    }
    setDefaultAlgorithms(){
        this.algorithm = this.algorithms[this.tool] ? this.algorithms[this.tool][0] : '';
    }
    onSearch(){
        this.searchRequest = {
            alg: this.algorithm,
            sequence: this.readSequence,
            set: this.selectedSet,
            species: this.selectedOrganisms,
            task: this.task,
            tool: this.tool,  
        };
        this.resetForm();
    }
    resetForm(){
        this.speciesList = [];
        this.selectedSet = this.setVariants[0];
        this.selectedOrganisms = [];
        this.tool = '';
        this.algorithm = '';
        if (this.formElement){
            this.formElement.reset();
        }
    }
}
