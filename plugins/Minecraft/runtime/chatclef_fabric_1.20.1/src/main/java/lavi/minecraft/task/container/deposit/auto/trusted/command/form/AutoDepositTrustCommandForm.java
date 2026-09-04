package lavi.minecraft.task.container.deposit.auto.trusted.command.form;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Represent only the exact direct-command forms approved for trusted registration.
public enum AutoDepositTrustCommandForm {
    ENGLISH_SINGLE("@auto_deposit_trust", false),
    ENGLISH_AREA("@auto_deposit_trust area 16x16", true),
    ENGLISH_RADIUS("@auto_deposit_trust \uBC18\uACBD 16x16", true),
    KOREAN_AREA("@\uC790\uB3D9\uBCF4\uAD00\uB4F1\uB85D \uC601\uC5ED 16x16", true),
    KOREAN_RADIUS("@\uC790\uB3D9\uBCF4\uAD00\uB4F1\uB85D \uBC18\uACBD 16x16", true);

    private final String canonicalInvocation;
    private final boolean bulk;

    AutoDepositTrustCommandForm(String canonicalInvocation, boolean bulk) {
        this.canonicalInvocation = canonicalInvocation;
        this.bulk = bulk;
    }

    public String canonicalInvocation() {
        return canonicalInvocation;
    }

    public boolean bulk() {
        return bulk;
    }
}
