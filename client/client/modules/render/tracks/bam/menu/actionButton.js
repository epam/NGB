export function getActionButton(name, label, action = null){
    return {
        label,
        name,
        perform: action ? action : () => {},
        type: 'button'
    };
}