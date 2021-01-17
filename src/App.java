import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


import analyser.Analyser;
import error.CompileError;
import generator.Generator;
import instruction.Instruction;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws CompileError, IOException {
        File input = new File(args[1]);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(args[2]));
        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var tokens = new ArrayList<Token>();
        if(rgs[0].equals("--tokenize")){
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                    System.out.println(token.toString());
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                System.err.println(e);
                System.exit(0);
                return;
            }
        }
        else if(rgs[0].equals("--analyse")){
            var analyzer = new Analyser(tokenizer);
            List<Instruction> instructions;
            analyzer.analyse();
        }
        else if(rgs[0].equals("--generate")){
            var analyzer = new Analyser(tokenizer);
            analyzer.analyse();
            var generator=new Generator(dos,analyzer);
            generator.generateo0();
        }
    }
    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
