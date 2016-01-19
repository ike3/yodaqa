package cz.brmlab.yodaqa.provider;

import java.util.*;

import org.apache.uima.resource.ResourceInitializationException;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;

public class MultiLanguageDictionaryFacade {
    private static Map<String, Dictionary> dictionaryMap = null;
    private static MultiLanguageDictionaryFacade instance = new MultiLanguageDictionaryFacade();

    public synchronized static MultiLanguageDictionaryFacade getInstance() throws ResourceInitializationException {
        if (dictionaryMap == null) {
            dictionaryMap = Wordnet.getDictionaryMap();
        }
        return instance;
    }

    public IndexWord getIndexWord(POS pos, String lemma) throws JWNLException {
        IndexWord result = dictionaryMap.get("en").getIndexWord(pos, lemma);
        return result != null ? result : dictionaryMap.get("ru").getIndexWord(pos, lemma);
    }

    public Synset getSynsetAt(POS pos, long offset) throws JWNLException {
        Synset result = dictionaryMap.get("en").getSynsetAt(pos, offset);
        return result != null ? result : dictionaryMap.get("ru").getSynsetAt(pos, offset);
    }
}
