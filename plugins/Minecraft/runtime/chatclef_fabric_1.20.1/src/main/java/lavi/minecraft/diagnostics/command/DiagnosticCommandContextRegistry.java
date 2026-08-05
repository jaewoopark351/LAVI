package lavi.minecraft.diagnostics.command;

//20260805_kpopmodder: Isolate diagnostic command-context provider ownership from event formatting.
public final class DiagnosticCommandContextRegistry {
    private volatile DiagnosticCommandContextProvider provider = DiagnosticCommandContextSnapshot::unavailable;

    public void register(DiagnosticCommandContextProvider provider) {
        if (provider != null) {
            this.provider = provider;
        }
    }

    public Object[] fields() {
        try {
            DiagnosticCommandContextSnapshot snapshot = provider.snapshot();
            return snapshot == null
                    ? DiagnosticCommandContextSnapshot.unavailable("provider_returned_null").fields()
                    : snapshot.fields();
        } catch (RuntimeException | LinkageError error) {
            return DiagnosticCommandContextSnapshot.unavailable(error.getClass().getSimpleName()).fields();
        }
    }

    public Object[] appendFields(Object... fields) {
        Object[] commandFields = fields();
        if (fields == null || fields.length == 0) {
            return commandFields;
        }
        Object[] merged = new Object[fields.length + commandFields.length];
        System.arraycopy(fields, 0, merged, 0, fields.length);
        System.arraycopy(commandFields, 0, merged, fields.length, commandFields.length);
        return merged;
    }
}
