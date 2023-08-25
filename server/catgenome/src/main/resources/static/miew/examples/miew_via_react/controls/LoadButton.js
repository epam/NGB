import React from 'react';

export default class LoadButton extends React.Component {
  onChange(_event) {
    if (this.props.viewer) {
      this.props.viewer.load(this.props.file);
    }
  }

  render() {
    return <button onClick={ (e) => this.onChange(e) }> {`Load ${this.props.file.toUpperCase()}.pdb`}</button>;
  }
}
