import {linearDimensionsConflict} from '../../../../../../utilities';

export default function splitChromosomeByDeletions (deletions) {
    let parts = [{startIndex: -Infinity, endIndex: Infinity}];
    const injectBreak = (startIndex, endIndex) => {
        const notAffected = parts.filter(part =>
            !linearDimensionsConflict(part.startIndex, part.endIndex, startIndex, endIndex)
        );
        const affected = parts
            .filter(part =>
                linearDimensionsConflict(part.startIndex, part.endIndex, startIndex, endIndex)
            )
            .map(part => {
                const points = [
                    ...(
                        new Set([
                            startIndex,
                            endIndex,
                            part.startIndex,
                            part.endIndex
                        ])
                    )
                ].sort((a, b) => a - b);
                const newParts = [];
                for (let i = 1; i < points.length; i++) {
                    const x1 = points[i - 1];
                    const x2 = points[i];
                    if (
                        x1 !== x2 &&
                        (
                            x1 >= endIndex ||
                            x2 <= startIndex
                        )
                    ) {
                        newParts.push({
                            startIndex: x1,
                            endIndex: x2
                        });
                    }
                }
                return newParts;
            })
            .reduce((r, c) => ([...r, ...c]), []);
        parts = [
            ...affected,
            ...notAffected
        ].sort((a, b) => a.startIndex - b.startIndex);
    };
    deletions
        .forEach(deletion => injectBreak(deletion.startIndex, deletion.endIndex + 1));
    return parts;
}
