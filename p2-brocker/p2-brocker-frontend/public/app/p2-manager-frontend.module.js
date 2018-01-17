"use strict";

angular.module('p2-manager-frontend', ['ngRoute', 'repositoryList', 'unitList', 'admin', 'ngMaterial', 'ngMessages', 'ngAria', 'ngAnimate'])
       .config(['$routeProvider', '$mdThemingProvider', ($routeProvider, $mdThemingProvider) => {
    $routeProvider
    .when('/browse', {
        templateUrl: 'app/browsing/browsing-view.template.html'
    })
    .when('/cart', {
        templateUrl: 'app/shopping-cart/shopping-cart-view.template.html',
        controller: 'shoppingCartController',
        controllerAs: '$ctrl'
    })
    .when('/admin', {
        templateUrl: 'app/admin/admin-view.template.html',
        controller: 'adminController',
        controllerAs: '$ctrl'
    })
    .otherwise({
       redirectTo: '/browse'
    });

    $mdThemingProvider.theme('default');
}]);