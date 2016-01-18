package cz.brmlab.yodaqa.analysis;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.*;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.flow.dashboard.*;
import cz.brmlab.yodaqa.pipeline.*;


public class AnswerAnalysisAETest {
    String dir = "C:\\Users\\skuzmin\\Projects\\BigData\\yodaqa\\dump\\";

    @Test
    public void run() throws Exception {
        AnalysisEngineDescription pipeline = createQuestionAnalysisAE();

        CollectionReaderDescription reader = createReaderDescription(
                LoadCASFromFile.class,
                LoadCASFromFile.PARAM_LOAD_DIR, dir + "/AnswerProducerAE",
                LoadCASFromFile.PARAM_FILE_MASK, "A-*.xmi");

        Question q = new Question(Integer.toString(1), "Who wrote Harry Potter?");
        QuestionDashboard.getInstance().askQuestion(q);
        QuestionDashboard.getInstance().getQuestionToAnswer();

        ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow
        MultiCASPipeline.runPipeline(reader, pipeline);
    }

    private AnalysisEngineDescription createQuestionAnalysisAE() throws ResourceInitializationException {
        AggregateBuilder builder = new AggregateBuilder();

        builder.add(AnswerAnalysisAE.createEngineDescription());

        AnalysisEngineDescription answerCASMerger = AnalysisEngineFactory.createEngineDescription(
                AnswerCASMerger.class,
                AnswerCASMerger.PARAM_ISLAST_BARRIER, 2,
                AnswerCASMerger.PARAM_PHASE, 1,
                ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1);
        builder.add(answerCASMerger);

        //builder.add(AnswerScoringAE.createEngineDescription(""));

        builder.add(AnalysisEngineFactory.createEngineDescription(DumpCAS2File.class, DumpCAS2File.PARAM_SAVE_DIR, dir,
                DumpCAS2File.PARAM_SUFFIX, "TestAE"));

        builder.setFlowControllerDescription(
                FlowControllerFactory.createFlowControllerDescription(
                    FixedFlowController.class,
                    FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

        AnalysisEngineDescription aed = builder.createAggregateDescription();
        aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.pipeline.YodaQA");
        //aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);

        return aed;
    }
}
