package cz.brmlab.yodaqa.analysis.tycor;

import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.*;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.PointerTarget;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.Synset;

import java.util.Map;

public class LATByWordnetGeneral extends LATByWordnet {

    @Override
    protected void genDerivedLATs(Map<Synset, WordnetLAT> latmap, LAT lat, String latpos) throws Exception {
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
            IndexWord w = dictionary.getIndexWord(wnpos, lat.getText(), lat.getLanguage());

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
}
