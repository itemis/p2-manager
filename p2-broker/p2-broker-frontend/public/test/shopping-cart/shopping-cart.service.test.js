"use strict";

describe('shopping cart service', function() {
    beforeEach(module('shoppingCart'));

    //TODO mock out $http, $cookies and $window

    let shoppingCart;
    let unitId;
    let version;

    beforeEach(inject(function(_shoppingCart_){
        shoppingCart = _shoppingCart_;
        unitId = "someId";
        version = "someVersion";
    }));

    describe('getters', function() {
        it('return empty lists after the shopping cart has been emptied', function() {
            shoppingCart.empty();
            expect(shoppingCart.getUnits().pop()).toBe(undefined);
            expect(shoppingCart.getRepositories().pop()).toBe(undefined);
        });
    });

    describe('isInCart', function() {
        it('returns true if called with a unit and version combination that has been added before', function() {
            shoppingCart.empty();
            expect(shoppingCart.isInCart(unitId, version)).toBe(false);
            shoppingCart.addUnit(unitId, version);
            expect(shoppingCart.isInCart(unitId, version)).toBe(true);
        });

        it('returns false if the combination of unit and version has been removed before the call', function() {
            shoppingCart.empty();
            shoppingCart.addUnit(unitId, version);
            shoppingCart.removeUnit(unitId, version);
            expect(shoppingCart.isInCart(unitId, version)).toBe(false);
        });
    });

/*
    describe('getTargetPlatform', function() {
        it('sets the $window.location to the address given in the response header', function() {
            // TODO
        });
    });

    describe('shopping cart', function() {
        it('has the contents of the cookie in the beginning', function() {
            // TODO
        });
        it('has the contents of the cookie in the beginning', function() {
            // TODO
        });
    });


    describe('stub', function() {
        it('is a stub', function() {

        });
    });
*/
});