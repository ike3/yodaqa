package cz.brmlab.yodaqa.provider;

import java.util.*;

import org.apache.uima.resource.ResourceInitializationException;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

/** A singleton that provides a shared dictionary instance. */
public class Wordnet {
    public static final String RESOURCE_CONFIG_PATH_RU = "/extjwnl_resource_properties_ru.xml";

	/* Singleton. */
	private Wordnet() {}

	private static Dictionary dictionary = null;
	public synchronized static Dictionary getDictionary() throws ResourceInitializationException {
		if (dictionary == null) {
			try {
				dictionary = Dictionary.getDefaultResourceInstance();
			} catch (JWNLException e) {
				throw new ResourceInitializationException(e);
			}
		}
		return dictionary;
	}

	private static Map<String, Dictionary> dictionaryMap = null;
	public synchronized static Map<String, Dictionary> getDictionaryMap() throws ResourceInitializationException {
	    if (dictionaryMap == null) {
	        try {
	            dictionaryMap = new HashMap<>();
	            dictionaryMap.put("en", Dictionary.getDefaultResourceInstance());
	            dictionaryMap.put("ru", Dictionary.getResourceInstance(RESOURCE_CONFIG_PATH_RU));
	        } catch (JWNLException e) {
	            throw new ResourceInitializationException(e);
	        }
	    }
	    return dictionaryMap;
	}
}
