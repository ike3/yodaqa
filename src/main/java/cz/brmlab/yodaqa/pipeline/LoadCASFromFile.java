package cz.brmlab.yodaqa.pipeline;

import java.io.*;
import java.util.*;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.*;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadCASFromFile extends CasCollectionReader_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LoadCASFromFile.class);

	public static final String PARAM_LOAD_DIR = "load-dir";
	@ConfigurationParameter(name = PARAM_LOAD_DIR, mandatory = true)
	protected String loadDir;

    public static final String PARAM_FILE_MASK = "file-mask";
	@ConfigurationParameter(name = PARAM_FILE_MASK, mandatory = true)
	protected String fileMask;

	private Iterator<String> fileIterator;
	private int fileCount;

    private int index = 0;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		loadDir = (String)aContext.getConfigParameterValue(PARAM_LOAD_DIR);
		fileMask = (String)aContext.getConfigParameterValue(PARAM_FILE_MASK);

		String[] files = new File(loadDir).list(new WildcardFileFilter(fileMask));
		if (files == null) throw new ResourceInitializationException("No files found by mask: " + fileMask,
		        new Object[] {}, new FileNotFoundException(fileMask));
        fileIterator = Arrays.asList(files).iterator();
        fileCount = files.length;
	}

    private AbstractCas loadNextCas(CAS jcas) {
        FileInputStream in = null;
        try {
            String loadFile = loadDir + File.separator + fileIterator.next();
            index++;

            // load XMI
            in = new FileInputStream(loadFile);
            logger.info("deserializing from {}", loadFile);
            XmiCasDeserializer.deserialize(in, jcas);
            in.close();
            return jcas;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getNext(CAS jcas) throws IOException, CollectionException {
        loadNextCas(jcas);
    }


    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(index, fileCount, Progress.ENTITIES)};
    }

    @Override
    public void close() {
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return fileIterator.hasNext();
    }
}
