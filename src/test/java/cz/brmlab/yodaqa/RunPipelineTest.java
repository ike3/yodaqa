package cz.brmlab.yodaqa;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.junit.Test;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.YodaQA;

public class RunPipelineTest {
    String dir = "C:\\Users\\skuzmin\\Projects\\BigData\\yodaqa\\dump\\";

    @Test
    public void run() throws Exception {
        System.setProperty("cz.brmlab.yodaqa.cas_dump_dir", dir);
        System.setProperty("cz.brmlab.yodaqa.save_answerfvs", dir);
        System.setProperty("cz.brmlab.yodaqa.save_answer1fvs", dir);
        System.setProperty("cz.brmlab.yodaqa.save_answer2fvs", dir);

        AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

        CollectionReaderDescription reader = createReaderDescription(
                SimpleQuestion.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "Who wrote Harry Potter?");

        AnalysisEngineDescription printer = createEngineDescription(InteractiveAnswerPrinter.class);

        ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow

        MultiCASPipeline.runPipeline(reader, pipeline, printer);
    }

}
