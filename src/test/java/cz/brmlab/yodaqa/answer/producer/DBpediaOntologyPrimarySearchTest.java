package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.util.*;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.flow.dashboard.*;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.pipeline.YodaQA;
import cz.brmlab.yodaqa.pipeline.structured.*;
import edu.stanford.nlp.util.StringUtils;

/*
 * Сравнивает Clue и NamedEntity
 */
public class DBpediaOntologyPrimarySearchTest extends MultiCASPipelineTest {

    private static String INPUT;
    private static List<String> OUTPUT = new ArrayList<>();
    private static String EXPECTED_TEXT;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas rView = jcas.createView("Result");
                rView.setDocumentText(INPUT);
                rView.setDocumentLanguage(jcas.getDocumentLanguage());
                ResultInfo ri = new ResultInfo(rView);
                ri.addToIndexes(rView);

                JCas qView = jcas.createView("Question");
                qView.setDocumentLanguage(jcas.getDocumentLanguage());
                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

                JCas pView = jcas.createView("PickedPassages");
                pView.setDocumentLanguage(jcas.getDocumentLanguage());
                pView.setDocumentText(rView.getDocumentText());

                Concept concept = new Concept(jcas);
                concept.setCookedLabel(INPUT);
                concept.addToIndexes(jcas);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                Question question = QuestionDashboard.getInstance().get(jcas.getView("Question"));
                // TODO: what to assert?
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runRU() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(DBpediaOntologyPrimarySearch.class));
        builder.add(createPrimitiveDescription(DBpediaPropertyPrimarySearch.class));

        INPUT = "путин, владимир владимирович";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
        System.out.println(StringUtils.join(OUTPUT, "\n"));
    }
}
