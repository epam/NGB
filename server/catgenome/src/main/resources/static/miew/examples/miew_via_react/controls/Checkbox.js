import React from 'react';

export default class Checkbox extends React.Component {
  constructor(prop) {
    super(prop);
    this.onChange = this.onChange.bind(this);
  }

  onChange(event) {
    let value = event.target.checked;
    if (this.props.viewer) {
      value = (value !== undefined) ? this.props.prefType(value) : this.props.prefType();
      if (!Number.isNaN(value)) {
        this.props.viewer.set(this.props.prefName, value);
      }
    }
  }

  render() {
    const value = (this.props.viewer && this.props.prefType(this.props.viewer.get(this.props.prefName))) || this.props.prefType();
    return <input type='checkbox' checked={ value } onChange={ this.onChange } />;
  }
}
