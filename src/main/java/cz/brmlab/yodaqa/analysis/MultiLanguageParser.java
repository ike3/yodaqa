package cz.brmlab.yodaqa.analysis;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.*;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.impl.AnalysisEngineFactory_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.*;


public abstract class MultiLanguageParser extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(MultiLanguageParser.class);

    public static final String PARAM_VIEW_NAME = "view-name";
    @ConfigurationParameter(name=PARAM_VIEW_NAME, defaultValue="")
    private String viewName;

	private Map<String, AnalysisEngine> pipelines = new HashMap<>();

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

        AnalysisEngineFactory_impl aeFactory = new AnalysisEngineFactory_impl();
        AnalysisEngineDescription pipelineDescRu = createEngineDescription("ru");
        pipelines.put("ru", (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDescRu, null));

        AnalysisEngineDescription pipelineDescEn = createEngineDescription("en");
        pipelines.put("en", (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDescEn, null));
	}

	protected abstract AnalysisEngineDescription createEngineDescription(String language) throws ResourceInitializationException;

    public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
        try {
            if (StringUtils.isEmpty(viewName)) {
                run(jcas);
                return;
            }

            for (Iterator<JCas> i = jcas.getViewIterator(); i.hasNext();) {
                JCas view = i.next();
                if (view.getViewName().equals(viewName)) {
                    run(view);
                    return;
                }
            }
        } catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }

	}

    private void run(JCas jcas) throws AnalysisEngineProcessException {
        if (StringUtils.isEmpty(jcas.getDocumentText())) {
            return;
        }

        AnalysisEngine analysisEngine = pipelines.get(jcas.getDocumentLanguage());
        if (analysisEngine != null) {
            analysisEngine.process(jcas);
        } else {
            logger.error(String.format("MultiLanguageParser has no engine for language %s. CAS text = %s, view = %s",
                    jcas.getDocumentLanguage(),
                    jcas.getDocumentText(),
                    viewName));
        }
    }

    public synchronized void destroy() {
	    for (Entry<String, AnalysisEngine> entry : pipelines.entrySet()) {
    		try {
    			entry.getValue().collectionProcessComplete();
    		} catch (Exception e) {
    		    logger.error("Error while destroying pipeline LANG=" + entry.getKey(), e);
    		}
    		entry.getValue().destroy();
	    }
	}

	public static String getLanguage(String text) {
	    Pattern ruPattern = Pattern.compile(
	            "[" +                   //начало списка допустимых символов
	                    "а-яА-ЯёЁ" +    //буквы русского алфавита
	                    "\\d" +         //цифры
	                    "\\s" +         //знаки-разделители (пробел, табуляция и т.д.)
	                    "\\p{Punct}" +  //знаки пунктуации
	            "]" +                   //конец списка допустимых символов
	            "*");                   //допускается наличие указанных символов в любом количестве
	    if (ruPattern.matcher(text).matches()) {
	        return "ru";
	    }

	    return "en";
	}
}

