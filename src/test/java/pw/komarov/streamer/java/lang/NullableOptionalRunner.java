package pw.komarov.streamer.java.lang;

import pw.komarov.java.lang.NullableOptional;

import java.util.Optional;

public class NullableOptionalRunner {
    public static void main(String[] args) {
        //case #1
        System.out.print("case #1: ");
        NullableOptional
                .empty()
                .ifPresent(o -> {
                        throw new AssertionError("expected: elseIf() completion, but ifPresent() completed");
                    })
                .elseIf(() -> System.out.println("passed! (elseIf() completed!)"));
        //case #2
        System.out.print("case #2: ");
        NullableOptional
                .ofNullableNotEmpty(null) //not empty
                .ifPresent(o -> System.out.printf("passed! (ifPresent(%s) completed!\n", o))
                .elseIf(() -> {
                    throw new AssertionError("expected: elseIf() completion, but ifPresent() completed");
                });
        //case #3
        System.out.print("case #3: ");
        NullableOptional
                .ofNullable(null) //empty
                .ifPresent(o -> {
                    throw new AssertionError("expected: elseIf() completion, but ifPresent() completed");
                })
                .elseIf(() -> System.out.println("passed! (elseIf() completed!)"));
        //case #4
        System.out.print("case #4: ");
        NullableOptional
                .ofNullable(new Object())
                .ifPresent(o -> System.out.printf("passed! (ifPresent(%s) completed!\n", o))
                .elseIf(() -> {
                    throw new AssertionError("expected: elseIf() completion, but ifPresent() completed");
                });
        //case #5
        System.out.print("case #5: ");
        NullableOptional
                .of(new Object())
                .ifPresent(o -> System.out.printf("passed! (ifPresent(%s) completed!\n", o))
                .elseIf(() -> {
                    throw new AssertionError("expected: elseIf() completion, but ifPresent() completed");
                });
        //case #6
        System.out.print("case #6: ");
        Exception exception = null;
        try {
            NullableOptional
                    .of(null)
                    .ifPresent(o -> {
                            throw new AssertionError("expected: exception throws, but ifPresent() completed");
                        })
                    .elseIf(() -> {
                            throw new AssertionError("expected: exception throws, but elseIf() completed");
                        });
        } catch (NullPointerException npe) {
            exception = npe;
        }

        if (exception == null)
            throw new AssertionError("expected: exception throws");
        else
            System.out.println("passed! exception throws: " + exception);
    }
}
