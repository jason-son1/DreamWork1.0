package com.dreamwork.core.job.engine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 경험치 수식 계산기
 * 
 * <p>
 * 문자열 형태의 수학 수식을 파싱하고 계산합니다.
 * 지원 연산자: +, -, *, /, ^ (거듭제곱)
 * 지원 변수: level
 * </p>
 * 
 * 예시: "100 + (level * 25) + (level^2 * 5)"
 */
public class ExpCalculator {

    // 연산자 우선순위
    private static final Map<String, Integer> PRECEDENCE = Map.of(
            "+", 1, "-", 1,
            "*", 2, "/", 2,
            "^", 3);

    private final String formula;
    private final List<String> postfix;

    public ExpCalculator(String formula) {
        this.formula = formula;
        this.postfix = convertToPostfix(formula);
    }

    /**
     * 특정 레벨에 대한 필요 경험치를 계산합니다.
     * 
     * @param level 현재 레벨
     * @return 필요 경험치
     */
    public double getRequiredExp(int level) {
        return evaluate(postfix, level);
    }

    /**
     * 중위 표기법(Infix)을 후위 표기법(Postfix)으로 변환 (Shunting-yard algorithm)
     */
    private List<String> convertToPostfix(String infix) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        // 토큰 분리 (숫자, 변수, 연산자, 괄호)
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?|[a-zA-Z]+|[+\\-*/^()]");
        Matcher matcher = pattern.matcher(infix);

        while (matcher.find()) {
            String token = matcher.group();

            if (isNumber(token) || token.equalsIgnoreCase("level")) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (!stack.isEmpty())
                    stack.pop(); // '(' 제거
            } else if (isOperator(token)) {
                while (!stack.isEmpty() && !stack.peek().equals("(") &&
                        getPrecedence(token) <= getPrecedence(stack.peek())) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }

        return output;
    }

    /**
     * 후위 표기법 수식 계산
     */
    private double evaluate(List<String> postfix, int level) {
        Stack<Double> stack = new Stack<>();

        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (token.equalsIgnoreCase("level")) {
                stack.push((double) level);
            } else if (isOperator(token)) {
                if (stack.size() < 2)
                    return 0;
                double b = stack.pop();
                double a = stack.pop();
                stack.push(applyOp(token, a, b));
            }
        }

        return stack.isEmpty() ? 0 : stack.pop();
    }

    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(String token) {
        return PRECEDENCE.containsKey(token);
    }

    private int getPrecedence(String op) {
        return PRECEDENCE.getOrDefault(op, 0);
    }

    private double applyOp(String op, double a, double b) {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b == 0 ? 0 : a / b;
            case "^" -> Math.pow(a, b);
            default -> 0;
        };
    }
}
