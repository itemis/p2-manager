"use strict";

describe('shopping cart service', function() {

    let shoppingCart;
    let constants;
    let httpBackend;
    let mockCookies;
    let mockWindow;
    let unitId;
    let version;

    beforeEach(module('shoppingCart'));

    beforeEach(module(function($provide) {
        mockCookies = {
            cookieContent: {},
            getObject: function(key) {
                return this.cookieContent[key];
            },
            putObject: function(key, value) {
                this.cookieContent[key] = value;
            }
        };
        mockWindow = {
            location: {
                href: "notSetYet/probablyAnError"
            }
        };
        $provide.value("$cookies", mockCookies);
        $provide.value("$window", mockWindow);
    }));

    beforeEach(inject(function(_shoppingCart_, _constants_, $httpBackend){
        shoppingCart = _shoppingCart_;
        constants = _constants_;
        httpBackend = $httpBackend;
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

    describe('getTargetPlatform', function() {
        // still not working, find out why
        it('sets the $window.location to the address given in the response header', function() {
            httpBackend.expectPOST((url) => {return true}).respond("", {
                "Location": "setCorrectly/testShouldPass"
            });
            shoppingCart.getTargetPlatform().then(function() {
                expect(mockWindow.location.href).toBe("setCorrectly/testShouldPass");
            });
            httpBackend.flush();
        });

        it('encodes the unit list correctly', function() {
            // TODO
        });
    });

/*
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