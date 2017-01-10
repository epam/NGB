export default class BaseController {

    dispatcher;

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
    }


    initEvents(){

        for (const eventName in this.events) {
            if (this.events.hasOwnProperty(eventName)) {
                this.dispatcher.on(eventName, this.events[eventName]);
            }
        }

        this.$scope.$on('$destroy', () => {
            if (this.events) {
                for (const eventName in this.events) {
                    if (this.events.hasOwnProperty(eventName)) {
                        this.dispatcher.removeListener(eventName, this.events[eventName]);
                    }
                }
            }
        });
    }

}