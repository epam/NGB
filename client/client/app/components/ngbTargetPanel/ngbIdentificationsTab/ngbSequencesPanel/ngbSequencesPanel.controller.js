const DATA = [
    {
        name: 'GENOMIC',
        value: [
            {
                sequenceName: {
                    name: 'NC_051831.1',
                    url: 'url'
                },
                sequenceDescription: 'reference',
                ncbi: {
                    name: 'NCBI',
                    url: 'url'
                }
            }
        ]
    }, {
        name: 'mRNA',
        value: []
    }, {
        name: 'PROTEINS',
        value: [
            {
                sequenceName: {
                    name: 'XP_038294636.1',
                    url: 'url'
                },
                sequenceDescription: 'GTPase KRas isoform X1',
                ncbi: {
                    name: 'NCBI',
                    url: 'url'
                }
            },
            {
                sequenceName: {
                    name: 'XP_038294637.1',
                    url: 'url'
                },
                sequenceDescription: 'GTPase KRas isoform X2',
                ncbi: {
                    name: 'NCBI',
                    url: 'url'
                }
            },
            {
                sequenceName: {
                    name: 'XP_038294638.1',
                    url: 'url'
                },
                sequenceDescription: 'GTPase KRas isoform X3',
                ncbi: {
                    name: 'NCBI',
                    url: 'url'
                }
            }
        ]
    }
];

export default class ngbSequencesPanelController {
    static get UID() {
        return 'ngbSequencesPanelController';
    }

    constructor() {
        Object.assign(this, {});
        this.data = DATA;
    }
}
