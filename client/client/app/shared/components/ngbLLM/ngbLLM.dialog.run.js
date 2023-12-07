import {LLMName, LLMProperties} from './models';

export default function run($mdDialog, $timeout, dispatcher) {
    const displayLLMConfigurationDialog = async (request) => {
        const {
            uuid,
            options,
            models = []
        } = request || {};
        if (uuid) {
            const {
                type: model
            } = options || {};
            const llmModels = models;
            $mdDialog.show({
                template: require('./ngbLLM.dialog.tpl.html'),
                controller: function ($scope) {
                    $scope.models = llmModels;
                    $scope.getModelName = function (modelType) {
                        if (!modelType) {
                            return undefined;
                        }
                        return LLMName[modelType] || modelType;
                    };
                    $scope.model = model;
                    const getProperties = (modelType, userProperties = {}) => {
                        if (!modelType) {
                            return [];
                        }
                        return (LLMProperties[modelType] || []).map((property) => ({
                            ...property,
                            value: userProperties && typeof userProperties[property.property] !== 'undefined'
                                ? userProperties[property.property]
                                : property.value,
                        }));
                    };
                    const onChangeModel = (userProperties = {}) => {
                        $scope.properties = getProperties($scope.model, userProperties);
                    };
                    const gatherProperties = (properties = []) => (properties || []).reduce((r, property) => ({
                        ...r,
                        [property.property]: property.value
                    }), {})
                    $scope.onChangeModel = () => onChangeModel(gatherProperties($scope.properties));
                    onChangeModel(options);
                    $scope.ok = function () {
                        dispatcher.emit('llm:model:configuration:done', {
                            uuid,
                            options: {
                                ...gatherProperties($scope.properties || []),
                                type: $scope.model,
                            }
                        });
                        $mdDialog.hide();
                    };
                    $scope.cancel = function () {
                        $mdDialog.hide();
                    };
                },
                clickOutsideToClose: true
            });
        }
    };
    dispatcher.on('llm:model:configure', displayLLMConfigurationDialog);
}
