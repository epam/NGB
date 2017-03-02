/* @ngInject */
export default function Interceptor($q, $location, dispatcher) {
    return {

        // optional method
        'requestError': function () {
        },

        // optional method
        'responseError': function (rejection) {
            dispatcher.emitServiceError(rejection);

            return $q.reject(rejection);
        }
    };
}




