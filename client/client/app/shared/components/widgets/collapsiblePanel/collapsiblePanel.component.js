export default {
    require: {
        collapsible: '^collapsible'
    },
    transclude: {
        title: 'collapsiblePanelTitle',
        body: 'collapsiblePanelContent'
    },
    bindings: {
        open: '=?isOpen',
        closeExpandOther: '<?',
        disableOthers: '<?'
    },
    controller: function() {
        this.$onInit = function() {
            this.collapsible.addPanel(this);
        };

        this.onClick = () => {
            this.open = !this.open;
            if (this.closeExpandOther) {
                this.collapsible.closeExpandOther(this);
            }
            if (this.disableOthers) {
                this.collapsible.disableEnableOther(this);
            }
        };
    },
    template: require('./collapsiblePanel.tpl.html')
};