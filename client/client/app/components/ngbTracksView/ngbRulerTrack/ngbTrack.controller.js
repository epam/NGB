import {tracks as trackConstructors} from '../../../../modules/render/';

export default class ngbTrackController {
    static instanceCache = [];
    domElement = null;
    dispatcher = null;
    projectContext;

    static get UID() {
        return 'ngbRulerTrackController';
    }

    isLoaded = false;

    get height() { return this.domElement ? this.domElement.offsetHeight : 0; }

    instanceConstructor = trackConstructors.Ruler;

    constructor($scope, $element, dispatcher, projectContext) {
        this.dispatcher = dispatcher;
        this.domElement = $element[0];
        this.projectContext = projectContext;

        this.domElement.style.position = 'relative';
        this.domElement.style.top = '0';

        this.possibleTrackHeight =
            this.instanceConstructor.config instanceof Object
            && Number.isFinite(this.instanceConstructor.config.height)
                ? this.instanceConstructor.config.height
                : 40;

        this.loadTrackInstance();

        $scope.$on('$destroy', () => {
            if (this.trackInstance) {
                this.trackInstance.destructor();
            }
            if (this.trackInstance && this.trackInstance.domElement && this.trackInstance.domElement.parentNode) {
                this.trackInstance.domElement.parentNode.removeChild(this.trackInstance.domElement);
            }
        });
    }

    loadTrackInstance() {
        this.trackInstance = new this.instanceConstructor({
            viewport: this.viewport,
            ...this.trackOpts,
            ...this.track,
            projectContext: this.projectContext,
            silentInteractions: this.silentInteractions
        });
        ngbTrackController.instanceCache.push(this.trackInstance);

        this.track.height = this.trackInstance.height;
        this.track.instance = this.trackInstance;

        if (this.trackInstance && this.trackInstance.domElement) {
            this.domElement.querySelector('.js-ngb-render-container-target')
                .appendChild(this.trackInstance.domElement);
        }

        this.isLoaded = true;
    }
}
