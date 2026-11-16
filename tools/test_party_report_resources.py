"""Check that the selected party-report events have their authored runtime data."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/coraline'

class PartyReportResources(unittest.TestCase):
    def test_report_pool_routes_to_registered_recordings(self):
        route = (ROOT / 'src/main/java/com/coraline/feast/PartyReportDialogue.java').read_text(encoding='utf8')
        pools = (ROOT / 'src/main/java/com/coraline/feast/PlayerParty.java').read_text(encoding='utf8')
        self.assertIn('PlayerParty.pool("guest_report",state)', route)
        self.assertIn('"checkin","guest_report","search"', pools)
        sounds = json.loads((ASSETS / 'sounds.json').read_text(encoding='utf8'))
        lengths = json.loads((ASSETS / 'visit_audio_lengths.json').read_text(encoding='utf8'))
        captions = json.loads((ASSETS / 'story_subtitles.json').read_text(encoding='utf8'))
        dialogue = json.loads((ASSETS / 'numbered_event_dialogue.json').read_text(encoding='utf8'))
        for state in ('low_low', 'mid_mid', 'high_low', 'low_high', 'high_high'):
            for i in (1, 2):
                event = f'other_mother_events_player_party_guest_report_{state}_0{i}'
                with self.subTest(event=event):
                    self.assertEqual(dialogue[f'player_party/report_{state}_{i}']['sound'], event)
                    self.assertIn(event, captions)
                    self.assertGreater(lengths[event], 0)
                    self.assertTrue(sounds[event]['sounds'])
                    for entry in sounds[event]['sounds']:
                        name = entry if isinstance(entry, str) else entry['name']
                        self.assertTrue((ASSETS / 'sounds' / (name.split(':')[-1] + '.ogg')).is_file(), name)

if __name__ == '__main__':
    unittest.main()
