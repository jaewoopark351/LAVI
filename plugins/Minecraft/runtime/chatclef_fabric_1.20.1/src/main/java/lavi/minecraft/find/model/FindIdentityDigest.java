//#if MC == 12001
//$$ package lavi.minecraft.find.model;
//$$
//$$ import java.nio.charset.StandardCharsets;
//$$ import java.security.MessageDigest;
//$$ import java.security.NoSuchAlgorithmException;
//$$ import java.util.HexFormat;
//$$
//$$ //20260914_kpopmodder: Keep identity hashing independent of Minecraft observation and world binding.
//$$ public final class FindIdentityDigest {
//$$     private FindIdentityDigest() { }
//$$     public static String digest(String input) {
//$$         try {
//$$             return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
//$$         } catch (NoSuchAlgorithmException impossible) {
//$$             throw new IllegalStateException("sha256_unavailable", impossible);
//$$         }
//$$     }
//$$ }
//#endif
