//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import java.lang.reflect.Modifier;
//$$ import java.lang.reflect.ParameterizedType;
//$$ import java.util.IdentityHashMap;
//$$ import java.util.Map;
//$$ import net.minecraft.entity.EntityType;
//$$ import net.minecraft.entity.mob.MobEntity;

//$$ //20260914_kpopmodder: Read vanilla declared type metadata without creating entities; unproven mod types stay unknown.
//$$ public final class FindEntityEligibility {
//$$     private FindEntityEligibility() { }
//$$     public static Map<EntityType<?>, String> captureDeclaredVanillaTypes() {
//$$         IdentityHashMap<EntityType<?>, String> result = new IdentityHashMap<>();
//$$         for (var field : EntityType.class.getFields()) {
//$$             if (!Modifier.isStatic(field.getModifiers()) || !(field.getGenericType() instanceof ParameterizedType generic)
//$$                     || generic.getRawType() != EntityType.class || generic.getActualTypeArguments().length != 1
//$$                     || !(generic.getActualTypeArguments()[0] instanceof Class<?> entityClass)) continue;
//$$             try {
//$$                 if (field.get(null) instanceof EntityType<?> type) result.put(type, MobEntity.class.isAssignableFrom(entityClass) ? "mob" : "non_mob");
//$$             } catch (IllegalAccessException ignored) { }
//$$         }
//$$         return Map.copyOf(result);
//$$     }
//$$ }

//#endif
