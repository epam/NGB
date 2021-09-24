export function getSessionFlags(session, viewport) {
    const scaleChanged = session.scale !== viewport.scale.tickSize;
    const rowChanged = session.row !== viewport.rows.center;
    const columnChanged = session.column !== viewport.columns.center;
    const widthChanged = session.width !== viewport.deviceWidth;
    const heightChanged = session.height !== viewport.deviceHeight;
    const positionChanged = rowChanged ||
        columnChanged ||
        widthChanged ||
        heightChanged;
    return positionChanged || scaleChanged;
}

export function updateSessionFlags(viewport) {
    return {
        scale: viewport.scale.tickSize,
        row: viewport.rows.center,
        column: viewport.columns.center,
        width: viewport.deviceWidth,
        height: viewport.deviceHeight
    };
}
