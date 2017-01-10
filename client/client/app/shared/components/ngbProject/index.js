import './project.scss';
import './registerTrack/register-track-forms/ngbRegisterTrackForm.scss';

import angular from 'angular';

import ngbCreateProjectComponent from './createProjectButton/ngbCreateProjectButton.component';
import ngbCreateProjectController from './createProjectButton/ngbCreateProjectButton.controller';

import projectGeneralComponent from './projectGeneral/projectGeneral.component';
import projectGeneralController from './projectGeneral/projectGeneral.controller';

import projectTracksComponent from './projectTracks/projectTracks.component';
import projectTracksController from './projectTracks/projectTracks.controller';

import editProjectButtonComponent from './editProjectButton/ngbEditProjectButton.component';
import editProjectButtonController from './editProjectButton/ngbEditProjectButton.controller';

import ngbDeleteProjectButtonComponent from './deleteProjectButton/ngbDeleteProjectButton.component';
import ngbDeleteProjectButtonController from './deleteProjectButton/ngbDeleteProjectButton.controller';

import ngbRegisterTrackComponent from './registerTrack/ngbRegisterTrack.component';
import ngbRegisterTrackController from './registerTrack/ngbRegisterTrack.controller';
import ngbRegisterTrackConstants from './registerTrack/ngbRegisterTrack.constants';

import ngbProjectService from './ngbProject.service';

import {
    ngbRegisterLocalComputerTrackComponent,
    ngbRegisterLocalComputerTrackController,
    ngbRegisterNgbServerTrackComponent,
    ngbRegisterNgbServerTrackController,
    ngbRegisterS3BucketTrackComponent,
    ngbRegisterS3BucketTrackController,
    ngbRegisterUrlTrackComponent,
    ngbRegisterUrlTrackController
} from './registerTrack/register-track-forms';

export default angular.module('ngbProjectModule', [])

    .constant('ngbRegisterTrackConstants', ngbRegisterTrackConstants)

    .component('ngbCreateProjectButton', ngbCreateProjectComponent)
    .component('ngbEditProjectButton', editProjectButtonComponent)
    .component('ngbDeleteProjectButton', ngbDeleteProjectButtonComponent)
    .component('projectGeneral', projectGeneralComponent)
    .component('projectTracks', projectTracksComponent)
    .component('ngbRegisterTrack', ngbRegisterTrackComponent)

    .component('ngbRegisterLocalComputer', ngbRegisterLocalComputerTrackComponent)
    .component('ngbRegisterNgbServerTrack', ngbRegisterNgbServerTrackComponent)
    .component('ngbRegisterS3BucketTrack', ngbRegisterS3BucketTrackComponent)
    .component('ngbRegisterUrlTrack', ngbRegisterUrlTrackComponent)

    .service('ngbProjectService', ngbProjectService)

    .controller(ngbCreateProjectController.UID, ngbCreateProjectController)
    .controller(editProjectButtonController.UID, editProjectButtonController)
    .controller(ngbDeleteProjectButtonController.UID, ngbDeleteProjectButtonController)
    .controller(projectGeneralController.UID, projectGeneralController)
    .controller(projectTracksController.UID, projectTracksController)
    .controller(ngbRegisterTrackController.UID, ngbRegisterTrackController)

    .controller(ngbRegisterLocalComputerTrackController.UID, ngbRegisterLocalComputerTrackController)
    .controller(ngbRegisterNgbServerTrackController.UID, ngbRegisterNgbServerTrackController)
    .controller(ngbRegisterS3BucketTrackController.UID, ngbRegisterS3BucketTrackController)
    .controller(ngbRegisterUrlTrackController.UID, ngbRegisterUrlTrackController)

    .name;