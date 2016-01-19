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

    public IndexWord getIndexWord(POS pos, String lemma, String language) throws JWNLException {
        return dictionaryMap.get(enByDefault(language)).getIndexWord(pos, lemma);
    }

    public Synset getSynsetAt(POS pos, long offset, String language) throws JWNLException {
        return dictionaryMap.get(enByDefault(language)).getSynsetAt(pos, offset);
    }

    private String enByDefault(String language) {
        return language == null ? "en" : language;
    }
}
