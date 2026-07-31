package lavi.minecraft.diagnostics;

import java.util.StringJoiner;

//20260731_kpopmodder: Keep caller-stack formatting out of input event assembly.
final class DiagnosticCallerStack {
    private DiagnosticCallerStack() {
    }

    static String captureExcluding(Class<?> excludedClass) {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String excludedName = excludedClass == null ? "" : excludedClass.getName();
            String ownName = DiagnosticCallerStack.class.getName();
            StringJoiner joiner = new StringJoiner("<-");
            int added = 0;
            for (int i = 2; i < stack.length && added < 10; i++) {
                StackTraceElement element = stack[i];
                String className = element.getClassName();
                if (className.equals(ownName) || className.equals(excludedName)) {
                    continue;
                }
                joiner.add(className + "." + element.getMethodName() + ":" + element.getLineNumber());
                added++;
            }
            return added == 0 ? "unavailable" : joiner.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }
}
