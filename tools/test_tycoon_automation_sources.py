"""Automation source guards. Native/graphical acceptance must run separately."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
def src(name):return (ROOT/'src/main/java/com/coraline/tycoon'/f'{name}.java').read_text(encoding='utf-8-sig')

class Automation(unittest.TestCase):
    def test_workers_are_real_registered_npcs_not_residents(self):
        text=src('TycoonWorker')
        self.assertIn('extends PathfinderMob',text)
        self.assertNotIn('TycoonCare.tickNpc',text)
        self.assertIn('getNavigation()',text)
        self.assertIn('SynchedEntityData',text)
        mod=(ROOT/'src/main/java/com/coraline/CoralineMod.java').read_text(encoding='utf-8-sig')
        self.assertIn('TycoonAutomationContent.init()',mod)
        self.assertIn('TycoonAutomationConfig.SPEC',mod)

    def test_all_worker_roles_have_executors(self):
        jobs=src('TycoonWorkerJobs')
        for role in ('OVERWORLD_MAID','OVERWORLD_CHEF','ORPHAN_BUYER','DOLL_MAKER','COMPLIANCE','OTHER_MANAGER','OTHER_CHEF','GUARD','GIFT_MANAGER','VISIT_COORDINATOR','SOUL_MANAGER','STRUCTURE_MANAGER','TECHNICIAN','VOID_OPERATOR','ACCOUNTANT','MASTER'):
            self.assertRegex(jobs,r'case '+role+r'\s*->')

    def test_payroll_operating_level_break_and_idempotency(self):
        text=src('TycoonWorkers')
        for check in ('worker.getInt("Operating")','worker.getInt("Unlocked")','w.getBoolean("Break")','SalaryRemainder','sum/60','LastTick','TycoonSoulLedger.debit(h,due','UNPAID_PAUSED'):
            self.assertIn(check,text)
        self.assertLess(text.index('boolean paid=actor!=null&&bill(h,w)'),text.index('TycoonWorkerJobs.step('))
        self.assertIn('finally{CONTEXT.remove();}',text)
        self.assertIn('WorkerReservations',text)

    def test_placement_and_dolls_reuse_canonical_ids_and_limits(self):
        agency=src('TycoonAgency').split('static String placeSupplied',1)[1]
        for check in ('h.cooldown()>0','maxResidents','survey.approved()','TycoonPlacements.commitSupplied'):
            self.assertIn(check,agency)
        placements=src('TycoonPlacements').split('static String commitSupplied',1)[1]
        self.assertIn('TycoonConfig.PLACEMENT_TICKS.get()',placements)
        self.assertIn('TycoonWarehouse.take(h,Items.EMERALD,fee)',placements)
        dolls=src('TycoonDolls').split('static boolean craftForWorker',1)[1]
        self.assertIn('WORKER_DELIVERY',dolls)
        self.assertIn('TycoonWarehouse.purchase',dolls)
        self.assertIn('doll.setUUID(record.getUUID("Entity"))',dolls)

    def test_paid_converter_and_real_input_extractor(self):
        text=src('TycoonMachines')
        for check in ('LastTick','row.getBoolean("Break")','OUTPUT_FULL','TycoonSoulLedger.debit(h,cost','ProducedResources','Items.AMETHYST_SHARD,Items.COAL,Items.REDSTONE','takeSupply(h,item,batches)','TycoonWorkers.constructing'):
            if check=='TycoonWorkers.constructing':continue
            self.assertIn(check,text)
        self.assertLess(text.index('TycoonSoulLedger.debit(h,cost'),text.index('stock.putLong(recipe'))
        self.assertIn('TycoonAttractions.generated(l,h)',text)
        config=src('TycoonAutomationConfig')
        for dangerous in ('void_heart','nether_star','little_door_key'):
            self.assertNotIn(dangerous,config)

    def test_warehouse_debits_real_items_and_preserves_full_inventory(self):
        text=src('TycoonWarehouse')
        for check in ('item.hasTag()','TycoonTransactions.consumeHeldCount','stock.putLong(itemId,amount-count)','Your inventory is full; all output remains stored','TycoonTransactions.purchase'):
            self.assertIn(check,text)
        self.assertIn('TycoonCheckpoints.home(p)',src('TycoonHauling'))
        self.assertIn('be.giftDebits.getBoolean(receipt)',src('TycoonHauling'))

    def test_no_hidden_full_knowledge_at_low_worker_levels(self):
        text=src('TycoonWorkerJobs')
        self.assertIn('if(rank==5)return r.trait(trait)',text)
        self.assertIn('TycoonDollAnalysis.progress(r)<.25',text)
        self.assertIn('rank==5?TycoonDialogue.trust(r)>=1',text)
        self.assertIn('!soul.getBoolean("Protected")',text)

    def test_default_permissions_and_explicit_limits(self):
        text=src('TycoonPolicies')
        for check in ('foodBudgetHour','giftBudgetHour','constructionPowerHour','investmentLimit','reserve_tile_','allow_structure_','allow_company_'):
            self.assertIn(check,text)
        self.assertNotIn('putBoolean("autoRelease",true)',text)
        self.assertNotIn('putBoolean("investing",true)',text)

    def test_market_is_server_wide_and_quotes_are_persistent(self):
        text=src('TycoonMarket')
        for check in ('extends SavedData','coraline_tycoon_market','quote!=market.revision()','Order already executed','TradeReceiptRevision','receipts.size()>=512','LastTick','Math.min(240','gross/100','CostBasis','RealizedProfitMicros'):
            self.assertIn(check,text)
        self.assertNotIn('while(receipts.size()>128)',text)
        client=(ROOT/'src/main/java/com/coraline/client/TycoonAutomationScreen.java').read_text(encoding='utf-8-sig')
        self.assertIn('getLongArray("History")',client)
        self.assertIn('Prices can fall',client)
        self.assertIn('Recorded price',client)

    def test_guard_needs_report_and_physical_access(self):
        text=src('TycoonWorkerJobs').split('private static void guard(',1)[1].split('private static void souls(',1)[0]
        self.assertIn('AlarmReported',text)
        self.assertIn('SpyMarker',text)
        self.assertIn('if(!detected||',text)
        self.assertIn('TycoonWorkers.approach',text)
        self.assertIn('TycoonEvidence.perceives',text)

    def test_all_tycoon_java_delimiters(self):
        for path in (ROOT/'src/main/java/com/coraline/tycoon').glob('*.java'):
            text=re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/','',path.read_text(encoding='utf-8-sig'));stack=[]
            for c in text:
                if c in '({[':stack.append(c)
                elif c in ')}]':self.assertTrue(stack and stack.pop()=={')':'(',']':'[','}':'{'}[c],path.name)
            self.assertFalse(stack,path.name)

if __name__=='__main__':unittest.main()
