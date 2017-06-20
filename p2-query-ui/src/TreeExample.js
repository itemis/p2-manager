import React from 'react';
import ReactDOM from 'react-dom';
import { Treebeard } from 'react-treebeard';

let data = [
  {
    "id": 1,
    "name": "http://download.eclipse.org/oomph/updates/milestone",
    "children": []
  },
  {
    "id": 2,
    "name": "http://download.eclipse.org/oomph/drops/milestone/S20170307-122318-1.7.0-M4",
    "children": []
  },
  {
    "id": 3,
    "name": "http://download.eclipse.org/oomph/drops/milestone/S20170301-061518-1.7.0-M3",
    "children": []
  },
  {
    "id": 4,
    "name": "http://download.eclipse.org/oomph/drops/milestone/S20170215-125145-1.7.0-M2",
    "children": []
  },
  {
    "id": 5,
    "name": "http://download.eclipse.org/oomph/drops/milestone/S20170201-120440-1.7.0-M1",
    "children": []
  }
]

class TreeExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
    this.onToggle = this.onToggle.bind(this);
    fetch(`http://localhost:8080/repositories`)
      .then(response => response.json())
      .then(json => {
        let x = json.map(element => {
          return {
            name: element.uri,
            id: element.id,
            children: []
          }
        });
        console.log(x);
      }

      )
  }
  onToggle(node, toggled) {
    if (this.state.cursor) { this.state.cursor.active = false; }
    node.active = true;
    if (node.children) { node.toggled = toggled; }
    this.setState({ cursor: node });
  }
  render() {
    return (
      <Treebeard
        data={data}
        onToggle={this.onToggle}
      />
    );
  }
}

export default TreeExample;