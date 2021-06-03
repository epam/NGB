export default class ngbBlastSearchAlignment {

    static get UID() {
        return 'ngbBlastSearchAlignment';
    }

    constructor() {
        this.initialize();
    }

    initialize() {
        switch(this.alignment.sequenceStrand) {
            case 'plus': {
                this.alignment.sequenceStrandView = '+';
                break;
            }
            case 'minus': {
                this.alignment.sequenceStrandView = '-';
                break;
            }
        }
    }

    calculateDiff(btop) {
        const splittedBtop = btop.match(/[\d]+|[^\d]+/g);
        let parsedItem, result = '';
        for(let i=0; i<splittedBtop.length; i++) {
            parsedItem = parseInt(splittedBtop[i]);
            if(isNaN(parsedItem)) {
                result += new Array(Math.ceil(splittedBtop[i].length/2) + 1).join(' ');
            } else {
                result += new Array(parsedItem + 1).join('|');
            }
        }
        return result;
    }
}
