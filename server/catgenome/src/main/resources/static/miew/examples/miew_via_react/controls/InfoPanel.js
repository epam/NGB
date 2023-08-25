import React from 'react';

export default class InfoPanel extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      autoRotation: 0.0,
    };
  }

  onChange(_event) {
    const { viewer } = this.props;
    if (viewer) {
      this.setState({ autoRotation: viewer.get('autoRotation') });
    }
  }

  render() {
    const { viewer } = this.props;
    const value = viewer ? viewer.get('autoRotation') : 0;
    return <a> {`Current speed: ${value.toFixed(1)}`} </a>;
  }
}
