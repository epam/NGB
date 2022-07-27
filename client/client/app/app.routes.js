import controller from './app.controller';

export default function routes($urlRouterProvider, $httpProvider, $stateProvider) {
    $httpProvider.interceptors.push('interceptor');

    $stateProvider
        .state('default', {
            controller: controller.UID,
            controllerAs:'$ctrl',
            params: {
                chromosome: {squash: true, value: null},
                end: {squash: true, value: null},
                referenceId: {squash: true, value: null},
                start: {squash: true, value: null},

                bookmark: null,
                layout: null,
                rewrite:null,
                screenshot: null,
                toolbar: null,
                tracks: null,
                filterByGenome: null,
                collapsedTrackHeaders: null,
                miew: null,
                heatmap: null,
                pathway: null,
                embedded: null,
                controls: null,
                auth: null,
                panels: null,
                hideLeftMenu: null
            },
            resolve: {
                $stateParams: '$stateParams'
            },
            reloadOnSearch: false,
            template: require('./app.tpl.html'),
            url: '/:referenceId/:chromosome/:start/:end?rewrite&bookmark&screenshot&toolbar&layout&tracks&filterByGenome&collapsedTrackHeaders&miew&heatmap&embedded&controls&pathway&auth&panels&hideLeftMenu'
        });
}
