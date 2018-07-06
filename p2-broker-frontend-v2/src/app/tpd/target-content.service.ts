import { Injectable } from '@angular/core';
import { Location } from '@angular/common';
import { InstallableUnit } from '../model/installable-unit';
import { BackendService } from '../rest/backend.service';
import { Repository } from '../model/repository';

@Injectable({
  providedIn: 'root'
})
export class TargetContentService {

  private units: Map<string, InstallableUnit> = new Map();
  private repositories: Map<string, Repository> = new Map();
  
  constructor(private backend: BackendService, private location: Location) { }

  public addUnit(unit: InstallableUnit) {
    if (this.units.get(unit.id) === undefined) {
      this.units.set(unit.id, unit);
      this.updateRepositoryList();
    }
  }

  public removeUnit(unitId: string) {
    this.units.delete(unitId);
    this.updateRepositoryList();
  }

  public isInTargetPlatform(unitId: string): boolean {
    return this.units.has(unitId);
  }

  public clearContent() {
    this.units.clear();
  }

  private updateRepositoryList() {

    if (this.units.size === 0) {
      this.repositories.clear();
    } else {
      this.backend.getReposForUnits();
    }
  }

  public getTargetPlatform() {}
    this.backend.getTargetPlatform(this.units, (locationValue: string) => this.location.go(locationValue));
  }
}
