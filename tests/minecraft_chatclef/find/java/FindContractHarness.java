//20260915_kpopmodder: Pure Java ID contracts; human-name contracts are tested at the Python boundary.
package lavi.minecraft.task.find;
import java.util.*;
public class FindContractHarness {
 static int checks;
 static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
 public static void main(String[] args){
  for(String raw:List.of("find villager","@find entity minecraft:villager report","find item mod:rare_gem","find player Steve")){
   var r=FindRequest.parse(raw);check(FindRequest.parse(r.command()).equals(r),"round trip "+raw);
  }
  for(String raw:List.of("find","@find item","find foo;bar","find foo#bar","find foo\nbar","find foo\tbar","find foo\u200bbar","find @attack","@@find foo","find player 주민","find player 12345678901234567","find "+"a".repeat(129),"find 주민","find iron golem","find entity Minecraft:iron_golem","find entity 철골램")){
   boolean rejected=false;try{FindRequest.parse(raw);}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"reject "+raw);
  }
  var index=new FindNameIndex();
  var villager=new FindNameIndex.Entry("entity","minecraft:villager","minecraft:villager","");index.add(villager);
  for(String id:List.of("villager","minecraft:villager"))
   check(index.resolve(new FindRequest("auto",id,"approach")).unique().equals(villager),"vanilla ID "+id);
  var chest=new FindNameIndex.Entry("block","minecraft:chest","minecraft:chest","");index.add(chest);
  index.add(new FindNameIndex.Entry("item","minecraft:chest","minecraft:chest","minecraft:chest"));
  check(index.resolve(FindRequest.parse("find chest")).unique().equals(chest),"BlockItem default selects placed block");
  check(index.resolve(FindRequest.parse("find item chest")).unique().kind().equals("item"),"explicit dropped BlockItem");
  var diamond=new FindNameIndex.Entry("block","minecraft:diamond_block","minecraft:diamond_block","");index.add(diamond);
  check(index.resolve(FindRequest.parse("find block diamond_block")).unique().equals(diamond),"exact block ID");
  index.add(new FindNameIndex.Entry("block","other:chest","other:chest",""));
  check(index.resolve(FindRequest.parse("find chest")).unique().equals(chest),"short ID means vanilla, not another namespace");
  check(index.resolve(FindRequest.parse("find block other:chest")).unique().id().equals("other:chest"),"explicit mod namespace");
  index.add(new FindNameIndex.Entry("entity","minecraft:chest","minecraft:chest",""));
  check(index.resolve(FindRequest.parse("find chest")).candidates().size()==2,"cross-registry collision is ambiguous");
  check(index.resolve(FindRequest.parse("find block minecraft:chest")).unique().equals(chest),"exact kind resolves collision");
  check(index.resolve(FindRequest.parse("find block missing:chest")).candidates().isEmpty(),"unknown namespace never defaults");
  index.add(new FindNameIndex.Entry("item","mod:foo_bar","one",""));index.add(new FindNameIndex.Entry("item","mod:foobar","two",""));
  check(index.resolve(FindRequest.parse("find item mod:foo_bar")).unique().id().equals("mod:foo_bar"),"ID underscore exactness");
  check(index.resolve(FindRequest.parse("find item foo_bar")).candidates().isEmpty(),"short ID never aliases another namespace");
  var positions=new ArrayList<>(List.of(1,2,3));var suggestions=new ArrayList<String>();
  var outcome=new FindOutcome(UUID.randomUUID().toString(),FindRequest.parse("find villager"),"ARRIVED","entity","minecraft:villager","minecraft:villager","minecraft:overworld",positions,64,4,true,true,UUID.randomUUID().toString(),0,suggestions);
  positions.set(0,99);check(outcome.position().equals(List.of(1,2,3)),"snapshot immutability");
  check(outcome.toMap().size()==20,"wire exact keys");check(outcome.success(),"ARRIVED success");check(outcome.koreanMessage().contains("1, 2, 3"),"native coordinate message");
  System.out.println("PURE_JAVA_CONTRACT_CHECKS="+checks);
 }
}
