const PICKER_WIDTH = 170;
const PICKER_HEIGHT = 150;
const PICKER_MARGIN = 5;
const NGB_COLOR_PICKER_OVERLAY_ID = 'ngb-color-picker-overlay';

export default class ngbColorPickerController {
    static get UID() {
        return 'ngbColorPickerController';
    }
    constructor($element, $scope, $compile) {
        this.opened = false;
        this.element = $element[0];
        this.overlay = this.ensureOverlay();
        this.createPickerCall = () => this.createPicker($scope, $compile);
        if (this.overlay) {
            this.handleOverlayClick = (e) => {
                if (e) {
                    e.stopPropagation();
                }
                this.hide();
            };
            const handleResize = () => this.correctPosition();
            this.overlay.addEventListener('click', this.handleOverlayClick);
            this.overlay.addEventListener('scroll', this.handleOverlayClick);
            window.addEventListener('resize', handleResize);
            $scope.$on('$destroy', () => {
                if (this.pickerContainer && this.overlay) {
                    this.overlay.removeChild(this.pickerContainer);
                }
                this.overlay.removeEventListener('click', this.handleOverlayClick);
                this.overlay.removeEventListener('scroll', this.handleOverlayClick);
                window.removeEventListener('resize', handleResize);
            });
        }
    }

    hide() {
        this.hidePickerContainer();
        this.hideOverlay();
    }

    correctPosition(event) {
        const target = event ? event.target : this.element;
        if (!target) {
            return;
        }
        const {
            x,
            y,
            width: targetWidth,
            height: targetHeight
        } = target.getBoundingClientRect();
        const cX = x + targetWidth / 2.0;
        const width = document.body.clientWidth;
        const height = document.body.clientHeight;
        let pickerX = cX;
        let pickerY = y + targetHeight + PICKER_MARGIN;
        if (pickerY + PICKER_HEIGHT >= height) {
            pickerY = Math.max(0, y - PICKER_HEIGHT - PICKER_MARGIN);
        }
        if (pickerX + PICKER_WIDTH >= width) {
            pickerX = Math.max(0, x - PICKER_WIDTH - PICKER_MARGIN);
        }
        this.createPickerCall();
        if (this.pickerContainer) {
            this.pickerContainer.style.top = `${pickerY}px`;
            this.pickerContainer.style.left = `${pickerX}px`;
        }
    }

    show(event) {
        this.correctPosition(event);
        this.showPickerContainer();
        this.showOverlay();
    }

    hideElement(element) {
        if (element && !element.classList.contains('hidden')) {
            element.classList.add('hidden');
        }
    }

    showElement(element) {
        if (element && element.classList.contains('hidden')) {
            element.classList.remove('hidden');
        }
    }

    hideOverlay() {
        this.hideElement(this.overlay);
    }

    hidePickerContainer() {
        this.createPickerCall();
        this.hideElement(this.pickerContainer);
    }

    showOverlay() {
        this.showElement(this.overlay);
    }

    showPickerContainer() {
        this.createPickerCall();
        this.showElement(this.pickerContainer);
    }

    ensureOverlay() {
        let element = document.getElementById(NGB_COLOR_PICKER_OVERLAY_ID);
        if (!element) {
            element = document.createElement('div');
            element.id = NGB_COLOR_PICKER_OVERLAY_ID;
            element.classList.add('ngb-color-picker-overlay');
            element.classList.add('hidden');
            document.body.appendChild(element);
        }
        return element;
    }

    createPicker(scope, compile) {
        if (this.overlay && !this.pickerContainer) {
            this.pickerContainer = document.createElement('div');
            this.pickerContainer.classList.add('ngb-color-picker-container');
            this.pickerContainer.classList.add('hidden');
            this.pickerContainer.style.width = `${PICKER_WIDTH}px`;
            this.pickerContainer.style.height = `${PICKER_HEIGHT}px`;
            this.pickerContainer.addEventListener('click', e => e.stopPropagation());
            this.overlay.appendChild(this.pickerContainer);
            const pickerScope = scope.$new();
            pickerScope.options = {...(this.options || {}), inline: true, swatch: false};
            pickerScope.ngCloak = this.ngCloak;
            pickerScope.color = this.color;
            pickerScope.$watch('color', (o) => this.color = o);
            const picker = compile(
                '<color-picker ng-cloak="ngCloak" ng-model="color" options="options"></color-picker>'
            )(pickerScope);
            if (picker && picker[0]) {
                this.pickerContainer.appendChild(picker[0]);
            }
        }
        return undefined;
    }
}
