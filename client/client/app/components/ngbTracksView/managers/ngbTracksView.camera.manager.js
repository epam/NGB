import {drawingConfiguration} from '../../../../modules/render/core';

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

                if(type === 'png') {
                    this._downloadPNGView(data);
                }
                else {
                    this._downloadSVGView(data);
                }
            });
        }
    }

    _downloadPNGView(data) {
        debugger;
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
        debugger;
        if (data && data.length > 0) {
            const width = this._getCanvasWidth(data);
            const height = this._getCanvasHeight(data);

            var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            svg.setAttribute("version", 1.1);
            svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
            svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", "http://www.w3.org/1999/xlink");
            svg.setAttribute("width", width);
            svg.setAttribute("height", height);

            let y = 0;
            data.forEach(x => {
                if (x.name) {
                    y += 15;

                    var svgText = document.createElementNS("http://www.w3.org/2000/svg", "text");
                    svgText.setAttribute("x", 0);
                    svgText.setAttribute("y", y - 3);
                    svgText.setAttribute("font-family", "Roboto, \"Helvetica Neue\", sans-serif");
                    svgText.setAttribute("font-size", '12px');
                    svgText.setAttribute("font-style", 'normal');
                    svgText.appendChild(document.createTextNode(`${x.format} ${x.name}`));

                    svg.appendChild(svgText);
                }
                if (x.img) {
                    var svgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
                    svgImage.setAttribute("width", x.img.width);
                    svgImage.setAttribute("height", x.img.height);
                    svgImage.setAttribute("x", 0);
                    svgImage.setAttribute("y", y);
                    svgImage.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", x.img.toDataURL());

                    svg.appendChild(svgImage);

                    y += x.height;
                }
            });

            var serializer = new XMLSerializer();
            var source = serializer.serializeToString(svg);

            if (!source.match(/^<svg[^>]+xmlns="http\:\/\/www\.w3\.org\/2000\/svg"/)) {
                source = source.replace(/^<svg/, '<svg xmlns="http://www.w3.org/2000/svg"');
            }
            if (!source.match(/^<svg[^>]+"http\:\/\/www\.w3\.org\/1999\/xlink"/)) {
                source = source.replace(/^<svg/, '<svg xmlns:xlink="http://www.w3.org/1999/xlink"');
            }

            source = '<?xml version="1.0" standalone="no"?>\r\n' + source;

            var url = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(source);

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