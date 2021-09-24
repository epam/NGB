import {getChannels} from '../../../../color-scheme/helpers';
const A_VERTEX_POSITION = 'aVertexPositions';

export {A_VERTEX_POSITION};

/**
 *
 * @param {Map<string|number, HeatmapGeometryCollection>[]} maps
 * @returns {Map<string|number, HeatmapGeometryCollection>}
 */
export function mergeMaps(...maps) {
    /**
     *
     * @type {Map<string|number, HeatmapGeometryCollection>}
     */
    const result = new Map();
    maps.forEach(map => {
        for (const [key, value] of map.entries()) {
            if (!result.has(key)) {
                result.set(key, value);
            } else {
                const current = result.get(key);
                current.geometries.push(...value.geometries);
            }
        }
    });
    return result;
}

function getChannelValue(value) {
    if (Math.floor(value) === value) {
        return value.toFixed(1);
    }
    return value;
}

function getVec3Color(color) {
    const {r, g, b} = getChannels(color, false);
    return `${getChannelValue(r)}, ${getChannelValue(g)}, ${getChannelValue(b)}`;
}

export function getVertexShader() {
    return `
precision mediump float;

attribute vec3 ${A_VERTEX_POSITION};

uniform mat3 translationMatrix;
uniform mat3 projectionMatrix;
uniform float alpha;

varying float ratio;
varying float vAlpha;

void main() {
    ratio = ${A_VERTEX_POSITION}.z;
    vAlpha = alpha;
    gl_Position = vec4((projectionMatrix * translationMatrix * vec3(${A_VERTEX_POSITION}.xy, 1.0)).xy, 0.0, 1.0);
}`;
}

export function getFragmentShader(gradientStop1, gradientStop2) {
    if (!gradientStop1 || !gradientStop2) {
        return undefined;
    }
    return `
precision mediump float;

varying float ratio;
varying float vAlpha;

void main() {
    vec4 start = vec4(${getVec3Color(gradientStop1.color)}, vAlpha);
    vec4 end = vec4(${getVec3Color(gradientStop2.color)}, vAlpha);
    gl_FragColor = start + ratio * (end - start);
}
`;
}
