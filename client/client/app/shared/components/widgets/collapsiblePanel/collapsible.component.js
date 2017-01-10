export default {
    transclude: true,
    controller: function() {
        this.allPanels = [];

        this.addPanel = function(panel) {
            this.allPanels.push(panel);
        };

        this.closeExpandOther = (panel) => {
            this.allPanels.forEach((pn) => {
                if(pn !== panel) {
                    panel.open ? pn.open = false : pn.open = true;
                }
            });
        };

        this.disableEnableOther = (panel) => {
            this.allPanels.forEach((pn) => {
                if(pn !== panel) {
                    panel.open ? pn.disabled = true : pn.disabled = false;
                }
            });
        };
    },
    template: '<div ng-transclude></div>'
};