export default class ngbTrackMenuController{

    static get UID(){
        return 'ngbTrackSettingsController';
    }   

    settings;


    constructor($scope, lastActionRepeater){
        this.lastActionRepeater = lastActionRepeater;
        const hotkeyPressedFn = () => $scope.$apply();
        const dispatcher = this.lastActionRepeater.dispatcher;
        dispatcher.on('hotkeyPressed', hotkeyPressedFn);
        $scope.$on('$destroy', ()=> {
            dispatcher.removeListener('hotkeyPressed', hotkeyPressedFn);
        });
    }
    
    handleField(e, field) {
        e.preventDefault();
        e.stopPropagation();
        field.isEnabled() ? field.disable() : field.enable();
        this.lastActionRepeater.rememberAction(field.name);
        return false;
    }

    handleButtonField(e, field){
        e.preventDefault();
        e.stopPropagation();
        field.perform();
        this.lastActionRepeater.rememberAction(field.name);
        return false;
    }
}
