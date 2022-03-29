export default function generateAnnotationBackground(colors) {
    if (!colors || colors.length === 0) {
        return undefined;
    }
    const MAX_COLUMNS = 5;
    const columns = Math.min(colors.length, MAX_COLUMNS);
    const rows = Math.ceil(colors.length / columns);
    const cellSize = rows >= 2 ? 0.75 : 1;
    const getCoordinate = (index) => ({
        column: index % columns,
        row: Math.floor(index / columns)
    });
    let svg = `<svg
 xmlns="http://www.w3.org/2000/svg"
 width="${columns * 16}" height="${rows * 16}"
 stroke="rgb(120, 120, 120)"
>`;
    for (let i = 0; i < colors.length; i++) {
        const {column, row} = getCoordinate(i);
        svg = svg.concat(`
<rect
 x="${column * 16}"
 y="${row * 16}"
 width="16"
 height="16"
 fill="${colors[i]}"
/>`);
    }
    svg = svg.concat('</svg>');
    svg = `url(data:image/svg+xml;base64,${window.btoa(svg)})`;
    return {
        backgroundImage: svg,
        width: columns * cellSize,
        height: rows * cellSize
    };
}
