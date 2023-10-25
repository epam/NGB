import { render } from 'preact';
import MainSection from "./sections/main/index";
import './style.css';
import KnownDrugs from "./sections/known-drugs";
import AssociatedDiseases from "./sections/associated-diseases";
import Sequences from "./sections/sequences";
import ComparativeGenomics from "./sections/comparative-genomics";
import Bibliography from "./sections/bibliography";
export function App() {
	return (
		<div>
			<MainSection />
      <KnownDrugs />
      <AssociatedDiseases />
      <Sequences />
      <ComparativeGenomics />
      <Bibliography />
		</div>
	);
}

render(<App />, document.getElementById('app'));
