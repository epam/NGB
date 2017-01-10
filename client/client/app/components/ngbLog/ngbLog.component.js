export template from './ngbLog.html';
export function controller(dispatcher, ngbLogService, $scope, $mdColors, $mdColorUtil) {

    const log = (fireEvent) => {
        ngbLogService.writeToLog(
            `LOG: ${fireEvent.name} -  state: ${JSON.stringify(fireEvent.state)}`);
    };
    const hub = (fireEvent) => {
        ngbLogService.writeToLog(
            `HUB: ${fireEvent.name} -  state: ${JSON.stringify(fireEvent.state)}`);
    };

    const error = (error) => {
        ngbLogService.writeToLog(
            `ERROR ${error.message} from '${error.source}'`);
    };

    dispatcher.on('log', log);
    //  dispatcher.on('eventHub', hub);
    dispatcher.on('error', error);

    $scope.$on('$destroy', () => {
        dispatcher.removeListener('error', error);
        dispatcher.removeListener('log', log);
        dispatcher.removeListener('eventHub', hub);
    });

    this.getEventNames = () => {

        const keys = dispatcher.eventsCollection();
        this.events = keys.map((key) => {
            const len = dispatcher._events[key].length;
            return {key, len};
        });

    };
    this.getState = () => {
        this.state = dispatcher.state;
        return dispatcher.state ? Object.keys(dispatcher.state) : [];
    };
    this.getMessage = () => ngbLogService.logMessage;
    this.getMessageHub = () => ngbLogService.logMessageHub;

    this.clearLog = () => {
        ngbLogService.clearLog();
    };

    this.colors =['50','100','200','300','400','500','600','700','800','900','A100','A200','A400','A700'];

    this.getColor = (color) => {
        const colorRgb = $mdColors.getThemeColor(color);
        return $mdColorUtil.rgbaToHex(colorRgb);
    };
}