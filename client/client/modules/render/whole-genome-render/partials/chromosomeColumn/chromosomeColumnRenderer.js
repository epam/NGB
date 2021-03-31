import { drawingConfiguration } from '../../../core';
import config from '../../whole-genome-config';

export class ChromosomeColumnRenderer {

    constructor({ container, canvasSize, chromosomes, range: maxChrSize }){

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize
        });
    }

    get width() { 
        return this.canvasSize ? this.canvasSize.width : 0;
    };
    get height() { 
        return this.canvasSize ? this.canvasSize.height : 0;
    };

    get containerHeight() { 
        return this.height - 2 * config.start.topMargin;
    };

    get topMargin() { 
        return config.start.topMargin;
    };

    get columnWidth() {
      return config.chromosomeColumn.width;
    }

    get chrPixelValue() {
        return (this.currentChromosome.size / this.maxChrSize) * this.containerHeight;
    }
  
    init() { 
        const container = new PIXI.Container();
        container.x = config.start.margin;
        container.y = this.topMargin;
        this.buildColumns(container);
        this.container.addChild(container);
        return container;
    }

    createColumn(container, position, chrPixelValue, id) {
      const column = new PIXI.Graphics();
      column.x = position.x;
      column.y = 0;
      container.addChild(column);

      column
          .beginFill(config.chromosomeColumn.fill, 1)
          .drawRect(
              position.x,
              0,
              this.columnWidth,
              chrPixelValue)
          .endFill()
          .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 1)
          .moveTo(position.x, 0)
          .lineTo(position.x + this.columnWidth, 0)
          .lineTo(position.x + this.columnWidth, chrPixelValue)
          .lineTo(position.x, chrPixelValue)
          .lineTo(position.x, 0);

      const label = this.createLabel(`chr ${id}`, position);
      container.addChild(label);
    }

    buildColumns(container) {

      let position = {
          x: config.start.x,
          y: 0 
      }
      for (let i = 0; i < this.chromosomes.length; i++){

        let chr = this.chromosomes[i];
        const pixelSize = this.convertToPixels(chr.size)
        this.createColumn(container, position, pixelSize, chr.id);
        position = {
          x: position.x + config.start.margin,
          y: 0
        }
      }
    }

    convertToPixels(size) {
      return (size / this.maxChrSize) * this.containerHeight;
    }

    createLabel(text, position) {
      const label = new PIXI.Text(text, {
        fill: 0x000000,
        font: 'normal 7pt arial',
        margin:4
    });
      label.resolution = drawingConfiguration.resolution;
      label.y = position.y - this.topMargin/2 - label.height/2;
      label.x = 2 * position.x;
      return label;
    }
}
