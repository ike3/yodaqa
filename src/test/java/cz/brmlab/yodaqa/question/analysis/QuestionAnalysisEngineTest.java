package cz.brmlab.yodaqa.question.analysis;

import cz.brmlab.yodaqa.Language;
import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.RootGenerator;
import cz.brmlab.yodaqa.analysis.question.*;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnetGeneral;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.SpotlightNameFinder;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;


public class QuestionAnalysisEngineTest {

    public static class Printer extends JCasAnnotator_ImplBase {

        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException {
            Iterator<Dependency> iter = JCasUtil.select(aJCas, Dependency.class).iterator();

            Focus focus = null;
            try {
                focus = JCasUtil.selectSingle(aJCas, Focus.class);
            }
            catch (IllegalArgumentException ex) {

            }
            System.out.println("\n\t\t\t" + aJCas.getDocumentText());
            System.out.println("Фокус - " + (focus != null ? focus.getCoveredText() : "null") + "[" + (focus != null ? focus.getToken().getLemma().getValue() : "null") + "]");
            System.out.println("SV - " + (!JCasUtil.select(aJCas, SV.class).isEmpty() ? JCasUtil.selectSingle(aJCas, SV.class).getCoveredText() : "null"));
            System.out.printf("%-10s [%-10s] %-10s - %-10s [%-10s] %-10s %-10s \n", "Governor", "POS", "Lemma", "Dependent", "POS", "Lemma", "DependencyType");
            while(iter.hasNext()) {
                Dependency dependency = iter.next();
                System.out.printf("%-10s [%-10s] %-10s - %-10s [%-10s] %-10s %-10s\n",
                        dependency.getGovernor().getCoveredText(),
                        dependency.getGovernor().getPos().getPosValue(),
                        dependency.getGovernor().getLemma() != null ? dependency.getGovernor().getLemma().getValue() : "null",
                        dependency.getDependent().getCoveredText(),
                        dependency.getDependent().getPos().getPosValue(),
                        dependency.getDependent().getLemma() != null ? dependency.getDependent().getLemma().getValue() : "null",
                        dependency.getDependencyType());
            }
            System.out.println("NamedEntities");
            for(NamedEntity namedEntity: JCasUtil.select(aJCas, NamedEntity.class)) {
                System.out.println(namedEntity.getCoveredText() + " - " + namedEntity.getValue());
            }
            System.out.println("Lats");
            for(LAT lat: JCasUtil.select(aJCas, LAT.class)) {
                System.out.println(lat.getCoveredText() + " - " + lat.getText());
            }
            System.out.println("Clues");
            for(Clue clue: JCasUtil.select(aJCas, Clue.class)) {
                System.out.println(clue.getCoveredText() + " - " + clue.getLabel() + " - " + clue.getType().getShortName());
            }
            System.out.println("Concepts");
            for(Concept concept: JCasUtil.select(aJCas, Concept.class)) {
                System.out.println(concept.getCoveredText() + " - " + concept.getFullLabel() + " - " + concept.getDescription());
            }
            System.out.println("Question Classes");
            for(QuestionClass questionClass: JCasUtil.select(aJCas, QuestionClass.class)) {
                System.out.println(questionClass.getQuestionClass());
            }
            System.out.println("\n");
        }
    }

    @Test
    public void testRuQuestionAnalysisAnnotators() throws UIMAException, IOException {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(TreeTaggerPosTagger.class));
//        builder.add(AnalysisEngineFactory.createEngineDescription(TreeTaggerPosToSynTagRus.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(de.tudarmstadt.ukp.dkpro.core.maltparser.MaltParser.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(RootGenerator.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(
                SpotlightNameFinder.class,
                SpotlightNameFinder.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2227/rest/annotate"
        ));
        builder.add(AnalysisEngineFactory.createEngineDescription(FocusGeneratorRu.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(SVGenerator.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(LATByFocusRu.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(LATBySVRu.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(LATByWordnetGeneral.class,
                LATByWordnet.PARAM_EXPAND_SYNSET_LATS, false));
        builder.add(AnalysisEngineFactory.createEngineDescription(ClueBySV.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(ClueByNE.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(ClueByLAT.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(CluesToConcepts.class,
                CluesToConcepts.PARAM_LANGUAGE, Language.RUSSIAN,
                CluesToConcepts.PARAM_FUZZY_LOOKUP_URL, "http://localhost:5000"));
        builder.add(AnalysisEngineFactory.createEngineDescription(CluesMergeByText.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(DashboardHook.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(ClassClassifier.class));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                SimpleQuestion.class,
                SimpleQuestion.PARAM_LANGUAGE, Language.RUSSIAN,
                SimpleQuestion.PARAM_INPUT, "Кто играет Гарри Поттера в фильме?");

        MultiCASPipeline.runPipeline(reader,builder.createAggregateDescription(), createEngineDescription(Printer.class));
    }

}
