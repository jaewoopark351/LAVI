<!-- 20260914_kpopmodder: Explain the isolated 1.20.1 test launcher and its exact named-registry visibility support. -->
# Automatic-deposit isolated tests

`auto-deposit-tests.init.gradle` registers only `:1.20.1` tasks. It reuses the existing strict Jupiter runner: failed, skipped, or aborted selected tests do not count as a passing suite. It does not modify production dependencies, vendor JARs, cached Minecraft JARs, game launch configuration, or the packaged mod.

## Named registry bootstrap

The raw named Minecraft JAR used by `JavaExec` lacks Fabric launcher's development package-access transformation. The first observed failure was `SimpleRegistry` calling package-private `RegistryEntry.Reference.setRegistryKey` after mappings place those classes in different packages. Read-only inspection of the locally installed Fabric Loader 0.16.2 `FabricTransformer` and `PackageAccessFixer` confirmed that the actual development launcher widens non-private access for this mapping case.

The test-only Java agent reproduces visibility for five registry members used by `SimpleRegistry` and one entity member encountered during real registry bootstrap:

| Class | Exact member |
| --- | --- |
| `RegistryEntry.Reference` | `setRegistryKey(RegistryKey): void` |
| `RegistryEntry.Reference` | `setValue(Object): void` |
| `RegistryEntry.Reference` | `setTags(Collection): void` |
| `RegistryEntryList.Named` | constructor `(RegistryEntryOwner, TagKey)` |
| `RegistryEntryList.Named` | `copyOf(List): void` |
| `LivingEntity` | `getAttackPos(): Vec3d` |

The first real bootstrap probe exposed a second named-package access failure: `MobEntity.getSquaredDistanceToAttackPosOf` invokes protected `LivingEntity.getAttackPos` on a `LivingEntity` instance across the mapped package boundary. Its exact declaration receives the same non-private-to-public visibility treatment. The original failure logs are retained; expanding the test-only list does not change Minecraft or defense method instructions.

It changes method access flags only. No method instructions, fields, private members, LAVI classes, inventory decisions, or Task behavior are altered. Existing ASM on the test runtime classpath performs the transformation. All generated agent classes and its JAR stay under `versions/1.20.1/build/auto-deposit-focused/`.

The agent validates exact targets before installing its transformer. A missing class, changed signature, or unexpected private target is an infrastructure failure. `autoDepositVerifyMinecraftBootstrap` runs real `SharedConstants.createGameVersion()` and `Bootstrap.initialize()`, then checks real AIR and DIAMOND registry IDs. The focused suite depends on this probe and starts a fresh JVM with the same agent. The probe does not launch Minecraft or exercise Fabric/Mixin game startup.

```powershell
.\gradlew.bat :1.20.1:autoDepositVerifyMinecraftBootstrap --no-daemon --offline --init-script .gradle/codex-build-init.gradle --init-script src/test/autoDeposit/auto-deposit-tests.init.gradle
```

Bootstrap and focused-test results must be read from the actual run logs. A successful probe alone does not establish suite success, deployment, Mixin injection, or gameplay acceptance.

## Verified bootstrap run

The Bootstrap-only run at `2026-09-14T03:12:58+09:00` exited 0 and printed `AUTO_DEPOSIT_MINECRAFT_BOOTSTRAP: PASS items=1255` with the six-member list above. Gradle reported three executed tasks and success in 23 seconds. See [the preserved bootstrap log](../../../../../../../logs/auto_deposit_1201_Bootstrap_20260914_031258_530/verification.log). The preceding five-member probe failed at `LivingEntity.getAttackPos`; its log remains in `logs/auto_deposit_1201_Bootstrap_20260914_031021_242/`.

This is evidence of the isolated test JVM's real registry bootstrap only. The subsequent full focused suite and 1.20.1 clean artifact verification are recorded separately by the implementation workflow.
