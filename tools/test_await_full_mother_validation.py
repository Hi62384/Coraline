import json
from pathlib import Path
import tempfile
import unittest
from await_full_mother_validation import require_completed_generation,wait_for_generation,MANIFEST


class SerialHandoffTests(unittest.TestCase):
    def complete(self):
        return {'manifest':MANIFEST,'stage':'synthesis','status':'passed','exit_code':0,
                'memory_measurement_available':True,
                'command':['pinned-python.exe','-u','tools/generate_other_mother_welcome.py','--memory-efficient']}

    def test_only_expected_full_guarded_synthesis_is_eligible(self):
        require_completed_generation(self.complete())
        for field,value in [('manifest','tools/compact_mother_voice_manifest.json'),('stage','validation'),
                            ('command',['python','other.py'])]:
            report=self.complete();report[field]=value
            with self.subTest(field=field),self.assertRaises(ValueError):require_completed_generation(report)

    def test_failed_or_unmeasured_jobs_never_start_validation(self):
        for field,value in [('status','low_memory_cutoff'),('exit_code',1),('memory_measurement_available',False)]:
            report=self.complete();report[field]=value
            with self.subTest(field=field),self.assertRaisesRegex(ValueError,'did not finish'):require_completed_generation(report)

    def test_wait_is_finite_and_partial_report_is_never_success(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'report.json';now=[0]
            def sleep(seconds):now[0]+=seconds
            with self.assertRaises(TimeoutError):wait_for_generation(path,2,clock=lambda:now[0],sleep=sleep)
            path.write_text('{',encoding='utf-8');now[0]=0
            with self.assertRaises(TimeoutError):wait_for_generation(path,2,clock=lambda:now[0],sleep=sleep)
            path.write_text(json.dumps(self.complete()),encoding='utf-8')
            self.assertEqual(wait_for_generation(path,2,clock=lambda:now[0],sleep=sleep),self.complete())


if __name__=='__main__':unittest.main()
