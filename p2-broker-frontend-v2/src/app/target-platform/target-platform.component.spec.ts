import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetPlatformComponent } from './target-platform.component';

describe('TargetPlatformComponent', () => {
  let component: TargetPlatformComponent;
  let fixture: ComponentFixture<TargetPlatformComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TargetPlatformComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TargetPlatformComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
