import $ from 'jquery';

import 'jquery-ui/themes/base/core.css';
import 'jquery-ui/themes/base/theme.css';
import 'jquery-ui/themes/base/dialog.css';
import 'jquery-ui/ui/core';
import 'jquery-ui/ui/widgets/dialog';
import './browserDetect.scss';
const template = require('./browserDetect.tpl.html');

export default function browserDetect() {

    const supportedBrowsers = [
        {
            name: 'Chrome',
            version: 56
        },
        {
            name: 'Safari',
            version: 9
        },
        {
            name: 'Firefox',
            version: 51
        },
        {
            name: 'Edge',
            version: 13
            //Currently, there are 3 versions out there: 12, 13, and 14. Version 12 was the initial release and corresponds to version 20 of the browser. Version 13 is the current release and it corresponds to version 25 of the browser. Lastly, version 14 is a future release that corresponds to version 38 of the browser.
        }
    ];

    const hideUnsupBrowserAlert = 'hideUnsupBrowserAlert';

    const isBrowserSupported = () => {
        const {name, version} = getBrowserInfo();
        const browsers = supportedBrowsers.filter(b => b.name === name && b.version <= version);
        return browsers && browsers.length > 0;
    };

    const getBrowserInfo = () => {
        const userAgent = navigator.userAgent;
        let browserName, majorVersion, offsetVersion;

        if ((offsetVersion = userAgent.indexOf('YaBrowser')) !== -1) {
            browserName = 'YaBrowser';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 10, userAgent.indexOf('.', offsetVersion)), 10);
        }
        else if ((offsetVersion = userAgent.indexOf('OPR')) !== -1) {
            browserName = 'Opera';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 4, userAgent.indexOf('.', offsetVersion)), 10);
        }
        else if ((offsetVersion = userAgent.indexOf('Edge')) !== -1) {
            browserName = 'Edge';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 5, userAgent.indexOf('.', offsetVersion)), 10);
        }
        else if ((offsetVersion = userAgent.indexOf('Firefox')) !== -1) {
            browserName = 'Firefox';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 8, userAgent.indexOf('.', offsetVersion)), 10);
        }
        else if ((offsetVersion = userAgent.indexOf('Safari')) !== -1 && userAgent.indexOf('Chrome') === -1) {
            browserName = 'Safari';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 7, userAgent.indexOf('.', offsetVersion)), 10);
            if ((offsetVersion = userAgent.indexOf('Version')) !== -1) {
                majorVersion = parseInt(userAgent.substring(offsetVersion + 8, userAgent.indexOf('.', offsetVersion)), 10);
            }
        }
        else if ((offsetVersion = userAgent.indexOf('Chrome')) !== -1) {
            browserName = 'Chrome';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 7, userAgent.indexOf('.', offsetVersion)), 10);
        }
        else if ((offsetVersion = userAgent.indexOf('Trident')) !== -1) {
            browserName = 'IE11';
            if ((offsetVersion = userAgent.indexOf('rv')) !== -1) {
                majorVersion = parseInt(userAgent.substring(offsetVersion + 3, userAgent.indexOf('.', offsetVersion)), 10);
            }
        }
        else if ((offsetVersion = userAgent.indexOf('MSIE')) !== -1) {
            browserName = 'IE';
            majorVersion = parseInt(userAgent.substring(offsetVersion + 5, userAgent.indexOf('.', offsetVersion)), 10);
        }

        return {name: browserName, version: majorVersion};
    };

    const openDlg = () => {

        $('body').append(template);
        $('body').append('<p id="allScreenLayer"></p>');

        $('#dialogBrowserDetect').dialog({
            dialogClass: 'ui-dialog-browser-detect',
            draggable: false,
            resizable: false,
            width: 400,
            height: 230,
            open: function (event) {
                $(event.target).parent().css('position', 'absolute');
                $(event.target).parent().css('top', '30%');
                $(event.target).parent().css('left', '40%');

                $('#btnOK').bind('click', function () {
                    if($('#showAgain').prop('checked')){
                        localStorage.setItem(hideUnsupBrowserAlert, true);
                    }
                    $('#dialogBrowserDetect').dialog('close');
                    $('#allScreenLayer').hide();
                });
            }
        });

        $('#allScreenLayer').show();
        $('#dialogBrowserDetect').dialog('open');
        $('body').attr('style', 'display: block !important;');
    };

    if (!localStorage.getItem(hideUnsupBrowserAlert) && !isBrowserSupported())
    {
        openDlg();
    }
}