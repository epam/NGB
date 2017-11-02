import {drawingConfiguration} from '../../../../modules/render/core';
import {SVG} from '../../../shared/utils/svg';

export default class ngbTracksViewBookmarkCamera {
    getTitle;
    getTracks;

    constructor(getTitle, getTracks) {
        this.getTitle = getTitle;
        this.getTracks = getTracks;
    }

    saveBrowserView(type) {
        const tracks = this.getTracks();
        if (tracks && tracks.length > 0) {
            requestAnimationFrame(() => {
                const data = tracks
                    .map(track => {
                        if (track.instance && track.instance.domElement) {
                            const element = track.instance.domElement;
                            const imgData = track.instance.getImageData();
                            if (imgData) {
                                return {
                                    'name': track.name,
                                    'format': track.format,
                                    'width': element.clientWidth * drawingConfiguration.scale,
                                    'height': element.clientHeight * drawingConfiguration.scale,
                                    'img': imgData
                                };
                            }
                            return null;
                        }
                        return null;
                    })
                    .filter(x => x);

                if (type === 'png') {
                    this._downloadPNGView(data);
                }
                else {
                    this._downloadSVGView(data);
                }
            });
        }
    }

    _downloadPNGView(data) {
        if (data && data.length > 0) {

            const canvas = this._getPureCanvas(data);
            const ctx = canvas.getContext('2d');

            let y = 0;
            data.forEach(x => {
                if (x.name) {
                    y += 15;
                    ctx.fillStyle = '#000000';
                    ctx.fillText(`${x.format} ${x.name}`, 0, y);
                }
                if (x.img) {
                    ctx.drawImage(x.img, 0, y);
                    y += x.height;
                }
            });

            Object.assign(document.createElement('a'), {
                download: `${this.getTitle()}.png`,
                name: `${this.getTitle()}.png`,
                href: canvas.toDataURL('image/png').replace('image/png', 'image/octet-stream')
            }).click();
        }
    }

    _downloadSVGView(data) {
        if (data && data.length > 0) {
            const width = this._getCanvasWidth(data);
            const height = this._getCanvasHeight(data);

            let svg = new SVG(width, height);
            let y = 0;

            data.forEach(x => {
                if (x.name) {
                    y += 15;
                    svg.addText(`${x.format} ${x.name}`, 0, y - 3);
                }
                if (x.img) {
                    svg.addImage(x.img.width, x.img.height, x.img.toDataURL(), 0, y);
                    y += x.height;
                }
            });

            let url = svg.getUrl();

            Object.assign(document.createElement('a'), {
                download: `${this.getTitle()}.svg`,
                name: `${this.getTitle()}.svg`,
                href: url
            }).click();
        }
    }

    _getPureCanvas(data) {
        const width = this._getCanvasWidth(data);
        const height = this._getCanvasHeight(data);

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.globalAlpha = 1.0;
        ctx.textBaseline = 'bottom';
        ctx.font = '12px arial';

        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, width, height);

        return canvas;
    }

    _getCanvasWidth(data) {
        return data.map(el => el.width).reduce((pre, cur) => Math.max(pre, cur), 0);
    }

    _getCanvasHeight(data) {
        return data.reduce((sum, x) => {
            const textHeight = (x.name) ? 15 : 0;
            return sum + x.height + textHeight;
        }, 0);
    }
}