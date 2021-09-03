export default function linearDimensionsConflict(a1, a2, b1, b2, margin = 0) {
    const o1p1 = a1;
    const o1p2 = a2;
    const o2p1 = b1;
    const o2p2 = b2;
    const o1size = Math.abs(o1p2 - o1p1);
    const o2size = Math.abs(o2p2 - o2p1);
    const oSize = Math.max(o1p1, o1p2, o2p1, o2p2) - Math.min(o1p1, o1p2, o2p1, o2p2);
    return o1size + o2size > oSize + margin;
}
