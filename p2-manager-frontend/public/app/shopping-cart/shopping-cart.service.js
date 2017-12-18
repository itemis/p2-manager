angular.module('shoppingCart')
.factory('shoppingCart', ($cookies, $http) => {
    let units = [];
    let repositories = [];

    const backend = "http://localhost:8080";

    const cookieUnitsInCart = $cookies.getObject("unitsInCart");
    if (cookieUnitsInCart === undefined) {
        $cookies.putObject("unitsInCart", units);
    } else {
        units = cookieUnitsInCart;
        updateRepositoryList();
    }

    return {
        addUnit: addUnit,
        removeUnit: removeUnit,
        empty: empty,
        isInCart: isInCart,
        getUnits: getUnits,
        getRepositories: getRepositories
    };

    
    function addUnit(unit, version) {
        const unitVersion = {
            "unitId" : unit,
            "version" : version
        }
        if (!units.includes(unitVersion)) {
            units.push(unitVersion);
            updateRepositoryList();
        }
    };
    
    function removeUnit(unit, version) {
        const unitVersion = {
            "unitId" : unit,
            "version" : version
        }
        units = units.filter(u => !unitEquals(unitVersion, u));
        updateRepositoryList();
    };
    
    function empty() {
        units.length = 0;
        repositories.length = 0;
    };

    function isInCart(unit, version) {
        const unitVersion = {
            "unitId" : unit,
            "version" : version
        }
        return units.some((cartUnit, i, units) => unitEquals(unitVersion, cartUnit));
    };
    
    function getUnits() {
        return units;
    };
    
    function getRepositories() {
        return repositories;
    };

    function updateRepositoryList() {
        if (units.length === 0) {
            repositories.length = 0;
        } else {
            let query = units.map(u => 'shoppingCart='+u.unitId+'+'+u.version)
                                        .reduce((acc, current) => acc+'&'+current);

            $http.get(backend+'/repositories?'+query).
            then(response => {
                repositories.length = 0;
                Array.prototype.push.apply(repositories, response.data);
                $cookies.putObject("unitsInCart", units);
            });
        }
    }
    
    //TODO: Refactoring - move this method to a more sensible location
    function unitEquals(unit1, unit2) {
        return unit1.unitId === unit2.unitId && unit1.version === unit2.version;
    };
});