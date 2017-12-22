angular
.module('unitList')
.component('unitList', {
    templateUrl: 'app/unit-list/unit-list.template.html',
    controller: ['$http', '$q', 'unitSearch', 'shoppingCart', '$timeout', function UnitListController($http, $q, unitSearch, shoppingCart, $timeout) {
        
        this.backend = "http://localhost:8080";
		this.unitsAreLoading = false;
		this.allUnitsLoaded = false;
		this.unitSearchField={"keywords":""};
        unitSearch.onSearchTextChange((keywords) => {
            this.unitSearchField.keywords = keywords;
            this.searchUnits();
        });
        this.shoppingCart = shoppingCart;

        this.units = {
            unitList: [],
            scrollLoadSize: 20,
            
            getItemAtIndex: function(index) {
                if (index >= this.unitList.length) {
                    this.loadMoreUnits(index);
                    return null;
                }
  
                return this.unitList[index];
            },

            getLength: function() {
                if (this.ctrl.allUnitsLoaded) {
                    return this.unitList.length
                }
                return this.unitList.length + 5;
            },
  
            loadMoreUnits: function(index) {
                if (index < this.unitList.length || this.ctrl.unitsAreLoading || this.ctrl.allUnitsLoaded) {
                    return;
                }
                
                this.ctrl.unitsAreLoading = true;
                const searchQuery = this.ctrl.unitSearchField.keywords.split(" ")
                                        .map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
                                        .reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
        
                $http.get(this.ctrl.backend+'/units?limit='+this.scrollLoadSize
                                                    +"&offset="
                                                    +this.getTrueUnitListSize()
                                                    +"&"+searchQuery)
                .then(response => {
                    this.ctrl.unitsAreLoading = false;
                    
                    if (response.status === 204) { // No Content 
                        this.ctrl.allUnitsLoaded = true;
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
            this.allUnitsLoaded = false;
        }
        
        this.loadMoreUnits = () => {
            if (this.unitsAreLoading || this.allUnitsLoaded) {
                return;
            }
            
            if (this.unitSearchField.keywords === undefined) {
                return;
            }
            
            this.unitsAreLoading = true;
            const searchQuery = this.unitSearchField.keywords.split(" ")
                                    .map(keyword => "searchTerm="+keyword.replace(/\s/g, ''))
                                    .reduce((keyword1, keyword2) => keyword1+"&"+keyword2);
    
            $http.get(this.backend+'/units?limit='+this.scrollLoadSize
                                                +"&offset="
                                                +this.units.reduce((length, unit) => unit.versions.length + length, 0)
                                                +"&"+searchQuery)
            .then(response => {
                this.unitsAreLoading = false;
                
                if (response.status === 204) { // No Content 
                    this.allUnitsLoaded = true;
                } else {
                    for (let unit of response.data) {
                        let existingUnit = this.units.find((elem, i, arr) => elem.unitId.valueOf() === unit.unitId.valueOf());
                        if (existingUnit !== undefined) {
                            existingUnit.versions.push({"version": unit.version});
                        } else {
                            this.units.push({
                                "unitId": unit.unitId,
                                "versions": [{"version": unit.version}]
                            });
                        }
                    }
                }
            });
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