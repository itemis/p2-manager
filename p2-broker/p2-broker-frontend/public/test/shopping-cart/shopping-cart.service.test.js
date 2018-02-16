"use strict";

describe('shopping cart service', function() {

    let shoppingCart;
    let constants;
    let httpBackend;
    let mockCookies;
    let mockWindow;
    let unitId;
    let version;

    const cookieName = "unitsInCart";

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
        it('sets the $window.location to the address given in the response header', function() {
            httpBackend.expectPOST((url) => {return true}).respond("", {
                "Location": "setCorrectly/testShouldPass"
            });
            shoppingCart.getTargetPlatform().then(function() {
                expect(mockWindow.location.href).toBe("setCorrectly/testShouldPass");
            });
            httpBackend.flush();
        });
    });

    describe('shopping cart', function() {
        const unitList = [
            {
                "unitId" : "foo",
                "version" : "bar"
            }, {
                "unitId" : "foo",
                "version" : "baz"
            }
        ];
        const unitList2 = [
            {
                "unitId" : "foo",
                "version" : "1"
            }, {
                "unitId" : "bar",
                "version" : "2"
            }
        ];

        it('creates an empty cookie if there is none', function() {
            expect(mockCookies.getObject(cookieName)).toEqual([]);
        });

        it('saves its contents to the cookie', function() {
            shoppingCart.addUnit("foo", "1");
            shoppingCart.addUnit("bar", "2");
            expect(mockCookies.getObject(cookieName)).toEqual(unitList2);
        });

        mockCookies.putObject(unitList);
        inject(function(_shoppingCart_){
            shoppingCart = _shoppingCart_;
        }
        xit('has the contents of the cookie in the beginning', function() {
            // mockCookies needs to be initialized with content before shoppingCart is injected
            expect(shoppingCart.getUnits()).toEqual(unitList);
        });
    });


    xdescribe('stub', function() {
        it('is a stub', function() {
            // copy and rename from "xdescribe" to "describe" to create a new test
        });
    });
});