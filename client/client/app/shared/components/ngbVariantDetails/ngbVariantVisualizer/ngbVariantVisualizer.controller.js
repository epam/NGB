import {ngbVariantDetailsController} from '../ngbVariantDetails.controller';
import ngbVariantVisualizerService from './ngbVariantVisualizer.service';
import {VariantRenderer} from '../../../../../modules/render';
import Tether from 'tether';
import $ from 'jquery';

export default class ngbVariantVisualizerController extends ngbVariantDetailsController {
    static get UID() {
        return 'ngbVariantVisualizerController';
    }

    _variantRendererDiv: HTMLElement;
    _variantRenderer: VariantRenderer;
    _service: ngbVariantVisualizerService;
    _variantVisualizerData = null;
    _scope;
    _timeout;
    _loaded = false;
    _geneFilesLoaded = false;
    _geneFiles = [];
    _selectedGeneFile = null;

    _tooltipElement = null;
    _tooltipTarget = null;
    _tetherElement: Tether = null;

    _tooltipContent = [];
    _visualizationError = null;

    _highlightBreakpoints = true;

    /* @ngInject */
    constructor($scope, $element, vcfDataService, constants, $timeout, ngbVariantVisualizerService, projectContext) {
        super($scope, vcfDataService, constants);
        this._scope = $scope;
        this._timeout = $timeout;
        this._service = ngbVariantVisualizerService;
        this.projectContext = projectContext;
        (async () => {
            await new Promise(resolve => $timeout(resolve));

            this._variantRendererDiv = $($element[0]).find('#cnv')[0];
            this._tooltipElement = $($element[0]).find('#vtooltip')[0];
            this._tooltipTarget = $($element[0]).find('.visualizer-tooltip-target')[0];
            const variantRenderer = this._variantRenderer = new VariantRenderer(this._variantRendererDiv, ::this.displayTooltip, ::this.affectedGeneTranscriptChanged);
            if (this._variantVisualizerData !== null && this._variantVisualizerData !== undefined) {
                this.variantRenderer.options = {
                    highlightBreakpoints: this._highlightBreakpoints
                };
                this.variantRenderer.variant = this._variantVisualizerData;
            }
            this.INIT();
            $scope.$on('$destroy', () => {
                variantRenderer.destroy();
            });

        })();
    }

    INIT() {
        if (this._service !== undefined && this._service !== null) {
            (async () => {
                if (this.variantRequest !== null && this.variantRequest !== undefined) {
                    const preAnalyzeResult = await this._service.preAnalyze(this.variantRequest);
                    if (preAnalyzeResult.error) {
                        this._visualizationError = preAnalyzeResult.error;
                        this._loaded = true;
                        this._geneFilesLoaded = true;
                    }
                    else {
                        this._visualizationError = null;
                        this._geneFilesLoaded = true;
                        this._geneFiles = preAnalyzeResult.geneFiles;
                        this._selectedGeneFile = preAnalyzeResult.selectedGeneFile;
                        this._scope.$watch('ctrl._variantVisualizerData.selectedAltField', () => {
                            if (this.variantRenderer !== null && this.variantRenderer !== undefined) {
                                this.variantRenderer.variantSubFeatureChanged();
                            }
                        });
                        this._scope.$watch('ctrl._highlightBreakpoints', () => {
                            if (this.variantRenderer !== null && this.variantRenderer !== undefined) {
                                this.variantRenderer.options = {
                                    highlightBreakpoints: this._highlightBreakpoints
                                };
                                this.variantRenderer.reRenderScene(true);
                            }
                        });
                        this._scope.$watch('ctrl._selectedGeneFile', () => {
                            this.getVariantVisualizerData();
                        });
                    }
                    this._scope.$apply();
                }
            })();
        }
    }

    getVariantVisualizerData() {
        (async () => {
            if (this.variantRequest !== null && this.variantRequest !== undefined) {
                if (!this._variantVisualizerData || this._variantVisualizerData.geneFile !== this._selectedGeneFile) {
                    this._loaded = false;
                    await new Promise(resolve => this._timeout(resolve));
                    this._scope.$apply();
                    this._variantVisualizerData = await this._service.analyze(this.variantRequest, this._selectedGeneFile);
                    await new Promise(resolve => this._timeout(resolve));
                    this._loaded = true;
                    if (this._variantVisualizerData.error) {
                        this._visualizationError = this._variantVisualizerData.error;
                    }
                    else {
                        this._visualizationError = null;
                    }
                    this._scope.$apply();
                    if (this.variantRenderer !== null && this.variantRenderer !== undefined && !this._variantVisualizerData.error) {
                        this.variantRenderer.options = {
                            highlightBreakpoints: this._highlightBreakpoints
                        };
                        this.variantRenderer.variant = this._variantVisualizerData;
                    }
                }
            }
        })();
    }

    affectedGeneChanged(breakpoint, gene) {
        breakpoint.affectedGene = gene;
        (async () => {
            if (this._variantVisualizerData) {
                this._variantVisualizerData = this._service.rebuildStructuralVariantVisualizerData(this._variantVisualizerData);
                await new Promise(resolve => this._timeout(resolve));
                this._loaded = true;
                if (this._variantVisualizerData.error) {
                    this._visualizationError = this._variantVisualizerData.error;
                }
                else {
                    this._visualizationError = null;
                }
                this._scope.$apply();
                if (this.variantRenderer !== null && this.variantRenderer !== undefined) {
                    this.variantRenderer.options = {
                        highlightBreakpoints: this._highlightBreakpoints
                    };
                    this.variantRenderer.variant = this._variantVisualizerData;
                }
            }

        })();
        return true;
    }

    affectedGeneTranscriptChanged(transcript) {
        (async () => {
            if (this._variantVisualizerData) {
                this._variantVisualizerData = this._service.changeAffectedTranscript(this._variantVisualizerData, transcript);
                await new Promise(resolve => this._timeout(resolve));
                this._loaded = true;
                if (this._variantVisualizerData.error) {
                    this._visualizationError = this._variantVisualizerData.error;
                }
                else {
                    this._visualizationError = null;
                }
                this._scope.$apply();
                if (this.variantRenderer !== null && this.variantRenderer !== undefined) {
                    this.variantRenderer.options = {
                        highlightBreakpoints: this._highlightBreakpoints
                    };
                    this.variantRenderer.updateVariant(this._variantVisualizerData, false);
                }
            }

        })();
        return true;
    }

    get variantRenderer(): VariantRenderer {
        return this._variantRenderer;
    }

    get variantRendererDiv(): HTMLElement {
        return this._variantRendererDiv;
    }

    displayTooltip(position, content) {
        if (content) {
            this._tooltipContent = content;
            this._scope.$apply();
            if ($(this._tooltipElement).hasClass('hidden')) {
                $(this._tooltipElement).removeClass('hidden');
            }
            this._tooltipTarget.style.left = `${position.x}px`;
            this._tooltipTarget.style.top = `${position.y}px`;
            if (!this._tetherElement) {
                this._tetherElement = new Tether({
                    target: this._tooltipTarget,
                    element: this._tooltipElement,
                    attachment: 'top left',
                    targetAttachment: 'middle middle',
                    offset: '-25 0'
                });
            }
            this._tetherElement.position();
        }
        else {
            this._tooltipContent = [];
            if (!$(this._tooltipElement).hasClass('hidden')) {
                $(this._tooltipElement).addClass('hidden');
            }
            if (this._tetherElement) {
                this._tetherElement.destroy();
                this._tetherElement = null;
            }
        }
    }

    saveVisualizerView() {
        if (this.variantRenderer !== null && this.variantRenderer !== undefined) {
            var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            svg.setAttribute("version", 1.1);
            svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
            svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", "http://www.w3.org/1999/xlink");
            svg.setAttribute("width", this.variantRenderer.width);
            svg.setAttribute("height", this.variantRenderer.height);

            var svgImage = document.createElementNS("http://www.w3.org/2000/svg", "image");
            svgImage.setAttribute("width", this.variantRenderer.width);
            svgImage.setAttribute("height", this.variantRenderer.height);
            svgImage.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", this.variantRenderer.canvasElement.querySelector('canvas').toDataURL());

            svg.appendChild(svgImage);

            var serializer = new XMLSerializer();
            var source = serializer.serializeToString(svg);

            if (!source.match(/^<svg[^>]+xmlns="http\:\/\/www\.w3\.org\/2000\/svg"/)) {
                source = source.replace(/^<svg/, '<svg xmlns="http://www.w3.org/2000/svg"');
            }
            if (!source.match(/^<svg[^>]+"http\:\/\/www\.w3\.org\/1999\/xlink"/)) {
                source = source.replace(/^<svg/, '<svg xmlns:xlink="http://www.w3.org/1999/xlink"');
            }

            source = '<?xml version="1.0" standalone="no"?>\r\n' + source;

            var url = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(source);
            const reference = this.projectContext.reference;
            const startIndex = this._variantVisualizerData.variantInfo.startIndex;
            const endIndex = this._variantVisualizerData.variantInfo.endIndex;

            Object.assign(document.createElement('a'), {
                download: `NGB_Variation_${reference.name}_${startIndex}_${endIndex}.svg`,
                name: `NGB_Variation_${reference.name}_${startIndex}_${endIndex}.svg`,
                href: url
            }).click();
        }
    }
}
