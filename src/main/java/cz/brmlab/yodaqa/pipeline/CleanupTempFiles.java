package cz.brmlab.yodaqa.pipeline;

import java.io.*;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.*;

public class CleanupTempFiles extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CleanupTempFiles.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
	    try {
	        File tmp = new File(System.getProperty("java.io.tmpdir"));
            cleanupTempFiles(tmp);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void cleanupTempFiles(File tmp) throws IOException {
        String[] files = tmp.list(new WildcardFileFilter(new String[] {
                "maltparser*", "jblas*", "tree-tagger*", "crfsuite*", "*.par", "*.crfsuite", "*.mco"
        }));
        if (files == null) return;
        for (String fileName : files) {
            File file = new File(tmp, fileName);
            if (file.isDirectory()) {
                cleanupTempFiles(file);
            }
            logger.info("Deleting file " + file.getAbsolutePath());
            file.deleteOnExit();
        }
    }
}
