import React from 'react';

export default class ChangeRepOptButton extends React.Component {
  onChange(_event) {
    if (this.props.viewer) {
      this.props.viewer.rep(this.props.rep, { mode: this.props.mode });
    }
  }

  render() {
    return <button onClick={ (e) => this.onChange(e) }> {`Set rep ${this.props.rep} to ${this.props.mode}`}</button>;
  }
}
