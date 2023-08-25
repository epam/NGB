import React from 'react';

const style = {
  width: '25px',
};

export default class SpeedButton extends React.Component {
  onChange(_event) {
    if (this.props.viewer) {
      const value = this.props.viewer.get(this.props.prefName) + this.props.delta;
      if (!Number.isNaN(value)) {
        this.props.viewer.set(this.props.prefName, value);
      }
    }
  }

  render() {
    return <button style = { style } onClick={ () => this.onChange() }> {this.props.delta > 0 ? '+' : '-'} </button>;
  }
}
