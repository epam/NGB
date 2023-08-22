import React from 'react';
import ReactDOM from 'react-dom';
import ViewerContainer from './components/ViewerContainer';
import Checkbox from './controls/Checkbox';
import SpeedButton from './controls/SpeedButton';
import InfoPanel from './controls/InfoPanel';
import LoadButton from './controls/LoadButton';
import ChangeRepOptButton from './controls/ChangeRepOptButton';

class App extends React.Component {
  constructor() {
    super();
    this._viewer = null;

    this._onViewerChange = this._onViewerChange.bind(this);
  }

  _onViewerChange = (changed) => {
    if (changed.viewer !== undefined) {
      this._viewer = changed.viewer;
    }
    this.forceUpdate();
  };

  render() {
    return <div>
      <ViewerContainer onChange={ this._onViewerChange } />
      <div style={ { 'padding-top': '0.5em' } }>
        <LoadButton viewer={ this._viewer } file = '1zlm' />&nbsp;
        <ChangeRepOptButton viewer={ this._viewer } rep = '0' mode = 'BS' />&nbsp;
        Axes =  <Checkbox viewer={ this._viewer } prefName='axes' prefType={Boolean}/>&nbsp;
        Rotation = &nbsp;
        <SpeedButton viewer={ this._viewer } prefName='autoRotation' delta = {0.1} prefType={Number}/>&nbsp;
        <SpeedButton viewer={ this._viewer } prefName='autoRotation' delta = {-0.1} prefType={Number}/>&nbsp;
        <InfoPanel viewer={ this._viewer }/>
      </div>
    </div>;
  }
}

const root = document.getElementsByClassName('main')[0];
ReactDOM.render(<App/>, root);
