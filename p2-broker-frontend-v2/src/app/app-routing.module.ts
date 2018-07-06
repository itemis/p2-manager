import { NgModule } from '@angular/core';
import { RouterModule, RouterLinkActive, Routes } from '@angular/router';
import { BrowseComponent } from './browse/browse.component';
import { TargetPlatformComponent } from './target-platform/target-platform.component';
import { AdminComponent } from './admin/admin.component';


const routes: Routes = [
  { path: 'browse', component: BrowseComponent },
  { path: 'target', component: TargetPlatformComponent },
  { path: 'admin', component: AdminComponent },
  { path: '', pathMatch: 'full', redirectTo: 'browse' }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],
  exports: [ RouterModule, RouterLinkActive ]
})
export class AppRoutingModule {
}
