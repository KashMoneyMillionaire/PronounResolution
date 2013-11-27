/**
 * Created with IntelliJ IDEA.
 * User: Kash
 * Date: 11/10/13
 * Time: 8:21 PM
 * To change this template use File | Settings | File Templates.
 */

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

public class CoNLP {
    public static Properties props = new Properties();
    public static StanfordCoreNLP pipeline;

    public CoNLP() {
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        pipeline = new StanfordCoreNLP(props);

    }

    public boolean testStanford(String text, String correctWord, String pronoun, PrintWriter writer) throws IOException, ClassNotFoundException {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);

        boolean contains = false;
        String temp = "";
        for (Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {

            temp = entry.getValue().toString();
            if (temp.contains("\"" + correctWord + "\"") && temp.contains("\"" + pronoun + "\"")) {
                contains = true;
                break;
            }
        }
        if (contains)
            writer.println("\t" + temp + " - " + contains);
        else
            writer.println("\t" + contains);
        return contains;
    }
}
