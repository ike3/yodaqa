package cz.brmlab.yodaqa;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.interactive.*;
import cz.brmlab.yodaqa.pipeline.YodaQA;

/**
 * Где родился Лучано Паваротти
 * Сколько планет в Солнечной Системе
 * Сколько стран на Земле (TODO java.lang.ArrayIndexOutOfBoundsException: originPsgByClue) (PassByClue.java:120)
 * как правильно держать вилку
 *
 */
public class Evaluate {

    private static final String dir = "C:\\Users\\skuzmin\\Projects\\BigData\\yodaqa\\dump"; //System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws Exception {
        System.setProperty("cz.brmlab.yodaqa.cas_dump_dir", dir);
        System.setProperty("cz.brmlab.yodaqa.pipline_ru", "true");

        AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

        CollectionReaderDescription reader = createReaderDescription(
                LoadQuestionsFromFile.class,
                LoadQuestionsFromFile.PARAM_LANGUAGE, Language.RUSSIAN,
                LoadQuestionsFromFile.PARAM_INPUT, dir + "/questions.txt");

        AnalysisEngineDescription printer = createEngineDescription(InteractiveAnswerWriter.class,
                InteractiveAnswerWriter.PARAM_DIRECTORY, dir + "/answers");

        ParallelEngineFactory.registerFactory();

        MultiCASPipeline.runPipeline(reader, pipeline, printer);
    }

}
