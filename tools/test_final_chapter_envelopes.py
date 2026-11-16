import array,copy,math,unittest
from build_final_chapter_envelopes import envelope,merge,cache_matches,waveform_hash

class FinalEnvelopeTests(unittest.TestCase):
    def test_silence_and_original_signal_have_twenty_hz_frames(self):
        samples=array.array('h',[0]*800+[12000]*800+[-12000]*800+[0]*800)
        values=envelope(samples.tobytes());self.assertEqual(len(values),4)
        self.assertEqual(values[0],0);self.assertEqual(values[3],0);self.assertEqual(values[1],values[2]);self.assertGreater(values[1],.9)
    def test_merge_preserves_unrelated_entries_and_inputs(self):
        prior={'original':[.1,.2],'crawling_hand_a':[0]};before=copy.deepcopy(prior);added={'crawling_hand_'+c:[0,.5,0] for c in 'abcdefghi'}
        result=merge(prior,added);self.assertEqual(result['original'],prior['original']);self.assertEqual(prior,before);self.assertEqual(len(result),10)
    def test_incomplete_or_invalid_waveforms_fail_closed(self):
        added={'crawling_hand_'+c:[0,.5] for c in 'abcdefghi'}
        with self.assertRaises(ValueError):merge({},dict(list(added.items())[:-1]))
        for value in ([],[math.nan],[math.inf],[-.1],[1.1],[True]):
            with self.subTest(value=value),self.assertRaises(ValueError):merge({},{**added,'crawling_hand_a':value})
    def test_verified_reuse_requires_exact_algorithm_recordings_masters_and_waveforms(self):
        values={'crawling_hand_'+c:[0,.5,0] for c in 'abcdefghi'}
        identities={k:{'ogg_sha256':'ogg_'+k,'source_sha256':'master_'+k} for k in values}
        proof={'status':'passed','algorithm_sha256':'algorithm','recordings':{k:{**v,'frames_20hz':3,'envelope_sha256':waveform_hash(values[k])} for k,v in identities.items()}}
        self.assertTrue(cache_matches(proof,identities,values,'algorithm'))
        self.assertFalse(cache_matches(proof,identities,values,'changed algorithm'))
        for field in ('ogg_sha256','source_sha256'):
            changed=copy.deepcopy(identities);changed['crawling_hand_a'][field]='changed'
            self.assertFalse(cache_matches(proof,changed,values,'algorithm'))
        self.assertFalse(cache_matches(proof,identities,{**values,'crawling_hand_a':[0,.6,0]},'algorithm'))
        self.assertFalse(cache_matches(proof,identities,{**values,'crawling_hand_a':[0,float('nan'),0]},'algorithm'))
        incomplete=copy.deepcopy(proof);del incomplete['recordings']['crawling_hand_a']['envelope_sha256']
        self.assertFalse(cache_matches(incomplete,identities,values,'algorithm'))

if __name__=='__main__':unittest.main()
