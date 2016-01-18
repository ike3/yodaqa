package cz.brmlab.yodaqa.pipeline;

import java.io.*;
import java.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.*;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;

public class DumpCAS2File extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(DumpCAS2File.class);

	/** Directory in which to store for serialized answer data. */
	public static final String PARAM_SAVE_DIR = "save-dir";
	public static final String PARAM_SUFFIX = "suffix";

	@ConfigurationParameter(name = PARAM_SAVE_DIR, mandatory = true)
	protected String saveDir;

	@ConfigurationParameter(name = PARAM_SUFFIX, mandatory = true)
	protected String suffix;

	protected boolean enable = true;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public static Map<String, Integer> ids = new HashMap<>();

	public void process(JCas jcas) throws AnalysisEngineProcessException {
	    if (!enable) return;

        String qKey = null, aKey = null, rKey = null, hlKey = null, key = null;

        try {
            key = qKey = "Q" + JCasUtil.selectSingle(jcas.getView("Question"), de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT.class).getCoveredText();
        } catch (Exception e) {
        }

        try {
            key = rKey = "R" + JCasUtil.selectSingle(jcas.getView("Result"), de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT.class).getCoveredText();
        } catch (Exception e) {
        }

        try {
            key = aKey = "A" + JCasUtil.selectSingle(jcas.getView("Answer"), de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT.class).getCoveredText();
        } catch (Exception e) {
        }

        try {
            key = hlKey = "HL" + JCasUtil.selectSingle(jcas.getView("AnswerHitlist"), de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT.class).getCoveredText();
        } catch (Exception e) {
        }

        Integer id = getNextId(key);

		FileOutputStream out = null;

		String dir = saveDir + File.separator + suffix;
		String fileName = dir + File.separator;
		if (hlKey != null) {
		    fileName += String.format("HL-%d.xmi", id);
		}
        else if (rKey != null) {
            fileName += String.format("R-%d.xmi", id);
        } else if (aKey != null) {
		    fileName += String.format("A-%d.xmi", id);
		} else {
		    try {
                for (Iterator<JCas> i = jcas.getViewIterator(); i.hasNext();) {
                    JCas view = i.next();
                    logger.error(view.getViewName());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
            fileName += String.format("%d.xmi", id);
		}

		try {
			(new File(dir)).mkdirs();

			// write XMI
			out = new FileOutputStream(fileName);
			logger.debug("serializing to {} - {}", fileName, key);
			XmiCasSerializer ser = new XmiCasSerializer(jcas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, true);
			ser.serialize(jcas.getCas(), xmlSer.getContentHandler());
			out.close();
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

    private Integer getNextId(String key) {
        Integer id = 0;
		synchronized(ids) {
		    id = ids.get(key);
		    if (id == null) id = 0;
		    ids.put(key, id + 1);
	    }
        return id;
    }
}
