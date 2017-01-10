import angular from 'angular';

// Import Style
import './<%= name %>.scss';

// Import internal modules
import component from './<%= name %>.component';
import controller from './<%= name %>.controller';
import service from './<%= name %>.service';


export default angular.module('<%= name %>Component', [])
    .component('<%= name %>', component)
    .controller(controller.UID, controller)
    .service('<%= name %>Service', service.instance)
    .name;






