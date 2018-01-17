"use strict";

angular.module('p2-manager-frontend')
.controller('p2-manager-frontend-controller', function ($mdToast, $cookies) {

    if ($cookies.get('cookieMessage') !== 'accepted') {
        $mdToast.show(
            $mdToast.simple()
                .textContent('By continuing to use this site you consent to the use of cookies on your device.')
                .action('OK')
                .highlightAction(true)
                .position('top left')
                .hideDelay(0)
        ).then(response => {
            if (response === 'ok') {
                $cookies.put('cookieMessage', 'accepted');
            }
        });
    }
});