import baseController from '../../shared/baseController';
import CytoscapeContext from '../../shared/cytoscapeContext';

export default class ngbStrainLineageController extends baseController {
    cytoscapeContext: CytoscapeContext = undefined;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
    };

    constructor(
        $scope,
        $mdMenu,
        dispatcher,
        ngbStrainLineageService,
        appLayout
    ) {
        super();

        Object.assign(
            this,
            {
                $scope,
                dispatcher,
                ngbStrainLineageService,
                appLayout
            }
        );

        this.initEvents();

        this.strainElements = {
            nodes: [
                {
                    data: {
                        id: 'n_1',
                        title: 'A3823.5',
                        desc: '(8/19/1965)'
                    }
                },
                {
                    data: {
                        id: 'n_2',
                        link: 'AA-104',
                        desc: '(2/22/1966)',
                        tooltip: 'Tooltip text'
                    }
                },
                {
                    data: {
                        id: 'n_3',
                        link: 'AD-299',
                        desc: '(6/7/1966)'
                    }
                },
                {
                    data: {
                        id: 'n_4',
                        link: 'AG-212',
                        desc: '(12/13/1966)'
                    }
                },
                {
                    data: {
                        id: 'n_5',
                        link: 'AM-229',
                        desc: '(6/14/1967)'
                    }
                },
                {
                    data: {
                        id: 'n_6',
                        link: 'AQ-71',
                        desc: '(12/13/1969)'
                    }
                },
                {
                    data: {
                        id: 'n_7',
                        title: '730.0',
                        link: 'OAP-149',
                        desc: '(11/13/1969)'
                    }
                },
                {
                    data: {
                        id: 'n_8',
                        title: '730.1',
                        link: 'PDQ-100',
                        desc: '(12/12/1970)'
                    }
                },
                {
                    data: {
                        id: 'n_9',
                        title: '730.2',
                        link: 'OCF-337',
                        desc: '(6/13/1972)'
                    }
                },
                {
                    data: {
                        id: 'n_10',
                        link: 'HS-148'
                    }
                },
                {
                    data: {
                        id: 'n_11',
                        title: '730.3',
                        link: 'OBC-131',
                        desc: '(4/5/1974)'
                    }
                },
                {
                    data: {
                        id: 'n_12',
                        title: '730.4',
                        link: 'OKZ-198',
                        desc: '(9/4/1974)'
                    }
                },
            ],
            edges: [
                {
                    data: {
                        id: 'e_1_2',
                        source: 'n_1',
                        target: 'n_2',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_2_3',
                        source: 'n_2',
                        target: 'n_3',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_3_4',
                        source: 'n_3',
                        target: 'n_4',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_4_5',
                        source: 'n_4',
                        target: 'n_5',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_5_6',
                        source: 'n_5',
                        target: 'n_6',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_5_7',
                        source: 'n_5',
                        target: 'n_7',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_6_8',
                        source: 'n_6',
                        target: 'n_8',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_8_9',
                        source: 'n_8',
                        target: 'n_9',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_8_10',
                        source: 'n_8',
                        target: 'n_10',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_10_11',
                        source: 'n_10',
                        target: 'n_11',
                        label: 'UV'
                    },
                },
                {
                    data: {
                        id: 'e_11_12',
                        source: 'n_11',
                        target: 'n_12',
                        label: 'UV'
                    },
                },
            ]
        };
    }

    static get UID() {
        return 'ngbStrainLineageController';
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.strainLineage.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }
}
