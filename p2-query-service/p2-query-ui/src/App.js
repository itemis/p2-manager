import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import TreeExample from './TreeExample.js';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>P2 Viewer</h2>
        </div>
        <div className="MyTree">
          <TreeExample />
        </div>
      </div>

    );
  }
}

export default App;