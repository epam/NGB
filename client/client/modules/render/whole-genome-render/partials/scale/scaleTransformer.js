import  NumberFormatter  from '../../../utilities/numberFormatter';

export class ScaleTransformer {

    static buildTicks(range, ticksCount) {

        const closestDecimalDegree = NumberFormatter.findClosestDecimalDegree(Math.max(range, 0), ticksCount);
        const decimalBase = 10;
        const module = Math.pow(decimalBase, closestDecimalDegree);
        const subModule = module / 2;
        const tickStep = ScaleTransformer.findBestStep(range / (ticksCount + 1), subModule);

        const ticks = [];
        for (let i = 0; i <= (range + tickStep); i += tickStep){
            ticks.push({ value: i })
        }
        return ticks;
        }

        static findBestStep(value, module) {
        const v1 = Math.floor(value / module) * module;
        const v2 = v1 + module;

        const d2 = (v2 - value) / module;
        const moduleAcceptanceFactor = 0.3;

        if (d2 < moduleAcceptanceFactor) {
            return v2;
        }
        return v1;
    }
}
