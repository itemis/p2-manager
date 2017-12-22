angular
.module('unitList')
.component('unitList', {
    templateUrl: 'app/unit-list/unit-list.template.html',
    controller: ['$http', '$q', 'unitSearch', 'shoppingCart', '$timeout', 'constants', function UnitListController($http, $q, unitSearch, shoppingCart, $timeout, constants) {
        
        this.backend = constants.backend;
		this.unitSearchField={"keywords":""};
        unitSearch.onSearchTextChange((keywords) => {
            this.unitSearchField.keywords = keywords;
            this.searchUnits();
        });
        this.shoppingCart = shoppingCart;

        this.units = {
            unitList: [],
            scrollLoadSize: 100,
            allUnitsLoaded: false,
            unitsAreLoading: false,
            
            getItemAtIndex: function(index) {
                if (index >= this.unitList.length) {
                    this.loadMoreUnits(index);
                    return null;
                }
  
                return this.unitList[index];
            },

            getLength: function() {
                if (this.allUnitsLoaded) {
                    return this.unitList.length
                }
                return this.unitList.length + 5;
            },
  
            loadMoreUnits: function(index) {
                if (index < this.unitList.length || this.unitsAreLoading || this.allUnitsLoaded) {
                    return;
                }
                
                this.unitsAreLoading = true;
                const searchQuery = this.ctrl.unitSearchField.keywords.split(" ")
                                        .map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
                                        .reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
        
                $http.get(this.ctrl.backend+'/units?limit='+this.scrollLoadSize
                                                    +"&offset="
                                                    +this.getTrueUnitListSize()
                                                    +"&"+searchQuery)
                .then(response => {
                    this.unitsAreLoading = false;
                    
                    if (response.status === 204) { // No Content 
                        this.allUnitsLoaded = true;
                    } else {
                        for (let unit of response.data) {
                            let existingUnit = this.unitList.find((elem, i, arr) => elem.unitId.valueOf() === unit.unitId.valueOf());
                            if (existingUnit !== undefined) {
                                existingUnit.versions.push({"version": unit.version});
                            } else {
                                this.unitList.push({
                                    "unitId": unit.unitId,
                                    "versions": [{"version": unit.version}]
                                });
                            }
                        }
                    }
                });
            },

            reset: function() {
                this.unitList = [];
                this.allUnitsLoaded = false;
            },

            getTrueUnitListSize: function() {
                return this.unitList.reduce((length, unit) => unit.versions.length + length, 0);
            }
        };
        this.units.ctrl = this;
        
        this.searchUnits = () => {
            if (this.searchUnitTimeout !== undefined) {
                this.searchUnitTimeout.resolve();
            }
            this.searchUnitTimeout = $q.defer();
    
            this.units.reset();
        }
        
        this.getRepositoriesForVersion = (unitId, version) => {
            if (!version.repositoriesLoaded) {
                $http.get(this.backend+'/units/'+unitId+'/versions/'+version.version+"/repositories").
                    then(response => {
                        version.repositoriesWithVersion = response.data;
                    });
                    version.repositoriesLoaded = true;
            }
            
            version.showRepositories = !version.showRepositories;
        }
    }]
});