import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Kash
 * Date: 11/10/13
 * Time: 11:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class NeuralNet {


    static LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
    static TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
    static int count = 0;

    public static void main(String[] args) throws Exception {
        double[] weights = {0.96730349, -0.28849325}; // getWeights();
        
        threeOutput(weights, args);

    }
    
    private static void threeOutput(double[] incoming, String[] args) throws Exception {
        String file = args[0];
        ArrayList<String> testing = readTestFile(file);

        boolean[] guessesS = getSMethodResults(testing);
        boolean[] guessesG = getGMethodResults(testing);
        
        //using orginal weights
        double[] weights = { -0.19120428157367558, 1.6597957057508668 };
        int[][] combination = combine2(guessesG, guessesS);
        boolean[] prediction = useWeights(combination, weights);
        writePrediction(testing, prediction, 1);
        
        //new weights
        boolean[] prediction2 = useWeights(combination, incoming);
        writePrediction(testing, prediction2, 2);
        
        //new weights and reversed (just in case)
        boolean[] prediction3 = reverse(prediction2);
        writePrediction(testing, prediction3, 3);
    }

    private static double[] getWeights() throws Exception {
        //*** FOR GETTING WEIGHTS! Run if you can get it to work and let me know the weights
        String file = "bigTrain.txt";
        ArrayList<String> testing = readFile(file);

        boolean[] trainbool = trainAndTest((testing.size()/10), 0.3);
        ArrayList<String> trainSet = getSet(testing, trainbool, true);
        boolean[] correct = getCorrect(trainSet);

        boolean[] guessesG = findTheRightArray(file, trainSet, "guessesG");
        boolean[] guessesS = getSMethodResults(trainSet);

        int[][] calculatingWeights = combine3(guessesG, guessesS, correct);
        double[] weights = {0, 0};
        weights = trainWeights(calculatingWeights, weights, 0.3, 10000);
        System.out.println("W[0]: " + weights[0] + ", W[1]: " + weights[1]);
        return weights;
    }

    private static void testStuff(double[] incoming, String[] args) throws Exception {
        String file = args[0];
        ArrayList<String> testing = readTestFile(file);
        boolean[] guessesS = getSMethodResults(testing);
        boolean[] guessesG = getGMethodResults(testing); //findTheRightArray(file, testing, "guessesG");

        int[][] combination = combine2(guessesG, guessesS);
        double[] weights = incoming;
        boolean[] prediction = useWeights(combination, weights);
        writePrediction(testing, prediction, 1);
    }
    
    private static void threeOutputTest(double[] incoming, String[] args) throws Exception {
  /*      //String file = args[0];
        String file = "bigTrain.txt";
        ArrayList<String> testing = readTestFile(file);
        boolean[] guessesS = getSMethodResults(testing);
        boolean[] guessesG = getGMethodResults(testing); //findTheRightArray(file, testing, "guessesG");

        int[][] combination = combine2(guessesG, guessesS);
        double[] weights = incoming;
        boolean[] prediction = useWeights(combination, weights);
        writePrediction(testing, prediction);*/
        
        String file = "bigTrain.txt";
        ArrayList<String> testing = readFile(file);

        boolean[] correct = getCorrect(testing);
        boolean[] guessesS = getSMethodResults(testing);
        boolean[] guessesG = getGMethodResults(testing);

        int[][] combinationGC = combine2(guessesG, correct);
        double tempPercentG = testSingle(combinationGC);
        System.out.println("GMethod accuracy:"+tempPercentG);
        writePrediction(testing, guessesG, 2);

        int[][] combinationSC = combine2(guessesS, correct);
        double tempPercentS = testSingle(combinationSC);
        System.out.println("SMethod accuracy:"+tempPercentS);
        writePrediction(testing, guessesS, 3);

        int[][] trainingWeights = combine3(guessesG, guessesS, correct);
        double[] weights = {0, 0};
        weights = trainWeights(trainingWeights, weights, 0.3, 10000);
        System.out.println("W[0]: "+weights[0]+", W[1]"+weights[1]);
       
        int[][] combination = combine2(guessesG, guessesS);
        boolean[] prediction = useWeights(combination, incoming);
        writePrediction(testing, prediction, 1);
    }

    private static boolean[] findTheRightArray(String file, ArrayList<String> trainSet, String guesses) throws Exception {

        boolean[] guessesG;
        File f = new File(file + guesses + ".dat");
        if (f.exists()) {
            System.out.println(file + guesses + ".dat exists and will be read from.");
            guessesG = new boolean[trainSet.size() / 5];
            FileInputStream in = new FileInputStream(file + guesses + ".dat");
            readBooleans(in, guessesG);
            in.close();
        } else {
            System.out.println(file + guesses + ".dat does not exist. Creating array from getGMethodResults().");
            guessesG = getGMethodResults(trainSet);
            FileOutputStream out = new FileOutputStream(file + guesses + ".dat");
            writeBooleans(out, guessesG);
            out.close();
        }
        return guessesG;
    }

    private static void writeBooleans(OutputStream out, boolean[] ar) throws IOException {
        for (int i = 0; i < ar.length; i += 8) {
            int b = 0;
            for (int j = Math.min(i + 7, ar.length - 1); j >= i; j--) {
                b = (b << 1) | (ar[j] ? 1 : 0);
            }
            out.write(b);
        }
    }

    private static void readBooleans(InputStream in, boolean[] ar) throws IOException {
        for (int i = 0; i < ar.length; i += 8) {
            int b = in.read();
            if (b < 0) return;
            for (int j = i; j < i + 8 && j < ar.length; j++) {
                ar[j] = (b & 1) != 0;
                b >>>= 1;
            }
        }
        System.out.println("It worked");
    }

    static boolean[] testWithStanford(ArrayList<String> test) {
        boolean[] sMethodResults = new boolean[test.size() / 10];
        CoNLP c = new CoNLP();

        for (int i = 0; i < test.size(); i += 10) {
            String sentence1 = test.get(i);
            String pronoun = test.get(i + 1);
            String test1 = test.get(i + 2);
            String test2 = test.get(i + 3);
            String sentence2 = test.get(i + 5);

//            boolean result1 = c.testStanford(sentence1, test1, pronoun);
//            boolean result2 = c.testStanford(sentence2, test2, pronoun);
//
//            if (result1 && !result2)
//                sMethodResults[i / 10] = true;
//            else if (!result1 && result2)
//                sMethodResults[i / 10] = false;
//            else
//                sMethodResults[i / 10] = true;
        }
        return sMethodResults;
    }

    static boolean[] getSMethodResults(ArrayList<String> training) {
        boolean[] sMethodResults = new boolean[training.size() / 5];
        CoNLP c = new CoNLP();

        for (int i = 0; i < training.size(); i += 5) {
            String sentence = training.get(i);
            String pronoun = training.get(i + 1);
            String word1 = training.get(i + 2), word2 = training.get(i + 3);

            sMethodResults[i / 5] = c.testStanford(sentence, word1, word2, pronoun);
        }
        return sMethodResults;
    }
    
    //returns a boolean array of the guesses gotten
    static boolean[] getGMethodResults(ArrayList<String> training) {
        boolean[] gMethodResults = new boolean[training.size()/5];
        int size = training.size();
        for (int i = 0; i < size; i += 10) {
            String sent1 = training.get(i);
            String sent2 = training.get(i+5);
            String pNoun1 = training.get(i+2).toLowerCase();
            String pNoun2 = training.get(i+3).toLowerCase();
            if (gMethod(sent1, sent2, pNoun1, pNoun2)) {
                gMethodResults[i/5] = true;
                gMethodResults[(i/5)+1] = false;
            }
            else {
                gMethodResults[i/5] = false;
                gMethodResults[(i/5)+1] = true;
            }
        }

        return gMethodResults;
    }
    
    //divide the training set into training (1-percentage) and testing (percentage)
    //***Divide the length of readFile by 10 to get this length***
    static boolean[] trainAndTest(int length, double percentage) {
        boolean trainTest[] = new boolean[length];
        for (int i = 0; i < length; i++) {
            trainTest[i] = (Math.random() > percentage);
        }
        return trainTest;
    }

    //set train = true to get training set and false to get test set
    static ArrayList<String> getSet(ArrayList<String> data, boolean[] trainTest, boolean train) {
        ArrayList<String> set = new ArrayList<String>();
        int size = data.size() / 10;
        for (int i = 0; i < size; i += 1) {
            if (train == trainTest[i]) {
                for (int j = 0; j < 10; j++) {
                    set.add(data.get((i * 10) + j));
                }
            }
        }
        return set;
    }

    //reads training data and finds most probably outcome for sentence pairs with proper names and those without
    static boolean[] getBaseValues(ArrayList<String> training) {
        int nameCount = 0;
        int nameTotal = 0;
        int restCount = 0;
        int restTotal = 0;
        for (int i = 0; i < training.size(); i += 10) {
            String pNoun1 = training.get(i + 2).toLowerCase();
            String pNoun2 = training.get(i + 3).toLowerCase();
            String correct1 = training.get(i + 4).toLowerCase();
            if (pNoun1.equals("john") || pNoun1.equals("bill") || pNoun1.equals("mary") || pNoun1.equals("sue") || pNoun2.equals("john") || pNoun2.equals("bill") || pNoun2.equals("mary") || pNoun2.equals("sue")) {
                if (pNoun1.equals(correct1)) {
                    nameCount++;
                }
                nameTotal++;
            } else {
                if (pNoun1.equals(correct1)) {
                    restCount++;
                }
                restTotal++;
            }
        }
        
        boolean baseValues[] = { (nameCount >= (nameTotal - nameCount)), (restCount >= (restTotal - restCount)) };
        return baseValues;
    }

    //runs tests and returns the phrase to be searched
    static String getPhrase(List<TypedDependency> tdl) {
        String temp = markTest(tdl);
        if (!temp.equals("nope")) {
            return temp;
        }

        temp = conj_butTest(tdl);
        if (!temp.equals("nope")) {
            return temp;
        }

        temp = restTest(tdl);
        if (!temp.equals("huh: rest")) {
            return temp;
        }

        return "didn't find";
    }

    //The meat and potatoes
    public static boolean gMethod(String sent1, String sent2, String p1, String p2) {
        if (p1.equals("john") || p1.equals("bill") || p1.equals("mary") || p1.equals("sue") || p2.equals("john") || p2.equals("bill") || p2.equals("mary") || p2.equals("sue")) {
            return true; //most likely
        }

        List<TypedDependency> tdl1 = getTDL(sent1);
        String phrase1 = getPhrase(tdl1);

        List<TypedDependency> tdl2 = getTDL(sent2);
        String phrase2 = getPhrase(tdl2);

        String p1PN1 = p1 + " " + phrase1;
        String p1PN2 = p2 + " " + phrase1;
        String p2PN1 = p1 + " " + phrase2;
        String p2PN2 = p2 + " " + phrase2;

        double resultp1PN1 = 1; //to stop rounding errors
        double resultp1PN2 = 1;
        double percentage = 1.4; //amount larger search results must be

        if (phrase1.equals("didn't find")) {
            if (phrase2.equals("didn't find")) {
                return true; //most likely
            } else {
                try {
                    resultp1PN2 += getResultsCountBing(p2PN1); //2nd sentence choosing 1st pronoun is equivilent to 1st sentence choosing 2nd pronoun
                    resultp1PN1 += getResultsCountBing(p2PN2);
                } catch (Exception e) {
                    System.out.println("gMethod error1:" + e.getMessage());
                    return true;
                }

                if (resultp1PN2 > percentage * resultp1PN1) {
                    return false;
                }
                if (resultp1PN1 > percentage * resultp1PN2) {
                    return true;
                }
            }
        } else {
            if (phrase2.equals("didn't find")) {
                try {
                    resultp1PN1 = getResultsCountBing(p1PN1);
                    resultp1PN2 = getResultsCountBing(p1PN2);
                } catch (Exception e) {
                    System.out.println("gMethod error2:" + e.getMessage());
                    return true;
                }

                if (resultp1PN1 > percentage * resultp1PN2) {
                    return true;
                }
                if (resultp1PN2 > percentage * resultp1PN1) {
                    return false;
                }
            } else {
                try {
                    resultp1PN1 += getResultsCountBing(p1PN1);
                    resultp1PN2 += getResultsCountBing(p1PN2);
                    resultp1PN2 += getResultsCountBing(p2PN1);
                    resultp1PN1 += getResultsCountBing(p2PN2);
                } catch (Exception e) {
                    System.out.println("gMethod error3:" + e.getMessage());
                    return true;
                }

                if (resultp1PN1 > percentage * resultp1PN2) {
                    return true;
                }
                if (resultp1PN2 > percentage * resultp1PN1) {
                    return false;
                }
            }
        }

        return true; //most likely of the rest (found from getBaseValues on training data)

    }

    //Uses the parser to return the Typed Dependency List
    public static List<TypedDependency> getTDL(String sentence) {
        List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(new StringReader(sentence)).tokenize();
        Tree parse = lp.apply(rawWords2);

        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

        return tdl;
    }

    //returns the phrase for the cases with a mark
    static String markTest(List<TypedDependency> tdl) {
        String found;
        int index=0;
        int mark = -1;
        int det = -1;
        int cop = -1;
        int dep = -1;
        int aux = -1;
        int auxpass = -1;
        int prep_in = -1;
        int dobj = -1;
        int nsubj = -1;
        int prep_on = -1;
        int prep_before = -1;
        int prep_after = -1;
        
        for(TypedDependency t : tdl) {
            String tD = t.toString();
            StringTokenizer token = new StringTokenizer(tD, "(");
            String id = token.nextToken();
            if (id.equals("mark")) {
                mark=index;
            }
            if (mark != -1) {
                if (id.equals("det")) {
                    if (det != -1) {
                        if (index > mark && (index-mark) < (det-mark)) {
                            det=index;
                        }
                        //****GET LOCATION OF PRONOUN AND USE THAT
                    }
                    else {
                        det=index;
                    }
                } else if (id.equals("cop")) {
                    cop=index;
                } else if (id.equals("dep")) {
                    dep=index;
                } else if (id.equals("aux")) {
                    aux=index;
                } else if (id.equals("auxpass")) {
                    auxpass=index;
                } else if (id.equals("prep_in")) {
                    prep_in=index;
                } else if (id.equals("dobj")) {
                    dobj=index;
                } else if (id.equals("nsubj")) {
                    nsubj=index;
                } else if (id.equals("prep_on")) {
                    prep_on=index;
                } else if (id.equals("prep_before")) {
                    prep_before=index;
                } else if (id.equals("prep_after")) {
                    prep_after=index;
                }
            }
            index++;;
        }
        if (mark == -1) {
            found = "nope";
        } else if (det == -1 && cop != -1) {
            found = mark1(tdl, cop);
            found += prepAdd(tdl, cop, prep_on, prep_before, prep_after);
        } else if (det != -1 && cop != -1) {
            found = mark2(tdl, cop, det);
            found += prepAdd(tdl, Math.max(cop, det), prep_on, prep_before, prep_after);
        } else if (dep == -1 && aux != -1) {
            found = mark1(tdl, aux);
            found += prepAdd(tdl, aux, prep_on, prep_before, prep_after);
        } else if (dep != -1 && aux != -1 && auxpass != -1) {
            found = mark5(tdl, dep, aux, auxpass);
            found += prepAdd(tdl, Math.max(Math.max(dep, aux), auxpass), prep_on, prep_before, prep_after);
        } else if (prep_in != -1 ) {
            found = mark6(tdl, prep_in);
        } else if (dobj != -1) {
            found = mark7(tdl, dobj);
            found += prepAdd(tdl, dobj, prep_on, prep_before, prep_after);
        } else if (dobj == -1 && nsubj != -1) {
            found = mark1(tdl, nsubj);
            found += prepAdd(tdl, nsubj, prep_on, prep_before, prep_after);
        } else {
            found = "huh: mark";
        }
        
        return found;
    }

    //helper methods for cases with a mark
    static String mark1(List<TypedDependency> tdl, int cop) {
        String temp = tdl.get(cop).toString();

        return getSecond(temp) + " " + getFirst(temp);
    }

    static String mark2(List<TypedDependency> tdl, int cop, int det) {
        String tempDet = tdl.get(det).toString();
        String tempCop = tdl.get(cop).toString();

        return getSecond(tempCop) + " " + getSecond(tempDet) + " " + getFirst(tempDet);

    }

    static String mark5(List<TypedDependency> tdl, int dep, int aux, int auxpass) {
        String tempDep = tdl.get(dep).toString();
        String tempAux = tdl.get(aux).toString();
        String tempAuxpass = tdl.get(auxpass).toString();

        return getSecond(tempDep) + " " + getSecond(tempAux) + " " + getSecond(tempAuxpass) + " " + getFirst(tempAuxpass);
    }

    static String mark6(List<TypedDependency> tdl, int prep) {
        String temp = tdl.get(prep).toString();

        return getFirst(temp) + " in a " + getSecond(temp);
    }

    static String mark7(List<TypedDependency> tdl, int dobj) {
        String temp = tdl.get(dobj).toString();

        return getFirst(temp) + " " + getSecond(temp);
    }

    //returns the phrase for the cases with a but conjunction
    static String conj_butTest(List<TypedDependency> tdl) {
        String found;
        int index = 0;
        int conj_but = -1;
        int cop = -1;
        int auxpass = -1;
        int prep_on = -1;
        int prep_before = -1;
        int prep_after = -1;

        for (TypedDependency t : tdl) {
            String tD = t.toString();
            StringTokenizer token = new StringTokenizer(tD, "(");
            String id = token.nextToken();
            if (id.equals("conj_but")) {
                conj_but = index;
            } else if (id.equals("cop")) {
                cop = index;
            } else if (id.equals("auxpass")) {
                auxpass = index;
            } else if (id.equals("prep_on")) {
                prep_on = index;
            } else if (id.equals("prep_before")) {
                prep_before = index;
            } else if (id.equals("prep_after")) {
                prep_after = index;
            }
            index++;
        }
        if (conj_but == -1) {
            found = "nope";
        } else if (cop != -1) {
            found = mark1(tdl, cop);
            found += prepAdd(tdl, cop, prep_on, prep_before, prep_after);
        } else if (auxpass != -1) {
            found = mark1(tdl, auxpass);
            found += prepAdd(tdl, auxpass, prep_on, prep_before, prep_after);
        } else {
            found = "huh: conj_but";
        }

        return found;
    }

    //returns the phrase for the remaining cases
    static String restTest(List<TypedDependency> tdl) {
        String found;
        int index = 0;
        int det = -1;
        int cop = -1;
        int parataxis = -1;
        int dep = -1;
        int aux = -1;
        int auxpass = -1;
        int ccomp = -1;
        int acomp = -1;
        int partmod = -1;
        int amod = -1;
        int dobj = -1;
        int nsubj = -1;
        int prep_on = -1;
        int prep_before = -1;
        int prep_after = -1;

        for (TypedDependency t : tdl) {
            String tD = t.toString();
            StringTokenizer token = new StringTokenizer(tD, "(");
            String id = token.nextToken();
            if (id.equals("det")) {
                det = index;
            } else if (id.equals("cop")) {
                cop = index;
            } else if (id.equals("parataxis")) {
                parataxis = index;
            } else if (id.equals("dep")) {
                dep = index;
            } else if (id.equals("aux")) {
                aux = index;
            } else if (id.equals("auxpass")) {
                auxpass = index;
            } else if (id.equals("ccomp")) {
                ccomp = index;
            } else if (id.equals("acomp")) {
                acomp = index;
            } else if (id.equals("partmod")) {
                partmod = index;
            } else if (id.equals("amod")) {
                amod = index;
            } else if (id.equals("dobj")) {
                dobj = index;
            } else if (id.equals("nsubj")) {
                nsubj = index;
            } else if (id.equals("prep_on")) {
                prep_on = index;
            } else if (id.equals("prep_before")) {
                prep_before = index;
            } else if (id.equals("prep_after")) {
                prep_after = index;
            }
            index++;
        }
        if (det == -1 && cop != -1) {
            found = mark1(tdl, cop);
            found += prepAdd(tdl, cop, prep_on, prep_before, prep_after);
        } else if (det != -1 && cop != -1) {
            if (parataxis != -1) {
                found = mark1(tdl, cop);
                found += prepAdd(tdl, cop, prep_on, prep_before, prep_after);
            } else {
                found = mark2(tdl, cop, det);
                found += prepAdd(tdl, Math.max(cop, det), prep_on, prep_before, prep_after);
            }
        } else if (dep == -1 && aux != -1) {
            found = mark1(tdl, aux);
            found += prepAdd(tdl, aux, prep_on, prep_before, prep_after);
        } else if (aux == -1 && auxpass != -1) {
            found = mark1(tdl, auxpass);
            found += prepAdd(tdl, auxpass, prep_on, prep_before, prep_after);
        } else if (ccomp != -1) {
            if (dep != -1) {
                found = mark1(tdl, dep);
                found += prepAdd(tdl, dep, prep_on, prep_before, prep_after);
            } else {
                found = mark7(tdl, ccomp);
                found += prepAdd(tdl, ccomp, prep_on, prep_before, prep_after);
            }
        } else if (acomp != -1) {
            found = mark7(tdl, acomp);
            found += prepAdd(tdl, acomp, prep_on, prep_before, prep_after);
        } else if (partmod != -1) {
            found = mark7(tdl, partmod);
            found += prepAdd(tdl, partmod, prep_on, prep_before, prep_after);
        } else if (amod != -1) {
            found = rest5(tdl, amod);
            found += prepAdd(tdl, amod, prep_on, prep_before, prep_after);
        } else if (dobj != -1) {
            found = mark7(tdl, dobj);
            found += prepAdd(tdl, dobj, prep_on, prep_before, prep_after);
        } else if (dobj == -1 && nsubj != -1) {
            found = rest6(tdl, nsubj);
            found += prepAdd(tdl, nsubj, prep_on, prep_before, prep_after);
        } else {
            found = "huh: rest";
        }

        return found;
    }

    static String rest5(List<TypedDependency> tdl, int amod) {
        String temp = tdl.get(amod).toString();

        return getSecond(temp);
    }

    static String rest6(List<TypedDependency> tdl, int nsubj) {
        String temp = tdl.get(nsubj).toString();

        return getFirst(temp);
    }

    //adds necessary prepositions to the phrase if necessary
    static String prepAdd(List<TypedDependency> tdl, int index, int p_on, int p_before, int p_after) {
        String addition = "";
        if (p_on != -1 && p_on > index) {
            String temp = tdl.get(p_on).toString();
            addition = " on " + getSecond(temp);
        } else if (p_before != -1 && p_before > index) {
            String temp = tdl.get(p_before).toString();
            addition = " before " + getSecond(temp);
        } else if (p_after != -1 && p_after > index) {
            String temp = tdl.get(p_after).toString();
            addition = " after " + getSecond(temp);
        }

        return addition;
    }

    //private ParserDemo() {} // static methods only

    //helper method, gets the first element in the string
    static String getFirst(String temp) {
        StringTokenizer token = new StringTokenizer(temp, "(");
        token.nextToken();

        String rest = token.nextToken();
        StringTokenizer token2 = new StringTokenizer(rest, "-");
        return token2.nextToken();
    }

    //helper method, gets the second element in the string
    static String getSecond(String temp) {
        StringTokenizer token = new StringTokenizer(temp, "(");
        token.nextToken();

        String rest = token.nextToken();
        StringTokenizer token2 = new StringTokenizer(rest, "-");
        token2.nextToken();
        token2.nextToken(" ");
        String second = token2.nextToken("-");
        return second.substring(1, second.length());
    }

    //reads the training file and breaks it up into necessary elements
    static ArrayList<String> readFile(String fileName) {
        ArrayList<String> trainingData = new ArrayList<String>();
        try {
            File file = new File(fileName);
            Scanner reader = new Scanner(file);
            int i=-1;
            while (reader.hasNext()) {
                //System.out.println(++i);
                trainingData.add(reader.nextLine()); //Sentence
                trainingData.add(reader.nextLine()); //Pronoun
                String temp = reader.nextLine();
                StringTokenizer token = new StringTokenizer(temp, ",");
                trainingData.add(token.nextToken()); //Noun1
                temp = token.nextToken();
                trainingData.add(temp); //Noun2
                trainingData.add(reader.nextLine()); //Correct Noun
                reader.nextLine(); //blank
            }
        }
        catch (Exception e) {
            System.out.println("Error in reading file: " + e.getMessage());
            //System.exit(1);
        }
        
        return trainingData;
    }
    
    //reads the testing file and breaks it up into necessary elements
    static ArrayList<String> readTestFile(String fileName) {
        ArrayList<String> testingData = new ArrayList<String>();
        try {
            File file = new File(fileName);
            Scanner reader = new Scanner(file);
            while (reader.hasNext()) {
                testingData.add(reader.nextLine()); //Sentence
                testingData.add(reader.nextLine()); //Pronoun
                String temp = reader.nextLine();
                StringTokenizer token = new StringTokenizer(temp, ",");
                testingData.add(token.nextToken()); //Noun1
                temp = token.nextToken();
                testingData.add(temp); //Noun2
                //System.out.println("after:"+testingData.get()
                testingData.add("");
                reader.nextLine(); //blank
                reader.nextLine(); //blank
            }
        }
        catch (Exception e) {
            System.out.println("Error in reading file: " + e.getMessage());
            //System.exit(1);
        }
        
        return testingData;
    }
    
    //Polls Google to get the number of results
    private static int getResultsCountGoogle(final String query) throws IOException {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {

        }
        final URL url = new URL("https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8"));
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        final Scanner reader = new Scanner(connection.getInputStream(), "UTF-8");
        while (reader.hasNextLine()) {
            final String line = reader.nextLine();
            if (!line.contains("<div id=\"resultStats\">"))
                continue;
            try {
                return Integer.parseInt(line.split("<div id=\"resultStats\">")[1].split("<")[0].replaceAll("[^\\d]", ""));
            } catch (Exception e) {
                System.out.println("getResultsCount error: " + e.getMessage());
            } finally {
                reader.close();
            }
        }
        reader.close();
        return 0;
    }
    
    //Polls Bing to get the number of results
    private static int getResultsCountBing(final String query) throws IOException {
        try {
            //Thread.sleep(6000);
        } catch (Exception e) {

        }
        final URL url = new URL("http://www.bing.com/search?q=" + URLEncoder.encode(query, "UTF-8"));
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        final Scanner reader = new Scanner(connection.getInputStream(), "UTF-8");
        while (reader.hasNextLine()) {
            final String line = reader.nextLine();
            if (!line.contains("class=\"sb_count\""))
                continue;
            try {
                return Integer.parseInt(line.split("<span class=\"sb_count\" id=\"count\">")[1].split("<")[0].replaceAll("[^\\d]", ""));
            } finally {
                reader.close();
                System.out.print(count++ + " " + (count % 30 == 0 ? "\n" : ""));
            }
        }
        reader.close();
        return 0;
    }
    
    //get correct answsers for comparing
    static boolean[] getCorrect(ArrayList<String> set) {
        boolean[] correct = new boolean[set.size()/5];
        
        for (int i=0; i<set.size(); i+=10) {
            String pNoun1 = set.get(i+2).toLowerCase();
            String pNoun2 = set.get(i+3).toLowerCase();
            String correct1 = set.get(i+4).toLowerCase();
            String correct2 = set.get(i+9).toLowerCase();
            if (pNoun1.equals(correct1)) {
                correct[i/5] = true;
                correct[(i/5)+1] = false;
            }
            else {
                correct[i/5] = false;
                correct[(i/5)+1] = true;
            }
        }
        
        return correct;
        
    }
    
    //combines the individual guesses and correct answers together
    static int[][] combine(boolean[] gMethodResults, boolean[] method2Results, boolean[] method3Results, boolean[] correctResults) {
        int[][] combination = new int[4][correctResults.length];
        
        for (int i=0; i<combination[0].length; i++) {
            for (int j=0; j<4; j++) { //seting everyting to 0
                combination[j][i]=0;
            }
            if (gMethodResults[i]) combination[0][i]=1;
            if (method2Results[i]) combination[1][i]=1;
            if (method3Results[i]) combination[2][i]=1;
            if (correctResults[i]) combination[3][i]=1;
        }
        
        return combination;
    }
    
    //Uses guesses from methods and the correct answers to create weights for the methods
    static double[] trainWeights(int[][] trainingData, double[] weights, double learningRate, int iterations) {
        for (int i = 0; i < iterations; i++) {
            int index = i % trainingData[0].length; //If the number of iterations are larger then the training set
            double[] newWeights = new double[weights.length];
            double correct = trainingData[trainingData.length-1][index]; //correct value
            double dotProduct = 0;
            for (int j = 0; j < weights.length; j++) { //dot product of weights and data
                dotProduct += weights[j] * trainingData[j][index];
            }
            double predicted = 1/(1+(Math.pow(Math.E, -1 * dotProduct))); //predicted value
            double delta = correct - predicted;
            double dSigma = predicted * (1 - predicted);
            for (int k = 0; k < weights.length; k++) {
                newWeights[k] = (weights[k] + (learningRate * delta * dSigma * trainingData[k][index]));
            }
            weights = newWeights;
        }
        return weights;
    }
    
    //Takes guesses and correct answers and outputs correct/total using weights
    static double testWeights(int[][] data, double[] weights) {
        double total = data[0].length;
        double correct = 0;
        int classIndex = (data.length - 1);
        for (int i = 0; i < data[0].length; i++) {
            double dotProduct = 0;
            for (int j = 0; j < classIndex; j++) {
                dotProduct += weights[j] * data[j][i];
            }
            double predictedDouble = Math.round((1/(1+(Math.pow(Math.E, -1 * dotProduct))))-0.01);
            int predicted = (int) predictedDouble;
            if (predicted == data[classIndex][i]) {
                correct++;
            }
        }
        return (correct / total);
    }
    
    //Takes guesses and correct answers and outputs correct/total using a majority method
    static double testMajority(int[][] data) {
        double total = data[0].length;
        double correct = 0;
        int classIndex = (data.length - 1);
        for (int i = 0; i < data[0].length; i++) {
            int temp = 0;
            int predicted = 0;
            for (int j = 0; j < classIndex; j++) {
                if (data[i][j] == 1) {
                    temp++;
                }
            }
            if (temp >= (classIndex - temp)) {
                predicted = 1;
            }
            if (predicted == data[i][classIndex]) {
                correct++;
            }
        }
        return (correct / total);
    }
    
    //Takes guesses and correct answers and outputs correct/total
    static double testSingle(int[][] data) {
        double total = data[0].length;
        double correct = 0;
        for (int i = 0; i < data[0].length; i++) {
            //System.out.println("data[0]["+i+"]="+data[0][i]+", data[1]["+i+"]="+data[1][i]);
            if (data[0][i] == data[1][i]) {
                correct++;
            }
        }
        return (correct / total);
    }
    
    //reads training data and finds most probably outcome for sentence pairs with proper names and those without
    static int[] getBaseValues2(ArrayList<String> training) {
        int nameCount = 0;
        int nameTotal = 0;
        int restCount = 0;
        int restTotal = 0;
        for (int i = 0; i < training.size(); i += 10) {
            String pNoun1 = training.get(i + 2).toLowerCase();
            String pNoun2 = training.get(i + 3).toLowerCase();
            String correct1 = training.get(i + 4).toLowerCase();
            if (pNoun1.equals("john") || pNoun1.equals("bill") || pNoun1.equals("mary") || pNoun1.equals("sue") || pNoun2.equals("john") || pNoun2.equals("bill") || pNoun2.equals("mary") || pNoun2.equals("sue")) {
                if (pNoun1.equals(correct1)) {
                    nameCount++;
                }
                nameTotal++;
            } else {
                if (pNoun1.equals(correct1)) {
                    restCount++;
                }
                restTotal++;
            }
        }
        double name = ((double)nameCount/(double)nameTotal);
        double rest = ((double)restCount/(double)restTotal);
        if (name < 0.5) {
            //System.out.println("Named: " + name + ", Rest: " + rest);
        }
        
        if ((nameTotal-restTotal)< 0) {
            //System.out.println("Named-Rest: " + (nameTotal-restTotal));
        }
        
        int baseValues[] = { nameCount, nameTotal, restCount, restTotal };

        return baseValues;
    }
    
    //Used for single method
    static int[][] combine2(boolean[] gMethodResults, boolean[] correctResults) {
        int[][] combination = new int[2][gMethodResults.length];
        
        for (int i=0; i<combination[0].length; i++) {
            for (int j=0; j<2; j++) { //seting everyting to 0
                combination[j][i]=0;
            }
            if (gMethodResults[i]) combination[0][i]=1;
            if (correctResults[i]) combination[1][i]=1;
            //System.out.println("i: "+i+", guess: "+gMethodResults[i]+", correct: "+correctResults[i]);
        }
        
        return combination;
    }
    
    //combines the individual guesses and correct answers together
    static int[][] combine3(boolean[] gMethodResults, boolean[] sMethodResults, boolean[] correctResults) {
        int[][] combination = new int[3][correctResults.length];
        
        for (int i=0; i<combination[0].length; i++) {
            for (int j=0; j<3; j++) { //seting everyting to 0
                combination[j][i]=0;
            }
            if (gMethodResults[i]) combination[0][i]=1;
            if (sMethodResults[i]) combination[1][i]=1;
            if (correctResults[i]) combination[2][i]=1;
        }
        
        return combination;
    }
    
    static void writePrediction(ArrayList<String> testing, boolean[] predictions, int post) {
        try {
            String fileName = "predictions" + post + ".out";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i=0; i<testing.size(); i++) {
                if ((i%5) < 2) {
                    writer.println(testing.get(i));
                }
                else if ((i%5) == 2) {
                    writer.println(testing.get(i)+","+testing.get(i+1));
                } else if ((i % 5) == 3) {
                    if (predictions[i/5]) {
                        writer.println(testing.get(i-1));
                    }
                    else {
                        writer.println(testing.get(i));
                    }
                }
                else {
                    writer.println();
                }
            }
            writer.close();
        }
        catch (Exception e) {
            System.out.println("Error in writing file: " + e.getMessage());
            System.out.println("Error in writing file: " + e.toString());
            System.exit(1);
        }
    }
    
    static boolean[] useWeights(int[][] data, double[] weights) {
        boolean[] finalGuess = new boolean[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double dotProduct = 0;
            for (int j = 0; j < data.length; j++) {
                dotProduct += weights[j] * data[j][i];
            }
            double predictedDouble = Math.round((1/(1+(Math.pow(Math.E, -1 * dotProduct))))-0.01);
            int predicted = (int) predictedDouble;
            if (predicted == 1) {
                finalGuess[i]=true;
            }
            else {
                finalGuess[i]=false;
            }
        }
        return finalGuess;
    }
    
    //Takes guesses and correct answers and outputs correct/total using a majority method
    static boolean[] useMajority(int[][] data) {
        boolean[] finalGuess = new boolean[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            int temp = 0;
            int predicted = 0;
            for (int j = 0; j < data.length; j++) {
                if (data[i][j] == 1) {
                    temp++;
                }
            }
            if (temp >= (data.length - temp)) {
                finalGuess[i]=true;
            }
            else {
                finalGuess[i]=false;
            }
        }
        return finalGuess;
    }
}
