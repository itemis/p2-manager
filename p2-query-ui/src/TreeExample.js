import React from 'react';
import ReactDOM from 'react-dom';
import { Treebeard } from 'react-treebeard';

class TreeExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      data: [{
        "id": 1,
        "name": "loading..."
      }]
    };
    this.onToggle = this.onToggle.bind(this)
    fetch(`http://localhost:8080/repositories`)
      .then(response => response.json())
      .then(json => {
        let newData = json.map(element => {
          return {
            name: element.uri,
            id: element.id,
            children: []
          }
        });
        this.setState({
          data: newData
        });
      })
  }
  onToggle(node, toggled) {
    node.loading = true;
    fetch(`http://localhost:8080/repositories/${node.id}/units`)
      .then(response => response.json())
      .then(json => {
        let newChildren = json.map(unit => {
          return {
            name: `${unit.id} - ${unit.version}`
          }
        })

        node.children = newChildren;
        node.loading = false;

        if (this.state.cursor) { this.state.cursor.active = false; }
        node.active = true;
        if (node.children) { node.toggled = toggled; }
        this.setState({ cursor: node });
      });

  }
  render() {
    return (
      <Treebeard
        data={this.state.data}
        onToggle={this.onToggle}
      />
    );
  }
}

export default TreeExample;