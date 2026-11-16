"""Source-only integration guards. Native gameplay tests are separate."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/coraline'

def read(name):
    return (JAVA/name).read_text(encoding='utf-8-sig')

class AttractionSources(unittest.TestCase):
    def test_ten_real_distinct_plans_and_registered_balance(self):
        types=read('tycoon/TycoonAttractionType.java')
        plan=read('tycoon/TycoonAttractionPlan.java')
        for kind in ('VILLAGE','WONDERLAND','CIRCUS','GARDEN','RESTAURANT','GALLERY','MAZE','PLAYGROUND','THEATRE','RETREAT'):
            self.assertRegex(types,r'\b'+kind+r'\(')
            self.assertIn('case '+kind+'->',plan)
        self.assertIn('TycoonAttractionConfig.SPEC',read('CoralineMod.java'))
        self.assertIn('storage_cabinet',plan)

    def test_placement_uses_existing_paid_job_and_protects_terrain(self):
        text=read('tycoon/TycoonAttractions.java')
        for check in ('TycoonLandscape.owned(h,point)','TycoonLandscape.core(h,point)','other.containsKey(at)||house.containsKey(at)','TycoonBuildJob.begin(p,','occupied(h,id)>0'):
            self.assertIn(check,text)
        job=read('tycoon/TycoonBuildJob.java')
        self.assertIn('TycoonAttractions.generated(l,h)',job)
        self.assertIn('structures.containsKey(at)',job)
        self.assertIn('ItemStack.of(job.getCompound("Refund"))',job)

    def test_matching_does_not_use_curiosity_or_giftableness(self):
        text=read('tycoon/TycoonAttractions.java').split('public static double match(',1)[1].split('public static double wanderChance(',1)[0]
        self.assertNotIn('Curiosity',text)
        self.assertNotIn('Giftableness',text)
        self.assertIn('EmotionalSensitivity',text)
        self.assertIn('crowd',text)

    def test_activities_count_real_simulation_and_have_bounded_memory(self):
        text=read('tycoon/TycoonAttractions.java')
        self.assertIn('a.putInt("Progress",progress+20)',text)
        self.assertIn('while(memories.size()>40)',text)
        self.assertIn('h.clock()-a.getLong("AssignedAt")>1200',text)
        visits=read('tycoon/TycoonVisits.java')
        self.assertIn('else if(!TycoonMeals.otherWorld(p,r,actor))TycoonAttractions.actorTick(p,r,actor)',visits)

    def test_gifts_are_signed_and_physically_debited(self):
        text=read('tycoon/TycoonGifts.java')
        self.assertIn('r.trait("Giftableness")<0',text)
        self.assertIn('r.trait("Giftableness")<=0',text)
        self.assertIn('TycoonTransactions.consumeHeld(',text)
        self.assertIn('TycoonCheckpoints.home(p)',text)
        self.assertIn('be.setItem(slot,ItemStack.of(n.getCompound("After")))',text)
        self.assertIn('be.giftDebits.getBoolean(receipt)',text)
        self.assertIn('TycoonFoodContent.index(item)==7',text)
        self.assertNotIn('new ItemStack(Items.DIAMOND',text)

    def test_detection_is_local_los_facing_and_expires(self):
        evidence=read('tycoon/TycoonEvidence.java')
        for check in ('range*range','getLookAngle().dot','ClipContext.Block.COLLIDER','LastSeen','EvidenceMemories'):
            self.assertIn(check,evidence)
        spies=read('tycoon/TycoonSpies.java')
        for check in ('TycoonEvidence.perceives','marker.getLong("Expires")<=h.clock()','TycoonAttractionConfig.TRACK_TICKS','PatrolAfter'):
            self.assertIn(check,spies)
        self.assertIn('Send to Structure',read('client/TycoonConversationScreen.java'))
        self.assertIn('Return to House',read('client/TycoonConversationScreen.java'))

    def test_new_java_delimiters_are_balanced(self):
        names=['TycoonAttractionConfig','TycoonAttractionType','TycoonAttractionPlan','TycoonAttractions','TycoonGifts','TycoonEvidence','TycoonSpies']
        for name in names:
            text=read('tycoon/'+name+'.java')
            text=re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/','',text)
            stack=[]
            for c in text:
                if c in '({[':stack.append(c)
                elif c in ')}]':self.assertTrue(stack and stack.pop()=={')':'(',']':'[','}':'{'}[c],name)
            self.assertFalse(stack,name)

if __name__=='__main__':unittest.main()
