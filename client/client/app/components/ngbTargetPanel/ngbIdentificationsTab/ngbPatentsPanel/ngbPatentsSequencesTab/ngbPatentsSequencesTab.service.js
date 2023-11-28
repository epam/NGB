export default class ngbPatentsSequencesTabService {

    _loadingProteins = false;
    proteins = [];
    _selectedProtein;

    get loadingProteins() {
        return this._loadingProteins;
    }
    set loadingProteins(value) {
        this._loadingProteins = value;
    }
    get selectedProtein() {
        return this._selectedProtein;
    }
    set selectedProtein(value) {
        this._selectedProtein = value;
    }

    static instance (dispatcher, ngbSequencesPanelService) {
        return new ngbPatentsSequencesTabService(dispatcher, ngbSequencesPanelService);
    }

    constructor(dispatcher, ngbSequencesPanelService) {
        Object.assign(this, {dispatcher, ngbSequencesPanelService});
        dispatcher.on('target:identification:sequences:updated', this.setProteins.bind(this));
    }

    setProteins(sequences) {
        this.loadingProteins = true;
        this.proteins = sequences.reduce((acc, curr) => {
            if (curr.sequences && curr.sequences.length) {
                for (let i = 0; i < curr.sequences.length; i++) {
                    const protein = curr.sequences[i].protein;
                    if (protein) {
                        acc.push(protein);
                    }
                }
            }
            return acc;
        }, []);
        this.selectedProtein = this.proteins[0];
        this.loadingProteins = false;
        this.dispatcher.emit('target:identification:patents:sequences:proteins:updated');
    }
}
