export class SVG {
    svg;

    constructor(width, height) {
        this.svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        this.svg.setAttribute("version", 1.1);
        this.svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        this.svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", "http://www.w3.org/1999/xlink");
        this.svg.setAttribute("width", width);
        this.svg.setAttribute("height", height);
    }

    addText(text, x = 0, y = 0) {
        let svgText = document.createElementNS("http://www.w3.org/2000/svg", "text");
        svgText.setAttribute("x", x);
        svgText.setAttribute("y", y);
        svgText.setAttribute("font-family", "Roboto, \"Helvetica Neue\", sans-serif");
        svgText.setAttribute("font-size", '12px');
        svgText.setAttribute("font-style", 'normal');
        svgText.appendChild(document.createTextNode(text));

        this.svg.appendChild(svgText);
    }

    addImage(width, height, href, x = 0, y = 0) {
        let svgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
        svgImage.setAttribute("width", width);
        svgImage.setAttribute("height", height);
        svgImage.setAttribute("x", x);
        svgImage.setAttribute("y", y);
        svgImage.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", href);

        this.svg.appendChild(svgImage);
    }

    getUrl() {
        let serializer = new XMLSerializer();
        let source = serializer.serializeToString(this.svg);

        if (!source.match(/^<svg[^>]+xmlns="http\:\/\/www\.w3\.org\/2000\/svg"/)) {
            source = source.replace(/^<svg/, '<svg xmlns="http://www.w3.org/2000/svg"');
        }
        if (!source.match(/^<svg[^>]+"http\:\/\/www\.w3\.org\/1999\/xlink"/)) {
            source = source.replace(/^<svg/, '<svg xmlns:xlink="http://www.w3.org/1999/xlink"');
        }
        source = '<?xml version="1.0" standalone="no"?>\r\n' + source;

        return "data:image/svg+xml;charset=utf-8," + encodeURIComponent(source);
    }
}