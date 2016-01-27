package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.io.debug.DumpConstituents;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionAnalysisEngineRu {

    final static Logger logger = LoggerFactory.getLogger(QuestionAnalysisEngineRu.class);

    public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(TreeTaggerPosTagger.class));

        if (logger.isDebugEnabled()) {
            builder.add(AnalysisEngineFactory.createEngineDescription(DumpConstituents.class));
            builder.add(AnalysisEngineFactory.createEngineDescription(
                    CasDumpWriter.class,
                    CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-qacas.txt"));
        }

        AnalysisEngineDescription aed = builder.createAggregateDescription();
        aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.question.QuestionAnalysisAE");
        return aed;
    }

}
