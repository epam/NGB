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
                projectId: {squash: true, value: null},
                start: {squash: true, value: null},

                bookmark: null,
                layout: null,
                rewrite:null,
                screenshot: null,
                toolbar: null,
                tracks: null
            },
            resolve: {
                $stateParams: '$stateParams'
            },
            template: require('./app.tpl.html'),
            url: '/:projectId/:chromosome/:start/:end?rewrite&bookmark&screenshot&toolbar&layout&tracks'
        });

}
