import React from 'react';
import { Treebeard } from 'react-treebeard';
import * as filters from './filter';

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
          data: newData,
          unfiltered: newData
        });
      })
  }
  onFilterMouseUp(e) {
    const filter = e.target.value.trim();
    if (!filter) {
      return this.setState({ data: this.state.unfiltered });
    }
    const filtered = filters.filterTree(this.state.data, filter);
    const expanded = filters.expandFilteredNodesRoot(filtered, filter);
    this.setState({ data: expanded });
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
      <div>
        <div>
          <div className="input-group">
            <span className="input-group-addon">
              <i className="fa fa-search"></i>
            </span>
            <input type="text"
              className="form-control"
              placeholder="Search the tree..."
              onKeyUp={this.onFilterMouseUp.bind(this)}
            />
          </div>
        </div>
        <Treebeard
          data={this.state.data}
          onToggle={this.onToggle}
        />
      </div>
    );
  }
}

export default TreeExample;