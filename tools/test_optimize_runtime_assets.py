import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from PIL import Image
from optimize_runtime_assets import optimize_png, deduplicate_sounds, optimize, rebuild_durations


class RuntimeAssetTests(unittest.TestCase):
    def png(self, size):
        image = Image.new('RGBA', size, (20, 100, 180, 0))
        image.paste((250, 150, 20, 255), (size[0]//4, size[1]//4, size[0]*3//4, size[1]*3//4))
        data = io.BytesIO()
        image.save(data, format='PNG', compress_level=0)
        return data.getvalue()

    def test_small_pixel_art_is_lossless(self):
        source = self.png((32, 16))
        result = optimize_png(source, 256)
        with Image.open(io.BytesIO(source)) as a, Image.open(io.BytesIO(result)) as b:
            self.assertEqual(a.size, b.size)
            self.assertEqual(a.tobytes(), b.tobytes())
        self.assertLess(len(result), len(source))

    def test_large_image_preserves_aspect_and_alpha(self):
        result = optimize_png(self.png((1254, 627)), 256)
        with Image.open(io.BytesIO(result)) as image:
            self.assertEqual(image.size, (256, 128))
            self.assertEqual(image.mode, 'RGBA')
            self.assertEqual(image.getextrema()[3], (0, 255))

    def test_animation_strip_unchanged(self):
        source = self.png((16, 1024))
        self.assertEqual(optimize_png(source, 256, animated=True), source)

    def test_cannot_optimize_source_tree(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            with self.assertRaises(ValueError):
                optimize(path, path/'cache', path/'report.json')

    def test_audio_alias_keeps_events_and_playback_properties(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory); assets = base/'coraline'; cache = base/'cache'
            (assets/'sounds').mkdir(parents=True); cache.mkdir()
            for name in ('a', 'b'):
                subprocess.run(['ffmpeg', '-v', 'error', '-f', 'lavfi', '-i', 'sine=frequency=440:duration=0.1',
                    '-c:a', 'libvorbis', str(assets/'sounds'/f'{name}.ogg')], check=True)
            sounds = {'event_a': {'sounds': ['coraline:a']},
                      'event_b': {'subtitle': 'subtitles.test', 'sounds': [{'name': 'coraline:b', 'stream': True, 'volume': .5}]},
                      'event_alias': {'sounds': [{'name': 'coraline:event_b', 'type': 'event'}]}}
            (assets/'sounds.json').write_text(json.dumps(sounds))
            duplicates = deduplicate_sounds(assets, cache)
            self.assertEqual(len(duplicates), 1)
            result = json.loads((assets/'sounds.json').read_text())
            self.assertEqual(set(result), set(sounds))
            self.assertEqual(result['event_b'], {'subtitle': 'subtitles.test', 'sounds': [{'name': 'coraline:a', 'stream': True, 'volume': .5}]})
            self.assertEqual(result['event_alias'], sounds['event_alias'])
            self.assertFalse((assets/'sounds/b.ogg').exists())
            self.assertEqual(deduplicate_sounds(assets, cache), [])
            (assets/'visit_audio_lengths.json').write_text('{}')
            rebuild_durations(assets, cache)
            lengths = json.loads((assets/'visit_audio_lengths.json').read_text())
            self.assertGreater(lengths['event_a'], 0)
            self.assertEqual(lengths['event_a'], lengths['event_b'])

    def test_high_rate_audio_keeps_full_duration(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory); assets = base/'coraline'; cache = base/'cache'
            (assets/'sounds').mkdir(parents=True); cache.mkdir()
            audio = assets/'sounds/music.ogg'
            subprocess.run(['ffmpeg', '-v', 'error', '-f', 'lavfi', '-i',
                'sine=frequency=440:duration=2:sample_rate=192000', '-c:a', 'libvorbis', '-q:a', '5', str(audio)], check=True)
            (assets/'sounds.json').write_text(json.dumps({'music': {'sounds': [{'name': 'coraline:music', 'stream': True}]}}))
            changed = []; deduplicate_sounds(assets, cache, changed)
            self.assertEqual(len(changed), 1)
            probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
                'stream=sample_rate,channels,duration', '-of', 'json', str(audio)], text=True))['streams'][0]
            self.assertEqual(probe['sample_rate'], '48000')
            self.assertEqual(probe['channels'], 1)
            self.assertAlmostEqual(float(probe['duration']), 2, places=2)


if __name__ == '__main__':
    unittest.main()
