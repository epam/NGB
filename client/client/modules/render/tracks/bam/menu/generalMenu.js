import {menu} from '../../../utilities';

export default {
    fields:[
        {
            disable: state => state.alignments = false,
            enable: state => state.alignments = true,
            isEnabled: state => state.alignments,
            label: 'Show alignments',
            name: 'bam>showAlignments',
            type: 'checkbox'
        },
        menu.getDivider(),
        {
            disable: state => state.mismatches = false,
            enable: state => state.mismatches = true,
            isEnabled: state => state.mismatches,
            label: 'Show mismatched bases',
            name: 'bam>showMismatchedBases',
            type: 'checkbox'
        },
        {
            disable: state => state.coverage= false,
            enable: state => state.coverage = true,
            isEnabled: state => state.coverage,
            label: 'Show coverage',
            name: 'bam>showCoverage',
            type: 'checkbox'
        },
        {
            disable: state => state.spliceJunctions = false,
            enable: state => state.spliceJunctions = true,
            isEnabled: state => state.spliceJunctions,
            label: 'Show splice junctions',
            name: 'bam>showSpliceJunctions',
            type: 'checkbox'
        },
        menu.getDivider(),
        menu.getActionButton(
          'bam>showSashimiPlot',
          'Sashimi plot',
          (tracks) => {
              const [dispatcher] = (tracks || [])
                  .map(track => track.dispatcher)
                  .filter(Boolean);
              if (dispatcher) {
                  dispatcher.emitSimpleEvent(
                      'bam:sashimi',
                      tracks.map(track => ({
                          cacheService: track.cacheService.clone(),
                          config: track.config
                      }))
                  );
              }
          }
        )
    ],
    label:'General',
    name:'bam>general',
    type: 'submenu'
};
