(function () {
    'use strict';
    
    var inverted = 'img {-webkit-filter: invert(100%);-moz-filter: invert(100%);-o-filter:  invert(100%);-ms-filter: invert(100%); }',
        normal = 'html {-webkit-filter: invert(0%); -moz-filter: invert(0%); -o-filter: invert(0%); -ms-filter: invert(0%); }',
        headElement = document.getElementsByTagName('head')[0],
        styleElement = document.createElement('style'),
        inversionToggle = inverted;
    
    if (!window.counter) {
        window.counter = 1;
    } else {
        window.counter += 1;
        if (window.counter % 2 === 0) {
            inversionToggle = normal;
        }
    }
    
    styleElement.type = 'text/css';
    
    if (styleElement.styleSheet) {
        styleElement.styleSheet.cssText = inversionToggle;
    } else {
        styleElement.appendChild(document.createTextNode(inversionToggle));
    }

    headElement.appendChild(styleElement);
}());
