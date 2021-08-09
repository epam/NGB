import * as PIXI from 'pixi.js-legacy';
import {drawingConfiguration} from '../../../../modules/render/core';

export default class ngbTracksViewBookmarkCamera {
    getTitle;
    getTracks;
    getTrackTitle;
    showOriginalName;

    constructor(getTitle, getTracks, getTrackTitle, showOriginalName) {
        this.getTitle = getTitle;
        this.getTracks = getTracks;
        this.getTrackTitle = getTrackTitle;
        this.showOriginalName = showOriginalName;
    }

    saveBrowserView() {
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
                                    'customName': this.getTrackTitle ? this.getTrackTitle(track) : undefined,
                                    'format': track.format,
                                    'height': element.clientHeight * drawingConfiguration.scale,
                                    'img': imgData,
                                    'name': track.name,
                                    'width': element.clientWidth * drawingConfiguration.scale
                                };
                            }
                            return null;
                        }
                        return null;
                    })
                    .filter(x => x);

                this._downloadView(data);
            });
        }
    }

    _downloadView(data) {
        if (data && data.length > 0) {

            const canvas = this._getPureCanvas(data);
            const ctx = canvas.getContext('2d');

            let y = 0;
            const margin = 5;
            data.forEach(x => {
                if (x.name) {
                    ctx.fillStyle = '#000000';
                    ctx.font = '17pt arial';
                    const name = x.customName || x.name;
                    const additional = x.customName && this.showOriginalName() ? ` (${x.name})` : '';
                    const text = `${x.format} ${name}${additional}`;
                    const {fontBoundingBoxAscent = 0, fontBoundingBoxDescent = 0} = ctx.measureText(text);
                    y += Math.max(15, fontBoundingBoxAscent + fontBoundingBoxDescent) + margin;
                    ctx.fillText(text, 0, y);
                    y += margin;
                }
                if (x.img) {
                    ctx.drawImage(x.img, 0, y);
                    y += x.height + margin;
                }
            });

            Object.assign(document.createElement('a'), {
                download: `${this.getTitle()}.png`,
                name: `${this.getTitle()}.png`,
                href: canvas.toDataURL('image/png').replace('image/png', 'image/octet-stream')
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
