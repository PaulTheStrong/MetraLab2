package by.pavel.metra.lab3;

import kotlin.Pair;
import org.jetbrains.kotlin.spec.grammar.tools.KotlinToken;
import org.jetbrains.kotlin.spec.grammar.tools.KotlinTokensList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static org.jetbrains.kotlin.spec.grammar.tools.KotlinGrammarToolsKt.tokenizeKotlinCode;

public class App {
    static Set<String> flowOps = new HashSet<>();
    static Set<String> flowStart = new HashSet<>();
    static Set<String> conditionals = new HashSet<>();
    static {
        flowOps.addAll(Arrays.asList("if", "when", "do", "while", "else", "->", "for", "{", "}"));
        flowStart.addAll(Arrays.asList("if", "when", "do", "while", "else", "for"));
        conditionals.addAll(Arrays.asList("if", "while", "->", "for"));
    }

    private static void printWithTabs(String s, int tabCount) {
        //for(int i = 0; i < tabCount; i++)
        //    System.out.print("   ");
        //System.out.println(s);
    }

    public static int countOperators(String code) {
        KotlinTokensList list = tokenizeKotlinCode(code);
        list.removeIf(kotlinToken -> kotlinToken.getText().equals(" ") || kotlinToken.getType().equals("LineComment") || kotlinToken.getType().equals("DelimitedComment") || kotlinToken.getText().equals("{") || kotlinToken.getText().equals("}"));

        list.removeIf(kotlinToken -> list.get(list.indexOf(kotlinToken)).getText().equals("\n") && list.indexOf(kotlinToken) + 1 < list.size() && list.get(list.indexOf(kotlinToken) + 1).getText().equals("\n"));

        int Result = 0;
        for (KotlinToken token : list)
            Result += token.getText().equals("\n") ? 1 : 0;
        return Result;
    }

    public static int countConditionals(String code) {
        KotlinTokensList list = tokenizeKotlinCode(code);
        int Result = 0;
        list.removeIf(kotlinToken -> kotlinToken.getText().equals(" ") || kotlinToken.getText().equals("\n") || kotlinToken.getType().equals("LineComment") || kotlinToken.getType().equals("DelimitedComment"));
        for(KotlinToken token : list) {
            if (list.get(list.indexOf(token)).getText().equals("else") && list.get(list.indexOf(token) + 1).getText().equals("->") )
                Result--;
            Result += conditionals.contains(token.getText()) ? 1 : 0;
        }
        return Result;
    }

    public static int countDepth(String code) throws Exception{
        KotlinTokensList list = tokenizeKotlinCode(code);
        list.removeIf(kotlinToken ->  kotlinToken.getText().equals(" ") || kotlinToken.getText().equals("\n") ||
                kotlinToken.getType().equals("LineComment") || kotlinToken.getType().equals("DelimitedComment"));
        KotlinTokensList filteredList = new KotlinTokensList(Collections.emptyList());
        int cntPars = 0;
        boolean isSkip = false;
        StringBuilder appendix = new StringBuilder("");
        //скип того, что находится  в скобках if, while ... кроме самих условных операторов, т.к. котлин умеет так
        for (KotlinToken kotlinToken : list) {
            if (flowStart.contains(kotlinToken.getText()) && !kotlinToken.getText().equals("else") && !kotlinToken.getText().equals("do") && cntPars == 0) {
                filteredList.add(kotlinToken);
                isSkip = true;
                continue;
            } else if (kotlinToken.getText().equals("("))
                cntPars++;
            else if (kotlinToken.getText().equals(")"))
                cntPars--;
            if (isSkip && cntPars == 0)
                isSkip = false;
            else if (!isSkip) {
                filteredList.add(kotlinToken);
            }
        }
        list = new KotlinTokensList(filteredList);
        filteredList.clear();
        isSkip = false;
        for (KotlinToken token : list) {
            if (flowOps.contains(token.getText())) {
                isSkip = false;
                filteredList.add(token);
            } else if (!isSkip) {
                isSkip = true;
                filteredList.add(token);
            }
        }

        int currentDepth = 0, maxDepth = 0;
        int flowDepth = -1;
        Stack<Pair<Integer, String>> stk = new Stack<>();
        boolean canInsertOp = false;
        for (KotlinToken token : filteredList) {
            switch (token.getText()) {
                case "if":
                case "when":
                case "do":
                case "for": {
                    if (!canInsertOp) {
                        while (!stk.empty() && !(stk.peek().getSecond().equals("{") || stk.peek().getSecond().equals("else")))
                            stk.pop();
                        if (!stk.empty()) {
                            currentDepth = stk.peek().getFirst();
                            if (stk.peek().getSecond().equals("else"))
                                stk.pop();
                        } else
                            currentDepth = 0;
                    }
                    canInsertOp = true;
                    printWithTabs(token.getText(), currentDepth);
                    if(token.getText().equals("when")) {
                        stk.push(new Pair<>(currentDepth, token.getText()));
                    }
                    else {
                        stk.push(new Pair<>(currentDepth++, token.getText()));
                    }
                    break;
                }
                case "->" : {
                    canInsertOp = true;
                    printWithTabs(token.getText(), currentDepth);
                    stk.push(new Pair<>(currentDepth++, token.getText()));
                    break;
                }
                case "{" : {
                    canInsertOp = false;
                    printWithTabs(token.getText(), currentDepth);
                    stk.push(new Pair<>(currentDepth, token.getText()));
                    break;
                }
                case "}" : {
                    while (!stk.peek().getSecond().equals("{"))
                        stk.pop();

                    canInsertOp = false;
                    currentDepth = stk.pop().getFirst();
                    printWithTabs(token.getText(), currentDepth);
                    break;
                }
                case "else" : {
                    if (filteredList.get(filteredList.indexOf(token) + 1).getText().equals("->"))
                    {
                        printWithTabs(token.getText(), --currentDepth);
                        continue;
                    }
                    currentDepth = stk.pop().getFirst();
                    printWithTabs(token.getText(), currentDepth);
                    stk.push(new Pair<>(currentDepth++, "else"));
                    canInsertOp = true;
                    break;
                }
                case "while" : {
                    if (stk.empty() || !stk.peek().getSecond().equals("do")) {
                        if (!canInsertOp) {
                            while (!stk.empty() && !(stk.peek().getSecond().equals("{") || stk.peek().getSecond().equals("else")))
                                stk.pop();
                            if (!stk.empty()) {
                                currentDepth = stk.peek().getFirst();
                                if (stk.peek().getSecond().equals("else"))
                                    stk.pop();
                            }
                        }
                        canInsertOp = true;
                        printWithTabs(token.getText(), currentDepth);
                        stk.push(new Pair<Integer, String>(currentDepth++, token.getText()));
                    } else {
                        canInsertOp = true;
                        printWithTabs(token.getText(), currentDepth--);
                        stk.pop();
                    }
                    break;
                }
                default : {
                    if (filteredList.get(filteredList.indexOf(token) + 1).getText().equals("->"))
                        continue;
                    if(!canInsertOp) {
                        while (!stk.empty() && !stk.peek().getSecond().equals("{"))
                            stk.pop();
                        if(!stk.empty())
                            currentDepth = stk.peek().getFirst();
                        else
                            currentDepth = 0;
                    }
                    canInsertOp = false;
                    printWithTabs("op", currentDepth);
                    if(!stk.empty() && stk.peek().getSecond().equals("else"))
                        stk.pop();
                    break;
                }
            }
            maxDepth = Math.max(currentDepth, maxDepth);
        }

        //System.out.println("Максимальная вложенность: " + (maxDepth - 1));
        return Math.max(maxDepth - 1, 0);
    }

    public static void main(String[] args) throws Exception {
        //String code = Files.readString(Path.of("/home/pavel/Учеба/Metra/Lab2J/text1"));

        String code = "if (a > b) {\n" +
        "print(a) }\n" +
        "else {\n" +
        "print(b)\n" +
        "}\n";

        System.out.println("Максимальная вложенность: " + countDepth(code));
        System.out.println("Всего операторов : " + countOperators(code));
        System.out.println("Условных операторов: " + countConditionals(code));
    }
}
