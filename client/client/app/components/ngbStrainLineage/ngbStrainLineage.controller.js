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
                        label: 'A3823.5'
                    }
                },
                {
                    data: {
                        id: 'n_2',
                        label: 'AA-104'
                    }
                },
                {
                    data: {
                        id: 'n_3',
                        label: 'AD-299'
                    }
                },
                {
                    data: {
                        id: 'n_4',
                        label: 'AG-212'
                    }
                },
                {
                    data: {
                        id: 'n_5',
                        label: 'AM-229'
                    }
                },
                {
                    data: {
                        id: 'n_6',
                        label: 'AQ-71'
                    }
                },
                {
                    data: {
                        id: 'n_7',
                        label: 'OAP-149'
                    }
                },
                {
                    data: {
                        id: 'n_8',
                        label: 'PDQ-100'
                    }
                },
                {
                    data: {
                        id: 'n_9',
                        label: 'OCF-337'
                    }
                },
                {
                    data: {
                        id: 'n_10',
                        label: 'HS-148'
                    }
                },
                {
                    data: {
                        id: 'n_11',
                        label: 'OBC-131'
                    }
                },
                {
                    data: {
                        id: 'n_12',
                        label: 'OKZ-198'
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
