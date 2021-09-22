import {Converter} from 'showdown';

const converter = new Converter();
converter.setFlavor('github');

export default {
    restrict: 'E',
    bindings: {
        markdown: '<'
    },
    controllerAs: '$ctrl',
    template: require('./ngbMarkdown.tpl.html'),
    controller: function ($sce, $scope) {
        const build = () => {
            this.html = $sce.trustAsHtml(converter.makeHtml(this.markdown || ''));
        };
        build();
        $scope.$watch('$ctrl.markdown', build);
    }
};
