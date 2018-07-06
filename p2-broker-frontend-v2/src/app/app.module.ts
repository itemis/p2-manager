import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { MatTabsModule } from '@angular/material';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { BrowseComponent } from './browse/browse.component';
import { AdminComponent } from './admin/admin.component';
import { TargetPlatformComponent } from './target-platform/target-platform.component';

@NgModule({
  declarations: [
    AppComponent,
    BrowseComponent,
    AdminComponent,
    TargetPlatformComponent
  ],
  imports: [
    BrowserModule,
    MatTabsModule,
    AppRoutingModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
