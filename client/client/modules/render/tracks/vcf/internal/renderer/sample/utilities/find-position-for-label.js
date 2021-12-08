function getZoneDistanceToPoint (zone, point) {
    if (zone.start <= point && zone.end >= point) {
        return 0;
    }
    const center = (zone.start + zone.end) / 2.0;
    return Math.abs(center - point);
}

export default function findPositionForLabel (freeZones = [], labelOptions = {}) {
    const {
        x,
        width,
        direction = 1,
        margin = 5
    } = labelOptions;
    const x1 = x;
    const x2 = x + width;
    const [best] = freeZones
        .filter(zone => (zone.end - zone.start) >= (width + 2.0 * margin))
        .filter(zone => (direction > 0 && x2 <= zone.end) ||
            (direction < 0 && x1 >= zone.start)
        )
        .sort((a, b) => getZoneDistanceToPoint(a, x) - getZoneDistanceToPoint(b, x));
    let result = x;
    if (best) {
        const index = freeZones.indexOf(best);
        if (direction > 0) {
            result = Math.max(x, best.start);
        } else {
            result = Math.min(x, best.end - width);
        }
        freeZones
            .splice(
                index,
                1,
                ...[
                    {
                        start: best.start,
                        end: result - margin
                    },
                    {
                        start: result + width + margin,
                        end: best.end
                    }
                ].filter(z => z.end - z.start > 0)
            );
        return result;
    }
    return undefined;
}
