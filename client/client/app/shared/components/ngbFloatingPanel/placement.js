const Placement = {
    topLeft: 'topLeft',
    bottomLeft: 'bottomLeft',
    topRight: 'ropRight',
    bottomRight: 'bottomRight'
};

function getInitialPlacement(placement = Placement.bottomRight) {
    switch (placement) {
        case Placement.topLeft:
            return {x: 0, y: 0};
        case Placement.topRight:
            return {x: window.innerWidth, y: 0};
        case Placement.bottomLeft:
            return {x: 0, y: window.innerHeight};
        case Placement.bottomRight:
        default:
            return {x: window.innerWidth, y: window.innerHeight};
    }
}

export {
    Placement,
    getInitialPlacement,
};
