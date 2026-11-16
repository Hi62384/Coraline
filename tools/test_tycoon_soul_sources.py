"""Phase 4 source invariants; not Java compilation or native execution."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
def src(name):return (ROOT/'src/main/java/com/coraline/tycoon'/f'{name}.java').read_text(encoding='utf-8-sig')

class Souls(unittest.TestCase):
    def test_exact_threshold_and_no_conversion_currency_payout(self):
        text=src('TycoonSouls')
        self.assertIn('TycoonDialogue.trust(r)>=1.0',text)
        self.assertIn('TycoonSoulConfig.REJECT_TRUST.get()',text)
        self.assertIn('TycoonSoulConfig.REJECT_SUSPICION.get()',text)
        self.assertIn('ProposalAfter',text)
        self.assertNotIn('creditSoul(',text)
        self.assertNotIn('creditMicros(',text)
        self.assertIn('UUID.nameUUIDFromBytes',text)

    def test_converted_identity_retires_every_live_capability(self):
        text=src('TycoonSouls')
        for check in ('"Status","CONVERTED"','r.n.remove("Bed")','"Status","INACTIVE"','"Phase","CONVERTED"','"FirstVisitEnded",true','actor.discard()'):
            self.assertIn(check,text)
        self.assertIn('r.n.getString("Status").equals("CONVERTED")',src('TycoonCare'))

    def test_farm_currency_uses_commit_time_balance(self):
        text=src('TycoonBuildJob')
        self.assertIn('long balance=h.state.getLong("SoulEssence")',text)
        self.assertIn('patch.putLong("SoulEssence",balance-soulCost)',text)
        self.assertIn('TycoonSoulFarms.constructionValid',text)
        self.assertIn('key.equals("AttractionChanges")||key.equals("FarmChanges")',src('TycoonTransactions'))

    def test_fractional_production_and_no_offline_catchup(self):
        text=src('TycoonSoulLedger')
        self.assertIn('SCALE=1_000_000L',text)
        self.assertIn('sum%SCALE',text)
        farms=src('TycoonSoulFarms')
        self.assertIn('LastProducedClock',farms)
        self.assertIn('farm.getBoolean("Damaged")||farm.getBoolean("Paused")',farms)
        self.assertIn('Math.max(.85,Math.min(1.25',farms)
        self.assertNotIn('currentTimeMillis',farms)

    def test_breach_is_physical_persistent_and_cannot_be_cancelled_by_sendhome(self):
        text=src('TycoonSoulFarms')
        for check in ('BREACH_TICKS=12000','TycoonEvidence.perceives','"KnowsSoulFarm",true','"Status","ESCAPED"','"Damaged",true','"Progress",Math.min(BREACH_TICKS,elapsed)','p.distanceToSqr(local(farm,-4,1,-2).getCenter())>16'):
            self.assertIn(check,text)
        self.assertIn('if(r.n.hasUUID("RescuingFarm")){breach=true;continue;}',src('TycoonVisits'))
        self.assertIn('r.n.getBoolean("KnowsSoulFarm")?1:unit(suspicion)',src('TycoonDialogue'))

    def test_market_size_and_assets_do_not_expand_for_farms(self):
        text=src('TycoonSoulFarms')
        self.assertIn('LIMIT=30',text)
        self.assertIn('10L*(ordinal-2)',text)
        self.assertIn('FarmPurchases',text)
        self.assertIn('TycoonSoulFarmPlan.create',text)
        self.assertIn('net.minecraft.world.level.block',src('TycoonSoulFarmPlan'))

    def test_new_java_delimiters(self):
        for name in ('TycoonSoulConfig','TycoonSoulLedger','TycoonSouls','TycoonSoulFarmPlan','TycoonSoulFarms','TycoonSoulInteractions','TycoonPhaseFourDebug'):
            text=re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/','',src(name));stack=[]
            for c in text:
                if c in '({[':stack.append(c)
                elif c in ')}]':self.assertTrue(stack and stack.pop()=={')':'(',']':'[','}':'{'}[c],name)
            self.assertFalse(stack,name)

if __name__=='__main__':unittest.main()
