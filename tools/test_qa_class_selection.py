import tempfile,unittest
from pathlib import Path
from qa_class_selection import stage_selected

class QaClassSelectionTests(unittest.TestCase):
    def test_only_unselected_fixture_families_are_omitted(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'compiler';source.mkdir();destination=Path(folder)/'qa'
            names=['Story.class','SelectedGameTests.class','SelectedGameTests$1.class','OtherGameTests.class','OtherGameTests$Inner.class','PlaybackFixtureClock.class']
            for name in names:(source/name).write_bytes(name.encode())
            self.assertEqual(stage_selected(source,destination,{'SelectedGameTests.java'}),4)
            self.assertEqual({p.name for p in destination.iterdir()},set(names)-{'OtherGameTests.class','OtherGameTests$Inner.class'})
            for p in destination.iterdir():self.assertEqual(p.read_bytes(),(source/p.name).read_bytes())
            self.assertEqual(len(list(source.iterdir())),6)
    def test_missing_selection_fails_before_staging(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'compiler';source.mkdir();destination=Path(folder)/'qa'
            with self.assertRaisesRegex(RuntimeError,'not all compiled'):stage_selected(source,destination,{'MissingGameTests.java'})
            self.assertFalse(destination.exists())
    def test_existing_destination_and_invalid_names_are_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'compiler';source.mkdir();destination=Path(folder)/'qa';destination.mkdir()
            with self.assertRaisesRegex(RuntimeError,'fresh destination'):stage_selected(source,destination,{'SelectedGameTests.java'})
            with self.assertRaisesRegex(RuntimeError,'Invalid'):stage_selected(source,destination,{'Story.java'})
    def test_shared_test_helpers_are_not_silently_stripped(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'compiler';source.mkdir();destination=Path(folder)/'qa'
            (source/'SelectedGameTests.class').write_bytes(b'com/coraline/feast/HelperGameTests\x00')
            (source/'HelperGameTests.class').write_bytes(b'helper fixture bytes')
            with self.assertRaisesRegex(RuntimeError,'HelperGameTests'):stage_selected(source,destination,{'SelectedGameTests.java'})
            self.assertFalse(destination.exists())

if __name__=='__main__':unittest.main()
