import { drawingConfiguration } from '../../../core';
import config from '../../whole-genome-config';

export class ChromosomeColumnRenderer {

    constructor({ container, canvasSize, chromosomes, range: maxChrSize, hits }){

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize,
            hits
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

    get hitsLimit() {
      return Math.floor(config.start.margin/(config.hit.width + config.hit.offset));
    }
  
    init() { 
        const container = new PIXI.Container();
        container.x = config.start.margin;
        container.y = this.topMargin;
        this.buildColumns(container);
        this.container.addChild(container);
        return container;
    }

    createColumn(container, position, chrPixelValue, chromosome, hits) {

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
          .lineTo(position.x, 0)
          .moveTo(position.x + config.hit.offset, 0)
      
      const sortedHits = this.sortHitsByLength(hits);
      for (let i = 0; i < sortedHits.length && i <= this.hitsLimit; i++){

          let hit = sortedHits[i];
          let offset = config.hit.offset;

          const hitPixelLength = this.convertToPixels(hit.endIndex - hit.startIndex);
          const hitPixelStart =  (hit.startIndex/chromosome.size) * chrPixelValue;

          column 
            .lineStyle(config.hit.width, config.hit.lineColor, 1)
            .moveTo(position.x + this.columnWidth +  offset * (i+1), hitPixelStart)
            .lineTo(position.x + this.columnWidth + offset * (i+1), hitPixelStart + hitPixelLength);
      }

      const label = this.createLabel(`chr ${chromosome.id}`, position);
      container.addChild(label);
    }

    buildColumns(container) {

      let position = {
          x: config.start.x,
          y: 0 
      }
      
      for (let i = 0; i < this.chromosomes.length; i++){

        let chr = this.chromosomes[i];
        const chrHits = this.hits.filter(hit => hit.chromosome === chr.name);

        const pixelSize = this.convertToPixels(chr.size);
        this.createColumn(container, position, pixelSize, chr, chrHits);

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
        fill: config.tick.label.fill,
        font: config.tick.label.font,
        margin:config.tick.label.margin,
    });
      label.resolution = drawingConfiguration.resolution;
      label.y = position.y - this.topMargin/2 - label.height/2;
      label.x = 2 * position.x;
      return label;
    }

    sortHitsByLength(hits){
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
}
