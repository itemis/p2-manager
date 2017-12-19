angular
.module('unitList')
.component('unitList', {
    templateUrl: 'app/unit-list/unit-list.template.html',
    controller: ['$http', '$q', 'unitSearch', 'shoppingCart', function UnitListController($http, $q, unitSearch, shoppingCart) {
        
        this.backend = "http://localhost:8080";
        this.units = [];
		this.unitsAreLoading = false;
		this.allUnitsLoaded = false;
		this.unitSearchField={"keywords":""};
        unitSearch.onSearchTextChange((keywords) => {
            this.unitSearchField.keywords = keywords;
            this.searchUnits();
        });
        this.shoppingCart = shoppingCart;

        this.scrollLoadSize = 100;
        this.scrollDistance = 2;

        this.units2 = {
            numLoaded: 0,
            toLoad: 0,
            scrollLoadSize: 100,
            
            getItemAtIndex: function(index) {
                if (index > this.numLoaded) {
                    this.fetchMoreItems(index);
                    return null;
                }
  
                return index;
            },

            getLength: function() {
                return this.numLoaded + 10;
            },
  
            fetchMoreItems_: function(index) {
                if (this.toLoad < index) {
                    this.toLoad += this.scrollLoadSize;
                    //TODO: call loadMoreUnits appropriately
                }
            }
        };
        
        this.searchUnits = () => {
            if (this.searchUnitTimeout !== undefined) {
                this.searchUnitTimeout.resolve();
            }
            this.searchUnitTimeout = $q.defer();
    
            this.units = [];
            this.allUnitsLoaded = false;
            this.loadMoreUnits();
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