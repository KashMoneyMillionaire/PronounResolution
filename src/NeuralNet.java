import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
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
    public static void main(String[] args) throws Exception {
        System.out.print(args[0]);
        PrintWriter writer = new PrintWriter("Output.txt", "UTF-8");

        //I am trying to change this file.
        ArrayList<String> input = readFile(args[0]);
        CoNLP c = new CoNLP();
        int l = 5;

        for (int x = 0; x < input.size(); x += 5) {
            writer.println(input.get(x));
            c.testStanford(input.get(x), input.get(x + 4), input.get(x + 1), writer);
        }

        writer.close();
    }
    
    //returns a boolean array of the guesses gotten
    static boolean[] getGMethodResults(ArrayList<String> training) {
        boolean[] gMethodResults = new boolean[training.size()/5];

        for (int i=0; i<training.size(); i+=10) {
            String sent1 = training.get(i);
            String sent2 = training.get(i+5);
            String pNoun1 = training.get(i+2).toLowerCase();
            String pNoun2 = training.get(i+3).toLowerCase();
            String correct1 = training.get(i+4).toLowerCase();
            String correct2 = training.get(i+9).toLowerCase();
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
    
    //divide the training set into training and testing 
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
        for (int i = 0; i < data.size(); i += 10) {
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


    public static boolean gMethod(String sent1, String sent2, String p1, String p2) {
        if (p1.equals("john") || p1.equals("bill") || p1.equals("mary") || p1.equals("sue") || p2.equals("john") || p2.equals("bill") || p2.equals("mary") || p2.equals("sue")) {
            System.out.print("Name ");
            return true; //most likely
        }/*
      else {
          return false;
      }*/

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
        double percentage = 1; //amount larger search results must be

        if (phrase1.equals("didn't find")) {
            if (phrase2.equals("didn't find")) {
                return true; //most likely
            } else {
                try {
                    resultp1PN2 += getResultsCountBing(p2PN1); //2nd sentence choosing 1st pronoun is equivilent to 1st sentence choosing 2nd pronoun
                    resultp1PN1 += getResultsCountBing(p2PN2);
                } catch (Exception e) {
                    System.out.println("gMethod error1:" + e.getMessage());
                    System.exit(1);
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
                    System.exit(1);
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
                    System.exit(1);
                }

                System.out.println("resultp1PN1: " + resultp1PN1 + ", resultp1PN2:" + resultp1PN2);

                if (resultp1PN1 > percentage * resultp1PN2) {
                    return true;
                }
                if (resultp1PN2 > percentage * resultp1PN1) {
                    return false;
                }
            }
        }

        System.out.println("not enough to guess");
        return false; //most likely of the rest (found from getBaseValues on training data)

    }

    //Uses the parser to return the Typed Dependency List
    public static List<TypedDependency> getTDL(String sentence) {
        LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
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

    //reads the input file and breaks it up into necessary elements
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
                trainingData.add(temp.substring(1, temp.length())); //Noun2
                trainingData.add(reader.nextLine()); //Correct Noun
                reader.nextLine(); //blank
            }
        }
        catch (Exception e) {
            System.out.println("Error in reading file: " + e.getMessage());
            System.exit(1);
        }
        
        return trainingData;
    }
    
    //Polls Google to get the number of results
    private static int getResultsCountGoogle(final String query) throws IOException {
        try {
            Thread.sleep(1000);
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
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        final URL url = new URL("http://www.bing.com/search?q=" + URLEncoder.encode(query, "UTF-8"));
        //System.out.println("url: "+url);
        final URLConnection connection = url.openConnection();
        //System.out.println("connection: "+connection.toString());
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        final Scanner reader = new Scanner(connection.getInputStream(), "UTF-8");
        //System.out.println("location:"+connection.getHeaderField("location"));
        while (reader.hasNextLine()) {
            final String line = reader.nextLine();
            //System.out.println("line: " + line);
            if (!line.contains("class=\"sb_count\""))
                continue;
            try {
                //System.out.println("line:"+line);
                //System.out.println("line split: " + line.split("<span class=\"sb_count\" id=\"count\">")[1].split("<")[0].replaceAll("[^\\d]", ""));
                return Integer.parseInt(line.split("<span class=\"sb_count\" id=\"count\">")[1].split("<")[0].replaceAll("[^\\d]", ""));
            } finally {
                reader.close();
            }
        }
        reader.close();
        return 0;
    }
    
    //get correct answsers for comparing
    boolean[] getCorrect(ArrayList<String> set) {
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
    int[][] combine(boolean[] gMethodResults, boolean[] method2Results, boolean[] method3Results, boolean[] correctResults) {
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
    double[] trainWeights(int[][] trainingData, double[] weights, ArrayList<String> names, double learningRate, int iterations) {
        for (int i = 0; i < iterations; i++) {
            int index = i % trainingData.length; //If the number of iterations are larger then the training set
            double[] newWeights = new double[weights.length];
            double correct = trainingData[index][trainingData[0].length - 1]; //correct value
            double dotProduct = 0;
            for (int j = 0; j < weights.length; j++) { //dot product of weights and data
                dotProduct += weights[j] * trainingData[index][j];
            }
            double predicted = 1 / (1 + (Math.pow(Math.E, -1 * dotProduct))); //predicted value
            double delta = correct - predicted;
            double dSigma = predicted * (1 - predicted);
            for (int k = 0; k < weights.length; k++) {
                newWeights[k] = (weights[k] + (learningRate * delta * dSigma * trainingData[index][k]));
            }
            weights = newWeights;
        }
        return weights;
    }
    
    //Takes guesses and correct answers and outputs correct/total using weights
    double testWeights(int[][] data, double[] weights) {
        double total = data.length;
        double correct = 0;
        int classIndex = (data[0].length - 1);
        for (int i = 0; i < data.length; i++) {
            double dotProduct = 0;
            for (int j = 0; j < classIndex; j++) {
                dotProduct += weights[j] * data[i][j];
            }
            double predictedDouble = 1 / (1 + (Math.pow(Math.E, -1 * dotProduct)));
            int predicted = Math.round((int) predictedDouble);
            if (predicted == data[i][classIndex]) {
                correct++;
            }
        }
        return (correct / total);
    }
    
    //Takes guesses and correct answers and outputs correct/total using a majority method
    double testMajority(int[][] data) {
        double total = data.length;
        double correct = 0;
        int classIndex = (data[0].length - 1);
        for (int i = 0; i < data.length; i++) {
            int temp = 0;
            int predicted = 0;
            for (int j = 0; j < classIndex; j++) {
                if (data[i][j] == 1) {
                    temp++;
                }
            }
            if (temp > (classIndex - temp)) {
                predicted = 1;
            }
            if (predicted == data[i][classIndex]) {
                correct++;
            }
        }
        return (correct / total);
    }
}
