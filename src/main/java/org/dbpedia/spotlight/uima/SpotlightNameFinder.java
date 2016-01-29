package org.dbpedia.spotlight.uima;

import java.io.*;
import java.util.*;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.response.*;
import org.slf4j.*;

import com.sun.jersey.api.client.*;

import cz.brmlab.yodaqa.provider.rdf.*;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;


public class SpotlightNameFinder extends JCasAnnotator_ImplBase {

    final Logger LOG = LoggerFactory.getLogger(SpotlightNameFinder.class);

	public static final String PARAM_ENDPOINT = "endPoint";
	@ConfigurationParameter(name=PARAM_ENDPOINT)
	private String SPOTLIGHT_ENDPOINT;

	// Default values for the web service parameters for the spotlight endpoint

	public static final String PARAM_CONFIDENCE = "confidence";
	@ConfigurationParameter(name=PARAM_CONFIDENCE, defaultValue="0.0")
	private double CONFIDENCE;
	public static final String PARAM_SUPPORT = "support";
	@ConfigurationParameter(name=PARAM_SUPPORT, defaultValue="0")
	private int SUPPORT;
	public static final String PARAM_TYPES = "types";
	@ConfigurationParameter(name=PARAM_TYPES, defaultValue="")
	private String TYPES;
	public static final String PARAM_SPARQL = "sparql";
	@ConfigurationParameter(name=PARAM_SPARQL, defaultValue="")
	private String SPARQL;
	public static final String PARAM_POLICY = "policy";
	@ConfigurationParameter(name=PARAM_POLICY, defaultValue="whitelist")
	private String POLICY;
	public static final String PARAM_COREFERENCE_RESOLUTION = "coferenceResolution";
	@ConfigurationParameter(name=PARAM_COREFERENCE_RESOLUTION, defaultValue="true")
	private boolean COREFERENCE_RESOLUTION;
	public static final String PARAM_SPOTTER = "spotter";
	@ConfigurationParameter(name=PARAM_SPOTTER, defaultValue="Default")
	private String SPOTTER;
	public static final String PARAM_DISAMBIGUATOR = "disambiguator";
	@ConfigurationParameter(name=PARAM_DISAMBIGUATOR, defaultValue="Default")
	private String DISAMBIGUATOR;

	private final int BATCH_SIZE = 10;

    private static final List<String> NER_VARIANTS = Arrays.asList(new String[] {
        "date", "location", "money", "organization",
        "percentage", "person", "time"
    });
    private static final Map<String, String> NER_MAPPING = new HashMap<>();
    {
        NER_MAPPING.put("company", "organization");
        NER_MAPPING.put("region", "location");
    }

	final DBpediaTypes dbt = new DBpediaTypes();

	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String documentText = aJCas.getDocumentText();

		// don't query endpoint without text
		if (documentText == null || documentText.isEmpty()) {
			return;
		}

		Client c = Client.create();

		BufferedReader documentReader = new BufferedReader(new StringReader(documentText));
		//Send requests to the server by dividing the document into sentence chunks determined by BATCH_SIZE.
		int documentOffset = 0;
		int numLines = 0;
		boolean moreLines = true;
		while (moreLines){
			String request = "";
			for (int index = 0; index < BATCH_SIZE; index++) {
				String line = null;
				try {
					line = documentReader.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LOG.error("Can't read from input file",e);
				}
				if (line == null) {
					moreLines = false;
					break;
				}else if (index !=0){
					request += "\n";
				}
				request += line;
				numLines++;
			}
			if (request == null || request.isEmpty()) {
				break;
			}


			Annotation response = null;
			boolean retry = false;
			int retryCount = 0;
			do{
				try{

					LOG.info("Sending request to the server");

					WebResource r = c.resource(SPOTLIGHT_ENDPOINT);
					response =
							r.queryParam("text", request)
							.queryParam("confidence", "" + CONFIDENCE)
							.queryParam("support", "" + SUPPORT)
							.queryParam("types", TYPES)
							.queryParam("sparql", SPARQL)
							.queryParam("policy", POLICY)
							.queryParam("coreferenceResolution",
									Boolean.toString(COREFERENCE_RESOLUTION))
							.queryParam("spotter", SPOTTER)
							.queryParam("disambiguator", DISAMBIGUATOR)
							.type("application/x-www-form-urlencoded;charset=UTF-8")
							.accept(MediaType.TEXT_XML)
							.post(Annotation.class);
					retry = false;
				} catch (Exception e){
					//In case of a failure, try sending the request with a 2 second delay at least three times before throwing an exception
					LOG.error("Server request failed. Will try again in 2 seconds..", e);
					LOG.error("Failed request payload: " +request);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						LOG.error("Thread interrupted",e1);
					}
					if (retryCount++ < 3){
						retry = true;
					} else {
						throw new AnalysisEngineProcessException("The server request failed", null);
					}
				}
			}while(retry);

					LOG.info("Server request completed. {} resources found", response.getResources().size());
					/*
					 * Add the results to the AnnotationIndex
					 */
					for (Resource resource : response.getResources()) {
                        int begin = documentOffset + new Integer(resource.getOffset());
                        int end = begin + resource.getSurfaceForm().length();
                        String label = extractLabel(resource.getURI());
                        if (label == null) {
                            label = aJCas.getDocumentText().substring(begin, end);
                        }

                        String variant = queryDbp(aJCas, label);
                        if (variant != null) {
                            NamedEntity ne = new NamedEntity(aJCas, begin, end);
                            ne.setValue(variant);
                            ne.addToIndexes(aJCas);
                        }

						/*JCasResource res = new JCasResource(aJCas);
						res.setBegin(begin);
                        res.setEnd(end);
						res.setSimilarityScore(new Double(resource.getSimilarityScore()));
						res.setTypes(resource.getTypes());
						res.setSupport(new Integer(resource.getSupport()));
						res.setURI(resource.getURI());
						res.addToIndexes(aJCas);*/
					}

					documentOffset += request.length() + 1 ;

		}
		try {
			documentReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    private String extractLabel(String uri) {
        int pos = uri.lastIndexOf('/');
        if (pos == -1) {
            return null;
        }

        return uri.substring(pos + 1).replace('_', ' ');
    }

    private String queryDbp(JCas aJCas, String label) {
        List<String> types = dbt.query(label, LOG, aJCas.getDocumentLanguage());
        List<String> mappedTypes = new ArrayList<>(types);
        for (String type : types) {
            String[] words = type.split("\\s+");
            for (String word : words) {
                String mapped = NER_MAPPING.get(word.toLowerCase());
                if (mapped != null) {
                    mappedTypes.add(0, mapped);
                }
            }
        }
        LOG.info("{} / {}", label, StringUtils.join(mappedTypes, ','));

        Set<String> variants = new LinkedHashSet<>(NER_VARIANTS);
        for (Iterator<String> variantIter = variants.iterator(); variantIter.hasNext(); ) {
            String variant = variantIter.next();
            for (String type : mappedTypes) {
                if (type.toLowerCase().contains(variant)) {
                    return variant;
                }
            }
        }

        return null;
    }

}
