import types
import unittest
from finish_parent_audio import steps,run_steps


class FinishParentAudioTests(unittest.TestCase):
    def test_all_required_steps_are_serial_and_full_coverage(self):
        planned=steps(True)
        self.assertEqual(len(planned),5)
        self.assertEqual(planned[0][0],'full_mother_quality')
        for name,command in planned:
            self.assertIn('--low-memory-attempt',command)
            self.assertNotIn('--existing-only',command)
            self.assertNotIn('--wait-for-files',command)
            if name.endswith('quality'):
                for flag in ('--validate','--asr-turbo','--resume-checks'):self.assertIn(flag,command)
            else:
                self.assertIn('--memory-efficient',command)
                self.assertNotIn('--validate',command)
        self.assertNotIn('--low-memory-attempt',steps(False)[0][1])

    def test_any_failed_model_prevents_every_later_step(self):
        calls=[];result={'steps':[]}
        def run(command,**kwargs):
            calls.append(command)
            return types.SimpleNamespace(returncode=1)
        with self.assertRaisesRegex(RuntimeError,'later models and packaging remain prohibited'):
            run_steps(steps(),result,lambda:None,{},run=run)
        self.assertEqual(len(calls),1)
        self.assertEqual(len(result['steps']),1)


if __name__=='__main__':unittest.main()
