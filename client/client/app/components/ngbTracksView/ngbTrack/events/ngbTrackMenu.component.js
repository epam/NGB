export default {
    bindings: {
        menuData: '='
    },
    controller(dispatcher, projectContext, $mdMenu){  
        this.menuData.forEach(item => {
            if(item.clipboard && item.fn){
                item.fn(item);
            }
        });

        this.clickItem = ($event, entry)=> {
            if (entry.events) {
                for (let i = 0; i < entry.events.length; i++) {
                    dispatcher.emitSimpleEvent(entry.events[i].name, entry.events[i].data);
                }
            } else if (entry.state) {
                projectContext.changeState(entry.state);
            }
            this.closeItem();
        };
        this.closeItem = () => {
            this.isClicked = true;
        };
        this.closeOthers = () => {
            $mdMenu.hide(null, { closeAll: true });
        };
    },
    template: require('./ngbTrackMenu.tpl.html')
};


