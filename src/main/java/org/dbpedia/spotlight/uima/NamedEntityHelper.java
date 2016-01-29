package org.dbpedia.spotlight.uima;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.*;

public class NamedEntityHelper {
    public static String getLabel(NamedEntity ne) {
        if (ne instanceof NamedEntityEx) {
            return ((NamedEntityEx) ne).getCanonText();
        }

        return ne.getCoveredText();
    }
}
