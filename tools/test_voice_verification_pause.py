import json,tempfile,unittest,uuid
from pathlib import Path
from voice_verification_pause import wait_if_requested

class VoiceVerificationPauseTests(unittest.TestCase):
    def fixture(self,root):
        controller=root/'build-character-cast/verified-staged-audio/run/report.json';controller.parent.mkdir(parents=True);controller.write_text(json.dumps({'status':'running','required_start_free_gib':1.75}))
        request=root/'build-character-cast/voice-verification-pause.json';token=str(uuid.uuid4());request.write_text(json.dumps({'schema':1,'parent_pid':123,'token':token,'controller_report':str(controller),'maximum_wait_seconds':300}));return request,token
    def test_only_addressed_parent_waits_and_release_preserves_receipt(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);request,token=self.fixture(root);self.assertIsNone(wait_if_requested(root,parent_pid=456))
            ticks=[0]
            def sleep(seconds):ticks[0]+=seconds;request.unlink()
            proof=wait_if_requested(root,parent_pid=123,clock=lambda:ticks[0],sleep=sleep);n=json.loads(proof.read_text());self.assertEqual(n['status'],'released');self.assertFalse(n['model_started_before_release']);self.assertEqual(n['waited_seconds'],1);self.assertEqual(n['minimum_resume_free_gib'],1.75)
    def test_expired_pause_fails_before_any_model_is_started(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);request,token=self.fixture(root);ticks=[0]
            with self.assertRaisesRegex(TimeoutError,'no voice model started'):wait_if_requested(root,parent_pid=123,clock=lambda:ticks[0],sleep=lambda seconds:ticks.__setitem__(0,ticks[0]+seconds))
            self.assertTrue(request.exists());n=json.loads((root/'build-character-cast/voice-verification-pauses'/(token+'.json')).read_text());self.assertEqual(n['status'],'failed');self.assertFalse(n['model_started_before_release'])
    def test_request_cannot_extend_its_own_wait_or_escape_reports(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);request,token=self.fixture(root);ticks=[0]
            def change(seconds):ticks[0]+=seconds;n=json.loads(request.read_text());n['maximum_wait_seconds']=900;request.write_text(json.dumps(n))
            with self.assertRaisesRegex(ValueError,'changed while waiting'):wait_if_requested(root,parent_pid=123,clock=lambda:ticks[0],sleep=change)

if __name__=='__main__':unittest.main()
