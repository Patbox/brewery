package eu.pb4.brewery.drink;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public record WrappedExpression(String input, Expression expression) {
    public static WrappedExpression create(String input, String... args) {
        return new WrappedExpression(input, new ExpressionBuilder(input).variables(args).functions(ExpressionUtil.FUNCTIONS).build());
    }

    public static WrappedExpression createDefault(String input) {
        return create(input,
                ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY
        );
    }
}
