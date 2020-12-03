export function getActionButton(name, label, action = null, ...rest){
    return {
        label,
        name,
        perform: action ? action : () => {},
        type: 'button',
        ...rest
    };
}