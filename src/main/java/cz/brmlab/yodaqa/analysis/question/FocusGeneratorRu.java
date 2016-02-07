package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Focus;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.N;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;

public class FocusGeneratorRu extends JCasAnnotator_ImplBase {

    private final String QUEST_CONJ = "когда|кто|кому|где|сколько";

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Token focusTok = null;
        Annotation focus = null;
        Collection<Dependency> dependencies = JCasUtil.select(aJCas, Dependency.class);

        for (Dependency dependency : dependencies) {
            if (dependency.getDependent().getLemma().getValue().matches(QUEST_CONJ) && dependency.getDependent().getBegin() == 0) {
                focusTok = dependency.getDependent();
                focus = focusTok;
                break;
            }
        }

        if(focus == null) {
            for(Dependency dependency: dependencies) {
                if(dependency.getDependencyType().equals("огранич") && dependency.getDependent().getPos() instanceof N) {
                    focusTok = dependency.getDependent();
                    focus = focusTok;
                    break;
                }
            }
        }

        if(focus == null) {
            for(Dependency dependency: dependencies) {
                if(dependency.getDependencyType().equals("предл")) {
                    focusTok = dependency.getGovernor();
                    focus = focusTok;
                    break;
                }
            }
        }

        if(focus == null) {
            for(Dependency dependency: dependencies) {
                if (dependency.getDependencyType().equals("опред")
                        && !(dependency.getGovernor().getPos().getPosValue().equals("S")
                        && dependency.getDependent().getPos().getPosValue().equals("V"))) {
                    focusTok = dependency.getGovernor();
                    focus = focusTok;
                    break;
                }
            }
        }

        if(focus == null) {
            for (Dependency dependency : dependencies) {
                if (dependency.getDependencyType().equals("обст") &&
                        dependency.getGovernor().getPos().getPosValue().equals("V") &&
                        dependency.getDependent().getPos().getPosValue().equals("S")) {
                    focusTok = dependency.getDependent();
                    focus = focusTok;
                    break;
                }
            }
        }

        if(focus != null) {
            Focus f = new Focus(aJCas);
            f.setBegin(focus.getBegin());
            f.setEnd(focus.getEnd());
            f.setBase(focus);
            f.setToken(focusTok);
            f.addToIndexes();
        }

    }

}
