package com.xiyu.bid.bidresult.core;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A functional Result wrapper representing either a Success (S) or a Failure (F).
 */
public final class FunctionalResult<S, F> {
    private final S success;
    private final F failure;
    private final boolean isSuccess;

    private FunctionalResult(S success, F failure, boolean isSuccess) {
        this.success = success;
        this.failure = failure;
        this.isSuccess = isSuccess;
    }

    public static <S, F> FunctionalResult<S, F> success(S value) {
        return new FunctionalResult<>(value, null, true);
    }

    public static <S, F> FunctionalResult<S, F> failure(F error) {
        return new FunctionalResult<>(null, Objects.requireNonNull(error), false);
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isFailure() {
        return !isSuccess;
    }

    public <T> FunctionalResult<T, F> map(Function<? super S, ? extends T> mapper) {
        if (isSuccess) {
            return success(mapper.apply(success));
        }
        return failure(failure);
    }

    public <T> FunctionalResult<T, F> flatMap(Function<? super S, FunctionalResult<T, F>> mapper) {
        if (isSuccess) {
            return mapper.apply(success);
        }
        return failure(failure);
    }

    public void ifSuccess(Consumer<? super S> action) {
        if (isSuccess) {
            action.accept(success);
        }
    }

    public S orElseThrow(Function<? super F, ? extends RuntimeException> exceptionSupplier) {
        if (isSuccess) {
            return success;
        }
        throw exceptionSupplier.apply(failure);
    }
}
