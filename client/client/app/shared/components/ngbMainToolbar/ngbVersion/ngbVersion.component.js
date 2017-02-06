export default {
    template: '<h3 class="md-title page-title to-right">{{::$ctrl.version}}</h3>',
    controller: function(othersDataService) {
        othersDataService.getVersion().then((version)=> {
            this.version = `${version}`;
        });
    }
};
