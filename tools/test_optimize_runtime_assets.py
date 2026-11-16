import io
import json
from pathlib import Path
import subprocess
import struct
import tempfile
import unittest
from unittest import mock
from PIL import Image, ImageChops, ImageStat
import optimize_runtime_assets as optimizer
from optimize_runtime_assets import optimize_png, deduplicate_sounds, optimize, rebuild_durations, prune_archived_voice_exports, minify_json, shortest_mesh_floats


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
            self.assertEqual(a.convert('RGBA').tobytes(), b.convert('RGBA').tobytes())
        self.assertLess(len(result), len(source))

    def test_large_image_preserves_aspect_and_alpha(self):
        result = optimize_png(self.png((1254, 627)), 256)
        with Image.open(io.BytesIO(result)) as image:
            image = image.convert('RGBA')
            self.assertEqual(image.size, (256, 128))
            self.assertEqual(image.mode, 'RGBA')
            self.assertEqual(image.getextrema()[3], (0, 255))

    def test_animation_strip_unchanged(self):
        source = self.png((16, 1024))
        self.assertEqual(optimize_png(source, 256, animated=True), source)

    def test_insect_parlor_wallpaper_keeps_authored_resolution(self):
        path='assets/coraline/textures/block/insect_parlor_wallpaper.png'
        self.assertEqual(optimizer.runtime_texture_limit(path),512)
        with Image.open(io.BytesIO(optimize_png(self.png((512,512)),optimizer.runtime_texture_limit(path)))) as image:
            self.assertEqual(image.size,(512,512))

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
                subprocess.run(['ffmpeg', '-v', 'error', '-threads', '1', '-filter_threads', '1', '-f', 'lavfi', '-i', 'sine=frequency=440:duration=0.1',
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
            subprocess.run(['ffmpeg', '-v', 'error', '-threads', '1', '-filter_threads', '1', '-f', 'lavfi', '-i',
                'sine=frequency=440:duration=2:sample_rate=192000', '-c:a', 'libvorbis', '-q:a', '5', str(audio)], check=True)
            (assets/'sounds.json').write_text(json.dumps({'music': {'sounds': [{'name': 'coraline:music', 'stream': True}]}}))
            changed = []; deduplicate_sounds(assets, cache, changed)
            self.assertEqual(len(changed), 1)
            probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
                'stream=sample_rate,channels,duration', '-of', 'json', str(audio)], text=True))['streams'][0]
            self.assertEqual(probe['sample_rate'], '48000')
            self.assertEqual(probe['channels'], 1)
            self.assertAlmostEqual(float(probe['duration']), 2, places=2)

    def test_oversized_resources_reencode_only_copied_audio(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);resources=root/'build/resources/main';assets=resources/'assets/coraline'
            audio=assets/'sounds/story/line.ogg';audio.parent.mkdir(parents=True)
            subprocess.run(['ffmpeg','-v','error','-threads','1','-filter_threads','1','-f','lavfi','-i',
                'sine=frequency=440:duration=4:sample_rate=24000','-c:a','libvorbis','-q:a','7',str(audio)],check=True)
            original=audio.read_bytes()
            source=root/'src/main/resources/assets/coraline/sounds/story/line.ogg';source.parent.mkdir(parents=True);source.write_bytes(original)
            mapping={'story_line':{'subtitle':'subtitles.story','sounds':[{'name':'coraline:story/line','stream':True,'volume':.5}]}}
            (assets/'sounds.json').write_text(json.dumps(mapping));(assets/'visit_audio_lengths.json').write_text('{}')
            (assets/'story_subtitles.json').write_text(json.dumps({'story_line':{'cues':[{'text':'Hello'}]}}))
            class Oversized:
                def __len__(self):return 75_000_001
            with mock.patch.object(optimizer,'__file__',str(root/'tools/optimize_runtime_assets.py')), mock.patch.object(optimizer.zlib,'compress',return_value=Oversized()), mock.patch.object(optimizer,'verified_speech_masters',return_value={}):
                optimize(resources,root/'cache',root/'report.json')
            self.assertEqual(source.read_bytes(),original)
            self.assertNotEqual(audio.read_bytes(),original)
            self.assertEqual(json.loads((assets/'sounds.json').read_text()),mapping)
            self.assertGreater(json.loads((assets/'visit_audio_lengths.json').read_text())['story_line'],0)
            self.assertEqual(len(json.loads((root/'report.json').read_text())['budget_audio_exports']),1)

    def test_timing_uses_vorbis_granules_instead_of_rounded_probe(self):
        from import_numbered_original_audio import ogg_measure, ogg_page_crc
        with tempfile.TemporaryDirectory() as directory:
            base=Path(directory);assets=base/'coraline';cache=base/'cache';audio=assets/'sounds/line.ogg'
            audio.parent.mkdir(parents=True);cache.mkdir()
            subprocess.run(['ffmpeg','-v','error','-threads','1','-f','lavfi','-i',
                'sine=frequency=440:duration=1:sample_rate=48000','-c:a','libvorbis',str(audio)],check=True)
            data=bytearray(audio.read_bytes());offset=0;last=0
            while offset<len(data):
                last=offset;segments=data[offset+26];offset+=27+segments+sum(data[offset+27:offset+27+segments])
            self.assertEqual(offset,len(data))
            struct.pack_into('<Q',data,last+6,struct.unpack_from('<Q',data,last+6)[0]+4800)
            struct.pack_into('<I',data,last+22,ogg_page_crc(data[last:]))
            audio.write_bytes(data);expected=ogg_measure(data)['ticks']
            (assets/'sounds.json').write_text(json.dumps({'line':{'sounds':['coraline:line']}}))
            (assets/'visit_audio_lengths.json').write_text('{}')
            (cache/'audio-durations.json').write_text(json.dumps({__import__('hashlib').sha256(data).hexdigest():1}))
            rebuild_durations(assets,cache)
            self.assertEqual(json.loads((assets/'visit_audio_lengths.json').read_text())['line'],expected)
            self.assertGreater(expected,20)

    def test_only_unreferenced_archived_voice_exports_are_omitted(self):
        with tempfile.TemporaryDirectory() as directory:
            base=Path(directory);assets=base/'coraline'
            keep=assets/'sounds/compact_events/mother/retained.ogg'
            unused=assets/'sounds/compact_events/mother/alternate_words.ogg'
            unknown=assets/'sounds/other_runtime/direct_access.ogg'
            master=base/'voicelines/alternate_words.wav'
            for path in (keep,unused,unknown,master):
                path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'unchanged')
            sounds={'retained':{'subtitle':'example','sounds':[{'name':'coraline:compact_events/mother/retained','stream':True,'volume':.5}]}}
            mapping=assets/'sounds.json';mapping.write_text(json.dumps(sounds))
            before=mapping.read_bytes()
            removed=prune_archived_voice_exports(assets)
            self.assertEqual(removed,[{'file':'compact_events/mother/alternate_words','bytes':9}])
            self.assertFalse(unused.exists())
            for path in (keep,unknown,master):self.assertEqual(path.read_bytes(),b'unchanged')
            self.assertEqual(mapping.read_bytes(),before)
            self.assertEqual(prune_archived_voice_exports(assets),[])

    def test_numeric_minification_preserves_json_strings_and_double_literals(self):
        data = b'{ "text": "12.34000 and \\\"  spaces", "weight": 0.12345678901234567, "id": 42 }'
        self.assertEqual(json.loads(minify_json(data)), json.loads(data))
        self.assertIn(b'0.12345678901234567', minify_json(data))

    def test_mesh_text_preserves_rendered_float32_and_geometry(self):
        original = {'meshes': [{'vertices': [[1.234567890123, -0.00000001999999, 12345.678901]],
                    'normals': [[0.0000000000003, 0.999999999, -0.0]],
                    'uv': [[0.333333333333333, 0.999999999999]], 'faces': [[0, 0, 0]],
                    'name': 'do not change 1.234567890123 in strings'}]}
        result = json.loads(shortest_mesh_floats(json.dumps(original).encode('utf-8')))
        a, b = original['meshes'][0], result['meshes'][0]
        self.assertEqual(a['faces'], b['faces'])
        self.assertEqual(a['name'], b['name'])
        for field in ('vertices', 'normals', 'uv'):
            for before, after in zip(a[field], b[field]):
                self.assertEqual([struct.pack('>f', v) for v in before], [struct.pack('>f', v) for v in after])

    def test_palette_budget_preserves_alpha_and_colour_error_bound(self):
        original = Image.new('RGBA', (256, 256))
        original.putdata([(x, y, (x+y)//2, (x*3+y)%256) for y in range(256) for x in range(256)])
        data = io.BytesIO()
        original.save(data, format='PNG')
        reduced = Image.open(io.BytesIO(optimize_png(data.getvalue(), 256, palette_budget=True))).convert('RGBA')
        self.assertEqual(original.size, reduced.size)
        self.assertEqual(original.getchannel('A').tobytes(), reduced.getchannel('A').tobytes())
        error = ImageStat.Stat(ImageChops.difference(original.convert('RGB'), reduced.convert('RGB'))).rms
        self.assertLessEqual(max(error), 4)


if __name__ == '__main__':
    unittest.main()
