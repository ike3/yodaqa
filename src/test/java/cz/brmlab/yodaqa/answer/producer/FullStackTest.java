package cz.brmlab.yodaqa.answer.producer;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.PassageAnalysisAE;
import cz.brmlab.yodaqa.analysis.passextract.PassageExtractorAE;
import cz.brmlab.yodaqa.flow.dashboard.*;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.SearchResult.*;
import cz.brmlab.yodaqa.pipeline.*;

/*
 */
public class FullStackTest extends MultiCASPipelineTest {
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

                ClueNE c1 = new ClueNE(qView);
                c1.setLabel(CLUE_NE);
                c1.addToIndexes(qView);

                ClueLAT c2 = new ClueLAT(qView);
                c2.setLabel(CLUE_LAT);
                c2.addToIndexes(qView);

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
    public void runEN() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(PassageExtractorAE.createEngineDescription(PassageExtractorAE.PARAM_PASS_SEL_BYCLUE));
        builder.add(PassageAnalysisAE.createEngineDescription());

        builder.add(AnalysisEngineFactory.createEngineDescription(
                DumpCAS2File.class,
                DumpCAS2File.PARAM_SAVE_DIR, dir,
                DumpCAS2File.PARAM_SUFFIX, "FullStack"));

        RESULT = "J. K. Rowling, a known writer of the book 'Harry Potter'";
        QUESTION = "Who wrote Harry Potter?";
        CLUE_NE = "Harry Potter";
        CLUE_LAT = "wrote";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(PassageExtractorAE.createEngineDescription(PassageExtractorAE.PARAM_PASS_SEL_BYCLUE));
        builder.add(PassageAnalysisAE.createEngineDescription());

        builder.add(AnalysisEngineFactory.createEngineDescription(
                DumpCAS2File.class,
                DumpCAS2File.PARAM_SAVE_DIR, dir,
                DumpCAS2File.PARAM_SUFFIX, "FullStack"));

        RESULT = "В сочельник вилку следует держать правой рукой. Это улучшает восприятие праздника 8 марта.";
        QUESTION = "игнор";
        CLUE_NE = "сочельник";
        CLUE_LAT = "8 марта";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
