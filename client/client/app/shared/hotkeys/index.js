import KeyboardJS from 'keyboardjs';
window.KeyboardJS = KeyboardJS;
const modifierKeysState = {
    ctrl: false,
    shift: false,
    alt: false
};

for (const key of Object.keys(modifierKeysState))
    KeyboardJS.bind(key,
        () => {modifierKeysState[key] = true;},
        () => {modifierKeysState[key] = false;});

export default modifierKeysState;