"use strict";

angular.module('p2-manager-frontend', ['ngRoute', 'repositoryList', 'unitList'])
       .config(['$routeProvider', ($routeProvider) => {
    $routeProvider
    .when('/browse', {
        templateUrl: 'app/views/browsing-view.template.html'
    })
    .when('/cart', {
        templateUrl: 'app/shopping-cart/shopping-cart-view.template.html',
        controller: 'shoppingCartController',
        controllerAs: '$ctrl'
    })
    .otherwise({
       redirectTo: '/browse'
    });
}]);