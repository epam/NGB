import {Converter} from 'showdown';
// todo: showdown-highlight brakes production build process. We need to upgrade either webpack or uglifyjs
// import showdownHighlight from 'showdown-highlight';
//
// import 'highlight.js/styles/github.css';

const converter = new Converter({
    extensions: []
});
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
