import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'p2 Broker';

  navEntries = [{link:"browse", label:"BROWSE"},
                {link:"target", label:"TARGET PLATFORM"},
                {link:"admin", label:"ADMIN"}];
}
