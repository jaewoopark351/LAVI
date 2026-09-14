//20260914_kpopmodder: Pure Java contracts; no Minecraft dependency or API stand-ins needed.
package lavi.minecraft.task.find;
import java.util.*;
public class FindContractHarness {
 static int checks;
 static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
 public static void main(String[] args){
  for(String raw:List.of("find 주민","@find entity minecraft:villager report","find item 모드 보석","find player Steve")){
   var r=FindRequest.parse(raw);check(FindRequest.parse(r.command()).equals(r),"round trip "+raw);
  }
  for(String raw:List.of("find","@find item","find foo;bar","find foo#bar","find foo\nbar","find foo\tbar","find foo\u200bbar","find @attack","@@find foo","find player 주민","find player 12345678901234567","find "+"a".repeat(129))){
   boolean rejected=false;try{FindRequest.parse(raw);}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"reject "+raw);
  }
  var index=new FindNameIndex();
  var villager=new FindNameIndex.Entry("entity","minecraft:villager","주민","");index.add(villager,"마을 주민","Villager");
  for(String alias:List.of("주민","마을주민","마을 주민","Villager","VILLAGER","minecraft:villager"))
   check(index.resolve(new FindRequest("auto",alias,"approach")).unique().equals(villager),"villager alias "+alias);
  var chest=new FindNameIndex.Entry("block","minecraft:chest","상자","");index.add(chest);
  index.add(new FindNameIndex.Entry("item","minecraft:chest","상자","minecraft:chest"));
  check(index.resolve(FindRequest.parse("find 상자")).unique().equals(chest),"BlockItem default selects placed block");
  check(index.resolve(FindRequest.parse("find item 상자")).unique().kind().equals("item"),"explicit dropped BlockItem");
  check(index.resolve(FindRequest.parse("find block 상자 블록")).unique().equals(chest),"extra Korean kind suffix");
  var diamond=new FindNameIndex.Entry("block","minecraft:diamond_block","다이아몬드 블록","");index.add(diamond);
  check(index.resolve(FindRequest.parse("find block 다이아몬드 블록")).unique().equals(diamond),"official block suffix preserved");
  index.add(new FindNameIndex.Entry("block","other:chest","상자",""));
  check(index.resolve(FindRequest.parse("find 상자")).candidates().size()==2,"namespace collision is ambiguous");
  check(index.resolve(FindRequest.parse("find block minecraft:chest")).unique().equals(chest),"exact ID resolves collision");
  check(index.resolve(FindRequest.parse("find block missing:chest")).candidates().isEmpty(),"unknown namespace never defaults");
  index.add(new FindNameIndex.Entry("item","mod:foo_bar","one",""));index.add(new FindNameIndex.Entry("item","mod:foobar","two",""));
  check(index.resolve(FindRequest.parse("find item mod:foo_bar")).unique().id().equals("mod:foo_bar"),"ID underscore exactness");
  check(index.resolve(FindRequest.parse("find item foo_bar")).candidates().size()==2,"normalized short aliases remain ambiguous");
  var positions=new ArrayList<>(List.of(1,2,3));var suggestions=new ArrayList<String>();
  var outcome=new FindOutcome(UUID.randomUUID().toString(),FindRequest.parse("find 주민"),"ARRIVED","entity","minecraft:villager","주민","minecraft:overworld",positions,64,4,true,true,UUID.randomUUID().toString(),0,suggestions);
  positions.set(0,99);check(outcome.position().equals(List.of(1,2,3)),"snapshot immutability");
  check(outcome.toMap().size()==20,"wire exact keys");check(outcome.success(),"ARRIVED success");check(outcome.koreanMessage().contains("1, 2, 3"),"native Korean coordinate message");
  System.out.println("PURE_JAVA_CONTRACT_CHECKS="+checks);
 }
}
