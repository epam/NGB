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
            title: 'Sessions',
            name: 'layout>bookmark'
        },
        browser: {
            icon: 'video_label',
            panel: 'ngbBrowser',
            position: 'center',
            title: 'Browser',
            name: 'layout>browser'
        },
        dataSets: {
            icon: 'art_track',
            panel: 'ngbDataSets',
            position: 'right',
            title: 'Datasets',
            name: 'layout>dataSets'
        },
        filter: {
            displayed: false,
            icon: 'filter_list',
            panel: 'ngbFilterPanel',
            position: 'right',
            title: 'Filter',
            name: 'layout>filter'
        },
        molecularViewer: {
            displayed: false,
            icon: 'device_hub',
            panel: 'ngbMolecularViewer',
            position: 'right',
            title: 'Molecular Viewer',
            name: 'layout>molecularViewer'
        },
        variants: {
            icon: 'format_list_numbered',
            panel: 'ngbVariantsTablePanel',
            position: 'right',
            title: 'Variants',
            name: 'layout>variants'
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