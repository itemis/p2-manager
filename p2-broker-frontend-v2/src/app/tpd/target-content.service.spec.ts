import { TestBed, inject } from '@angular/core/testing';

import { TargetContentService } from './target-content.service';

describe('TargetContentService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TargetContentService]
    });
  });

  it('should be created', inject([TargetContentService], (service: TargetContentService) => {
    expect(service).toBeTruthy();
  }));
});
