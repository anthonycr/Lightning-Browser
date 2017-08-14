(function () {
    'use strict';
    
    var metas, i, tag;
    
    metas = document.getElementsByTagName('meta');
    
    if (metas !== null) {
        for (i = 0; i < metas.length; i += 1) {
            
            tag = metas[i].getAttribute('name');
            
            if (tag !== null && tag.toLowerCase() === 'theme-color') {
                return metas[i].getAttribute('content');
            }
            
            console.log(tag);
        }
    }

    return '';
}());
