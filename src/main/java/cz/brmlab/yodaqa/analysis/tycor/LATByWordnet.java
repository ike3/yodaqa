package cz.brmlab.yodaqa.analysis.tycor;

import java.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.*;

import cz.brmlab.yodaqa.model.TyCor.*;
import cz.brmlab.yodaqa.provider.MultiLanguageDictionaryFacade;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.*;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import net.sf.extjwnl.data.*;

/**
 * Generate less specific LAT annotations from existing LAT annotations
 * based on Wordnet relationships.  At this point, we generate LATs
 * with gradually reduced specificity based on hypernymy. */

public class LATByWordnet extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByWordnet.class);

	/** Whether to expand even LATs with synsets specified.
	 * Oftentimes, these are generated by precise-match rules (like from
	 * the question words or named entities) and should not be expanded
	 * anymore.  XXX: Maybe this should be just a separate LAT attribute. */
	public static final String PARAM_EXPAND_SYNSET_LATS = "expand-synset-lats";
	@ConfigurationParameter(name = PARAM_EXPAND_SYNSET_LATS, mandatory = false, defaultValue = "true")
	protected boolean expandSynsetLATs;

	MultiLanguageDictionaryFacade dictionary = null;

	/* We don't generalize further over the noun.Tops words that
	 * represent the most abstract hierarchy and generally words
	 * that have nothing in common anymore.
	 *
	 * sed -ne 's/^{ //p' data/wordnet/dict/dbfiles//noun.Tops | sed -re 's/[.:^_a-zA-Z0-9]+,[^ ]+ //g; s/ \(.*$//; s/\[ | \]|,//g; s/ .*$//'
	 *
	 * XXX: It would be better to have these as synset IDs, but that
	 * would be more complicated to obtain.
	 *
	 * N.B. there is another generalization limit instilled in
	 * LATMatchTyCor.
	 */
	protected static String tops_list[] = {
		"entity", "physical entity", "abstraction", "thing", "object",
		"whole", "congener", "living thing", "organism", "benthos",
		"dwarf", "heterotroph", "parent", "life", "biont", "cell",
		"causal agent", "person", "animal", "plant", "native",
		"natural object", "substance", "substance1", "matter", "food",
		"nutrient1", "artifact", "article", "psychological feature",
		"cognition", "motivation", "attribute", "state", "feeling",
		"location", "shape", "time", "space", "absolute space",
		"phase space", "event", "process", "act", "group", "relation",
		"possession", "social relation", "communication", "measure",
		"phenomenon",
	};
	protected Set<String> tops;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		if (tops == null)
		{
			tops = new HashSet<String>(Arrays.asList(tops_list));
		}

		super.initialize(aContext);

		dictionary = MultiLanguageDictionaryFacade.getInstance();
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Gather all LATs. */
		List<LAT> lats = new LinkedList<LAT>();
		for (LAT lat : JCasUtil.select(jcas, LAT.class)) {
			if (!expandSynsetLATs && lat.getSynset() != 0)
				continue;
			lats.add(lat);
		}

		/* Generate derived LATs. */
		Map<Synset, WordnetLAT> latmap = new HashMap<Synset, WordnetLAT>();
		/* TODO: Populate with existing LATs for deduplication. */
		for (LAT lat : lats) {
			try {
				genDerivedLATs(latmap, lat, lat.getPos().getPosValue());
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

		/* Add the remaining LATs. */
		for (WordnetLAT lat : latmap.values())
			lat.addToIndexes();
	}

	protected void genDerivedLATs(Map<Synset, WordnetLAT> latmap, LAT lat, String latpos) throws Exception {
		/* TODO: Use pos information from the parser?
		 * Currently, we just assume a noun and the rest is
		 * a fallback path that derives the noun.
		 * (Typically: How hot is the sun? -> hotness) */

		net.sf.extjwnl.data.POS wnpos;
        POS pos = lat.getPos();
        if (lat.getBase() instanceof NamedEntity) {
            wnpos = net.sf.extjwnl.data.POS.NOUN;
        } else if (pos instanceof N) {
            wnpos = net.sf.extjwnl.data.POS.NOUN;
        } else if (pos instanceof ADJ) {
            wnpos = net.sf.extjwnl.data.POS.ADJECTIVE;
        } else if (pos instanceof ADV) {
            wnpos = net.sf.extjwnl.data.POS.ADVERB;
        } else if (pos instanceof V) {
            wnpos = net.sf.extjwnl.data.POS.VERB;
        } else {
            logger.info("?! cannot expand LAT of POS " + latpos);
            return;
        }

		/* For a debug message, concatenate all generated wordnet LATs
		 * in this string. */
		boolean foundNoun = false;
		StringBuilder wnlist = new StringBuilder();

		if (lat.getSynset() == 0) {
			IndexWord w = null;
            try {
                w = dictionary.getIndexWord(wnpos, lat.getText(), lat.getLanguage());
            } catch (Exception e) {
                logger.warn("Error getting index word for: " + lat.getText(), e);
            }

			if (w == null)
			{
				logger.info("?! word " + lat.getText() + " of POS " + latpos + " not in Wordnet (mapped pos = " + wnpos + ")");
				return;
			}

			if (wnpos == net.sf.extjwnl.data.POS.NOUN)
			{
				/* Got a noun right away. */
				genDerivedSynsets(latmap, lat, w, wnlist, lat.getSpecificity() - 1);
				logger.debug("expanded LAT " + lat.getText() + " to wn LATs: " + wnlist.toString());
				return;
			}

			/* Try to derive a noun. */
			for (Synset synset : w.getSenses()) {
				boolean fnhere = genNounSynsets(latmap, lat, synset, wnpos, wnlist);
				foundNoun = foundNoun || fnhere;
				if (wnpos == net.sf.extjwnl.data.POS.VERB) {
					// ignore other senses since
					// nominalization is highly noisy;
					// see getNounSynsets() for details
					break;
				}
			}
		} else {
			Synset s = dictionary.getSynsetAt(wnpos, lat.getSynset(), lat.getLanguage());
			if (s == null) {
				logger.warn("?! word " + lat.getText() + "/" + lat.getSynset() + " of POS " + latpos + " not in Wordnet even though it has Wordnet sense assigned");
				return;
			}

			if (wnpos == net.sf.extjwnl.data.POS.NOUN) {
				/* Got a noun right away. */
				for (PointerTarget t : s.getTargets(PointerType.HYPERNYM)) {
					genDerivedSynsets(latmap, lat, (Synset) t, wnlist, lat.getSpecificity() - 1);
				}
				logger.debug("expanded LAT " + lat.getText() + "/" + lat.getSynset() + " to wn LATs: " + wnlist.toString());
				return;
			}

			/* Try to derive a noun. */
			foundNoun = genNounSynsets(latmap, lat, s, wnpos, wnlist);
		}

		if (!foundNoun && !latpos.matches(".*XSURROGATE$")) {
			/* We didn't find a noun but it turns out that
			 * we may need to flip adverb/adjective tag, e.g.
			 * for "how long" we need to consider "long" as
			 * an adjective to get to "length". */
			if (wnpos == net.sf.extjwnl.data.POS.ADVERB) {
				genDerivedLATs(latmap, lat, "JJXSURROGATE");
				return;
			} else if (wnpos == net.sf.extjwnl.data.POS.ADJECTIVE) {
				genDerivedLATs(latmap, lat, "RBXSURROGATE");
				return;
			}
		}

		if (!foundNoun) {
			logger.info("?! word " + lat.getText() + " of POS " + latpos + " in Wordnet as non-noun but derived from no noun");
		}
	}

	protected boolean genNounSynsets(Map<Synset, WordnetLAT> latmap, LAT lat,
			Synset synset, net.sf.extjwnl.data.POS wnpos, StringBuilder wnlist) throws Exception
	{
		boolean foundNoun = false;
		List<Word> nounWords = synset.getWords();
		Word nounWord = nounWords.get(0);

		logger.debug("checking noun synsets of " + nounWord.getLemma() + "/" + synset.getOffset());
		for (PointerTarget t : synset.getTargets(PointerType.ATTRIBUTE)) {
			Synset noun = (Synset) t;

			List<Word> nounFoundWords = noun.getWords();
			Word foundWord = nounFoundWords.get(0);
			foundNoun = true;

			logger.debug(".. adding LAT noun " + foundWord.getLemma());
			genDerivedSynsets(latmap, lat, noun, wnlist, lat.getSpecificity());
			logger.debug("expanded LAT " + lat.getText() + " to wn LATs: " + wnlist.toString());
		}
		if (wnpos == net.sf.extjwnl.data.POS.VERB) {
			/* For other non-nouns, this is too wide.  E.g. for
			 * "how deep", we want "depth" but not "mystery",
			 * "richness", "deepness", "obscureness", ... */
			Word nominalw = null;
			for (PointerTarget t : synset.getTargets(net.sf.extjwnl.data.PointerType.DERIVATION)) {
				Word nounw = (Word) t;
				foundNoun = true;
				if (nounw.getPOS() != net.sf.extjwnl.data.POS.NOUN)
					continue;
				nominalw = nounw;
			}
			/* Take only the last word (which is the most common
			 * one), to avoid pulling in anything obscure - and
			 * there is a lot of obscure stuff.  E.g. for die:
			 * death Death die breakdown dead_person end passing
			 * failure */
			if (nominalw != null) {
				logger.debug(".. adding LAT noun " + nominalw.getLemma());
				genDerivedSynsets(latmap, lat, nominalw.getSynset(), wnlist, lat.getSpecificity());
				logger.debug("expanded LAT " + lat.getText() + " to wn LATs: " + wnlist.toString());
			}
		}
		return foundNoun;
	}

	protected void genDerivedSynsets(Map<Synset, WordnetLAT> latmap, LAT lat,
			IndexWord wnoun, StringBuilder wnlist, double spec)
			throws Exception {
        try {
            for (Synset synset : wnoun.getSenses()) {
                for (PointerTarget t : synset.getTargets(PointerType.HYPERNYM)) {
                    genDerivedSynsets(latmap, lat, (Synset) t, wnlist, spec);
                }
            }
        } catch (Exception e) {
            logger.warn("Error getting senses from wordnet: " + lat.getText());
            return;
        }
	}

	protected void genDerivedSynsets(Map<Synset, WordnetLAT> latmap, LAT lat,
			Synset synset2, StringBuilder wnlist, double spec)
			throws Exception {
		WordnetLAT l2 = latmap.get(synset2);
		if (l2 != null) {
			/* Ok, already exists. Try to raise
			 * specificity if possible. */
			if (l2.getSpecificity() < spec) {
				l2.setSpecificity(spec);
				l2.setBase(lat.getBase());
				l2.setBaseLAT(lat);
			}
			return;
		}

		List<Word> words = synset2.getWords();
		Word word = words.get(0);
		String lemma = word.getLemma().replace('_', ' ');

		/* New LAT. */
		l2 = new WordnetLAT(lat.getCAS().getJCas());
		l2.setBegin(lat.getBegin());
		l2.setEnd(lat.getEnd());
		l2.setBase(lat.getBase());
		l2.setBaseLAT(lat);
		l2.setText(lemma);
		l2.setSpecificity(spec);
		l2.setIsHierarchical(true);
		l2.setSynset(synset2.getOffset());
		latmap.put(synset2, l2);

		wnlist.append(" | " + lemma + "/" + synset2.getOffset() + ":" + Double.toString(spec));

		/* ...and recurse, unless we got into the noun.Tops
		 * realm already. */
		if (!tops.contains(lemma)) {
			for (PointerTarget t : synset2.getTargets(PointerType.HYPERNYM)) {
				genDerivedSynsets(latmap, l2, (Synset) t, wnlist, spec - 1);
			}
		}
	}
}
