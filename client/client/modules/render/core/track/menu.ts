import Tether from 'tether';
import angular from 'angular';

const menuElementContainer:HTMLElement = document.body;

const defaultStyle = `
width: auto;
height: auto;
z-index: 1000;
display:block;
background-color: white;`;

const defaultTetherOpts = {
    attachment: 'top left',
    constraints: [
        {
            attachment: 'together',
            to: 'window'
        }
    ],
    targetAttachment: 'top left',
    targetOffset: '',
};

export default function menuFactory(track) {
    const menuElement = document.createElement('track-menu');
    menuElement.setAttribute('style', defaultStyle);
    const opts = {
        ...defaultTetherOpts,
        element: menuElement,
        target: track,
    };
    const tether = new Tether(opts);
    tether.opts = opts;

    const destructor = function () {
        menuElementContainer.removeChild(menuElement);
    };

    const hide = function () {
        menuElement.innerHTML = '';
    };


    return {
        isShown: false,
        show(position, content) {

            if (!this.isShown)
                menuElementContainer.appendChild(menuElement);
            const pxOffset = 50;
            const targetOffset = `${position.y - pxOffset}px ${position.x - pxOffset}px`;
            tether.setOptions({...tether.opts, targetOffset});

            this.isShown = true;
            menuElement.innerHTML = '';
            angular.element(menuElement).append(content);
            content.on('mouseleave', hide);
        },
        ...hide,
        ...destructor
    };
}


