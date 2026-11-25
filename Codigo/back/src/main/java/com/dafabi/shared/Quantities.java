package com.dafabi.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quantidades de estoque com até três casas decimais, permitindo itens fracionados
 * como "meio frango" (0,5 unidade).
 */
public final class Quantities {

    public static final int SCALE = 3;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private Quantities() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /** Valor para respostas da API sem zeros à direita: 3 em vez de 3.000 e 0.5 em vez de 0.500. */
    public static BigDecimal forJson(BigDecimal value) {
        BigDecimal stripped = of(value).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /** Texto para mensagens: "3" para inteiros e "0,5" para frações. */
    public static String format(BigDecimal value) {
        return of(value).stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
