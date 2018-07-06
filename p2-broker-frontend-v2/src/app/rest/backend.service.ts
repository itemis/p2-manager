import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { InstallableUnit } from '../model/installable-unit'
import { Observable } from '../../../node_modules/rxjs';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  private readonly address = "http://localhost:80";

  constructor(private http: HttpClient) { }

  public getTargetPlatform(units: Map<string, InstallableUnit>, action: (location: string) => void) {
    let unitList = {};
    units.forEach((unit, unitId, map) => unitList[unit.name] = unit.version );

    return this.http.post(this.address+'/tpd?tpdInfo='+encodeURIComponent(JSON.stringify(unitList)), "", { observe: 'response' })
                    .subscribe((data: HttpResponse<any>) => action.apply(data.headers.get("Location")));
  }

  public getReposForUnits() {
    // this.http.post<RepositoryDTO>
    /*
      let query = units.map(u => 'shoppingCart='+u.unitId+'+'+u.version)
                                  .reduce((acc, current) => acc+'&'+current);

      $http.get(backend+'/repositories?'+query).
      then(response => {
          repositories.length = 0;
          Array.prototype.push.apply(repositories, response.data);
          $cookies.putObject("unitsInCart", units);
      });*/
  }
}
