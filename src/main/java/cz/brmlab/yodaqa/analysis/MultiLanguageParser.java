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

	private Map<String, AnalysisEngine> pipelines = new HashMap<>();

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

        AnalysisEngineFactory_impl aeFactory = new AnalysisEngineFactory_impl();
        AnalysisEngineDescription pipelineDescRu = createEngineDescription("ru");
        pipelines.put("ru", (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDescRu, null));

        AnalysisEngineDescription pipelineDescEn = createEngineDescription("en");
        pipelines.put("en", (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDescEn, null));

        logger.info("{} instance is initialized", getClass().getName());
	}

	protected abstract AnalysisEngineDescription createEngineDescription(String language) throws ResourceInitializationException;

    public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
        try {
            run(jcas);
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
            class Watcher extends Thread {
                volatile boolean finished = false;
                Class<?> cls;
                String text, language;

                @Override
                public void run() {
                    int time = 0;
                    while (!finished) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        time+=1000;

                        if (time > 5000) {
                            logger.info("{} ({}) is still working on {}", cls.getName(), language, text);
                        }
                    }
                }
            };

            Watcher watcher = new Watcher();
            watcher.cls = getClass();
            watcher.text = jcas.getDocumentText();
            watcher.language = jcas.getDocumentLanguage();
            watcher.start();
            try {
                analysisEngine.process(jcas);
            } catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            } finally {
                watcher.finished = true;
                logger.info("{} ends to process jcas {}", getClass().getName(), jcas.getDocumentText());
            }
        } else {
            logger.error(String.format("MultiLanguageParser has no engine for language %s. CAS text = %s",
                    jcas.getDocumentLanguage(),
                    jcas.getDocumentText()));
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
	    Pattern ruPattern = Pattern.compile(".*[а-яА-ЯёЁ]+.*");
	    if (ruPattern.matcher(text).matches()) {
	        return "ru";
	    }

	    return "en";
	}
}

