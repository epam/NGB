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

    createColumn(container, position, chrPixelValue, chromosome, hitsColumns) {

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

            let offset = config.hit.offset;

            for (let colIndex = 0; colIndex < hitsColumns.length && colIndex <= this.hitsLimit; colIndex++){

            const hitColumn = hitsColumns[colIndex];

                for (let hitIndex = 0; hitIndex < hitColumn.length; hitIndex++){
                    let hit = hitColumn[hitIndex];
                    const hitPixelLength = this.convertToPixels(hit.endIndex - hit.startIndex);
                    const hitPixelStart =  (hit.startIndex/chromosome.size) * chrPixelValue;
                    column 
                        .lineStyle(config.hit.width, config.hit.lineColor, 1)
                        .moveTo(position.x + this.columnWidth + offset, hitPixelStart)
                        .lineTo(position.x + this.columnWidth + offset, hitPixelStart + hitPixelLength);
                }

                offset += config.hit.offset;
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
            const sortedHits = this.sortHitsByLength(chrHits);
            const hitsColumns = this.initBuildColumns(sortedHits);
            this.createColumn(container, position, pixelSize, chr, hitsColumns);
            position = {
                x: position.x + config.start.margin,
                y: 0
            };
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

    initBuildColumns(hits, columnsArray = []) {

        let remain = [];
        let column = [];
        const hitsColumns = columnsArray;

        hits.forEach((el, index) => {
            if (index === 0) {
                column.push(el);
            } else if (el.startIndex > column[column.length-1].endIndex){
                column.push(el);
            } else {
                remain.push(el);
            };
        });

        hitsColumns.push(column);

        if (remain.length){
                this.initBuildColumns(remain, hitsColumns);
                return hitsColumns;
        } else {
                return hitsColumns;
        };
  }
}
