package cz.brmlab.yodaqa.answer.producer;

import cz.brmlab.yodaqa.MultiCASPipelineTest;
import cz.brmlab.yodaqa.PrintAnnotations;
import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.passage.PassageAnalysisAE;
import cz.brmlab.yodaqa.analysis.passextract.PassageExtractorAE;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceEnwiki;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.ClueLAT;
import cz.brmlab.yodaqa.model.Question.ClueNE;
import cz.brmlab.yodaqa.model.Question.QuestionClass;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.pipeline.YodaQA;
import cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocAnswerProducer;
import cz.brmlab.yodaqa.pipeline.solrfull.SolrFullAnswerProducer;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by okabanov on 16.02.2016.
 */
public class SolrTest extends MultiCASPipelineTest {
    private static final String dir = System.getProperty("java.io.tmpdir");

    private static String QUESTION;
    private static String RESULT;
    private static String CLUE_NE;
    private static String CLUE_LAT;

    public static class Tested extends SimpleQuestion {
        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas qView = jcas.createView("Question");
                qView.setDocumentText(QUESTION);
                qView.setDocumentLanguage(jcas.getDocumentLanguage());

                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

                QuestionClass qc = new QuestionClass(qView);
                qc.setQuestionClass("HUM");
                qc.addToIndexes(qView);

                ClueNE c1 = new ClueNE(jcas);
                c1.setLabel(CLUE_NE);
                c1.addToIndexes(jcas);

                ClueLAT c2 = new ClueLAT(jcas);
                c2.setLabel(CLUE_LAT);
                c2.addToIndexes(jcas);

                JCas rView = jcas.createView("Result");
                rView.setDocumentText(RESULT);
                rView.setDocumentLanguage(jcas.getDocumentLanguage());

                ResultInfo ri = new ResultInfo(rView);
                ri.setDocumentTitle(CLUE_NE);
                ri.addToIndexes(rView);

                AnswerSourceEnwiki as = new AnswerSourceEnwiki(AnswerSourceEnwiki.ORIGIN_FULL, "Some title", 1);
                QuestionDashboard.getInstance().get(qView).storeAnswerSource(as);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                JCas pickedPassagesView = jcas.getView("PickedPassages");
                PrintAnnotations.printDependecies(pickedPassagesView, System.out);
                FSIterator<TOP> answers = pickedPassagesView.getJFSIndexRepository().getAllIndexedFS(CandidateAnswer.type);
                int count = 0;
                while (answers.hasNext()) {
                    CandidateAnswer answer = (CandidateAnswer) answers.next();
                    System.out.println("ANSWER = " + answer.getCoveredText());
                    count++;
                }
                Assert.assertTrue(count > 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @Ignore
    public void solrTestEn() throws Exception {
        new YodaQA();
        AggregateBuilder builder = getAggregateBuilder();
        RESULT = "Garry potter answer";
        QUESTION = "Garry";
        CLUE_NE = "Potter";
        CLUE_LAT = "Garry";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }

    @Test
    @Ignore
    public void solrTestRu() throws Exception {
        new YodaQA();
        AggregateBuilder builder = getAggregateBuilder();
        RESULT = "Гарри поттер результат из теста";
        QUESTION = "Гарри";
        CLUE_NE = "Поттер";
        CLUE_LAT = "Гарри";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }

    private AggregateBuilder getAggregateBuilder() throws ResourceInitializationException {
        AggregateBuilder builder = new AggregateBuilder();
        AnalysisEngineDescription solrDoc = SolrDocAnswerProducer.createEngineDescription();
        AnalysisEngineDescription solrFull = SolrFullAnswerProducer.createEngineDescription();

        builder.add(PassageExtractorAE.createEngineDescription(PassageExtractorAE.PARAM_PASS_SEL_BYCLUE));
        builder.add(PassageAnalysisAE.createEngineDescription());
        builder.add(solrDoc);
        builder.add(solrFull);
        return builder;
    }
}
