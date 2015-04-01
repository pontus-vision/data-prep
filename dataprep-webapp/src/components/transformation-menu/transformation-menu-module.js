(function() {
    'use strict';

    angular.module('data-prep.transformation-menu', [
        'talend.widget',
        'data-prep.type-validation',
        'data-prep.transformation-params',
        'data-prep.services.dataset',
        'data-prep.services.recipe',
        'data-prep.services.preparation'
    ]);
})();