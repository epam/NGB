import { drawingConfiguration } from '../../../core';

export class ChromosomeColumnRenderer {

    constructor(container, canvasSize, drawingConfig, chromosomes, maxChrSize){
      
        Object.assign(this, {
            container,
            canvasSize,
            drawingConfig,
            chromosomes,
            maxChrSize
        });
    }

    get width() { 
        return this.canvasSize.width 
    };
    get height() { 
        return this.canvasSize.height 
    };

    get containerHeight() { 
        return this.height - 2 * this.drawingConfig.start.topMargin
    };

    get topMargin() { 
        return this.drawingConfig.start.topMargin;
    };

    get columnWidth() {
      return this.drawingConfig.chromosomeColumn.width;
    }

    get chrPixelValue() {
        return (this.currentChromosome.size/ this.maxChrSize) * this.containerHeight;
    }
  
    init() { 
        const container = new PIXI.Container();
        container.x = this.drawingConfig.start.margin;
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
          .beginFill(this.drawingConfig.chromosomeColumn.fill, 1)
          .drawRect(
              position.x,
              0,
              this.columnWidth,
              chrPixelValue)
          .endFill()
          .lineStyle(this.drawingConfig.chromosomeColumn.thickness, this.drawingConfig.chromosomeColumn.lineColor, 1)
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
          x: this.drawingConfig.start.x,
          y: 0 
      }
      for (let i = 0; i < this.chromosomes.length; i++){

        let chr = this.chromosomes[i];
        const pixelSize = this.convertToPixels(chr.size)
        this.createColumn(container, position, pixelSize, chr.id);
        position = {
          x: position.x + this.drawingConfig.start.margin,
          y: 0
        }
      }
    }

    convertToPixels(size) {
      const pixelSize = (size/ this.maxChrSize) * this.containerHeight;
      return pixelSize;
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
