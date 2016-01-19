package cz.brmlab.yodaqa.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.*;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.*;

/*
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX : <http://dbpedia.org/resource/>
PREFIX dbpedia2: <http://dbpedia.org/property/>
PREFIX dbpedia: <http://dbpedia.org/>
PREFIX dbo: <http://dbpedia.org/ontology/>
SELECT ?type ?superType WHERE { {
  ?res rdfs:label "Солнце"@ru.
} UNION {
  ?redir dbo:wikiPageRedirects ?res .
  ?redir rdfs:label "Солнце"@ru .
}
?res rdf:type ?type .
OPTIONAL {
  ?type rdfs:subClassOf ?superType .
}
FILTER ( regex(str(?res), '^http://dbpedia.org/resource/', 'i') )
FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )
}
 */
public class LATByDBpediaTest {
    private static String EXPECTED_OUTPUT;

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                AnswerInfo ai = new AnswerInfo(jcas);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(jcas);
                AnswerFV fv = new AnswerFV(ai);
                ai.setFeatures(fv.toFSArray(jcas));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                for (DBpLAT lat : JCasUtil.select(jcas, DBpLAT.class)) {
                    if (EXPECTED_OUTPUT.equals(lat.getText()))
                        return;
                }
                Assert.fail();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByDBpedia.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "sun");

        EXPECTED_OUTPUT = "star";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByDBpedia.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "Солнце");

        EXPECTED_OUTPUT = "star";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
