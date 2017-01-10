/**
 * inspired by angular-drag-and-drop-lists API
 * https://github.com/marceljuenemann/angular-drag-and-drop-lists
 */
import angular from 'angular';

const prepareElement = (element) => {
    element.addClass('dndDragging');
    const nativeElement = element[0];
    nativeElement.style.position = 'absolute';
    nativeElement.style.top = '0';
    nativeElement.style.transform = 'translateY(0)';
    nativeElement.style.transition = 'none';
    nativeElement.style.zIndex = 1000;
};

const resetElement = (element) => {
    element.removeClass('dndDragging');
    const nativeElement = element[0];
    nativeElement.style.position = null;
    nativeElement.style.top = null;
    nativeElement.style.transform = null;
    nativeElement.style.transition = null;
    nativeElement.style.zIndex = null;
};

export default angular.module('dndLists', [])
    .directive('dndDraggable', () => function (scope, element, attr) {
        element.on('mousedown', (event) => {
            const initialEvent = event.originalEvent || event;
            if (initialEvent._ignoreDrag)
                return;

            const positionCorrection =
                element[0].getBoundingClientRect().top - element[0].parentElement.getBoundingClientRect().top;

            const maxY = element[0].parentElement.scrollHeight - element[0].scrollHeight;

            const getCurrentPos = event => positionCorrection + event.pageY - initialEvent.pageY - 1;

            const moveTarget = event => {

                element[0].style.transform = `translateY(${Math.max(0, Math.min(getCurrentPos(event), maxY))}px)`;
            };

            const onDragging = (event) => {
                scope.$emit('dragging', {
                    newPos: getCurrentPos(event),
                    applyElementStyle: () => moveTarget(event)
                });
                event.stopPropagation();
            };

            const endDraggingHandler = (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();

                scope.$emit('dragend', {
                    newPos: getCurrentPos(event),
                    applyElementStyle: () => resetElement(element)
                });
                window.removeEventListener('mousemove', onDragging);
                window.removeEventListener('mouseup', endDraggingHandler);
                window.removeEventListener('mouseleave', endDraggingHandler);
            };

            initialEvent.preventDefault();
            initialEvent.stopImmediatePropagation();

            window.addEventListener('mousemove', onDragging);
            window.addEventListener('mouseup', endDraggingHandler);
            window.addEventListener('mouseleave', endDraggingHandler);

            scope.$emit('dragstart', {
                dragTargetHeight: element[0].scrollHeight + 1, /*some chrome bug?*/
                transferable: scope.$eval(attr.dndDraggable),
                elementInstance: element[0],
                newPos: getCurrentPos(initialEvent),
                applyElementStyle: () => {
                    prepareElement(element);
                    moveTarget(initialEvent);
                }
            });
        });
    })

    .directive('dndList', () => function (scope, element, attr) {
        element[0].style.position = 'relative';
        scope.$on('dragstart', (e, {dragTargetHeight, transferable, applyElementStyle, elementInstance, newPos}) => {
            const children = [...element[0].children]
                .filter(child => child !== elementInstance);

            const heights = children
                .map(child => child.scrollHeight);

            //noinspection CommaExpressionJS
            const grid = ((totalHeight = 0) =>
                heights.map(height => {
                    const result = totalHeight + height / 2;
                    totalHeight += height;
                    return result;
                }))();

            const getIndex = (newPos) => {
                for (let i = 0; i < children.length; i++)
                    if (grid[i] >= newPos)
                        return Math.max(0, i - 1);
                return children.length;
            };

            const onDragging = (e, {newPos, applyElementStyle}) => {
                applyElementStyle();
                const index = getIndex(newPos);
                for (let i = 0; i < children.length; i++) {
                    children[i].style.transform =
                        i > index
                            ? `translateY(${dragTargetHeight}px)`
                            : 'translateY(0)';
                }
            };

            const offDragging = scope.$on('dragging', (e, data) => {
                completeInit();
                onDragging(e, data);
            });

            const offDragend = scope.$on('dragend', (e, {newPos, applyElementStyle}) => {
                let index = getIndex(newPos);

                element.removeClass('dndHasDraggingElement');
                applyElementStyle();
                for (let i = 0; i < children.length; i++) {
                    children[i].style.transition = 'none';
                    children[i].style.transform = null;
                    children[i].style.transition = null;
                }

                element[0].style.height = null;
                offDragging();
                offDragend();

                scope.$apply(function () {
                    const dndList = scope.$eval(attr.dndList);

                    const entryPlaceholder = {};
                    const originalIndex = dndList.indexOf(transferable);
                    //replace with placeholder to keep index
                    if (originalIndex !== -1)
                        dndList.splice(originalIndex, 1, entryPlaceholder);


                    if (index >= originalIndex)
                        index++; //It definitely has logic why it works in that way, I just cannot explain it.

                    dndList.splice(index, 0, transferable);

                    //remove placeholder
                    if (originalIndex !== -1)
                        dndList.splice(dndList.indexOf(entryPlaceholder), 1);

                    const tracksSettings = scope.ctrl.projectContext.tracksState || [];

                    const hiddenTracks = tracksSettings.filter(m => m.hidden === true);

                    const tracksState = [
                        ...dndList.map(file => {
                            const [fileSettings]= tracksSettings.filter(m => m.bioDataItemId.toString() === file.bioDataItemId.toString());
                            if (!fileSettings) {
                                return {
                                    bioDataItemId: file.bioDataItemId
                                };
                            } else {
                                const height = fileSettings.height;
                                const hidden = fileSettings.hidden;
                                return {
                                    height, hidden,
                                    bioDataItemId: file.bioDataItemId
                                };
                            }
                        }),
                        ...hiddenTracks];
                    console.log(tracksState);

                    scope.ctrl.projectContext.changeState({tracksState}, true);

                });
            });

            const completeInit = () => {
                if (completeInit.called)
                    return;
                completeInit.called = true;
                for (let i = 0; i < children.length; i++)
                    children[i].style.transitionDuration = null;

                element.addClass('dndHasDraggingElement');
            };

            element[0].style.height = `${element[0].clientHeight}px`;

            for (let i = 0; i < children.length; i++)
                children[i].style.transitionDuration = '0';

            onDragging(e, {
                newPos,
                applyElementStyle
            });

            process.nextTick(completeInit);
        });
    })

    .directive('dndNodrag', () => function (scope, element) {
        element.on('mousedown', (event) => {
            event = event.originalEvent || event;
            event._ignoreDrag = true;
        });
    })
    .name;