export default  {
    Columns: {
        center: {
            width: 75
        },
        right: {
            width: 25
        }
    },
    Panels: {
        bookmark: {
            displayed: false,
            icon: 'bookmark_outline',
            panel: 'ngbBookmarksPanel',
            position: 'left',
            title: 'Sessions'
        },
        browser: {
            icon: 'video_label',
            panel: 'ngbBrowser',
            position: 'center',
            title: 'Browser'
        },
        dataSets: {
            icon: 'art_track',
            panel: 'ngbDataSets',
            position: 'right',
            title: 'Datasets'
        },
        filter: {
            displayed: false,
            icon: 'filter_list',
            panel: 'ngbFilterPanel',
            position: 'right',
            title: 'Filter'
        },
        molecularViewer: {
            displayed: false,
            icon: 'device_hub',
            panel: 'ngbMolecularViewer',
            position: 'right',
            title: 'Molecular Viewer'
        },
        variants: {
            icon: 'format_list_numbered',
            panel: 'ngbVariantsTablePanel',
            position: 'right',
            title: 'Variants'
        },
        ...(() => {
            const devPanels = {
                DevLog: {
                    displayed: false,
                    icon: 'toys',
                    panel: 'ngbLog',
                    position: 'left',
                    title: 'Dev Log'
                }
            };

            return __DEV__ && devPanels;
        })()

    }
};